package net.horizonpeaks.bot.actions.suggestions;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.jspecify.annotations.Nullable;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
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
        // Wait for the response and make it ephemeral
        event.deferReply(true).queue();

        // Read required form values
        ModalMapping titleModal = event.getValue("title");
        ModalMapping descriptionModal = event.getValue("description");

        if (titleModal == null || descriptionModal == null) {
            event.getHook().editOriginal("The suggestion form is missing required values.").queue();
            return;
        }

        String title = titleModal.getAsString();
        String description = descriptionModal.getAsString();

        // Read optional image
        @Nullable
        ModalMapping imageModal = event.getValue("image");
        @Nullable
        String image = imageModal == null || imageModal.getAsString().isBlank() ? null : imageModal.getAsString();

        // Read optional examples
        @Nullable
        ModalMapping exModal = event.getValue("examples");
        @Nullable
        String examples = exModal == null || exModal.getAsString().isBlank()
                ? null
                : exModal.getAsString();

        // The submission modal is expected to come from a guild interaction
        Member member = event.getMember();

        if (member == null) {
            event.getHook().editOriginal("Suggestions can only be submitted from the server.").queue();
            return;
        }

        // Build suggestion
        CompletableFuture<Integer> suggestionId = getNextSuggestionId(event);

        Suggestion suggestion;
        try {
            suggestion = new Suggestion(event.getJDA(), suggestionId.get(), title, description, image, examples,
                    member, null, Status.OPEN, event.getTimeCreated());
        } catch (InterruptedException | ExecutionException e) {
            event.getHook().editOriginal("The suggestion could not be submitted.").queue();
            return;
        }

        // Send suggestion and initialize voting and discussion
        Status.OPEN.channel(event.getJDA())
                .sendMessageEmbeds(suggestion.toMessageEmbed())
                // Add button
                .addComponents(ActionRow.of(Button.primary("suggestion:create", "Make a suggestion")))
                // Send
                .queue(message -> {
                    suggestion.addVoting(message);
                    suggestion.createDiscussionThread(message);

                    // Confirmation message with a link to the suggestion
                    event
                            .getHook()
                            .editOriginal("Suggestion submitted successfully. You can see it here: "
                                    + message.getJumpUrl())
                            .queue();

                    // Error message
                }, error -> event.getHook().editOriginal("The suggestion could not be submitted.").queue());
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