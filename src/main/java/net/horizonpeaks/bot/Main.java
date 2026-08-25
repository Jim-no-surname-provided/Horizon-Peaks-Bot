package net.horizonpeaks.bot;

import java.io.IOException;
import java.util.List;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.horizonpeaks.bot.data.CommandDefinition;
import net.horizonpeaks.bot.data.Environment;
import net.horizonpeaks.bot.data.FileLoader;
import net.horizonpeaks.bot.data.ModalDefinition;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws InterruptedException, IOException {

        // Load .env definitions
        FileLoader.getOrCreate(".env", ".env.example");

        Environment environment = Environment.load();
        String token = environment.discordToken();
        String guildId = environment.discordGuildId();

        // Load config
        Config config = Config.fromYaml(FileLoader.readOrCreate("definitions/config.yaml", "definitions/config.yaml"));

        // Load commands
        List<CommandDefinition> commands = CommandDefinition.fromYaml(
                FileLoader.readOrCreate("definitions/commands.yaml", "definitions/commands.yaml"));

        // Load commands
        List<ModalDefinition> modals = ModalDefinition.fromYaml(
                FileLoader.readOrCreate("definitions/modals.yaml", "definitions/modals.yaml"));

        // Load welcome.md
        FileLoader.readOrCreate("definitions/welcome.yaml", "definitions/welcome.yaml");

        // Build bot
        JDA jda;
        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MEMBERS, GatewayIntent.MESSAGE_CONTENT)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)
                    .setChunkingFilter(ChunkingFilter.ALL)
                    .addEventListeners(new BotListener(config, commands, modals))
                    .build()
                    .awaitReady();

        } catch (InvalidTokenException | IllegalArgumentException e) {
            System.err.println("""
                    The bot has not been configured correctly.

                    If this is your first time running it, open .env and add your Discord bot token.
                    Otherwise, check that DISCORD_TOKEN contains a valid bot token.
                    """);
            return;
        }

        // Check guild
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            throw new IllegalStateException("Bot is not in guild: " + guildId);
        }

        // Load commands
        // Make a slashCommand out of them
        // Make them a list
        // Register to guild
        guild.updateCommands()
                .addCommands(commands.stream().map(CommandDefinition::toSlashCommand).toList())
                .queue();

        System.out.println("Horizon Peaks bot is online as " + jda.getSelfUser().getName());
    }
}
