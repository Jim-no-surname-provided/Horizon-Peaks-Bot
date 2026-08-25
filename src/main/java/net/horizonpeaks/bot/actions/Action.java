package net.horizonpeaks.bot.actions;

import com.fasterxml.jackson.annotation.JsonCreator;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.horizonpeaks.bot.data.CommandDefinition;

/**
 * Defines a custom action that can be executed by a configured slash command.
 *
 * <p>
 * Concrete action classes must be located in the same package as this
 * interface and implement {@link Action}. Their class names are referenced
 * directly from {@code commands.yaml}.
 * </p>
 *
 * <p>
 * For example, {@code action: Announce} resolves to a class named
 * {@code Announce} in this package.
 * </p>
 */
public interface Action {

    /**
     * Executes this action for a slash command interaction.
     *
     * @param event the Discord slash command interaction that triggered the action
     */
    void act(CommandDefinition command, SlashCommandInteractionEvent event);

    /**
     * Resolves an action name into an instantiated {@link Action}.
     *
     * <p>
     * The provided name must exactly match the name of a class in the same
     * package as this interface. The class must implement {@link Action} and
     * provide an accessible zero-argument constructor.
     * </p>
     *
     * <p>
     * A {@code null} or blank name resolves to {@code null}, allowing an
     * action to be omitted or explicitly left empty in the configuration.
     * </p>
     *
     * @param name the action class name as defined in {@code commands.yaml}
     * @return the instantiated action, or {@code null} if no action was specified
     * @throws IllegalArgumentException if the named class does not exist, does not
     *                                  implement {@link Action}, or cannot be
     *                                  instantiated
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    static Action fromName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        String RED = "\u001B[31m";
        String RESET = "\u001B[0m";
        try {
            // Get class from its name
            Class<?> actionClass = Class.forName(Action.class.getPackageName() + "." + name);

            // Make new instance from that class
            return actionClass
                    .asSubclass(Action.class)
                    .getDeclaredConstructor()
                    .newInstance();

        } catch (ClassNotFoundException e) {
            System.err.println(RED + "Action class does not exist: " + name + RESET);
            System.exit(1);
            return null;

        } catch (ReflectiveOperationException | ClassCastException e) {
            System.err.println(RED + "Invalid action class: " + name + RESET);
            System.exit(1);
            return null;
        }
    }
}