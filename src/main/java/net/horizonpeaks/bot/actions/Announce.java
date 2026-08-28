package net.horizonpeaks.bot.actions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import java.awt.Color;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.horizonpeaks.bot.Config;
import net.horizonpeaks.bot.data.CommandDefinition;
import net.horizonpeaks.bot.data.FileLoader;

/**
 * Responds with the contents of a markdown file.
 */
public final class Announce implements Action {

    /**
     * Sends a markdown file
     *
     * @param command the command calling this action
     * @param event   the slash command interaction
     */
    @Override
    public void act(CommandDefinition command, SlashCommandInteractionEvent event) {
        // Get argument
        OptionMapping option = event.getOption("name");
        if (option == null) {
            event.reply("No announcement name was provided.")
                    .setEphemeral(true)
                    .queue();
            return;
        }

        try {
            // Load announcement
            String fileName = option.getAsString();

            if (!fileName.endsWith(".md")) {
                fileName += ".md";
            }
            
            Path path = FileLoader.getOrCreate("announcements/" + fileName);

            String announcement = Files.readString(path);

            // Send announcement
            EmbedBuilder embed = new EmbedBuilder()
                    .setDescription(announcement)
                    .setColor(Color.decode(Config.get().colors().brand()));

            event.replyEmbeds(embed.build()).queue();

        } catch (IOException e) {
            event.reply("The announcement path doesn't exist or couldn't be loaded.")
                    .setEphemeral(true)
                    .queue();
        }
    }
}