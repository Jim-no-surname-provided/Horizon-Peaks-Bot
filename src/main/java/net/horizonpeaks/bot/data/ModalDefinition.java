package net.horizonpeaks.bot.data;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import net.dv8tion.jda.api.components.ModalTopLevelComponent;
import net.dv8tion.jda.api.components.checkbox.Checkbox;
import net.dv8tion.jda.api.components.checkboxgroup.CheckboxGroup;
import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.label.LabelChildComponent;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.modals.Modal;
import net.horizonpeaks.bot.PlaceholderResolver;

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
            ModalFieldType style,
            @Nullable String placeholder,
            @Nullable Integer maxLength,
            @Nullable Boolean required) {
    }

    /**
     * Describes the type of input rendered for a modal field.
     */
    public enum ModalFieldType {
        SHORT_TEXT,
        LONG_TEXT,
        CHECKBOX
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
        Modal.Builder builder = Modal.create(id, PlaceholderResolver.resolveConfig(title));

        // Add optional text above the form
        if (text != null) {
            builder.addComponents(TextDisplay.of(PlaceholderResolver.resolveConfig(text)));
        }

        // Build each configured input field
        for (ModalField field : fields) {
            switch (field.style()) {
                case SHORT_TEXT:
                    builder.addComponents(getTextLabel(field, TextInputStyle.SHORT));
                    break;
                case LONG_TEXT:
                    builder.addComponents(getTextLabel(field, TextInputStyle.PARAGRAPH));
                    break;
                case CHECKBOX:
                    builder.addComponents(getCheckBoxLabel(field));
                    break;
            }
        }

        return builder.build();
    }

    /**
     * Builds the checkbox component for a modal field.
     *
     * <p>
     * Required checkboxes are represented as a single-option checkbox group,
     * because Discord does not support required standalone checkboxes.
     * </p>
     *
     * @param field the modal field definition
     * @return the labeled checkbox component
     */
    private ModalTopLevelComponent getCheckBoxLabel(ModalField field) {
        String resolvedLabel = PlaceholderResolver.resolveConfig(field.label());
        LabelChildComponent checkbox;

        // Use a checkbox group when the user must explicitly confirm the option
        if (Boolean.TRUE.equals(field.required())) {
            checkbox = CheckboxGroup.create(field.id())
                    .addOption(resolvedLabel, "accepted")
                    .setRequired(true)
                    .build();
        } else {
            checkbox = Checkbox.of(field.id());
        }

        return Label.of(resolvedLabel, checkbox);
    }

    /**
     * Builds a labeled text input for a modal field.
     *
     * @param field the modal field definition
     * @param style the Discord text input style
     * @return the labeled text input component
     */
    private Label getTextLabel(ModalField field, TextInputStyle style) {
        TextInput.Builder input = TextInput.create(field.id(), style);

        // Apply optional input constraints
        if (field.placeholder() != null) {
            input.setPlaceholder(PlaceholderResolver.resolveConfig(field.placeholder()));
        }

        if (field.maxLength() != null) {
            input.setMaxLength(field.maxLength());
        }

        if (field.required() != null) {
            input.setRequired(field.required());
        }

        return Label.of(
                PlaceholderResolver.resolveConfig(field.label()),
                input.build());
    }

}