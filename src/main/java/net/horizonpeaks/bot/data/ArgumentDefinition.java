package net.horizonpeaks.bot.data;

/**
 * Describes a slash command argument loaded from {@code commands.yaml}.
 *
 * @param name the argument name shown in the slash command
 * @param description the description shown by Discord for the argument
 * @param required whether the argument must be provided
 */
public record ArgumentDefinition(
        String name,
        String description,
        boolean required
) {
}