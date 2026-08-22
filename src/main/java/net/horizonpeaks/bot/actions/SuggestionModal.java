package net.horizonpeaks.bot.actions;

import net.dv8tion.jda.api.components.label.Label;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.textinput.TextInput;
import net.dv8tion.jda.api.components.textinput.TextInputStyle;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.modals.Modal;

import net.horizonpeaks.bot.data.Config;

/**
 * Starts the community suggestion submission process.
 */
public final class SuggestionModal implements Action {

    private final Config config = Config.get();

    /**
     * Opens the suggestion submission form.
     *
     * @param event the slash command interaction
     */
    @Override
    public void act(SlashCommandInteractionEvent event) {
        Modal modal = buildModal();
        event.replyModal(modal).queue();
    }

    /**
     * Builds the suggestion submission form.
     *
     * @return the suggestion modal
     */
    private Modal buildModal() {
        // Add submission information above the form
        TextDisplay disclaimer = TextDisplay.of(buildDisclaimer());

        // Build form inputs
        TextInput title = TextInput.create("title", TextInputStyle.SHORT)
                .setPlaceholder("A short title for your suggestion")
                .setMaxLength(100)
                .build();

        TextInput description = TextInput.create("description", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Describe your suggestion in detail")
                .setMaxLength(1024)
                .build();

        TextInput image = TextInput.create("image", TextInputStyle.SHORT) 
                .setPlaceholder("https://example.com/image.png")
                .setMaxLength(256)
                .setRequired(false)
                .build();

        TextInput examples = TextInput.create("examples", TextInputStyle.PARAGRAPH)
                .setPlaceholder("Text examples or links to illustrate your idea")
                .setMaxLength(1024)
                .setRequired(false)
                .build();

        // Assemble the modal
        return Modal.create("suggestion:submit", "Submit a Suggestion")
                .addComponents(
                        disclaimer,
                        Label.of("Title", title),
                        Label.of("Description", description),
                        Label.of("Image URL (optional)", image),
                        Label.of("Examples (optional)", examples))
                .build();
    }

    /**
     * Builds the disclaimer shown above the suggestion form.
     *
     * @return the formatted suggestion disclaimer
     */
    private String buildDisclaimer() {
        Config.Suggestions suggestions = config.suggestions();

        return """
                **Before submitting, please note:**

                • Suggestions initially stay open for **%d days**
                • If there are not enough votes, they may be extended by **%d days**, then by **%d final day**
                • At least **%d total votes** are required
                • You may have at most **%d active suggestions**
                • Administrators may approve or reject suggestions at any time
                • Denied suggestions may be submitted again, but please do not spam them
                """.formatted(
                suggestions.initialDays(),
                suggestions.firstExtensionDays(),
                suggestions.finalExtensionDays(),
                suggestions.minimumVotes(),
                suggestions.maxActivePerUser());
    }
}