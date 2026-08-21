package net.horizonpeaks.bot;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import net.horizonpeaks.bot.data.CommandDefinition;
import net.horizonpeaks.bot.data.Config;
import net.horizonpeaks.bot.data.Embed;
import net.horizonpeaks.bot.data.EmbedField;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

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

    private final Config config;

    /**
     * Creates a renderer using the bot configuration.
     *
     * @param config general bot configuration
     */
    public MsgSender(Config config) {
        this.config = config;
    }

    /**
     * Renders and sends the configured response for a slash command.
     *
     * @param command the command definition to render
     * @param event   the slash command interaction that triggered the response
     */
    public void render(CommandDefinition command, SlashCommandInteractionEvent event) {
        // Make message reply
        ReplyCallbackAction reply = event.deferReply();

        // Add text
        String text = resolve(command.text(), event);
        if (text != null && !text.isBlank()) {
            reply = reply.setContent(text);
        }

        // Add embeds
        if (command.embeds() != null) {
            List<MessageEmbed> embeds = new ArrayList<>();

            for (Embed embed : command.embeds()) {
                embeds.add(renderEmbed(embed, event));
            }

            reply = reply.addEmbeds(embeds);
        }

        // Send
        reply.queue();
    }

    /**
     * Converts a configured embed into a JDA embed.
     *
     * @param embed the configured embed
     * @param event the current slash command interaction
     * @return the rendered JDA embed
     */
    private MessageEmbed renderEmbed(Embed embed, SlashCommandInteractionEvent event) {
        // Replace placeholders with their values
        Embed resolved = embed.resolved(value -> resolve(value, event));

        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(resolved.title(), resolved.url())
                .setDescription(resolved.description())
                .setColor(parseColor(resolved.color()))
                .setThumbnail(resolved.thumbnail())
                .setImage(resolved.image())
                .setFooter(resolved.footer());

        if (resolved.fields() != null) {
            for (EmbedField field : resolved.fields()) {
                builder.addField(field.name(), field.value(), field.inline());
            }
        }

        return builder.build();
    }

    /**
     * Resolves placeholders in configured text.
     *
     * @param value text containing optional placeholders
     * @param event the current slash command interaction
     * @return the resolved text, or {@code null} if the input was {@code null}
     */
    private String resolve(String value, SlashCommandInteractionEvent event) {
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
        resolved = resolved
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
                .replace("%suggestions.firstExtensionDays%", String.valueOf(config.suggestions().firstExtensionDays()))
                .replace("%suggestions.finalExtensionDays%", String.valueOf(config.suggestions().finalExtensionDays()))
                .replace("%suggestions.maxActivePerUser%", String.valueOf(config.suggestions().maxActivePerUser()));

        return resolved;
    }

    /**
     * Converts a hexadecimal color string into a Java color.
     *
     * @param value color in {@code #RRGGBB} format
     * @return the parsed color
     * @throws IllegalArgumentException if the color is invalid
     */
    private Color parseColor(String value) {
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