# Horizon Peaks Discord Bot

Minimal Java + Maven + JDA starter for the Horizon Peaks Discord bot.

## Requirements

- Java 21
- Maven
- A Discord application with a bot user
- The bot invited to your Discord server

## Secrets and environment

Secrets are loaded at runtime, not compiled into the JAR.

The bot supports two ways of providing configuration values:

1. **Operating-system environment variables** — the same mechanism used for variables such as `PATH` or `JAVA_HOME`.
2. **A local `.env` file** — a plain-text file read by the `dotenv-java` library.

The bot checks real operating-system environment variables first. If a value is not defined there, it falls back to `.env`.

For local development, copy `.env.example` to `.env` and fill in:

```text
DISCORD_TOKEN=your_real_bot_token
DISCORD_GUILD_ID=your_server_id
```

`.env` is ignored by Git and should never be committed.

A `.env` file does **not** become part of the compiled JAR. It is read when the bot starts.

For production hosting, either approach is valid:

- If the host supports custom environment variables, define `DISCORD_TOKEN` and `DISCORD_GUILD_ID` there.
- Otherwise, place a private `.env` file alongside the bot files.

Because real environment variables take priority, the same JAR can be used locally and in production without rebuilding it or embedding secrets.

## Build

```bash
mvn package
```

This creates:

```text
target/horizon-peaks-bot.jar
```

## Run

From the repository root:

```bash
java -jar target/horizon-peaks-bot.jar
```

For local development, keep `.env` in the repository root so dotenv-java can find it.

## Current behavior

On startup the bot:

1. Loads `DISCORD_TOKEN` and `DISCORD_GUILD_ID`.
2. Connects to Discord.
3. Registers a guild-scoped `/ping` command.
4. Replies `Pong!` when `/ping` is used.

This is intentionally minimal. The next layer will be the YAML-driven command system.
