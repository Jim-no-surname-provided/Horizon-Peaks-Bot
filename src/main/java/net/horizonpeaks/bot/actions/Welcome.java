package net.horizonpeaks.bot.actions;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.horizonpeaks.bot.Config;
import net.horizonpeaks.bot.MsgSender;
import net.horizonpeaks.bot.data.Embed;
import net.horizonpeaks.bot.data.FileLoader;

public class Welcome {

    public static void welcome(GuildMemberJoinEvent event) {
        Embed welcome;
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        try {

            welcome = yamlMapper.readValue(
                    FileLoader.getOrCreate("messages/welcome.yaml", "messages/welcome.yaml").toFile(),
                    Embed.class);

        } catch (IOException e) {
            System.err.println("File messages/welcome.yaml doesn't exist.");
            return;
        }

        MessageEmbed embed = MsgSender.renderEmbed(welcome, value -> resolve(value, event));

        TextChannel channel = event.getGuild().getTextChannelById(Config.get().channels().welcome());

        if (channel == null) {
            throw new IllegalStateException("Configured welcome channel does not exist");
        }

        channel.sendMessageEmbeds(embed).queue();
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
                .replace("%guild.id%", guild.getId())
                .replace("%member.avatar%", member.getUser().getEffectiveAvatarUrl())
                .replace("%guild.memberCount%", String.valueOf(guild.getMemberCount()));
    }
}
