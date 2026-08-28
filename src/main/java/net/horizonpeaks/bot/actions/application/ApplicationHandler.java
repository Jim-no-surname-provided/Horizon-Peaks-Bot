package net.horizonpeaks.bot.actions.application;

import java.util.Map;

import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.horizonpeaks.bot.Config;
import net.horizonpeaks.bot.actions.Welcome;
import net.horizonpeaks.bot.data.ModalDefinition;

public class ApplicationHandler {

    /**
     * Validates the submitted application and shows the applicant a private
     * confirmation message.
     *
     * <p>
     * Applications with a clearly numeric age below the configured minimum are
     * rejected immediately. Otherwise, the applicant is shown their submitted
     * information and Minecraft identity and can either confirm the application
     * or change their Minecraft username.
     * </p>
     *
     * @param event the submitted application modal interaction
     */
    public static void confirmMessage(ModalInteractionEvent event) {
        Application app = Application.fromModal(event);
        Config config = Config.get();

        if (app.isUnderMinimumAge()) {
            event.reply("Sorry! You have to be a minimum age of " + config.minimumAge())
                    .setEphemeral(true)
                    .queue();
            return;
        }

        MessageEmbed msgEmbed = app.toMessageEmbed();
        MessageEmbed mcEmbed = app.toMinecraftEmbed();

        event.replyEmbeds(msgEmbed, mcEmbed)
                .addComponents(ActionRow.of(
                        Button.primary("application:submit", "Confirm"),
                        Button.danger("application:change_name",
                                "Change Minecraft username")))
                .setEphemeral(true)
                .queue();
    }

    public static void submit(ButtonInteractionEvent event) {
        Application app = Application.fromMessage(event.getMessage());

        String channelId = Config.get().channels().applicationReview();
        TextChannel channel = event.getGuild().getChannelById(TextChannel.class, channelId);

        if (channel == null) {
            throw new IllegalStateException("Configured application review channel does not exist");
        }

        // Acknowledge the button interaction while the application is submitted
        event.deferEdit().queue();

        channel.sendMessageEmbeds(app.toMessageEmbed())
                .addComponents(ActionRow.of(
                        Button.success("application:accept", "Accept"),
                        Button.danger("application:reject", "Reject")))
                .queue(message -> {
                    event.getHook()
                            .editOriginal(
                                    "Application submitted! You'll get a message when it is resolved by a staff member.")
                            .setEmbeds()
                            .setComponents()
                            .queue();

                }, error -> event.getHook()
                        .editOriginal("The application could not be submitted. Contact a staff member.")
                        .setEmbeds()
                        .queue());
    }

    /**
     * Reopens the application modal with the previous answers prefilled,
     * leaving the Minecraft name empty so it can be entered again.
     *
     * @param event            the button interaction requesting the change
     * @param applicationModal the configured application modal
     */
    public static void changeMcName(ButtonInteractionEvent event, ModalDefinition applicationModal) {
        // Remove previous button
        event.getHook().editOriginal("Nothing was submitted").setEmbeds().setComponents().queue();

        Application app = Application.fromMessage(event.getMessage());

        // Preserve the previous answers while forcing the Minecraft name to be
        // re-entered
        Map<String, String> defaults = Map.of(
                "age", app.getAge(),
                "heardFrom", app.getHeardFrom(),
                "favoriteThing", app.getFavoriteThing());

        event.replyModal(applicationModal.toModalWithDefaults(defaults)).queue();
    }

    /**
     * Accepts an application and grants the applicant access to the server.
     *
     * @param event the staff acceptance button interaction
     */
    public static void accept(ButtonInteractionEvent event) {
        Application app = Application.fromMessage(event.getMessage());
        Member member = app.getMember();

        Role memberRole = member.getGuild().getRoleById(Config.get().roles().member());

        if (memberRole == null) {
            throw new IllegalStateException("Configured member role does not exist");
        }

        // Remove the review buttons immediately
        event.editComponents().queue();

        // Grant membership before sending acceptance messages
        member.getGuild().addRoleToMember(member, memberRole).queue(
                success -> {
                    member.getUser()
                            .openPrivateChannel()
                            .queue(channel -> channel.sendMessage(
                                    "Your application to **%s** has been accepted! 🎉"
                                            .formatted(Config.get()
                                                    .branding()
                                                    .name())));

                    TextChannel smpConsole = event.getGuild().getChannelById(TextChannel.class,
                            Config.get().channels().smpConsole());
                    TextChannel cmpConsole = event.getGuild().getChannelById(TextChannel.class,
                            Config.get().channels().cmpConsole());

                    smpConsole.sendMessage("whitelist add " + app.getMcName()).queue();
                    cmpConsole.sendMessage("whitelist add " + app.getMcName()).queue();
                    
                    Welcome.welcome(member);
                },
                error -> {
                    error.printStackTrace();

                    String message = error.getMessage() != null
                            ? error.getMessage()
                            : error.getClass().getSimpleName();

                    event.getHook()
                            .sendMessage("Something went wrong while accepting this user:\n```"
                                    + message + "```")
                            .setEphemeral(true)
                            .queue();

                });
    }

    public static void reject(ButtonInteractionEvent event) {
        Application app = Application.fromMessage(event.getMessage());

        // Remove the review buttons immediately
        event.editComponents().queue();

        app.getMember().getUser()
                .openPrivateChannel()
                .queue(channel -> channel.sendMessage(
                        "Your application to **%s** has been rejected."
                                .formatted(Config.get().branding().name())));
    }
}