package net.horizonpeaks.bot.actions.suggestions;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import static net.horizonpeaks.bot.actions.suggestions.Suggestion.Status.*;

/**
 * Reconciles active suggestions whose voting period has expired
 */
public final class Reconciliation {

    private Reconciliation() {
    }

    /**
     * Checks all active suggestions and processes those whose voting period
     * has expired
     * 
     * @param jda the running Discord connection
     */
    public static void run(JDA jda) {
        TextChannel channel = OPEN.channel(jda);

        // Check every active suggestion
        for (Message message : channel.getIterableHistory()) {
            Suggestion suggestion;

            try {
                suggestion = Suggestion.fromMessage(message);
            } catch (IllegalArgumentException e) {
                continue;
            }

            if (!suggestion.isExpired()) {
                continue;
            }

            reconcileSuggestion(jda, suggestion, message);
        }
    }

    /**
     * Resolves an expired suggestion according to its vote count
     * 
     * <p>
     * Suggestions without enough votes are rejected. Otherwise, the community
     * vote determines whether the suggestion is accepted or rejected
     * </p>
     *
     * @param jda        the running Discord connection
     * @param suggestion the expired suggestion
     * @param message    the Discord message representing the suggestion
     */
    private static void reconcileSuggestion(JDA jda, Suggestion suggestion, Message message) {
        // Reject suggestions without enough votes
        if (!suggestion.hasMinimumVotes()) {
            suggestion.changeStatus("Insufficient votes", REJECTED, message);
            return;
        }

        // Resolve according to the community vote
        if (suggestion.isAcceptedByVotes()) {
            suggestion.changeStatus(null, ACCEPTED, message);
        } else {
            suggestion.changeStatus(null, REJECTED, message);
        }
    }
}