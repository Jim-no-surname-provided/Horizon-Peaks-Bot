package net.horizonpeaks.bot.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Represents the general configuration of the bot.
 *
 * <p>
 * The record components are mapped automatically from properties in
 * {@code config.yaml} using Jackson.
 * </p>
 *
 * @param branding    general Horizon Peaks branding
 * @param servers     Minecraft server addresses
 * @param links       public Horizon Peaks links
 * @param colors      reusable Discord embed colors
 * @param images      reusable image URLs
 * @param channels    Discord channel configuration
 * @param suggestions suggestion system configuration
 */
public record Config(
        Branding branding,
        Servers servers,
        Links links,
        Colors colors,
        Images images,
        Channels channels,
        Suggestions suggestions) {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    /**
     * Represents general Horizon Peaks branding.
     *
     * @param name   network/community name
     * @param banner path to the banner image
     */
    public record Branding(
            String name,
            String banner) {
    }

    /**
     * Represents Minecraft server addresses.
     *
     * @param network  main network address
     * @param smp      SMP server address
     * @param creative Creative server address
     */
    public record Servers(
            String network,
            String smp,
            String creative) {
    }

    /**
     * Represents public Horizon Peaks links.
     *
     * @param website main website URL
     * @param map     world map URL
     * @param vote    voting page URL
     */
    public record Links(
            String website,
            String map,
            String vote) {
    }

    /**
     * Represents reusable Discord embed colors.
     *
     * @param brand   main Horizon Peaks color
     * @param success success and approved color
     * @param error   error and denied color
     * @param vip     VIP color
     * @param booster Discord booster color
     */
    public record Colors(
            String brand,
            String success,
            String error,
            String vip,
            String booster) {
    }

    /**
     * Represents reusable image URLs.
     *
     * @param vip     VIP image URL
     * @param booster Discord booster image URL
     */
    public record Images(
            String vip,
            String booster) {
    }

    /**
     * Represents Discord channels used by the bot.
     *
     * @param welcome             welcome channel ID
     * @param communityInfo       community information channel ID
     * @param activeSuggestions   active suggestions channel ID
     * @param approvedSuggestions approved suggestions channel ID
     * @param deniedSuggestions   denied suggestions channel ID
     */
    public record Channels(
            String welcome,
            String communityInfo,
            String activeSuggestions,
            String approvedSuggestions,
            String deniedSuggestions) {
    }

    /**
     * Represents suggestion system settings.
     *
     * @param initialDays        initial voting period in days
     * @param minimumVotes       minimum total votes required
     * @param firstExtensionDays first extension duration in days
     * @param finalExtensionDays final extension duration in days
     * @param maxActivePerUser   maximum active suggestions per user
     */
    public record Suggestions(
            int initialDays,
            int minimumVotes,
            int firstExtensionDays,
            int finalExtensionDays,
            int maxActivePerUser) {
    }

    /**
     * Parses a YAML string into a {@link Config} instance.
     *
     * <p>
     * YAML property names are automatically matched to record component
     * names by Jackson.
     * </p>
     *
     * @param yaml the YAML configuration to parse
     * @return the parsed configuration
     * @throws IllegalArgumentException if the YAML cannot be parsed as a valid
     *                                  config
     */
    public static Config fromYaml(String yaml) {
        try {
            return MAPPER.readValue(yaml, Config.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid config.yaml", e);
        }
    }
}