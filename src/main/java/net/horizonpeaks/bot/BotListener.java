package net.horizonpeaks.bot;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.horizonpeaks.bot.actions.application.ApplicationHandler;
import net.horizonpeaks.bot.actions.suggestions.Submission;
import net.horizonpeaks.bot.actions.suggestions.Voting;
import net.horizonpeaks.bot.data.CommandDefinition;
import net.horizonpeaks.bot.data.ModalDefinition;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * Handles Discord events received by the bot.
 *
 * <p>
 * Slash commands are matched against the definitions loaded from
 * {@code commands.yaml}. Commands with an action execute their Java behavior,
 * while declarative commands are rendered from their configured response.
 * </p>
 *
 * <p>
 * The listener also handles suggestion forms and voting, as well as member
 * join behavior such as welcome messages and role assignment.
 * </p>
 */
public final class BotListener extends ListenerAdapter {

    private final Config config;
    private final List<CommandDefinition> commands;
    private final List<ModalDefinition> modals;

    /**
     * Creates a listener using the configured slash commands and bot
     * configuration.
     *
     * @param commands command definitions loaded from {@code commands.yaml}
     * @param config   general bot configuration
     */
    public BotListener(Config config, List<CommandDefinition> commands, List<ModalDefinition> modals) {
        this.config = config;
        this.commands = commands;
        this.modals = modals;
    }

    /**
     * Handles an invoked slash command.
     *
     * <p>
     * Commands with an associated action execute that action directly.
     * Declarative commands are rendered through {@link MsgSender}.
     * </p>
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
                command.action().act(command, event);
                return;
            }

            if (command.modalId() != null) {
                ModalDefinition modal = getModalDef(command.modalId());

                if (modal == null) {
                    throw new IllegalStateException("Unknown modal: " + command.modalId());
                }

                event.replyModal(modal.toModal()).queue();
                return;
            }

            MsgSender.renderAndSend(command, event);
            return;

        }
    }

    /**
     * Handles submitted Discord modals.
     *
     * <p>
     * Suggestion submissions are forwarded to {@link Submission}
     * when the submitted modal matches the suggestion form.
     * </p>
     *
     * @param event the submitted modal interaction
     */
    @Override
    public void onModalInteraction(ModalInteractionEvent event) {
        if (event.getModalId().equals("modal:suggestion:create")) {
            Submission.submit(event);
            return;
        }
        if (event.getModalId().equals("modal:application:create")) {
            ApplicationHandler.confirmMessage(event);
            return;
        }
    }

    /**
     * Handles reactions added to messages.
     *
     * <p>
     * Reactions added by the bot itself are ignored. Reactions in the active
     * suggestions channel are forwarded to {@link Voting}.
     * </p>
     *
     * @param event the reaction-add event
     */
    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        // Ignore the bot's own seed reactions
        if (event.getUserId().equals(event.getJDA().getSelfUser().getId())) {
            return;
        }

        // Forward reactions from the active suggestions channel
        String activeChannelId = config.channels().activeSuggestions();
        if (event.getChannel().getId().equals(activeChannelId)) {
            Voting.handle(event);
        }
    }

    /**
     * Handles button interactions used to start suggestion submission.
     *
     * <p>
     * Clicking the suggestion creation button opens the same submission modal
     * used by the suggestion slash command.
     * </p>
     *
     * @param event the button interaction
     */
    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String id = event.getComponentId();

        if (id.startsWith("modal:")) {
            event.replyModal(getModalDef(id).toModal()).queue();
        }

        if (id.equals("application:submit")) {
            ApplicationHandler.submit(event);
        }

        if (id.equals("application:change_name")) {
            ApplicationHandler.changeMcName(event, getModalDef("modal:application:create"));
        }
        if (id.equals("application:accept")) {
            ApplicationHandler.accept(event);
        }
        if (id.equals("application:reject")) {
            ApplicationHandler.reject(event);
        }

    }

    /**
     * Finds a configured modal by its interaction ID.
     *
     * @param id the modal interaction ID
     * @return the matching modal definition, or {@code null} if none exists
     */
    private @Nullable ModalDefinition getModalDef(String id) {
        // Find and open the requested modal
        for (ModalDefinition modal : modals) {
            if (modal.id().equals(id)) {
                return modal;
            }
        }
        return null;
    }
}