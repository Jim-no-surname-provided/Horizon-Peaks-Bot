# Horizon Peaks Bot - Replication Specification

The replacement should reproduce the **user-facing behavior and appearance** of the existing bot, without reproducing its Sapphire/Bun/BullMQ/Redis architecture.

## 1. Overall visual identity

The bot consistently presents itself as part of **Horizon Peaks**.

### Brand values

- Network name: `Horizon Peaks`
- SMP name: `HorizonPeaksSMP`
- Main brand/embed color: `#11806A`
- Success/approved color: `#57F287`
- Denied/error color: `#ED4245`
- VIP color: `#F1C40F`
- Booster color: approximately `#CC377E`

### General embed style

The bot mostly uses standard Discord embeds with:

- a colored left border
- relatively short titles
- emoji in titles only where appropriate
- Markdown bold for emphasis
- code blocks for values intended to be copied
- thumbnails for user/avatar or perk imagery
- large images only for banners/content images
- footer containing Horizon Peaks or contextual metadata

There is no elaborate custom UI. The design is mostly Discord-native and clean.

### Brand banner

The existing `/info` and rules use the same large Horizon Peaks banner image.

The current URL points to a Discord CDN attachment and should ideally be replaced in the new bot by either:

- a stable URL we control, or
- an image shipped with the bot and uploaded as an attachment

Do not rely permanently on the current temporary-looking Discord CDN URL.

---

# 2. Configuration

The new bot should put almost all editable values in YAML rather than source code.

Configuration should include:

- Discord token, preferably kept separately in `.env`
- guild/server ID
- channel IDs
- server addresses
- website/map/vote URLs
- brand name
- colors
- suggestion settings
- permissions
- welcome text
- reusable post definitions

The Discord token should **not** be in YAML committed to Git.

Conceptually:

`config.yaml`
- branding
- channels
- links
- servers
- suggestions
- permissions

`commands.yaml`
- simple informational commands and their embeds

`posts/`
- rules
- perks
- announcements
- changelogs

Only functionality requiring actual behavior should live in Python.

---

# 3. `/info` and legacy `!info` / `!ip`

## Purpose

Posts the main server/network information.

## Current permissions

There are actually two different behaviors.

### Slash command `/info`

- Administrator-only
- currently incorrectly restricted further to `BOT_OWNER_IDS`
- should be changed to **Administrator-only**

### Prefix commands `!info` and `!ip`

- available to normal users
- reply only with the server-address embed
- do **not** post the banner or website embed

This inconsistency appears accidental.

When rebuilding, decide whether prefix commands are worth keeping at all.

## Current `/info` output

Running `/info` sends **two public messages** to the current channel.

The command interaction itself receives an ephemeral success confirmation.

### Public message 1

Contains two embeds.

#### Embed A - banner

- no title
- no description
- main Horizon Peaks color
- large Horizon Peaks banner image

#### Embed B - Server Details

Title:

`Horizon Peaks - Server Details`

Color:

Horizon Peaks main teal.

Fields, each displayed vertically:

**Network IP**

```text
play.horizon-peaks.net
```

**SMP IP**

```text
smp.horizon-peaks.net
```

**Creative IP**

```text
creative.horizon-peaks.net
```

**World Map**

```text
map.horizon-peaks.net
```

The values are deliberately inside Discord code blocks so they are visually prominent and easy to copy.

### Public message 2

One embed.

Title:

`🌐 Horizon Peaks - Website`

The title itself links to the website.

Description:

`Visit our website!`

followed by a bold clickable link to:

`horizon-peaks.net`

Color:

Horizon Peaks teal.

### Private confirmation

`✅ Server info posted successfully!`

---

# 4. `/map` and `!map`

## Availability

- `/map`: public
- `!map`: public

## Appearance

Single embed.

Title:

`🗺️ Horizon Peaks - World Map`

The title links directly to the map.

Description:

`Explore the world map!`

