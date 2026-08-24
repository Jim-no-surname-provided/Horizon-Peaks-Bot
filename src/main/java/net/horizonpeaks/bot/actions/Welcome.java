package net.horizonpeaks.bot.actions;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.horizonpeaks.bot.MsgSender;

public class Welcome {

    public static void welcome(GuildMemberJoinEvent event) {

    }

    private static String resolve(String value, GuildMemberJoinEvent event) {

        String resolved = MsgSender.resolveConfig(value);

        if (resolved == null) {
            return null;
        }

        Member member = event.getMember();
        Guild guild = event.getGuild();

        return resolved
                .replace("%member.name%", member.getUser().getName())
                .replace("%member.mention%", member.getAsMention())
                .replace("%member.id%", member.getId())
                .replace("%member.displayName%", member.getEffectiveName())
                .replace("%guild.name%", guild.getName())
                .replace("%guild.id%", guild.getId());
    }
}
