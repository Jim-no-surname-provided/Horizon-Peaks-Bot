package net.horizonpeaks.bot.actions.suggestions;

import static net.horizonpeaks.bot.actions.suggestions.Suggestion.Status.OPEN;

import java.awt.Color;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Footer;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.horizonpeaks.bot.Config;

/**
 * Represents a community suggestion and its current state
 *
 * <p>
 * Suggestions can be reconstructed from their Discord message, rendered back
 * into an embed, and moved between open, accepted, and rejected states
 * </p>
 */
public class Suggestion {

    /**
     * Possible states of a suggestion
     *
     * <p>
     * Each status defines its display name, emoji, color, and destination
     * Discord channel
     * </p>
     */
    public enum Status {
        OPEN("Open", "📋"),
        ACCEPTED("Accepted", "✅"),
        REJECTED("Rejected", "⛔");

        private final String name;
        private final String emoji;

        Status(String name, String emoji) {
            this.name = name;
            this.emoji = emoji;
        }

        /**
         * Returns the human-readable name of this status
         *
         * @return the display name
         */
        public String displayName() {
            return name;
        }

        /**
         * Returns the emoji representing this status
         *
         * @return the status emoji
         */
        public String emoji() {
            return emoji;
        }

        /**
         * Returns the configured color for this status
         *
         * @return the hexadecimal status color
         */
        public String color() {
            Config.Colors colors = Config.get().colors();

            return switch (this) {
                case OPEN -> colors.brand();
                case ACCEPTED -> colors.success();
                case REJECTED -> colors.error();
            };
        }

        /**
         * Returns the configured Discord channel for this status
         *
         * @param jda the running Discord connection
         * @return the channel belonging to this status
         * @throws IllegalStateException if the configured channel does not exist
         */
        public TextChannel channel(JDA jda) {
            Config.Channels channels = Config.get().channels();

            TextChannel channel = switch (this) {
                case OPEN -> jda.getTextChannelById(channels.activeSuggestions());
                case ACCEPTED -> jda.getTextChannelById(channels.approvedSuggestions());
                case REJECTED -> jda.getTextChannelById(channels.deniedSuggestions());
            };

            if (channel == null) {
                throw new IllegalStateException("Configured suggestion channel does not exist for status: " + name);
            }

            return channel;
        }
    }

    private final JDA jda;
    private final int id;
    private final String title;
    private final String description;
    private final @Nullable String image;
    private final @Nullable String examples;
    private final Member author;
    private final int upvotes;
    private final int downvotes;
    private final OffsetDateTime submittedAt;
    private final @Nullable OffsetDateTime closedAt;
    private final @Nullable String reason;
    private final Status status;

    /**
     * Creates a complete suggestion representation
     *
     * @param jda         the running Discord connection
     * @param id          the numeric suggestion ID
     * @param title       the suggestion title
     * @param description the suggestion description
     * @param image       the optional image URL
     * @param examples    the optional examples
     * @param author      the member who submitted the suggestion
     * @param reason      the optional reason for the current status
     * @param status      the current suggestion status
     * @param submittedAt the time the current voting period began
     * @param closedAt    the time the suggestion was closed, or {@code null}
     * @param upvotes     the number of community upvotes
     * @param downvotes   the number of community downvotes
     */
    public Suggestion(JDA jda, int id, String title, String description, @Nullable String image,
            @Nullable String examples, Member author, @Nullable String reason, Status status,
            OffsetDateTime submittedAt, @Nullable OffsetDateTime closedAt, int upvotes, int downvotes) {
        this.jda = jda;
        this.id = id;
        this.title = title;
        this.description = description;
        this.image = image;
        this.examples = examples;
        this.author = author;
        this.upvotes = upvotes;
        this.downvotes = downvotes;
        this.submittedAt = submittedAt;
        this.closedAt = closedAt;
        this.reason = reason;
        this.status = status;

    }

