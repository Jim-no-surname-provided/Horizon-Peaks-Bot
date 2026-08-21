package net.horizonpeaks.bot.data;

import java.util.function.Function;

/**
 * Describes a field inside a Discord embed.
 *
 * @param name   the field name
 * @param value  the field contents
 * @param inline whether Discord may display the field beside other inline
 *               fields
 */
public record EmbedField(String name, String value, boolean inline) {


        public EmbedField resolved(Function<String, String> resolver) {
                return new EmbedField(
                                resolver.apply(name),
                                resolver.apply(value),
                                inline);
        }
}