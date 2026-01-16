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

## Customizing Modules

Slackcat modules are designed to be customizable through inheritance. You can override module behavior by extending the module class and overriding protected properties or methods.

### Example: Customizing KudosModule

The `KudosModule` includes spam protection by default (5-minute cooldown between giving kudos to the same user). You can disable this by creating a custom module:

```kotlin
class CustomKudosModule : KudosModule() {
    override val spamProtectionEnabled = false  // Disable spam protection
}
```

Then register your custom module instead of the default:

```kotlin
// In your DI configuration (e.g., Koin)
single<SlackcatModule> { CustomKudosModule() }
```

### Best Practices for Module Customization

1. **Use `protected open` properties** for configurable behavior
   - Makes customization explicit and type-safe
   - Follows Kotlin property override pattern
   - Example: `protected open val spamProtectionEnabled: Boolean = true`

2. **Provide sensible defaults**
   - Default values should represent the most common use case
   - Downstream users must explicitly opt-out of defaults

3. **Document overridable properties**
   - Clearly document what each property controls
   - Specify the default behavior
   - Example use cases help users understand when to customize

4. **Pass configuration through constructor parameters**
   - DAOs and internal components should accept configuration via constructor
   - Use default parameters for backward compatibility
   - Example: `class KudosDAO(private val spamProtectionEnabled: Boolean = true)`

### Pattern Reference

See `JeopardyModule` for another example that overrides `botName` and `botIcon` properties.

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

### Option 1: Using the App Manifest (Recommended)

The easiest way to set up your Slack app is to use the provided manifest file:

1. Go to https://api.slack.com/apps
2. Click **Create New App**
3. Select **From an app manifest**
4. Choose your workspace
5. Copy the contents of `slack-manifest.yml` from this repository
6. Paste it into the YAML tab
7. Click **Next** → **Create**
8. Navigate to **OAuth & Permissions** and click **Install App to Workspace**
9. Approve the permissions

The manifest includes all required scopes and event subscriptions automatically.

### Option 2: Manual Configuration

If you prefer to configure manually or need to update an existing app:

#### Enable Event Subscriptions

1. Go to your Slack app's dashboard at https://api.slack.com/apps
2. Navigate to **Features** > **Event Subscriptions**
3. Toggle **Enable Events** to **On**

#### Subscribe to Bot Events

1. In the **Event Subscriptions** page, scroll down to **Subscribe to bot events**
2. Click on **Add Bot User Event**
3. Add the following events:
    - `message.channels` (for messages in public channels)
    - `message.im` (for direct messages)
    - `message.groups` (for private channels)
    - `message.mpim` (for group direct messages)
    - `reaction_added` (for emoji reactions added to messages)
    - `reaction_removed` (for emoji reactions removed from messages)

#### Set Up OAuth Scopes

1. Navigate to **OAuth & Permissions** in your app's dashboard
2. In the **Scopes** section, under **Bot Token Scopes**, add the following scopes:
    - `chat:write` (to send messages)
    - `channels:history` (to read messages in channels)
    - `groups:history` (for private channels)
    - `im:history` (for direct messages)
    - `mpim:history` (for group direct messages)
    - `channels:join` (if your bot needs to join channels automatically)
    - `reactions:read` (to receive reaction events)
    - **`users:read`** ⚠️ **REQUIRED** - To fetch user display names for kudos and other features

**⚠️ Important:** The `users:read` scope is critical for modules like KudosModule that display user information. Without it, you'll see `missing_scope` errors in the logs.

#### Reinstall the App

1. After making changes to scopes and event subscriptions, you **must reinstall** the app to your workspace
2. Go to **OAuth & Permissions** and click **Reinstall App to Workspace**
3. Approve the new permissions when prompted
4. Restart your bot: `docker compose down && docker compose up -d`

#### Add the Bot to Channels

1. Ensure that your bot is a member of the channels where it needs to listen to messages
2. You can invite the bot to a channel by typing `/invite @YourBotName` in Slack

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
- Check logs if not working: `docker compose logs --tail 50`

## Troubleshooting

### Error: `missing_scope` - `needed=users:read`

**Symptoms:**
```
x-slack-failure: missing_scope
needed=users:read
java.lang.Exception: Failed to fetch user info: missing_scope
```

**Cause:** Your Slack app doesn't have the `users:read` OAuth scope, which is required for features that display user information (like kudos).

**Solution:**
1. Go to https://api.slack.com/apps and select your app
2. Navigate to **OAuth & Permissions**
3. Under **Bot Token Scopes**, add `users:read`
4. Click **Reinstall App to Workspace** at the top of the page
5. Approve the new permissions
6. Restart your bot: `docker compose down && docker compose up -d`

### Error: `duplicate key value violates unique constraint`

**Symptoms:**
```
ERROR: duplicate key value violates unique constraint "kudos_transactions_giver_id_recipient_id_thread_ts_unique"
```

**Cause:** Per-thread deduplication is being violated (attempting to give kudos twice in the same thread).

