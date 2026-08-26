package net.horizonpeaks.bot;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

public class PlaceholderResolver {
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
                .replace("%channels.rules%", config.channels().rules())

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
    public static String resolve(String value, SlashCommandInteractionEvent event) {
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
}
