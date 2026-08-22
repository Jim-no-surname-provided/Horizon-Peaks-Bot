package net.horizonpeaks.bot.actions;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.horizonpeaks.bot.data.Config;

/**
 * Handles submitted community suggestion forms.
 */
public final class SuggestionSubmission {

    /**
     * Processes a submitted suggestion form and posts it to the active
     * suggestions channel.
     *
     * @param event the submitted modal interaction
     */
    public static void submit(ModalInteractionEvent event) {
        // "Wait for the response" and make it ephimeral
        event.deferReply(true).queue();

        // Read submitted form values
        String title = getRequiredValue(event, "title");
        String description = getRequiredValue(event, "description");
        String image = getOptionalValue(event, "image");
        String examples = getOptionalValue(event, "examples");

        // Build suggestion
        CompletableFuture<Integer> suggestionId = getNextSuggestionId(event);
        MessageEmbed suggestion;
        TextChannel channel;
        try {
            suggestion = buildSuggestion(event, suggestionId.get(), title, description, image, examples);

            channel = getSuggestionChannel(event);
        } catch (InterruptedException | ExecutionException e) {
            event.getHook().editOriginal("The suggestion could not be submitted: " + e.getMessage()).queue();
            return;
        }

        // Send suggestion. When it's up, add voting emojis and a thread
        channel.sendMessageEmbeds(suggestion).queue(message -> {
            addVoting(message);
            createDiscussionThread(message, title);

            // Confirmation message with a link to the suggestion
            event.getHook()
                    .editOriginal("Suggestion submitted successfully. You can see it here: "
                            + message.getJumpUrl())
                    .queue();

            // Error message
        }, error -> event.getHook()
                .editOriginal("The suggestion could not be submitted.")
                .queue()

        );
    }

    /**
     * Returns a required value from the submitted modal.
     *
     * @param event the submitted modal interaction
     * @param id    the modal input ID
     * @return the submitted value
     * @throws IllegalArgumentException if the value is missing
     */
    private static String getRequiredValue(ModalInteractionEvent event, String id) {
        ModalMapping value = event.getValue(id);

        if (value == null) {
            throw new IllegalArgumentException("Missing modal value: " + id);
        }

        return value.getAsString();
    }

    /**
     * Returns an optional value from the submitted modal.
     *
     * @param event the submitted modal interaction
     * @param id    the modal input ID
     * @return the submitted value, or {@code null} if none was provided
     */
    private static String getOptionalValue(ModalInteractionEvent event, String id) {
        ModalMapping value = event.getValue(id);

        if (value == null || value.getAsString().isBlank()) {
            return null;
        }

        return value.getAsString();
    }

    /**
     * Finds the next suggestion ID by inspecting the latest active, approved,
     * and denied suggestion messages.
     *
     * @param event the submitted modal interaction
     * @return the next available numeric suggestion ID
     */
    private static CompletableFuture<Integer> getNextSuggestionId(ModalInteractionEvent event) {
        Config.Channels channels = Config.get().channels();

        TextChannel active = event.getJDA().getTextChannelById(channels.activeSuggestions());
        TextChannel approved = event.getJDA().getTextChannelById(channels.approvedSuggestions());
        TextChannel denied = event.getJDA().getTextChannelById(channels.deniedSuggestions());

        CompletableFuture<Integer> activeId = getLastSuggestionId(active);
        CompletableFuture<Integer> approvedId = getLastSuggestionId(approved);
        CompletableFuture<Integer> deniedId = getLastSuggestionId(denied);

        // Max of the last suggestions + 1
        return CompletableFuture.allOf(activeId, approvedId, deniedId)
                .thenApply(ignored -> Math.max(
                        activeId.join(),
                        Math.max(approvedId.join(), deniedId.join())) + 1);
    }

    private static final Pattern SUGGESTION_ID_PATTERN = Pattern.compile("S-(\\d+)");

