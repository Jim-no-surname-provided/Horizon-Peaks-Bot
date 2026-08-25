package net.horizonpeaks.bot.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class for loading runtime files from disk and creating them
 * from bundled default resources when they do not yet exist.
 */
public final class FileLoader {

    private static final boolean DEV = Boolean.getBoolean("horizon.dev");

    private FileLoader() {
    }

    /**
     * Returns the requested runtime file, creating it from a bundled
     * resource if it does not already exist.
     *
     * <p>
     * If the target file is inside a directory that does not yet exist,
     * the required parent directories are created automatically.
     * </p>
     *
     * @param fileName        the path of the runtime file
     * @param defaultResource the bundled resource used as the initial file contents
     * @return the path to the existing or newly created runtime file
     * @throws IOException if the bundled resource is missing or the file cannot be
     *                     created
     */
    public static Path getOrCreate(String fileName, String defaultResource) throws IOException {
        Path file = Path.of(fileName);

        // Always use an existing runtime file
        if (Files.exists(file)) {
            return file;
        }

        // During development, read the bundled resource directly
        if (DEV) {
            try {
                var resource = FileLoader.class.getClassLoader().getResource(defaultResource);

                if (resource == null) {
                    throw new IOException("Missing bundled resource: " + defaultResource);
                }

                return Path.of(resource.toURI());

            } catch (Exception e) {
                throw new IOException("Could not load development resource: " + defaultResource, e);
            }
        }

        // In production, create the runtime file from the bundled default
        try (InputStream input = FileLoader.class
                .getClassLoader()
                .getResourceAsStream(defaultResource)) {

            if (input == null) {
                throw new IOException("Missing bundled resource: " + defaultResource);
            }

            Path parent = file.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.copy(input, file);
        }

        return file;
    }

    /**
     * Reads the requested runtime file as a string, creating it from a
     * bundled resource first if it does not already exist.
     *
     * @param fileName        the path of the runtime file
     * @param defaultResource the bundled resource used as the initial file contents
     * @return the contents of the runtime file
     * @throws IOException if the file cannot be created or read
     */
    public static String readOrCreate(String fileName, String defaultResource) throws IOException {
        return Files.readString(getOrCreate(fileName, defaultResource));
    }
}