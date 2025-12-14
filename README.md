# slackcat-kt

Slackcat but strongly typed

## Using Slackcat as a Library

You can include slackcat-kt in your own project as a dependency from GitHub Packages.

### 1. Configure GitHub Packages Repository

Add the GitHub Packages repository to your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/konecnyna/slackcat-kt")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

Or add it directly in your `build.gradle.kts`:

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/konecnyna/slackcat-kt")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}
```

### 2. Add Dependencies

Add slackcat modules to your `build.gradle.kts`:

```kotlin
dependencies {
    // Core library
    implementation("com.slackcat:slackcat:0.0.1")

    // Optional: Pre-built modules (weather, crypto, emoji, etc.)
    implementation("com.slackcat:slackcat-modules:0.0.1")
}
```

### 3. Configure Credentials

Create `~/.gradle/gradle.properties` (in your home directory):

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.token=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
```

**To create a GitHub Personal Access Token:**
1. Go to GitHub Settings → Developer settings → Personal access tokens
2. Create a token with `read:packages` scope
3. Copy the token and add it to your gradle.properties

### 4. Basic Usage Example

```kotlin
import com.slackcat.SlackcatBot
import com.slackcat.common.SlackcatConfig

fun main() {
    val config = SlackcatConfig(
        slackAppToken = System.getenv("SLACK_APP_TOKEN"),
        slackBotToken = System.getenv("SLACK_BOT_TOKEN")
    )

    val bot = SlackcatBot(config)
    bot.start()
}
```

For detailed publishing and consumption instructions, see:
- `PUBLISHING.md` - Complete guide to publishing and consuming the library
- `VERSIONING.md` - Version management and release process

## Note

The`*Graphs.kt` classes are a very simple global singleton dependency graph.
For a real app, you would use something like Dagger instead.

## Run locally

You can run commands locally without using SlackRTM webscoket by adding your command to the program arguments in the run config

For instance if you add `?ping` to program arguments you should get

```shell
Cli is connected
Starting slackcat using Cli engine
Incoming message: ?ping
Outgoing message: OutgoingChatMessage(channelId=123456789, text=pong)
```


# Setup
## Add env vars

1. Edit run configuration
2. Open environmental variables
3. add
    * SLACK_APP_TOKEN
    * SLACK_BOT_TOKEN
4. Profit.


## Slack Workspace setup:

### Enable Event Subscriptions

1. Go to your Slack app's dashboard.
2. Navigate to **Features** > **Event Subscriptions**.
3. Toggle **Enable Events** to **On**.

### Subscribe to Bot Events

1. In the **Event Subscriptions** page, scroll down to **Subscribe to bot events**.
2. Click on **Add Bot User Event**.
3. Add the following events based on your needs:
    - `message.channels` (for messages in public channels)
    - `message.im` (for direct messages)
    - `message.groups` (for private channels)
    - `message.mpim` (for group direct messages)
    - `reaction_added` (for emoji reactions added to messages)
    - `reaction_removed` (for emoji reactions removed from messages)

### Set Up OAuth Scopes

1. Navigate to **OAuth & Permissions** in your app's dashboard.
2. In the **Scopes** section, under **Bot Token Scopes**, add the following scopes:
    - `channels:history` (to read messages in channels)
    - `groups:history` (for private channels)
    - `im:history` (for direct messages)
    - `mpim:history` (for group direct messages)
    - `channels:join` (if your bot needs to join channels automatically)
    - `chat:write` (to send messages)
    - `reactions:read` (to receive reaction events like reaction_added and reaction_removed)
    - `users:read` (to fetch user display names for kudos and other features)

### Reinstall the App

1. After making changes to scopes and event subscriptions, you need to reinstall the app to your workspace for the changes to take effect.
2. Go to **OAuth & Permissions** and click **Install App to Workspace**.
3. Approve the new permissions when prompted.

### Add the Bot to Channels

1. Ensure that your bot is a member of the channels where it needs to listen to messages.
2. You can invite the bot to a channel by typing `/invite @YourBotName` in Slack.

## Enabling Reaction Features

To enable reaction-based features (like giving kudos with `:heavy_plus_sign:`), follow these steps:

### 1. Add Event Subscriptions
1. Go to your Slack app's dashboard at https://api.slack.com/apps
2. Navigate to **Event Subscriptions**
3. Ensure the following bot events are subscribed:
   - `reaction_added`
   - `reaction_removed`

### 2. Add OAuth Scope
1. Navigate to **OAuth & Permissions**
2. Under **Bot Token Scopes**, ensure you have:
   - `reactions:read` (required to receive reaction events)

### 3. Reinstall the App
1. Go to **OAuth & Permissions**
2. Click **Reinstall App to Workspace**
3. Approve the new `reactions:read` permission

### 4. Restart Your Bot
After updating permissions, restart your bot to apply changes:
```bash
docker compose down && docker compose up --build -d
```

### Verify It's Working
- Add a `:heavy_plus_sign:` (➕) reaction to any message in a channel where the bot is present
- The bot should respond with a kudos message for the message author
- Check logs if not working: `docker logs slack-bot --tail 50`



# Docker

```shell
docker-compose --env-file path/to/your/.env up --build
```

Testing
```shell
docker-compose up --build -d && docker exec -it slack-bot /bin/sh
```


## Dump Databse

```bash
docker compose exec -T postgres \
  pg_dump -U newuser -d slackcatdb \
| gzip > slackcatdb_$(date +%F).sql.gz
```

## Restore Database

```bash
docker-compose exec postgres \
  psql -U newuser -d postgres \
  -c "SELECT pg_terminate_backend(pid)
      FROM pg_stat_activity
      WHERE datname='slackcatdb'
        AND pid <> pg_backend_pid();"

# Drop the DB (runs outside a transaction)
docker-compose exec postgres \
  dropdb -U newuser slackcatdb

# Recreate it, owned by newuser
docker-compose exec postgres \
  createdb -U newuser -O newuser slackcatdb
  
gunzip -c slackcatdb_2025-05-27.sql.gz \
  | docker compose exec -T postgres \
      psql -U newuser -d slackcatdb
```
