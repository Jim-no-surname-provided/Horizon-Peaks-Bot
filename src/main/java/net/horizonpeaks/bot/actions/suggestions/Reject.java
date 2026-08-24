package net.horizonpeaks.bot.actions.suggestions;

import static net.horizonpeaks.bot.actions.suggestions.Suggestion.Status.*;

import net.horizonpeaks.bot.actions.suggestions.Suggestion.Status;

/**
 * Manually rejects a suggestion from its discussion thread
 */
public class Reject extends Move {

    @Override
    protected Status status() {
        return REJECTED;
    }

    @Override
    protected String reason() {
        return "Rejected by admin";
    }
}