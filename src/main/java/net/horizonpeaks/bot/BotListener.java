package net.horizonpeaks.bot;

import java.util.List;

import net.horizonpeaks.bot.actions.Welcome;
import net.horizonpeaks.bot.actions.suggestions.Modal;
import net.horizonpeaks.bot.actions.suggestions.Submission;
import net.horizonpeaks.bot.actions.suggestions.Voting;
import net.horizonpeaks.bot.data.CommandDefinition;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
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

    private final List<CommandDefinition> commands;
    private final Config config;

    /**
     * Creates a listener using the configured slash commands and bot
     * configuration.
     *
     * @param commands command definitions loaded from {@code commands.yaml}
     * @param config   general bot configuration
     */
    public BotListener(List<CommandDefinition> commands, Config config) {
        this.commands = commands;
        this.config = config;
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

            MsgSender.render(command, event);
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
        if (event.getModalId().equals("suggestion:submit")) {
            Submission.submit(event);
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
     * Handles members joining the guild.
     *
     * <p>
     * New members receive the configured member role and are greeted with the
     * configured welcome message.
     * </p>
     *
     * @param event the member-join event
     */
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        Welcome.welcome(event);
        assignMemberRole(event);
    }

    /**
     * Assigns the configured member role to a newly joined member.
     *
     * @param event the member-join event
     * @throws IllegalStateException if the configured member role does not exist
     */
    private void assignMemberRole(GuildMemberJoinEvent event) {
        Member member = event.getMember();
        Role memberRole = event.getGuild().getRoleById(Config.get().roles().member());

        if (memberRole == null) {
            throw new IllegalStateException("Configured member role does not exist");
        }

        event.getGuild().addRoleToMember(member, memberRole).queue();
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
        if (event.getComponentId().equals("suggestion:create")) {
            net.dv8tion.jda.api.modals.Modal modal = Modal.buildModal();
            event.replyModal(modal).queue();
            return;
        }
    }
}