followed by a bold clickable `map.horizon-peaks.net` link.

Color:

Horizon Peaks teal.

---

# 5. `/vote` and `!vote`

## Availability

- `/vote`: public
- `!vote`: public

## Appearance

Single embed.

Title:

`🗳️ Horizon Peaks - Vote`

Title links to the voting page.

Description:

`Support the server by voting!`

followed by a bold clickable `horizon-peaks.net/vote` link.

Color:

Horizon Peaks teal.

---

# 6. `!verify` / `!link`

There is currently **no slash command provided by this bot** for this feature.

`!verify` and alias `!link` are prefix commands.

This is slightly confusing because the instructions inside the resulting embed tell the user to execute `/link`, apparently provided by the Minecraft/Discord linking bot.

## Appearance

Title:

`Horizon Peaks - Verification Steps`

Color:

Horizon Peaks teal.

### Fields

**1. Log into the server**

Explains that the user should connect using the network or SMP address and shows:

```text
play.horizon-peaks.net
```

**2. Get the verification code**

Explains that the first failed login gives them a verification code in the error message.

**3. Use the linking command with the provided code**

Tells them to run `/link` in Discord using the provided code.

**4. Log into the SMP**

Tells the user to reconnect and states that they are ready.

Then there is an empty spacer field.

**⚠️ Important**

Explains that there are two linking bots/server linking commands and that the user must use the appropriate one.

Footer:

`Horizon Peaks`

## Note for rewrite

The wording is probably outdated relative to the current server setup and should be verified before copying literally.

The **layout/style** should nevertheless be preserved unless intentionally redesigned.

---

# 7. `!help` / `!commands`

Prefix only.

Aliases:

- `!help`
- `!commands`

## Appearance

Title:

`Horizon Peaks - Commands`

Color:

Horizon Peaks teal.

Fields:

- `!info / !ip` - Show the server IPs and world map
- `!verify / !link` - Show the verification guide
- `!ping` - Check bot latency
- `!help` - Show this help message

Footer:

`Prefix: !`

## Existing bug/inconsistency

The bot also supports `!map` and `!vote`, but the help embed doesn't list them.

This should be corrected if prefix commands survive the rewrite.

Ideally the new help command should derive its contents automatically from command configuration instead of maintaining a second manually written list.

---

# 8. `!ping`

Prefix only.

The bot first replies:

`Pinging...`

Then edits that message into:

`Pong! Latency: **Xms** | API: **Yms**`

Where:

- `Latency` measures the command/reply round-trip
- `API` is Discord websocket latency

No embed.

---

# 9. New-member welcome

Triggered whenever a Discord member joins.

It posts an embed to the configured welcome channel.

## Appearance

Title:

`Welcome to Horizon Peaks! 🏔️`

Description begins by directly mentioning the new member:

`Welcome aboard, @member! 🎉`

Then:

`We're thrilled to have you join our Horizon Peaks community!`

### Getting Started

Points them to the configured community-information channel using a real Discord channel mention.

### ⚠️ Whitelisting

Explains:

- a staff member has to whitelist them
- it is usually done before acceptance
- it may take several minutes
- contact staff if there are issues

Then:

`Looking forward to seeing you in-game! 🚀`

Finally a visually subdued tip:

`💡 Tip: Use the !help command for a list of available commands.`

## Thumbnail

The new member's Discord avatar.

## Footer

`Member #N`

where N is the guild's current member count.

## Timestamp

Current time.

## Color

Discord-style green `#57F287`.

---

# 10. Welcome-channel command blocking

If somebody uses a **prefix command** such as `!help` in the configured welcome channel, the bot responds:

`Commands are disabled in this channel.`

Important detail:

It does **not actually delete or block the original message** and doesn't inherently prevent Sapphire from processing it.

It merely sends this warning.

The replacement should either deliberately preserve this behavior or implement actual command suppression if that was the intended behavior.

---

# 11. `/rules`

Administrator-only.

Posts the server rules to the channel where the command is executed.

