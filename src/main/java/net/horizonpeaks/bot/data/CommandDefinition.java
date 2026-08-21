package net.horizonpeaks.bot.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;

import java.util.List;

import net.horizonpeaks.bot.actions.Action;

/**
 * Describes a slash command loaded from {@code commands.yaml}.
 *
 * <p>Each definition contains the information required to register the
 * command with Discord, as well as the response text or custom action
 * associated with the command.</p>
 *
 * @param name the slash command name, without the leading {@code /}
 * @param description the description shown by Discord for the command
 * @param text optional static response text
 * @param arguments optional list of slash command arguments
 * @param action optional custom action implemented in Java
 */
public record CommandDefinition(
        String name,
        String description,
        String text,
        List<Embed> embeds,
        List<ArgumentDefinition> arguments,
        Action action) {

    private static final ObjectMapper MAPPER =
            new ObjectMapper(new YAMLFactory());

    /**
     * Parses a YAML document containing a list of command definitions.
     *
     * <p>YAML property names are automatically matched to the corresponding
     * record components by Jackson.</p>
     *
     * @param yaml the contents of {@code commands.yaml}
     * @return the parsed command definitions
     * @throws IllegalArgumentException if the YAML cannot be parsed
     */
    public static List<CommandDefinition> fromYaml(String yaml) {
        try {
            return MAPPER.readValue(
                    yaml,
                    MAPPER.getTypeFactory()
                            .constructCollectionType(List.class, CommandDefinition.class));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid commands.yaml", e);
        }
    }

    /**
     * Converts this definition into a JDA slash command definition.
     *
     * <p>Arguments are currently registered as string options. Additional
     * Discord option types can be supported later if needed.</p>
     *
     * @return the JDA slash command representation of this definition
     */
    public SlashCommandData toSlashCommand() {
        SlashCommandData command = Commands.slash(name, description);

        if (arguments != null) {
            for (ArgumentDefinition argument : arguments) {
                command.addOption(
                        OptionType.STRING, // TODO generalize to other argument types if needed
                        argument.name(),
                        argument.description(),
                        argument.required());
            }
        }

        return command;
    }
}