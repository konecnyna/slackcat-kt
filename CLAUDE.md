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
