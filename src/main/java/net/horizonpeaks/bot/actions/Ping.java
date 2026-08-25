package net.horizonpeaks.bot.actions;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.horizonpeaks.bot.data.CommandDefinition;

/**
 * Responds with the bot's current Discord gateway latency.
 */
public final class Ping implements Action {

    /**
     * Responds to the command with the current gateway latency.
     *
     * @param command the command calling this action
     * @param event the slash command interaction
     */
    @Override
    public void act(CommandDefinition command, SlashCommandInteractionEvent event) {
        long latency = event.getJDA().getGatewayPing();

        event.reply("Pong! API: **" + latency + "ms**").queue();
    }
}