**Solution:** This is expected behavior - users can only give kudos once per thread. The error indicates the validation is working correctly. If you're seeing this in production logs, it means someone tried to give kudos multiple times in the same thread rapidly.

### Bot Not Responding to Messages

**Checklist:**
1. Verify the bot is invited to the channel: `/invite @YourBotName`
2. Check that Socket Mode is enabled in your app settings
3. Verify environment variables are set correctly:
   - `SLACK_APP_TOKEN` should start with `xapp-`
   - `SLACK_BOT_TOKEN` should start with `xoxb-`
4. Check logs for connection errors: `docker compose logs -f`

### Bot Not Responding to Reactions

**Checklist:**
1. Verify `reactions:read` scope is enabled (see OAuth Scopes above)
2. Verify `reaction_added` event is subscribed (see Bot Events above)
3. Reinstall the app if you just added these permissions
4. Check logs for the reaction event: `docker compose logs -f | grep "Reaction added"`



# Docker

```shell
docker-compose --env-file path/to/your/.env up --build
```

Testing
```shell
docker-compose up --build -d && docker exec -it slack-bot /bin/sh
```


# Database

Slackcat uses [Exposed SQL](https://github.com/JetBrains/Exposed) as the ORM with automatic schema migrations. When you add new tables or columns, they're automatically created on application startup.

## How Schema Management Works

The database layer uses `SchemaUtils.createMissingTablesAndColumns()` which automatically:
- Creates new tables that don't exist
- Adds new columns to existing tables
- Preserves existing data

**Note:** Column deletions and type changes are NOT automatic - you'll need to write manual migrations for those.

## Best Practices for Schema Changes

### Always Use Default Values for NOT NULL Columns

When adding a NOT NULL column to an existing table with data, **ALWAYS** specify a default value. This allows Exposed to automatically migrate existing rows.

**❌ Bad - Will Fail on Tables with Data:**
```kotlin
object KudosMessageTable : Table("kudos_messages") {
    val threadTs = text("thread_ts")
    val botMessageTs = text("bot_message_ts")
    val createdAt = long("created_at")  // ❌ No default - breaks migration!
    val expiresAt = long("expires_at")  // ❌ No default - breaks migration!
}
```

**✅ Good - Automatic Migration Works:**
```kotlin
object KudosMessageTable : Table("kudos_messages") {
    val threadTs = text("thread_ts")
    val botMessageTs = text("bot_message_ts")
    val createdAt = long("created_at").default(0L)  // ✅ Default value
    val expiresAt = long("expires_at").default(0L)  // ✅ Default value
}
```

**Why This Matters:**
- PostgreSQL/SQLite cannot add NOT NULL columns to tables with existing data unless a default is provided
- Exposed generates: `ALTER TABLE kudos_messages ADD COLUMN created_at BIGINT DEFAULT 0`
- Existing rows automatically get the default value (0)
- New rows get actual values from application code
- Works seamlessly for all downstream users

**When to Use This Pattern:**
- Adding timestamp columns (`created_at`, `updated_at`, `expires_at`)
- Adding counters or numeric fields
- Adding status flags or enum columns
- Any NOT NULL column added after the table already has data

**Alternative: Nullable Columns**
If a sensible default doesn't exist, make the column nullable instead:
```kotlin
val nickname = text("nickname").nullable()  // ✅ No default needed
```

## Adding a New Column

Let's say you want to add a `nickname` column to the Kudos table. Here's how:

### 1. Update the Table Definition

In your DAO file (e.g., `KudosDAO.kt`), add the new column to the table object:

```kotlin
object KudosTable : Table() {
    val id = integer("id").autoIncrement()
    val userId = text("user_id").uniqueIndex()
    val count = integer("count")
    val nickname = text("nickname").nullable()  // ← Add this line
    override val primaryKey = PrimaryKey(id)
}
```

### 2. Update the Data Class

Add the new field to your data class:

```kotlin
data class KudosRow(
    val id: Int,
    val userId: String,
    val count: Int,
    val nickname: String?,  // ← Add this line
)
```

### 3. Update Queries

Update any queries that map results to include the new column:

```kotlin
KudosRow(
    id = resultRow[KudosTable.id],
    userId = resultRow[KudosTable.userId],
    count = resultRow[KudosTable.count],
    nickname = resultRow[KudosTable.nickname],  // ← Add this line
)
```

### 4. Deploy

That's it! When you restart the application, the new column will be automatically created.

```bash
docker compose down && docker compose up --build -d
```

## Advanced: Using Exposed Directly

If you need advanced Exposed SQL features not available through the `DatabaseTable` abstraction, you can access the underlying table:

```kotlin
import com.slackcat.database.UnstableExposedApi

@OptIn(UnstableExposedApi::class)
fun myAdvancedQuery() {
    val exposedTable = myDatabaseTable.toExposedTable()
    // Use Exposed SQL directly
}
```

⚠️ **Warning:** This API is marked unstable and may change at any time. Use only when necessary.

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