    /**
     * Creates a newly opened suggestion without closing data or stored votes
     *
     * @param jda         the running Discord connection
     * @param id          the numeric suggestion ID
     * @param title       the suggestion title
     * @param description the suggestion description
     * @param image       the optional image URL
     * @param examples    the optional examples
     * @param author      the member who submitted the suggestion
     * @param reason      the optional reason for the current status
     * @param status      the current suggestion status
     * @param submittedAt the time the suggestion was submitted
     */
    public Suggestion(JDA jda, int id, String title, String description, @Nullable String image,
            @Nullable String examples, Member author, @Nullable String reason, Status status,
            OffsetDateTime submittedAt) {
        this(jda, id, title, description, image, examples, author, reason, status, submittedAt, null, 0, 0);
    }

    /**
     * Builds the Discord embed representing this suggestion
     *
     * @return the rendered suggestion embed
     */
    public MessageEmbed toMessageEmbed() {
        Config config = Config.get();
        OffsetDateTime expiresAt = submittedAt.plusDays(config.suggestions().initialDays());

        // Build base suggestion information
        EmbedBuilder builder = new EmbedBuilder()
                .setTitle(status.emoji + title)
                .setDescription(description)
                .setColor(Color.decode(status.color()))
                .setThumbnail(author.getUser().getEffectiveAvatarUrl())
                .addField("Author", "[%s](https://discord.com/users/%s)"
                        .formatted(author.getEffectiveName(), author.getId()), true)
                .addField("Status", "**" + status.name + "**", true)
                .setTimestamp(submittedAt);

        // Add information specific to the current status
        if (status == Status.OPEN) {
            builder.setFooter("S-%04d | Expires %s | %s".formatted(
                    id,
                    expiresAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")),
                    config.branding().name()));

        } else {
            if (closedAt == null) {
                throw new IllegalStateException("Closed suggestion has no closing time");
            }

            builder.setFooter("S-%04d | %s".formatted(id, config.branding().name()))
                    .addField("Votes", "%s %d | %s %d"
                            .formatted(Status.ACCEPTED.emoji, upvotes, Status.REJECTED.emoji, downvotes), false)
                    .addField("Submitted", "<t:%d:F>".formatted(submittedAt.toEpochSecond()), true)
                    .addField("Closed", "<t:%d:F>".formatted(closedAt.toEpochSecond()), true);

        }

        // Add optional status reason
        if (reason != null) {
            builder.addField("Reason", reason, false);
        }

        // Add image
        if (image != null) {
            builder.setImage(image);
        }

        // Add examples
        if (examples != null) {
            builder.addField("Examples", examples, false);
        }

        return builder.build();
    }

