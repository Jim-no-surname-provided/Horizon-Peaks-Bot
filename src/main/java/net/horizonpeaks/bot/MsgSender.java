package net.horizonpeaks.bot;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import net.horizonpeaks.bot.data.CommandDefinition;
import net.horizonpeaks.bot.data.Embed;
import net.horizonpeaks.bot.data.Embed.EmbedField;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.FileUpload;

/**
 * Renders configured command responses into Discord messages.
 *
 * <p>
 * The renderer resolves placeholders using the current interaction and bot
 * configuration, converts configured embeds into JDA embeds, and sends the
 * resulting response.
 * </p>
 */
public final class MsgSender {
    private MsgSender() {
    }

    /**
     * Renders and sends the configured response for a slash command.
     *
     * @param command  the command definition to render
     * @param event    the slash command interaction that triggered the response
     * @param modifier if you want to modify the reply before it gets sent, you can
     *                 do so by passing a modifyer
     */
    public static void render(CommandDefinition command, SlashCommandInteractionEvent event) {
        render(command, event, reply -> reply);
    }

    /**
     * Renders and sends the configured response for a slash command.
     *
     * @param command  the command definition to render
     * @param event    the slash command interaction that triggered the response
     * @param modifier if you want to modify the reply before it gets sent, you can
     *                 do so by passing a modifyer
     */
    public static void render(CommandDefinition command, SlashCommandInteractionEvent event,
            UnaryOperator<ReplyCallbackAction> modifier) {
        String text = resolve(command.text(), event);
        List<FileUpload> files = new ArrayList<>();
        List<MessageEmbed> embeds = new ArrayList<>();

        // Render embeds
        if (command.embeds() != null) {
            for (Embed embed : command.embeds()) {
                MessageEmbed rendered = renderEmbed(embed, value -> resolve(value, event), files);

                if (rendered.getLength() > MessageEmbed.EMBED_MAX_LENGTH_BOT) {
                    event.reply("One of the configured embeds is too long. Split its content across multiple embeds.")
                            .setEphemeral(true).queue();
                    return;
                }

                embeds.add(rendered);
            }
        }

        // Split embeds into messages under Discord's combined embed limit
        List<List<MessageEmbed>> batches = new ArrayList<>();
        List<MessageEmbed> current = new ArrayList<>();
        int currentLength = 0;

        for (MessageEmbed embed : embeds) {
            if (currentLength + embed.getLength() > MessageEmbed.EMBED_MAX_LENGTH_BOT
                    || current.size() >= 10) {
                batches.add(current);
                current = new ArrayList<>();
                currentLength = 0;
            }

            current.add(embed);
            currentLength += embed.getLength();
        }

        if (!current.isEmpty()) {
            batches.add(current);
        }

        // Send the first message through the interaction reply
        ReplyCallbackAction reply = event.deferReply();

        if (text != null && !text.isBlank()) {
            reply = reply.setContent(text);
        }

        if (!batches.isEmpty()) {
            reply = reply.addEmbeds(batches.getFirst());
        }

        if (!files.isEmpty()) {
            reply = reply.addFiles(files);
        }

        // Allow actions to add buttons or otherwise modify the reply
        reply = modifier.apply(reply);

        reply.queue(hook -> {
            // Send remaining embed batches as follow-ups
            for (int i = 1; i < batches.size(); i++) {
                hook.sendMessageEmbeds(batches.get(i)).queue();
            }
        });
    }

    /**
     * Converts a configured embed into a JDA embed.
     *
     * @param embed the configured embed
     * @param event the current slash command interaction
     * @return the rendered JDA embed
     */
    public static MessageEmbed renderEmbed(Embed embed, Function<String, String> resolver, List<FileUpload> files) {
        // Replace placeholders with their values
        Embed resolved = embed.resolved(resolver);

        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(resolved.title(), resolved.url())
                .setDescription(resolved.description())
                .setColor(parseColor(resolved.color()))
                .setThumbnail(resolved.thumbnail())
                .setFooter(resolved.footer());

        addImage(builder, resolved.image(), files);

        if (resolved.fields() != null) {
            for (EmbedField field : resolved.fields()) {
                builder.addField(field.name(), field.value(), field.inline());
            }
        }

        return builder.build();
    }