The command gives the administrator an ephemeral:

`✅ Rules posted successfully!`

## Structure

It currently sends the rules as **two Discord messages containing multiple embeds**.

The content itself lives in JSON rather than code.

This is a good concept and should absolutely survive the rewrite.

## Styling

Main rules color:

decimal `1146986`, equivalent to Horizon Peaks teal `#11806A`.

The first embed is only the large Horizon Peaks banner.

Subsequent embeds are individual rule sections.

Examples:

- General Statement
- Chat | Rule 1
- Griefing/Stealing | Rule 2
- Exploits | Rule 3
- Hacking/Banned Mods | Rule 4
- further rules through Rule 7
- Voice Chat | Rule 7

The actual rule text uses Discord `ansi` code blocks heavily to provide colored headings and text.

This formatting is therefore part of the current visual style and should be preserved if exact visual replication matters.

Some sections also use embed fields such as `Allowed Exceptions`.

## Replacement architecture

Rules should remain entirely declarative.

For example:

`posts/rules.yaml`

The Python should know only:

> load a post definition and render its messages/embeds

It should know nothing about the individual rules.

---

# 12. `/vip-perks`

Administrator-only.

Currently contains the same accidental owner-ID restriction as `/info`.

It should be Administrator-only.

Posts an embed to the current channel and responds privately:

`✅ VIP perks posted successfully!`

## Appearance

Title:

`VIP Perks!`

Color:

yellow/gold `#F1C40F`

Thumbnail:

custom VIP image.

The beginning of the description is an ANSI yellow introductory line:

`Awarded to players who win an event, competition, or contribute to the server in a significant way`

Then perk entries:

- 🏷️ **VIP Rank**
- ✏️ **One Time Nickname Change**
- 🎨 **Optional Name Colour**
- ✨ **Optional Player Glow**
- 🔧 **Axiom Access**
- 🔒 **VIP Lounge Access**

Each perk can have a smaller descriptive line underneath it.

Content currently comes from JSON and should remain configurable data.

---

# 13. `/booster-perks`

Administrator-only.

Currently has the same mistaken owner-only second check.

Should simply be Administrator-only.

Private confirmation:

`✅ Booster perks posted successfully!`

## Appearance

Title:

`New Discord Booster Perks!`

Color:

pink/magenta `#CC377E`

Thumbnail:

Discord boost icon.

Begins with an ANSI magenta introductory phrase:

`Unlock these perks by boosting the server`

Lists:

- Booster Rank
- One Time Nickname Change
- Optional Name Colour
- Optional Player Glow
- Axiom Access

Then a visual text separator and:

`📩 Message me to get the perks once the boost was applied to the server.`

Footer:

`Monthly based • perks will be removed if the boost has ended`

---

# 14. `/announce name:<name>`

Administrator-only.

Loads a named announcement definition from:

`announcements/<name>.json`

and posts it in the current channel.

Example:

`/announce name:partyz`

loads the Partyz announcement.

## Behavior

- filename is sanitized
- announcement may contain multiple embeds
- Discord's maximum 10 embeds per message is respected by batching
- administrator gets an ephemeral success/error response

Success:

`✅ Announcement "partyz" posted!`

## Announcement format

An announcement embed supports:

- title
- description
- color
- image
- thumbnail
- footer
- fields
- inline fields

This is exactly the kind of system we should generalize in the rewrite.

---

# 15. Partyz announcement

The only announcement currently included is `partyz`.

Title:

`🎉 Introducing Partyz - Private Party Chat`

Color:

Horizon Peaks teal.

Description explains what Partyz does.

Fields:

### 📋 How It Works

Numbered usage instructions.

### 🔧 Commands

A Discord code block listing Partyz commands.

### 👁️ Staff Visibility Notice

Explains that staff can see and log party chats and that server rules still apply.

Footer:

`Horizon Peaks - Partyz is an in-house mod exclusive to this server`