    /**
     * Reconstructs a suggestion from its Discord message
     *
     * @param message the Discord message representing the suggestion
     * @return the reconstructed suggestion
     * @throws IllegalArgumentException if the message cannot be interpreted as a
     *                                  suggestion
     */
    public static Suggestion fromMessage(Message message) {
        if (message.getEmbeds().isEmpty()) {
            throw new IllegalArgumentException("Message has no suggestion embed");
        }

        MessageEmbed embed = message.getEmbeds().getFirst();

        // Read ID
        int id = getId(message);

        if (id == -1) {
            throw new IllegalArgumentException("Suggestion has no valid ID");
        }

        // Read status
        String statusName = getField(embed, "Status").replace("*", "");

        Status status = switch (statusName) {
            case "Open" -> Status.OPEN;
            case "Accepted" -> Status.ACCEPTED;
            case "Rejected" -> Status.REJECTED;
            case "Denied" -> Status.REJECTED;
            default -> throw new IllegalArgumentException(
                    "Unknown suggestion status: " + statusName);
        };

        // Read title without status emoji
        String title = embed.getTitle();

        if (title == null) {
            throw new IllegalArgumentException("Suggestion has no title");
        }
        title = title.substring(status.emoji.length()).stripLeading();

        // Read description
        String description = embed.getDescription();

        if (description == null) {
            throw new IllegalArgumentException("Suggestion has no description");
        }

        // Read author
        String authorField = getField(embed, "Author");

        Matcher authorMatcher = Pattern.compile(
                "https://discord\\.com/users/(\\d+)")
                .matcher(authorField);
        if (!authorMatcher.find()) {
            throw new IllegalArgumentException("Suggestion has no valid author");
        }
        String authorId = authorMatcher.group(1);
        Member author = message.getGuild().getMemberById(authorId);

        if (author == null) {
            throw new IllegalArgumentException("Suggestion author is not in the guild");
        }

        // Read optional values
        String image = embed.getImage() == null ? null : embed.getImage().getUrl();

        String examples = getOptionalField(embed, "Examples");

        // Read votes
        int upvotes = 0;
        int downvotes = 0;
        String votes = getOptionalField(embed, "Votes");

        if (votes != null) {
            Matcher votesMatcher = Pattern.compile("✅\\s*(\\d+).*⛔\\s*(\\d+)").matcher(votes);

            if (votesMatcher.find()) {
                upvotes = Integer.parseInt(votesMatcher.group(1));
                downvotes = Integer.parseInt(votesMatcher.group(2));
            }
        }

        // For open suggestions read the reactions instead
        if (status == Status.OPEN) {
            for (MessageReaction reaction : message.getReactions()) {
                if (reaction.getEmoji().getName().equals("✅")) {
                    upvotes = Math.max(0, reaction.getCount() - 1);
                }

                if (reaction.getEmoji().getName().equals("⛔")) {
                    downvotes = Math.max(0, reaction.getCount() - 1);
                }
            }
        }

        // Read dates
        OffsetDateTime submittedAt = message.getTimeCreated();
        OffsetDateTime closedAt = null;

        if (status != Status.OPEN) {
            submittedAt = parseDate(getField(embed, "Submitted"));
            closedAt = parseDate(getField(embed, "Closed"));
        }

        // Read reason
        String reason = getOptionalField(embed, "Reason");

        return new Suggestion(message.getJDA(), id, title, description, image, examples, author, reason,
                status, submittedAt, closedAt, upvotes, downvotes);
    }

    /**
     * Returns a required field from a suggestion embed
     *
     * @param embed the suggestion embed
     * @param name  the field name
     * @return the field value
     * @throws IllegalArgumentException if the field does not exist
     */
    private static String getField(MessageEmbed embed, String name) {
        String value = getOptionalField(embed, name);

        if (value == null) {
            throw new IllegalArgumentException(
                    "Suggestion is missing field: " + name);
        }

        return value;
    }