    private static void addImage(EmbedBuilder builder, String image, List<FileUpload> files) {
        if (image == null) {
            return;
        }

        // Remote images can be used directly
        if (image.startsWith("http://") || image.startsWith("https://")) {
            builder.setImage(image);
            return;
        }

        // Local images are uploaded and referenced as attachments
        try (InputStream input = MsgSender.class.getClassLoader().getResourceAsStream(image)) {
            if (input == null) {
                throw new IllegalStateException("Embed image does not exist: " + image);
            }

            String fileName = image.substring(image.lastIndexOf('/') + 1);

            files.add(FileUpload.fromData(input.readAllBytes(), fileName));
            builder.setImage("attachment://" + fileName);

        } catch (IOException e) {
            throw new IllegalStateException("Could not read embed image: " + image, e);
        }
    }

    public static String resolveConfig(String value) {
        Config config = Config.get();
        if (value == null) {
            return null;
        }

        return value
                .replace("%branding.name%", config.branding().name())
                .replace("%branding.banner%", config.branding().banner())

                .replace("%servers.network%", config.servers().network())
                .replace("%servers.smp%", config.servers().smp())
                .replace("%servers.creative%", config.servers().creative())

                .replace("%links.website%", config.links().website())
                .replace("%links.map%", config.links().map())
                .replace("%links.vote%", config.links().vote())

                .replace("%colors.brand%", config.colors().brand())
                .replace("%colors.success%", config.colors().success())
                .replace("%colors.error%", config.colors().error())
                .replace("%colors.vip%", config.colors().vip())
                .replace("%colors.booster%", config.colors().booster())

                .replace("%images.vip%", config.images().vip())
                .replace("%images.booster%", config.images().booster())

                .replace("%channels.welcome%", config.channels().welcome())
                .replace("%channels.communityInfo%", config.channels().communityInfo())
                .replace("%channels.activeSuggestions%", config.channels().activeSuggestions())
                .replace("%channels.approvedSuggestions%", config.channels().approvedSuggestions())
                .replace("%channels.deniedSuggestions%", config.channels().deniedSuggestions())

                .replace("%suggestions.initialDays%", String.valueOf(config.suggestions().initialDays()))
                .replace("%suggestions.minimumVotes%", String.valueOf(config.suggestions().minimumVotes()))
                .replace("%suggestions.extensionDays%", String.valueOf(config.suggestions().extensionDays()))
                .replace("%suggestions.maxActivePerUser%", String.valueOf(config.suggestions().maxActivePerUser()));

    }

    /**
     * Resolves placeholders in configured text.
     *
     * @param value text containing optional placeholders
     * @param event the current slash command interaction
     * @return the resolved text, or {@code null} if the input was {@code null}
     */
    private static String resolve(String value, SlashCommandInteractionEvent event) {
        if (value == null) {
            return null;
        }

        Guild guild = event.getGuild();
        Member member = event.getMember();

        if (guild == null || member == null) {
            throw new IllegalStateException("Slash command was used outside a guild");
        }

        String resolved = value;

        // Command arguments
        for (OptionMapping option : event.getOptions()) {
            resolved = resolved.replace(
                    "%" + option.getName() + "%",
                    option.getAsString());
        }

        // Event values
        resolved = resolved
                .replace("%member.name%", event.getUser().getName())
                .replace("%member.mention%", event.getUser().getAsMention())
                .replace("%member.id%", event.getUser().getId())
                .replace("%guild.name%", guild.getName())
                .replace("%guild.id%", guild.getId())
                .replace("%channel.name%", event.getChannel().getName())
                .replace("%channel.mention%", event.getChannel().getAsMention())
                .replace("%channel.id%", event.getChannel().getId())
                .replace("%command.name%", event.getName());

        if (event.getMember() != null) {
            resolved = resolved.replace(
                    "%member.displayName%",
                    member.getEffectiveName());
        }

        // Config values
        resolved = resolveConfig(resolved);

        return resolved;
    }

    /**
     * Converts a hexadecimal color string into a Java color.
     *
     * @param value color in {@code #RRGGBB} format
     * @return the parsed color
     * @throws IllegalArgumentException if the color is invalid
     */
    private static Color parseColor(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Color.decode(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid embed color: " + value, e);
        }
    }
}