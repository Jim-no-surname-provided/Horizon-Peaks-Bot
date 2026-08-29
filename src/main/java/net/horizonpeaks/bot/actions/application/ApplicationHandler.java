package net.horizonpeaks.bot.actions.application;

import java.awt.Color;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.horizonpeaks.bot.Config;
import net.horizonpeaks.bot.actions.Welcome;
import net.horizonpeaks.bot.data.ModalDefinition;

/**
 * Handles the application workflow, including submission, review, interviews,
 * acceptance, rejection, and active-application lookup.
 */
public class ApplicationHandler {

    /**
     * Opens the application modal if the member has no active application.
     *
     * <p>
     * Members with an unresolved application are shown an ephemeral message
     * instead of being allowed to submit another one.
     * </p>
     *
     * @param event            the application button interaction
     * @param applicationModal the configured application modal
     */
    public static void apply(ButtonInteractionEvent event, ModalDefinition applicationModal) {
        if (getActiveApplicationMsg(event.getMember()).isEmpty()) {
            event.replyModal(applicationModal.toModal()).queue();
            return;
        }

        event.reply("You can only have one application active at a time!")
                .setEphemeral(true).queue();

    }

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

        existsMcName(app.getMcName()).thenAccept(exists -> {
            if (exists) {
                // Add skin picture and confirm buttons
                MessageEmbed mcEmbed = app.toMinecraftEmbed();

                event.replyEmbeds(msgEmbed, mcEmbed)
                        .addComponents(ActionRow.of(
                                Button.primary("application:submit", "Confirm"),
                                Button.danger("application:change_name",
                                        "Change Minecraft username")))
                        .setEphemeral(true)
                        .queue();

            } else {
                // Tell mcname doesn't exist
                MessageEmbed mcEmbed = new EmbedBuilder()
                        .setTitle("Minecraft account not found")
                        .setDescription("I couldn't find a Minecraft Java account with that username.\n\n"
                                + "-# If you think that's wrong, contact an admin.")
                        .setColor(Color.decode(Config.get().colors().error()))
                        .build();

                event.replyEmbeds(msgEmbed, mcEmbed)
                        .addComponents(ActionRow.of(
                                Button.danger("application:change_name",
                                        "Change Minecraft username")))
                        .setEphemeral(true)
                        .queue();

            }
        }).exceptionally(error -> {
            // If mojand didn't respond
            error.printStackTrace();

            event.getHook()
                    .editOriginal("Something went wrong while checking the Minecraft username. "
                            + "Please try again later.")
                    .queue();

            return null;
        });

    }

    private static final Pattern MC_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    /**
     * Checks whether a Minecraft Java username exists.
     *
     * @param name the Minecraft username
     * @return whether the username belongs to an existing Minecraft Java profile
     */
    public static CompletableFuture<Boolean> existsMcName(String name) {
        // Reject names Minecraft itself cannot accept
        if (!MC_NAME_PATTERN.matcher(name).matches()) {
            return CompletableFuture.completedFuture(false);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.minecraftservices.com/minecraft/profile/lookup/name/" + name))
                .GET()
                .build();

        return HTTP_CLIENT
                .sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return true;
                    }

                    if (response.statusCode() == 404) {
                        return false;
                    }

                    throw new IllegalStateException("Minecraft profile API returned HTTP " + response.statusCode());
                });
    }

    /**
     * Submits a confirmed application to the configured staff review channel.
     *
     * <p>
     * The review message contains controls for accepting, rejecting, or starting
     * an interview. The applicant's ephemeral confirmation message is replaced
     * with the submission result.
     * </p>
     *
     * @param event the application confirmation button interaction
     */
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
                        Button.danger("application:reject", "Reject"),
                        Button.secondary("application:interview", "Interview")))
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
                                                    .name()))
                                    .queue());

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

    /**
     * Creates a private interview channel for an application.
     *
     * <p>
     * The applicant is granted access to the channel and the application embed is
     * copied into it with a link to the original review message. The original
     * Interview button is then replaced with a link to the interview channel.
     * </p>
     *
     * @param event the staff interview button interaction
     */
    public static void startInterview(ButtonInteractionEvent event) {
        Application app = Application.fromMessage(event.getMessage());
        Member member = app.getMember();
        Message applicationMessage = event.getMessage();

        Category category = event.getGuild().getCategoryById(
                Config.get().channels().applicationCategory());

        if (category == null) {
            throw new IllegalStateException("Configured application category does not exist");
        }

        // Acknowledge the interaction before doing asynchronous work
        event.deferEdit().queue();

        category.createTextChannel("interview-" + member.getEffectiveName())
                .queue(channel -> {
                    // Add permission to the member to see the channel
                    channel.upsertPermissionOverride(member)
                            .grant(
                                    Permission.VIEW_CHANNEL,
                                    Permission.MESSAGE_SEND,
                                    Permission.MESSAGE_HISTORY)
                            .queue();

                    // Resend same message's embed with a link to the original application
                    channel.sendMessageEmbeds(applicationMessage.getEmbeds())
                            .addComponents(ActionRow.of(
                                    Button.link(applicationMessage.getJumpUrl(), "Open application")))
                            .queue();

                    // Replace Interview with a link to the interview channel
                    applicationMessage.editMessageComponents(
                            ActionRow.of(
                                    Button.success("application:accept", "Accept"),
                                    Button.danger("application:reject", "Reject"),
                                    Button.link(channel.getJumpUrl(), "Interview")))
                            .queue();
                });
    }

    /**
     * Finds the unresolved application review message for a member.
     *
     * <p>
     * Applications without buttons are considered resolved and are ignored.
     * Unrelated messages in the application review channel are also ignored.
     * </p>
     *
     * @param member the applicant
     * @return the active application message, or an empty optional if none exists
     */
    public static Optional<Message> getActiveApplicationMsg(Member member) {
        TextChannel channel = member.getGuild().getTextChannelById(
                Config.get().channels().applicationReview());

        if (channel == null) {
            throw new IllegalStateException("Configured application review channel does not exist");
        }

        for (Message message : channel.getIterableHistory()) {
            // Resolved applications no longer have buttons
            boolean hasButtons = message.getComponents().stream()
                    .filter(component -> component instanceof ActionRow)
                    .map(component -> (ActionRow) component)
                    .anyMatch(row -> !row.getButtons().isEmpty());

            if (!hasButtons) {
                continue;
            }

            try {
                Application app = Application.fromMessage(message);

                if (app.getMember().getId().equals(member.getId())) {
                    return Optional.of(message);
                }
            } catch (IllegalArgumentException ignored) {
                // Ignore unrelated messages
            }
        }

        return Optional.empty();
    }

}