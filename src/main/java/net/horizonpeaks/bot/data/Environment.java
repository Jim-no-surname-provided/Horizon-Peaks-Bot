package net.horizonpeaks.bot.data;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Represents environment-specific bot configuration loaded from {@code .env}.
 *
 * <p>This record contains values that should not normally be stored in the
 * regular YAML configuration, such as the Discord bot token.</p>
 *
 * @param discordToken Discord bot authentication token
 * @param discordGuildId Discord guild ID used by the bot
 */
public record Environment(
        String discordToken,
        String discordGuildId
) {

    /**
     * Loads the bot environment from the process environment and {@code .env}.
     *
     * @return the loaded environment values
     */
    public static Environment load() {
        Dotenv dotenv = Dotenv.load();

        return new Environment(
                dotenv.get("DISCORD_TOKEN"),
                dotenv.get("DISCORD_GUILD_ID")
        );
    }
}