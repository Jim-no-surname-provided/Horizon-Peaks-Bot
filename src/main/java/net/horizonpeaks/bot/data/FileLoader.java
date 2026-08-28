package net.horizonpeaks.bot.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

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
     * @param fileName the path of the runtime file
     * @return the path to the existing or newly created runtime file
     * @throws IOException if the bundled resource is missing or the file cannot be
     *                     created
     */
    public static Path getOrCreate(String fileName) throws IOException {
        return getOrCreate(fileName, fileName);
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
     * @return the contents of the runtime file
     * @throws IOException if the file cannot be created or read
     */
    public static String readOrCreate(String fileName) throws IOException {
        return readOrCreate(fileName, fileName);
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

    /**
     * Ensures that a runtime directory exists, copying its bundled resource
     * directory if it does not.
     *
     * @param directoryName   the path of the runtime directory
     * @return {@code true} if the runtime directory already existed, otherwise
     *         {@code false}
     * @throws IOException if the bundled resource is missing or cannot be copied
     */
    public static boolean checkOrCreateDirectory(String directoryName) throws IOException {
        return checkOrCreateDirectory(directoryName, directoryName);
    }

    /**
     * Ensures that a runtime directory exists, copying its bundled resource
     * directory if it does not.
     *
     * @param directoryName   the path of the runtime directory
     * @param defaultResource the bundled resource directory used as its initial
     *                        contents
     * @return {@code true} if the runtime directory already existed, otherwise
     *         {@code false}
     * @throws IOException if the bundled resource is missing or cannot be copied
     */
    public static boolean checkOrCreateDirectory(String directoryName, String defaultResource) throws IOException {
        Path directory = Path.of(directoryName);

        // Keep an existing runtime directory untouched
        if (Files.exists(directory) || DEV) {
            return true;
        }

        URL resource = FileLoader.class.getClassLoader().getResource(defaultResource);

        if (resource == null) {
            throw new IOException("Missing bundled resource directory: " + defaultResource);
        }

        try {
            URI uri = resource.toURI();

            if (uri.getScheme().equals("file")) {
                copyDirectory(Path.of(uri), directory);
            } else if (uri.getScheme().equals("jar")) {
                try (FileSystem fileSystem = FileSystems.newFileSystem(uri, Map.of())) {
                    copyDirectory(fileSystem.getPath(defaultResource), directory);
                }
            } else {
                throw new IOException("Unsupported resource URI: " + uri);
            }
        } catch (URISyntaxException e) {
            throw new IOException("Could not load bundled resource directory: " + defaultResource, e);
        }

        return false;
    }

    /**
     * Recursively copies one directory to another.
     *
     * @param source the source directory
     * @param target the target directory
     * @throws IOException if the directory cannot be copied
     */
    private static void copyDirectory(Path source, Path target) throws IOException {
        try (Stream<Path> paths = Files.walk(source)) {
            for (Path path : paths.toList()) {
                Path destination = target.resolve(source.relativize(path).toString());

                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Files.copy(path, destination);
                }
            }
        }
    }
}