package net.horizonpeaks.bot.actions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

/**
 * Responds with the contents of a markdown file.
 */
public final class Announce implements Action {

    /**
     * Sends a markdown file
     *
     * @param event the slash command interaction
     */
    @Override
    public void act(SlashCommandInteractionEvent event) {
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
            Path path = Path.of("announcements", option.getAsString() + ".md");
            String announcement = Files.readString(path);

            // Send announcement
            event.reply(announcement).queue();

        } catch (IOException e) {
            event.reply("The announcement path doesn't exist or couldn't be loaded.")
                    .setEphemeral(true)
                    .queue();
        }
    }
}