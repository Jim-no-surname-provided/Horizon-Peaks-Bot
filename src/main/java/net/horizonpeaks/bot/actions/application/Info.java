package net.horizonpeaks.bot.actions.application;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.horizonpeaks.bot.MsgSender;
import net.horizonpeaks.bot.actions.Action;
import net.horizonpeaks.bot.data.CommandDefinition;

public class Info implements Action {

    @Override
    public void act(CommandDefinition command, SlashCommandInteractionEvent event) {
        MsgSender.render(command, event,
                reply -> reply.addComponents(
                        ActionRow.of(Button.primary("modal:application:create", "Apply"))));
    }

}
