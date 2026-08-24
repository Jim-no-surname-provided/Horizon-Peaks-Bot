package net.horizonpeaks.bot;

import java.util.Scanner;

import net.dv8tion.jda.api.JDA;
import net.horizonpeaks.bot.actions.suggestions.Reconciliation;

/**
 * Listens for commands entered into the bot console.
 */
public final class ConsoleListener {

    private ConsoleListener() {
    }

    /**
     * Starts listening for console commands on a separate thread.
     */
    public static void start(JDA jda) {
        // Create console listener thread
        Thread thread = new Thread(() -> {
            // Open scanner in try-resources
            try (Scanner scanner = new Scanner(System.in)) {
                // Wait for console input
                while (scanner.hasNextLine()) {
                    // Read input
                    String command = scanner.nextLine();
                    
                    // If input equals "reconcile" call Reconciliation
                    if (command.equalsIgnoreCase("reconcile")) {
                        Reconciliation.run(jda);
                    }
                }
            }
        });

        // Do not keep the JVM alive for this thread
        thread.setDaemon(true);
        // Start thread
        thread.start();
    }
}