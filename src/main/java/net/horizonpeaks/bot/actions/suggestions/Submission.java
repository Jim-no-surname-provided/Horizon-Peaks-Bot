package net.horizonpeaks.bot.actions.suggestions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.horizonpeaks.bot.actions.suggestions.Suggestion.Status;

/**
 * Handles submitted community suggestion forms.
 */
public final class Submission {

    private Submission() {
    }

    /**
     * Processes a submitted suggestion form and posts it to the active
     * suggestions channel.
     *
     * @param event the submitted modal interaction
     */
    public static void submit(ModalInteractionEvent event) {
        event.deferReply(true).queue();

        CompletableFuture<Integer> suggestionId = getNextSuggestionId(event);

        Suggestion suggestion;
        try {
            suggestion = Suggestion.fromModal(event, suggestionId.get());
        } catch (InterruptedException | ExecutionException | IllegalArgumentException e) {
            event.getHook().editOriginal("The suggestion could not be submitted.").queue();
            return;
        }

        Status.OPEN.channel(event.getJDA())
                .sendMessageEmbeds(suggestion.toMessageEmbed())
                .addComponents(ActionRow.of(
                        Button.primary("modal:suggestion:create", "Make a suggestion")))
                .queue(message -> {
                    suggestion.addVoting(message);
                    suggestion.createDiscussionThread(message);

                    event.getHook()
                            .editOriginal("Suggestion submitted successfully. You can see it here: "
                                    + message.getJumpUrl())
                            .queue();

                }, error -> event.getHook()
                        .editOriginal("The suggestion could not be submitted.")
                        .queue());
    }

    /**
     * Finds the next suggestion ID by inspecting the latest active, approved,
     * and rejected suggestion messages.
     *
     * @param event the submitted modal interaction
     * @return the next available numeric suggestion ID
     */
    private static CompletableFuture<Integer> getNextSuggestionId(ModalInteractionEvent event) {
        TextChannel active = Status.OPEN.channel(event.getJDA());
        TextChannel approved = Status.ACCEPTED.channel(event.getJDA());
        TextChannel denied = Status.REJECTED.channel(event.getJDA());

        CompletableFuture<Integer> activeId = getLastSuggestionId(active);
        CompletableFuture<Integer> approvedId = getLastSuggestionId(approved);
        CompletableFuture<Integer> deniedId = getLastSuggestionId(denied);

        // Get the highest existing ID and increment it
        return CompletableFuture.allOf(activeId, approvedId, deniedId)
                .thenApply(ignored -> Math.max(
                        activeId.join(),
                        Math.max(approvedId.join(), deniedId.join())) + 1);
    }

    /**
     * Returns the ID of the most recent suggestion in a channel.
     *
     * @param channel the suggestion channel
     * @return a future containing the latest suggestion ID, or {@code -1} if
     *         no suggestion exists
     */
    private static CompletableFuture<Integer> getLastSuggestionId(TextChannel channel) {
        return CompletableFuture.supplyAsync(() -> {
            for (Message message : channel.getIterableHistory()) {
                int id = Suggestion.getId(message);

                if (id != -1) {
                    return id;
                }
            }

            return -1;
        });
    }
}