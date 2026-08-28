package net.horizonpeaks.bot.actions.application;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import net.horizonpeaks.bot.Config;

public class Application {

    private final String age;
    private final Member member;
    private final String mcName;
    private final String heardFrom;
    private final String favoriteThing;

    public Application(String age, Member member, String mcName, String heardFrom, String favoriteThing) {
        this.age = age;
        this.member = member;
        this.mcName = mcName;
        this.heardFrom = heardFrom;
        this.favoriteThing = favoriteThing;
    }

    public static Application fromModal(ModalInteractionEvent event) {

        ModalMapping age = event.getValue("age");
        ModalMapping mcName = event.getValue("mcName");
        ModalMapping heardFrom = event.getValue("heardFrom");
        ModalMapping favoriteThing = event.getValue("favoriteThing");

        if (age == null ||
                mcName == null ||
                heardFrom == null ||
                favoriteThing == null) {

            System.out.println("Age is %s".formatted(age));
            System.out.println("mcName is %s".formatted(mcName));
            System.out.println("heardFrom is %s".formatted(heardFrom));
            System.out.println("favoriteThing is %s".formatted(favoriteThing));
            throw new IllegalArgumentException("Application form is missing required values");

        }

        Member member = event.getMember();

        if (member == null) {
            throw new IllegalArgumentException("Application must be submitted from a guild");
        }

        return new Application(
                age.getAsString(),
                member,
                mcName.getAsString(),
                heardFrom.getAsString(),
                favoriteThing.getAsString());
    }

    /**
     * Checks whether the submitted age is below the configured minimum age.
     *
     * <p>
     * If the submitted age cannot be parsed as an integer, this method returns
     * {@code false} so unusual formats such as {@code "16+"} or minor input
     * mistakes can be reviewed manually instead of being rejected automatically.
     * </p>
     *
     * @return {@code true} if the age is numeric and below the minimum age
     */
    public boolean isUnderMinimumAge() {
        try {
            return Integer.parseInt(age) < Config.get().minimumAge();
        } catch (NumberFormatException e) {
            // Not a number
            return false;
        }
    }

    /**
     * Builds the Discord embed used to review this application.
     *
     * @return the application review embed
     */
    public MessageEmbed toMessageEmbed() {
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle("Application - " + member.getEffectiveName())
                .setThumbnail(member.getEffectiveAvatarUrl())
                .addField("Applicant",
                        "[%s](https://discord.com/users/%s)".formatted(
                                member.getEffectiveName(),
                                member.getId()),
                        true)
                .addField("Age", age, true)
                .addField("Minecraft Username", mcName, false)
                .addField("Where did you hear about us?", heardFrom, false)
                .addField("Favorite thing about Minecraft", favoriteThing, false);

        return builder.build();
    }

    private static final Pattern USER_URL_PATTERN = Pattern.compile("https://discord\\.com/users/(\\d+)");

    public static Application fromMessage(Message message) {
        if (message.getEmbeds().isEmpty()) {
            throw new IllegalArgumentException("Message has no application embed");
        }

        // Assume the first embed has the info
        MessageEmbed embed = message.getEmbeds().getFirst();

        // Get Member
        String applicantField = getField(embed, "Applicant");

        Matcher matcher = USER_URL_PATTERN.matcher(applicantField);

        if (!matcher.find()) {
            throw new IllegalArgumentException("Application has no valid applicant");
        }

        String memberId = matcher.group(1);

        Member member = message.getGuild().getMemberById(memberId);

        if (member == null) {
            throw new IllegalArgumentException("Applicant is not a member of the guild");
        }

        // Get the rest of the fields
        String age = getField(embed, "Age");
        String mcName = getField(embed, "Minecraft Username");
        String heardFrom = getField(embed, "Where did you hear about us?");
        String favoriteThing = getField(embed, "Favorite thing about Minecraft");

        return new Application(age, member, mcName, heardFrom, favoriteThing);

    }

    /**
     * Returns a required field from an application embed
     *
     * @param embed the application embed
     * @param name  the field name
     * @return the field value
     * @throws IllegalArgumentException if the field does not exist
     */
    private static String getField(MessageEmbed embed, String name) {
        String value = embed.getFields().stream()
                .filter(field -> name.equals(field.getName()))
                .map(MessageEmbed.Field::getValue)
                .findFirst()
                .orElse(null);

        if (value == null) {
            throw new IllegalArgumentException("Application is missing field: " + name);
        }

        return value;
    }

    /**
     * Builds an embed showing the submitted Minecraft player.
     *
     * @return the Minecraft player confirmation embed
     */
    public MessageEmbed toMinecraftEmbed() {
        return new EmbedBuilder()
                .setTitle("Is this you?")
                .setDescription("Minecraft username: **" + mcName + "**")
                .setImage("https://mc-heads.net/player/" + mcName + "/300")
                .build();
    }

    public String getAge() {
        return age;
    }

    public Member getMember() {
        return member;
    }

    public String getMcName() {
        return mcName;
    }

    public String getHeardFrom() {
        return heardFrom;
    }

    public String getFavoriteThing() {
        return favoriteThing;
    }

}
