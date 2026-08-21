package net.horizonpeaks.bot.data;

import java.util.List;
import java.util.function.Function;

/**
 * Describes a Discord embed loaded from configuration.
 *
 * <p>
 * All properties are optional. Placeholders are resolved later when the
 * embed is rendered for a Discord interaction.
 * </p>
 *
 * @param title       optional embed title
 * @param url         optional URL opened when the title is clicked
 * @param description optional embed description
 * @param color       optional embed color, typically a configured placeholder
 * @param thumbnail   optional thumbnail image URL
 * @param image       optional large image URL
 * @param footer      optional footer text
 * @param fields      optional list of embed fields
 */
public record Embed(
        String title,
        String url,
        String description,
        String color,
        String thumbnail,
        String image,
        String footer,
        List<EmbedField> fields) {

    public Embed resolved(Function<String, String> resolver) {
        return new Embed(
                resolver.apply(title),
                resolver.apply(url),
                resolver.apply(description),
                resolver.apply(color),
                resolver.apply(thumbnail),
                resolver.apply(image),
                resolver.apply(footer),
                fields == null ? null
                        : fields
                                .stream()
                                .map(field -> field.resolved(resolver))
                                .toList());
    }
}