package net.horizonpeaks.bot.actions.suggestions;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.EmojiUnion;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

/**
 * Handles voting on active community suggestions.
 */
public final class Voting {

    private Voting() {
    }

    /**
     * Enforces exclusive voting on active suggestions.
     *
     * <p>
     * When a user votes in one direction, any vote they previously placed
     * in the opposite direction is removed.
     * </p>
     *
     * @param event the reaction-add event
     */
    public static void handle(MessageReactionAddEvent event) {
        EmojiUnion addedEmoji = event.getEmoji();
        Emoji oppositeEmoji;

        // Ignore unrelated reactions
        if (addedEmoji.getName().equals("✅")) {
            oppositeEmoji = Emoji.fromUnicode("⛔");

        } else if (addedEmoji.getName().equals("⛔")) {
            oppositeEmoji = Emoji.fromUnicode("✅");

        } else {
            return;
        }

        String userId = event.getUserId();

        // Remove the user's opposite vote if present
        event.getChannel()
                .retrieveReactionUsersById(event.getMessageId(), oppositeEmoji)
                // After getting the users
                .queue(users -> {
                    // Use a stream to filter or null
                    User user = users.stream()
                            .filter(u -> u.getId().equals(userId))
                            .findFirst()
                            .orElse(null);

                    // Remove the opposite reaction only if the user has one
                    if (user != null) {
                        TextChannel channel = event.getChannel().asTextChannel();
                        channel.removeReactionById(event.getMessageId(), oppositeEmoji, user).queue();
                    }
                });
    }
}