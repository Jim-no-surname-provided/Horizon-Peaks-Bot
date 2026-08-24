package net.horizonpeaks.bot.actions.suggestions;

import static net.horizonpeaks.bot.actions.suggestions.Suggestion.Status.*;

import net.horizonpeaks.bot.actions.suggestions.Suggestion.Status;

/**
 * Manually approves a suggestion from its discussion thread
 */
public class Approve extends Move {

    @Override
    protected Status status() {
        return ACCEPTED;
    }

    @Override
    protected String reason() {
        return "Approved by admin";
    }

}