There is no special Python logic for Partyz. It is purely a configurable announcement.

---

# 16. `/changelog version:<version>`

Administrator-only.

Loads:

`changelogs/<version>.json`

and posts the relevant changelog into the current channel.

Examples currently included:

- `1.0.0`
- `1.1.0`

## Changelog definition

Each changelog contains:

- version
- date
- title
- optional summary

Then it supports two formats.

### Current grouped format

`sections`

Each section contains:

- heading
- list of items

### Legacy flat format

`changes`

Each change contains:

- type
- description

Types map to:

- added → ✅
- changed → 🔄
- fixed → 🐛
- removed → ❌
- unknown → `•`

## Grouped changelog appearance

First embed:

Title:

`Horizon Peaks | <changelog title>`

Description:

summary

Color:

Horizon Peaks teal.

Then **one embed per section**.

Each section's heading becomes the embed title.

Items appear as:

`• item`

The final embed gets footer:

`Version X.Y.Z • Month Day, Year`

## Important

Discord allows only 10 embeds per message.

If the changelog produces more than 10, the bot sends multiple messages.

Private confirmation:

`✅ Changelog vX.Y.Z posted!`

---

# 17. `/changelog-update version:<version>`

Administrator-only.

Used after editing an existing changelog file.

Instead of posting a duplicate, it tries to find the existing changelog in the **current Discord channel** and update it in place.

## Identification

It searches the latest 100 messages.

Only messages written by this bot count.

It recognizes matching changelogs through either:

- footer beginning `Version <version>`
- matching changelog title

## Update behavior

If the new changelog needs the same number of Discord messages:

- edit them

If it needs additional messages:

- edit existing messages
- send additional ones

If it now needs fewer:

- edit needed messages
- delete excess old ones

If none can be found:

`❌ No posted changelog vX.Y.Z found in this channel.`

This behavior is worth reproducing.

A more robust replacement could save the Discord message IDs in SQLite rather than scanning the latest 100 messages.

---

# 18. Suggestion channel protection

There are three special channels:

- active suggestions
- approved suggestions
- denied suggestions

If a normal user sends a message directly into any of those three channels:

- the bot deletes it

Exceptions:

- messages written by bots
- messages inside threads

The explicit thread exception exists because each suggestion has its own discussion thread.

---

# 19. `/suggestion`

Public slash command.

This is the largest interactive feature.

## Active suggestion limit

Each Discord user may have at most:

**2 active suggestions**

If they already have two:

`You already have 2 active suggestions. Please wait for one to be resolved before submitting another.`

The response is ephemeral.

---

# 20. Suggestion disclaimer screen

Running `/suggestion` does not immediately show the form.

It first displays an ephemeral disclaimer.

Content:

**Before submitting, please note:**

- suggestions initially last 7 days
- if insufficient votes, they can be extended 3 days and then 1 day
- minimum 7 votes required by default
- maximum 2 active suggestions
- admins can veto/reject at any time
- denied suggestions may be resubmitted, but users must not spam

The exact durations and vote threshold are generated from configuration rather than hardcoded in the prose.

There is one blue Discord button:

`Continue to Suggestion Form`

The button works for 60 seconds.

Pressing it opens the suggestion modal and removes the ephemeral disclaimer.

---

# 21. Suggestion form

Modal title:

`Submit a Suggestion`

Contains four inputs.

### Title

- required
- short input
- max 100 characters
- placeholder: `A short title for your suggestion`

### Description

- required
- paragraph input
- max 1024 characters
- placeholder: `Describe your suggestion in detail`

### Image URL (optional)

- short input
- max 256 characters
- placeholder: `https://example.com/image.png`

If supplied, it must parse as a valid URL.

### Examples (optional)

- paragraph input
- max 1024 characters
- placeholder: `Text examples or links to illustrate your idea`

---

# 22. Suggestion IDs

Every submitted suggestion gets a monotonically increasing ID:

`S-0001`

then:

`S-0002`

