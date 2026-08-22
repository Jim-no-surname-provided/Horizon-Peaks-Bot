package net.horizonpeaks.bot;

import java.util.List;

import net.horizonpeaks.bot.actions.SuggestionSubmission;
import net.horizonpeaks.bot.data.CommandDefinition;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.horizonpeaks.bot.data.Config;

/**
 * Handles Discord events received by the bot.
 *
 * <p>
 * Slash commands are matched against the definitions loaded from
 * {@code commands.yaml}. Commands with an action execute their Java behavior,
 * while declarative commands are rendered from their configured response.
 * </p>
 */
public final class BotListener extends ListenerAdapter {

    private final List<CommandDefinition> commands;
    private final MsgSender msgSender;

    /**
     * Creates a listener using the configured slash commands.
     *
     * @param commands command definitions loaded from {@code commands.yaml}
     */
    public BotListener(List<CommandDefinition> commands, Config config) {
        this.commands = commands;
        this.msgSender = new MsgSender(config);
    }

    /**
     * Handles an invoked slash command.
     *
     * @param event the Discord slash command interaction
     */
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        for (CommandDefinition command : commands) {
            if (!event.getName().equals(command.name())) {
                continue;
            }

            if (command.action() != null) {
                command.action().act(event);
                return;
            }

            msgSender.render(command, event);
            return;

        }
    }

    /**
     * Handles submitted Discord modals.
     *
     * <p>
     * Suggestion submissions are forwarded to {@link SuggestionSubmission}
     * when the submitted modal matches the suggestion form.
     * </p>
     *
     * @param event the submitted modal interaction
     */
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("suggestion:submit")) {
            SuggestionSubmission.submit(event);
            return;
        }
    }
}