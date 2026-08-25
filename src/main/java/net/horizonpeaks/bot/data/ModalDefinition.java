package net.horizonpeaks.bot.data;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;

/**
 * Defines a Discord modal and its input fields.
 *
 * @param id     the modal interaction ID
 * @param title  the modal title
 * @param text   optional text shown above the inputs
 * @param fields the modal input fields
 */
public record ModalDefinition(
        String id,
        String title,
        @Nullable String text,
        List<ModalField> fields) {

    /**
     * Defines one input field inside a modal.
     *
     * @param id          the field interaction ID
     * @param label       the field label
     * @param style       the input style
     * @param placeholder optional placeholder text
     * @param maxLength   optional maximum input length
     * @param required    whether the field is required
     */
    public record ModalField(
            String id,
            String label,
            TextInputStyle style,
            @Nullable String placeholder,
            @Nullable Integer maxLength,
            @Nullable Boolean required) {
    }

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    /**
     * Parses a YAML document containing a list of command definitions.
     *
     * <p>
     * YAML property names are automatically matched to the corresponding
     * record components by Jackson.
     * </p>
     *
     * @param yaml the contents of {@code commands.yaml}
     * @return the parsed modals
     * @throws IllegalArgumentException if the YAML cannot be parsed
     */
    public static List<ModalDefinition> fromYaml(String yaml) {
        try {
            return MAPPER.readValue(
                    yaml,
                    MAPPER.getTypeFactory()
                            .constructCollectionType(List.class, ModalDefinition.class));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid modals.yaml", e);
        }
    }

    /**
     * Builds the Discord modal represented by this definition.
     *
     * @return the Discord modal
     */
    public Modal toModal() {
        var builder = net.dv8tion.jda.api.modals.Modal.create(id, title);

        // Add optional text above the form
        if (text != null) {
            builder.addComponents(TextDisplay.of(text));
        }

        // Build each configured input field
        for (ModalField field : fields) {
            TextInput.Builder input = TextInput.create(field.id(), field.style());

            if (field.placeholder() != null) {
                input.setPlaceholder(field.placeholder());
            }

            if (field.maxLength() != null) {
                input.setMaxLength(field.maxLength());
            }

            if (field.required() != null) {
                input.setRequired(field.required());
            }

            builder.addComponents(Label.of(field.label(), input.build()));
        }

        return builder.build();
    }

}