etc.

The existing bot stores this counter in Redis.

The replacement should store the next number in SQLite.

IDs should never be reused after restart or after a suggestion is resolved.

---

# 23. Newly posted suggestion appearance

After submission, the bot posts one embed in the configured active-suggestions channel.

## Title

Exactly the user-provided suggestion title.

## Color

Horizon Peaks teal / pending color:

`#11806A`

## Description

Exactly the user's suggestion description.

## Thumbnail

Submitter's Discord avatar.

## Optional image

The supplied image URL.

## Optional field

**Examples**

Containing the submitted examples.

## Fields

**Author**

User's current Discord display name.

Inline.

**Status**

`**Active**`

Inline.

## Footer

Conceptually:

`S-0001 | Expires March 14, 2026 at 3:48 PM | Horizon Peaks`

During extensions, text is appended:

`(First Extension)`

or:

`(Final Extension)`

## Timestamp

Original submission time.

---

# 24. Suggestion voting

Immediately after posting, the bot adds two reactions:

- ✅
- ⛔

The bot's own reactions are ignored when counting votes.

## Exclusive voting

A user may vote only one way.

If somebody who has already reacted ✅ adds ⛔:

- their ✅ reaction is automatically removed

And vice versa.

Users may also remove their vote completely.

---

# 25. Suggestion discussion threads

Immediately after a suggestion is posted, the bot creates a thread from it.

Thread name:

`Discussion: <suggestion title>`

The title is truncated so the resulting thread name fits Discord's limit.

Normal conversation is allowed in this thread even though messages are forbidden directly inside the active-suggestions channel.

---

# 26. Automatic suggestion resolution

Default configuration:

- initial period: 7 days
- minimum total votes: 7
- first extension: 3 days
- final extension: 1 day

Total votes means:

`upvotes + downvotes`

not unique activity beyond the enforced one-vote-per-user behavior.

## After 7 days

If there are at least 7 votes:

- more ✅ than ⛔ → approved
- otherwise → denied

Important edge case:

A tie is **denied** because approval requires strictly:

`upvotes > downvotes`

## If fewer than 7 votes

After the first 7 days:

- extend by 3 days

After those 3 days:

- if still insufficient, extend by 1 additional day

After that final day:

- if still fewer than 7 votes, deny it

Reason:

`Insufficient votes`

---

# 27. Automatically approved suggestion appearance

The original active suggestion message is deleted.

A new embed is posted into the approved-suggestions channel.

## Title

`✅ <suggestion title>`

## Color

green `#57F287`

## Description

Original description.

## Thumbnail/image/examples

Preserved.

## Fields

**Author**

original display name.

**Status**

`**Approved**`

**Votes**

For example:

`✅ 8 (80%) | ⛔ 2 (20%)`

**Submitted**

Discord full timestamp.

**Resolved**

Discord full timestamp.

## Footer

`S-0001 | Horizon Peaks`

## Timestamp

Time the resolved embed is created.

---

# 28. Automatically denied suggestion appearance

Same structure as approved, except:

Title:

`❌ <suggestion title>`

Color:

red `#ED4245`

Status:

`**Denied**`

Reason field added when appropriate.

Possible automatic reasons:

- `Downvoted by community`
- `Insufficient votes`

---

# 29. `/suggestion-approve`

Administrator-only.

Arguments:

- `suggestion_id` - required, such as `S-0001`
- `reason` - optional

Default reason:

`Admin approval`

## Behavior

- finds the currently active suggestion
- reads current reaction counts
- removes its scheduled resolution
- deletes the active suggestion message
- posts it immediately to the approved channel
- retains the current vote results
- includes the admin reason

Private confirmation:

`Suggestion S-0001 has been approved.`

---

# 30. `/suggestion-reject`

Administrator-only.

Arguments:

- `suggestion_id` - required
- `reason` - optional

Default reason:

`Admin rejection`

Same behavior as manual approval, except it moves the result to the denied channel.