    /**
     * Returns the ID of the most recent suggestion in a channel.
     *
     * @param channel the suggestion channel
     * @return a future containing the suggestion ID, or {@code -1} if the channel
     *         is
     *         empty
     */
    private static CompletableFuture<Integer> getLastSuggestionId(TextChannel channel) {
        if (channel == null) {
            throw new IllegalStateException("Suggestion channel does not exist");
        }

        // Fetch only the newest message
        return channel.getIterableHistory()
                .takeAsync(1)
                .thenApply(messages -> {
                    // Guard empty channel
                    if (messages.isEmpty()) {
                        return -1;
                    }

                    Message message = messages.getFirst();

                    // Guard empty message
                    if (message.getEmbeds().isEmpty()) {
                        return -1;
                    }

                    MessageEmbed.Footer footer = message.getEmbeds().getFirst().getFooter();

                    // Guard empty footer
                    if (footer == null || footer.getText() == null) {
                        return -1;
                    }

                    Matcher matcher = SUGGESTION_ID_PATTERN.matcher(footer.getText());

                    // Guard no found id
                    if (!matcher.find()) {
                        return -1;
                    }

                    // return first found id
                    return Integer.valueOf(matcher.group(1));
                });
    }

    /**
     * Builds the active suggestion embed.
     *
     * @param event        the submitted modal interaction
     * @param suggestionId the numeric suggestion ID
     * @param title        the submitted suggestion title
     * @param description  the submitted suggestion description
     * @param image        the optional image URL
     * @param examples     the optional examples
     * @return the suggestion embed
     */
    private static MessageEmbed buildSuggestion(
            ModalInteractionEvent event,
            int suggestionId,
            String title,
            String description,
            String image,
            String examples) {

        Member member = event.getMember();

        // Guard member
        if (member == null) {
            throw new IllegalStateException("Suggestion was submitted outside a guild");
        }

        Config config = Config.get();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusDays(config.suggestions().initialDays());

        // Build actual embed with the information
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(Color.decode(config.colors().brand()))
                .setThumbnail(event.getUser().getEffectiveAvatarUrl())
                .addField(
                        "Author",
                        "[%s](https://discord.com/users/%s)"
                                .formatted(member.getEffectiveName(), member.getId()),
                        true)
                .addField("Status", "**Active**", true)
                .setFooter(
                        "S-%04d | Expires %s | %s".formatted(
                                suggestionId,
                                expiresAt.format(DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a")),
                                config.branding().name()))
                .setTimestamp(OffsetDateTime.now());

        // Add image
        if (image != null) {
            builder.setImage(image);
        }

        // Add examples
        if (examples != null) {
            builder.addField("Examples", examples, false);
        }

        return builder.build();
    }

    /**
     * Returns the configured active suggestions channel.
     *
     * @param event the submitted modal interaction
     * @return the active suggestions channel
     */
    private static TextChannel getSuggestionChannel(ModalInteractionEvent event) {
        TextChannel channel = event.getJDA().getTextChannelById(
                Config.get().channels().activeSuggestions());

        if (channel == null) {
            throw new IllegalStateException("Active suggestions channel does not exist");
        }

        return channel;
    }

    /**
     * Adds the voting reactions to a suggestion message.
     *
     * @param message the posted suggestion message
     */
    private static void addVoting(Message message) {
        message.addReaction(Emoji.fromUnicode("✅")).queue();
        message.addReaction(Emoji.fromUnicode("⛔")).queue();
    }

    /**
     * Creates the discussion thread for a suggestion.
     *
     * @param msg the posted suggestion message
     * @param title   the suggestion title
     */
    private static void createDiscussionThread(Message msg, String title) {
        String name = "Discussion: " + title;

        // trim message if it's too long
        if (name.length() > 100) {
            name = name.substring(0, 100);
        }

        msg.createThreadChannel(name).queue();
    }
}