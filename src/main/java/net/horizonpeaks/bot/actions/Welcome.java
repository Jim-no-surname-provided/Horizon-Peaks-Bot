package net.horizonpeaks.bot.actions;

import java.io.IOException;
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.horizonpeaks.bot.Config;
import net.horizonpeaks.bot.MsgSender;
import net.horizonpeaks.bot.PlaceholderResolver;
import net.horizonpeaks.bot.data.Embed;
import net.horizonpeaks.bot.data.FileLoader;

public class Welcome {

    public static void welcome(Member member) {
        Embed welcome;
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        try {

            welcome = yamlMapper.readValue(
                    FileLoader.getOrCreate("definitions/welcome.yaml", "definitions/welcome.yaml").toFile(), Embed.class);

        } catch (IOException e) {
            System.err.println("File definitions/welcome.yaml doesn't exist.");
            return;
        }

        MessageEmbed embed = MsgSender.renderEmbed(welcome, value -> resolve(value, member), new ArrayList<>());

        TextChannel channel = member.getGuild().getTextChannelById(Config.get().channels().welcome());

        if (channel == null) {
            throw new IllegalStateException("Configured welcome channel does not exist");
        }

        channel.sendMessageEmbeds(embed).queue();
    }

    private static String resolve(String value, Member member) {

        String resolved = PlaceholderResolver.resolveConfig(value);

        if (resolved == null) {
            return null;
        }

        Guild guild = member.getGuild();

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