Private confirmation:

`Suggestion S-0001 has been rejected.`

---

# 31. `/suggestion-info`

Administrator-only.

Posts and pins a permanent explanatory embed about the suggestion system in the current channel.

## Appearance

Title:

`📋 Community Suggestions`

Color:

suggestion teal.

Description:

`Have an idea to improve the server?`
`Submit a suggestion and let the community vote on it!`

### How to Submit

Explains `/suggestion` and the form.

### Voting

Explains:

- ✅ support
- ⛔ disagree
- only one vote direction
- switching automatically removes the other reaction

### How Suggestions Are Resolved

Dynamically describes:

- initial duration
- minimum votes
- extensions
- insufficient-vote rejection
- majority voting

### Rules

- maximum two active suggestions
- admins may approve/reject
- denied suggestions may be resubmitted without spamming
- use discussion threads

Footer:

`Horizon Peaks`

Timestamp:

current time.

The bot then attempts to **pin this message**.

Private confirmation:

`Suggestion info embed sent and pinned!`

---

# 32. `/suggestion-repurpose`

Administrator-only.

Allows an administrator to take a previously approved **or** denied suggestion and place it back into active voting.

Argument:

`suggestion_id`

Example:

`S-0001`

## Lookup

The existing implementation searches only the **latest 100 messages** of:

- approved channel
- denied channel

It identifies suggestions by finding the ID in the embed footer.

The replacement should instead store resolved suggestions in SQLite, eliminating this limit.

## Behavior

- finds the resolved suggestion
- retains the same suggestion ID
- reconstructs its title, description, author display name, avatar, image and examples
- deletes the old approved/denied message
- reposts it into active suggestions
- adds ✅ and ⛔
- starts another discussion thread

## Special voting period

Repurposed suggestions get:

**3 days**

They receive:

**no extensions**

The existing implementation achieves this by marking them internally as already fully extended.

Private confirmation states that it was re-suggested for three days and that no extensions will be granted.

---

# 33. Suggestion persistence/restart behavior

The existing implementation attempts to survive restarts through BullMQ/Redis.

It also has a fallback recovery mechanism.

Every five minutes, and once shortly after startup, it:

- scans the latest 100 active-suggestion messages
- reads their expiry dates from embed footers
- checks whether expired suggestions still exist
- recreates missing resolution jobs

This is complicated because state is spread between Discord embeds and Redis jobs.

## Replacement requirement

Use SQLite as the source of truth.

Each suggestion should persist at least:

- numeric suggestion ID
- Discord message ID
- Discord thread ID if useful
- author Discord ID
- author's display name at submission
- author avatar URL
- title
- description
- image URL
- examples
- submission time
- expiration time
- extension number
- current status
- whether it is a repurposed suggestion
- resolution time
- resolution reason

Then a simple background loop can periodically execute:

> Find active suggestions whose `expires_at <= now`, and resolve them.

No Redis.

No queue server.

No recovery parsing from human-readable Discord footer text.

---

# 34. Current channels required by the bot

The current configuration expects IDs for:

- welcome channel
- rules channel
- general channel
- patch notes channel
- community info channel
- active suggestions channel
- approved suggestions channel
- denied suggestions channel

Not all of them are actually used by current command code.

Notably, the rules and changelog commands post to **the channel where the administrator executes them**, rather than automatically using `RULES_CHANNEL_ID` or `PATCH_NOTES_CHANNEL_ID`.

Those two configured IDs therefore appear to be remnants or planned functionality.

We shouldn't replicate unused configuration unless we decide to make those commands target fixed channels.

---

# 35. Current network links

Currently hardcoded:

Network:

`play.horizon-peaks.net`

SMP:

`smp.horizon-peaks.net`

Creative:

`creative.horizon-peaks.net`

Website:

`https://horizon-peaks.net`

Vote:

`https://horizon-peaks.net/vote`

Map:

`https://map.horizon-peaks.net`

