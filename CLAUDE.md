# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Development Commands

```bash
# Build fat JAR
./gradlew :app:shadowJar

# Run all tests
./gradlew test

# Run a specific module's tests
./gradlew :library:slackcat-modules:test

# Run a single test class
./gradlew :library:slackcat-modules:test --tests "com.slackcat.modules.simple.PingModuleTest"

# Lint
./gradlew ktlintCheck
./gradlew ktlintFormat

# Run locally in CLI mode (no Slack tokens needed)
./gradlew :app:run --args="?ping"

# Publish to GitHub Packages
./gradlew publish

# Docker (production with PostgreSQL)
docker-compose --env-file path/to/.env up --build
```

## Architecture

slackcat-kt is a Kotlin Slack bot framework that doubles as a runnable application and a publishable library. Commands are `?`-prefixed (e.g., `?ping`, `?weather`).

### Core Flow

1. **`Application.kt`** → **`SlackcatApp`** bootstraps Koin DI and creates **`SlackcatBot`**
2. **`SlackcatBot`** selects a `ChatEngine`: `SlackChatEngine` (production, Socket Mode RTM) or `CliChatEngine` (local testing via `--args`)
3. `ChatEngine` emits `SlackcatEvent`s into a `MutableSharedFlow`
4. **`Router`** dispatches events: parses `?command` prefix, looks up the target module, calls `onInvoke()`; fans out reactions and other events to subscribers

### Key Abstractions

- **`SlackcatModule`** — Base class for command modules. Override `commandInfo()`, `onInvoke()`, `help()`. Optionally handle reactions via `reactionsToHandle()` + `onReaction()`. Extends `KoinComponent` for DI access.
- **`StorageModule`** — Interface for modules with database tables. Declares Exposed tables; auto-migrated on startup.
- **`SlackcatEventsModule`** — Interface to receive ALL bot events (STARTED, MessageReceived, etc.)
- **`ChatEngine`** — Interface with Slack and CLI implementations
- **`BotMessage` / `buildMessage { }`** — Platform-agnostic message DSL with `MessageStyle` (INFO/WARNING/ERROR/SUCCESS/NEUTRAL/Custom)

### Module Registration

Modules are registered as `KClass` references in `app/di/AppModule.kt`. Start from `SlackcatModules.all`, remove/replace defaults, append app-specific modules. Customize behavior via inheritance (override `protected open val` properties).

### Two-Pass Module Instantiation

`SlackcatBot.setupChatModule()` instantiates modules in two passes: first modules that don't need `Router`, then those that do (e.g., `LearnModule`). This resolves circular dependencies.

## Project Layout

- **`app/`** — Runnable bot application (shadow JAR, app-specific modules like Jeopardy, DeployBot)
- **`library/slackcat/`** — Core library: `SlackcatBot`, `Router`, models, DI
- **`library/slackcat/core/`** — Submodules: `common` (messages, events, parser), `database` (Exposed), `network` (Ktor client), `server` (Ktor server)
- **`library/slackcat/data/chat/`** — `ChatEngine` interface + Slack/CLI implementations
- **`library/slackcat-modules/`** — Pre-built module library (simple, network, storage categories)
- **`slackcat-gradle-plugin/`** — Custom Gradle plugin with `slackcat { features { } }` DSL for dependency opt-in (`coroutines()`, `exposed()`, `ktorClient()`, `koin()`, `enableJunitTesting()`, etc.)
- **`buildSrc/AppVersion.kt`** — Single source of truth for semver (MAJOR/MINOR/PATCH constants)

## Conventions

- **DI**: Koin (not Dagger/Hilt). Global singleton graphs for network, database, server.
- **Database**: JetBrains Exposed ORM. SQLite in CLI mode, PostgreSQL in production. Always use `.default(value)` on NOT NULL columns added to existing tables.
- **Testing**: JUnit 5 + MockK. Tests call `startKoin { modules(...) }` in `@BeforeEach` and `stopKoin()` in `@AfterEach`. Coroutine tests use `runTest`.
- **Linting**: ktlint across all subprojects.
- **Versioning**: Semver managed in `buildSrc/AppVersion.kt`. CI auto-bumps via `scripts/increment-version.sh`.
- **CI**: `pr-tests.yml` runs ktlint + tests on PRs. `release-and-publish.yml` publishes to GitHub Packages on manual dispatch.
- **Extensibility**: All core modules, clients, and data classes in `library/slackcat-modules/` should be `open` so downstream consumers can extend them. This includes module classes, client classes, enums (prefer sealed classes/interfaces over enums when extensibility is needed), and data models.
- **Always Verify Locally**: After creating or modifying any module, always test it locally using CLI mode before considering the work done. Run `./gradlew :app:run --args="?<command>"` to verify the module responds correctly. For modules without a direct command, run the relevant test suite with `./gradlew test`. Never skip local verification.

## Slack Link & Markdown Formats

Slack has two link syntaxes. Both are current. `mrkdwn` is **not** deprecated.

| Direction | Format | Where |
|---|---|---|
| **Inbound** (what the bot receives) | `<url>`, `<url\|label>` | `message` event `text` field. Unchanged. |
| **Outbound** section/context blocks | `<url\|label>` (`mrkdwn`) | `SectionBlock`, `ContextBlock`. Standard Markdown does **not** render here. |
| **Outbound** `markdown` block | `[label](url)` | Added 2025-02-03. 12,000 char cumulative cap. |
| **Outbound** `chat.postMessage` | `[label](url)` via `markdown_text` | Do not combine with `blocks` or `text`. |

### Rules

- **Never regex link syntax by hand.** Use `SlackLinkFormatter` in `library/slackcat/core/common`.
- **Modules emit standard Markdown.** Write `[label](url)` in `buildMessage { text(...) }`, or use `link(url, label)`. `SlackMessageConverter` rewrites it to `mrkdwn`.
- **Slack entities are not links.** `<@U1>`, `<#C1|general>`, `<!here>`, `<!subteam^S1>` must survive untouched. A naive `<`/`>` strip corrupts them.
- **Private Slack file URLs cannot go in an image block.** `files.slack.com/...` and `<team>.slack.com/files/...` need a bearer token. Post them as a link instead. Public CDN hosts such as `emoji.slack-edge.com` are fine.
- **Markdown images become links.** Slack renders `![alt](url)` as a hyperlink, not an embedded image.
- **`text` is an approximation.** Since 2019-09-01 the precise inbound structure lives in `rich_text` blocks.

## Slack API Limits & Common Errors

### Block Text Character Limit (3000 chars)

Slack's Block Kit has a **3000 character limit** for text in section blocks. Exceeding this causes the following error:

```json
{
  "ok": false,
  "error": "invalid_blocks",
  "errors": [
    "failed to match all allowed schemas [json-pointer:/blocks/0/text]",
    "must be less than 3001 characters [json-pointer:/blocks/0/text/text]"
  ]
}
```

**Solution**: The `SlackMessageConverter` automatically chunks long text into multiple blocks. When creating modules that may produce long output, this is handled transparently. If you encounter this error, ensure you're using the standard `buildMessage { }` DSL rather than constructing raw Slack blocks.
