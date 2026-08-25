package net.horizonpeaks.bot.actions.suggestions;

import org.jspecify.annotations.Nullable;

import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.horizonpeaks.bot.actions.Action;
import net.horizonpeaks.bot.actions.suggestions.Suggestion.Status;
import net.horizonpeaks.bot.data.CommandDefinition;

/**
 * Base action for manually changing the status of a suggestion from its
 * discussion thread.
 *
 * <p>
 * Subclasses define the target {@link Status} and the reason stored with the
 * status change.
 * </p>
 */
public abstract class Move implements Action {

    /**
     * Returns the status this action moves the suggestion to.
     *
     * @return the target suggestion status
     */
    protected abstract Status status();

    /**
     * Returns the reason stored with this status change.
     *
     * @return the status change reason, or {@code null} if none should be stored
     */
    protected abstract @Nullable String reason();

    /**
     * Moves the suggestion belonging to the current discussion thread to the
     * configured target status.
     *
     * <p>
     * The command can only be used inside a thread whose starter message can be
     * parsed as a suggestion.
     * </p>
     *
     * @param command the command calling this action
     * @param event the slash command interaction
     */
    @Override
    public void act(CommandDefinition command, SlashCommandInteractionEvent event) {
        // Only allow this command inside a suggestion thread
        if (!(event.getChannel() instanceof ThreadChannel thread)) {
            event.reply("This command can only be used inside a thread").setEphemeral(true).queue();
            return;
        }

        // The thread ID is also the suggestion message ID
        thread.getParentChannel()
                .asTextChannel()
                .retrieveMessageById(thread.getId())
                .queue(message -> {
                    try {
                        Suggestion suggestion = Suggestion.fromMessage(message);
                        suggestion.changeStatus(reason(), status(), message);

                    } catch (IllegalArgumentException e) {
                        event.reply("This is not a suggestion: " + e.getMessage()).setEphemeral(true).queue();
                        return;
                    }

                    event.reply("Suggestion moved").setEphemeral(true).queue();

                }, error -> event.reply("Could not find the suggestion for this thread").setEphemeral(true).queue());
    }
}