These belong in configuration in the replacement.

---

# 36. Existing unused/placeholder configuration

There are constants for:

- title image
- verification GIF
- verification guide

whose URLs are literally placeholder URLs.

They are not actually part of the currently displayed verification command.

Do not treat these placeholders as functionality that needs to be reproduced.

---

# 37. Admin permission model

The intended model appears to be:

### Public

- `/map`
- `/vote`
- `/suggestion`
- prefix informational commands

### Administrators

- `/info`
- `/rules`
- `/vip-perks`
- `/booster-perks`
- `/announce`
- `/changelog`
- `/changelog-update`
- `/suggestion-info`
- `/suggestion-approve`
- `/suggestion-reject`
- `/suggestion-repurpose`

### Owner-only

There does not appear to be a feature that actually **needs** to be owner-only.

The existing owner-only checks on `/info`, `/vip-perks`, and `/booster-perks` look accidental/redundant rather than a coherent security model.

The rewrite should remove the concept of bot-owner IDs unless we later find a genuine use for it.

---

# 38. Recommended replacement architecture

Use:

- **Python 3.12+**
- **discord.py**
- **PyYAML**
- **SQLite**
- Python's normal logging
- optionally `python-dotenv` for the token

No Node.js.

No TypeScript.

No Sapphire.

No Bun.

No Redis.

No BullMQ.

No compilation/build directory.

## Suggested project structure

`bot.py`

Starts the bot and registers feature modules.

`config.yaml`

Server IDs, channels, links, branding, colors and general settings.

`.env`

Discord token only.

`commands.yaml`

Simple commands whose behavior is only “render this configured Discord response”.

`posts/`

Contains things such as:

- `rules.yaml`
- `vip-perks.yaml`
- `booster-perks.yaml`
- `announcements/partyz.yaml`
- `changelogs/1.1.0.yaml`

`suggestions.py`

All actual suggestion-system behavior.

`database.py`

Very small SQLite access layer.

`events.py`

Welcome message and protected-channel behavior.

`renderer.py`

Generic conversion of YAML descriptions into Discord messages/embeds.

---

# 39. Declarative command goal

A simple command should not require a Python source file.

For example, conceptually, `/map` should be describable entirely through configuration:

- name: map
- description: Show the world map link
- permissions: everyone
- response:
  - embed:
    - title
    - title URL
    - description
    - color

Likewise `/info`, `/vote`, rules, perks and announcements should mostly be data.

Python should be necessary only for things involving actual behavior:

- suggestion lifecycle
- member joins
- voting exclusivity
- admin actions
- changelog updating
- perhaps latency/ping

This is the biggest architectural principle I'd preserve for the new bot:

**If changing the wording or appearance of a command requires editing Python, we should first ask whether that command should simply be configurable data instead.**

A couple of things jump out from doing this inventory.

First, **the old bot already wanted to be config-driven**. Rules, perks, announcements, and changelogs are JSON. The original author just didn't take that idea far enough. We can make one generic Discord-message renderer and let YAML describe almost everything.

Second, I would probably reconsider the old `!` commands entirely. Discord slash commands give us descriptions, autocomplete, proper permissions, and discoverability. The old bot is in a weird halfway state where `/map` exists but `/ping` doesn't, `/info` behaves differently from `!info`, and `!help` forgets commands that actually exist. A clean rebuild is a good opportunity to make **slash commands canonical**, while keeping aliases only if there's a genuine reason.

And third, SQLite makes the supposedly scary suggestion system pretty mundane. We don't need scheduled jobs at all: every ~30-60 seconds, query `WHERE status = 'active' AND expires_at <= now`, resolve anything returned, then go back to sleep. That's robust even if the bot is offline for six hours-the overdue suggestion gets processed after startup. No Redis daemon, no job resurrection, no parsing dates back out of Discord embeds.

If we build it that way, I think this bot can become **small enough that you can understand essentially the entire architecture at a glance**, even without much Python Discord experience.