    /**
     * Returns an optional field from a suggestion embed
     *
     * @param embed the suggestion embed
     * @param name  the field name
     * @return the field value, or {@code null} if it does not exist
     */
    private static @Nullable String getOptionalField(MessageEmbed embed, String name) {
        return embed.getFields().stream()
                .filter(field -> name.equals(field.getName()))
                .map(MessageEmbed.Field::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * Parses a Discord timestamp stored in a suggestion embed
     *
     * @param value the Discord timestamp
     * @return the parsed timestamp
     * @throws IllegalArgumentException if the timestamp cannot be parsed
     */
    private static OffsetDateTime parseDate(String value) {
        Matcher matcher = Pattern.compile("<t:(\\d+):F>")
                .matcher(value);

        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                    "Invalid suggestion timestamp: " + value);
        }

        long epoch = Long.parseLong(matcher.group(1));

        return OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(epoch),
                ZoneOffset.UTC);
    }

    private static final Pattern SUGGESTION_ID_PATTERN = Pattern.compile("^S-(\\d{4,})\\b");

    /**
     * Reads the numeric suggestion ID from a Discord message
     *
     * @param message the Discord message
     * @return the suggestion ID, or {@code -1} if no valid ID is present
     */
    public static int getId(Message message) {
        // Guard empty message
        if (message.getEmbeds().isEmpty()) {
            return -1;
        }

        Footer footer = message.getEmbeds().getFirst().getFooter();

        // Guard empty footer
        if (footer == null || footer.getText() == null) {
            return -1;
        }

        Matcher matcher = SUGGESTION_ID_PATTERN.matcher(footer.getText());

        // Guard against no match
        if (!matcher.find()) {
            return -1;
        }

        return Integer.valueOf(matcher.group(1));
    }

    /**
     * Moves this suggestion to another status
     *
     * <p>
     * A new suggestion message and discussion thread are created in the
     * destination channel. The previous discussion thread is archived and the
     * previous suggestion message is removed after the new message was sent
     * </p>
     *
     * @param reason  the optional reason for the status change
     * @param status  the new suggestion status
     * @param message the current Discord message representing the suggestion
     */
    public void changeStatus(@Nullable String reason, Status status, Message message) {
        OffsetDateTime newClosedAt = OffsetDateTime.now();
        OffsetDateTime newSubmitedAt = submittedAt;

        // Reset timing when reopening
        if (status == Status.OPEN) {
            newClosedAt = null;
            newSubmitedAt = OffsetDateTime.now();

            // Preserve closing time when moving between closed states
        } else if (closedAt != null) {
            newClosedAt = closedAt;
        }

        Suggestion suggestion = new Suggestion(jda, id, title, description, image, examples, author, reason, status,
                newSubmitedAt, newClosedAt, upvotes, downvotes);

        status.channel(jda).sendMessageEmbeds(suggestion.toMessageEmbed()).queue(sent -> {
            suggestion.createDiscussionThread(sent);

            // Only open suggestions can receive votes
            if (status == OPEN)
                suggestion.addVoting(sent);

            // Archive old discussion thread
            ThreadChannel oldThread = message.getStartedThread();
            if (oldThread != null) {
                oldThread.getManager().setArchived(true).queue();
            }

            // Remove the old suggestion only after the new one was sent
            message.delete().queue();
        });

    }

    /**
     * Adds the voting reactions to a suggestion message
     *
     * @param message the posted suggestion message
     */
    public void addVoting(Message message) {
        message.addReaction(Emoji.fromUnicode("✅")).queue();
        message.addReaction(Emoji.fromUnicode("⛔")).queue();
    }

    /**
     * Creates the discussion thread for a suggestion
     *
     * @param msg the posted suggestion message
     */
    public void createDiscussionThread(Message msg) {
        String name = "Discussion: " + title;

        // Trim thread name if it is too long
        if (name.length() > 100) {
            name = name.substring(0, 100);
        }

        msg.createThreadChannel(name).queue();
    }

    /**
     * Checks whether this suggestion has reached its expiration time
     *
     * <p>
     * Suggestions without the required minimum number of votes receive the
     * configured extension before expiring
     * </p>
     *
     * @return whether the suggestion has expired
     */
    public boolean isExpired() {
        Config config = Config.get();

        OffsetDateTime expiresAt = submittedAt.plusDays(config.suggestions().initialDays());

        // Give suggestions with insufficient votes one extension
        if (!hasMinimumVotes()) {
            expiresAt = expiresAt.plusDays(config.suggestions().extensionDays());
        }

        return OffsetDateTime.now().isAfter(expiresAt);
    }

    /**
     * Checks whether this suggestion has received the required number of votes
     *
     * @return whether the suggestion has enough votes
     */
    public boolean hasMinimumVotes() {
        return upvotes + downvotes >= Config.get().suggestions().minimumVotes();
    }

    /**
     * Checks whether the community vote accepts this suggestion
     *
     * @return whether the suggestion has at least as many upvotes as downvotes
     */
    public boolean isAcceptedByVotes() {
        return upvotes >= downvotes;
    }

}