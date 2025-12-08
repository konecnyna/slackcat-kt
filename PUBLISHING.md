# Publishing Slackcat Library to GitHub Packages

This guide explains how to publish the Slackcat library modules to GitHub Packages and how to consume them in other projects.

## Prerequisites

### 1. Create a GitHub Personal Access Token (PAT)

You need a GitHub Personal Access Token with the following permissions:
- `write:packages` - To publish packages
- `read:packages` - To download packages
- `repo` - For private repositories

**Steps to create a PAT:**
1. Go to GitHub Settings → Developer settings → Personal access tokens → Tokens (classic)
2. Click "Generate new token (classic)"
3. Select the scopes mentioned above
4. Copy the token (you won't be able to see it again!)

### 2. Configure Local Credentials

Create or edit `~/.gradle/gradle.properties` (in your home directory, NOT the project directory):

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.token=YOUR_PERSONAL_ACCESS_TOKEN
```

**Important:** Never commit credentials to the repository!

## Publishing the Library

### Publish All Public Modules

```bash
./gradlew :library:slackcat:publish
./gradlew :library:slackcat-modules:publish
```

Or publish a specific module:

```bash
./gradlew :library:slackcat:publish
```

**Note:** Only `slackcat` and `slackcat-modules` are published. Internal core modules (`common`, `database`, `network`, `server`, `chat`) are included as transitive dependencies and are not published separately.

### Using Environment Variables (CI/CD)

You can also use environment variables instead of gradle.properties:

```bash
export GITHUB_REPOSITORY=konecnyna/slackcat-kt
export GITHUB_ACTOR=your-username
export GITHUB_TOKEN=your-token

./gradlew publish
```

## Consuming the Library in Other Projects

### 1. Add GitHub Packages Repository

In your consuming project's `settings.gradle.kts`:

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

Or in `build.gradle.kts` (if not using centralized repository management):

```kotlin
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
```

### 2. Add Dependencies

In your `build.gradle.kts`:

```kotlin
dependencies {
    // Core library with all internal dependencies
    implementation("com.slackcat:slackcat:0.0.1")

    // Optional: Pre-built modules (weather, crypto, emoji, etc.)
    implementation("com.slackcat:slackcat-modules:0.0.1")
}
```

### 3. Configure Credentials

Same as publishing - create `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.token=YOUR_PERSONAL_ACCESS_TOKEN
```

## Available Modules

- `com.slackcat:slackcat:0.0.1` - Core library (includes all internal dependencies)
- `com.slackcat:slackcat-modules:0.0.1` - Pre-built modules (weather, crypto, emoji, kudos, etc.)

**Internal modules (not published separately):**
These are automatically included as transitive dependencies when you use `slackcat`:
- `common` - Common utilities and models
- `database` - Database layer
- `network` - Network client
- `server` - Server components
- `chat` - Chat engine implementations

## Versioning

The current version is managed in `buildSrc/src/main/kotlin/AppVersion.kt`:

```kotlin
object AppVersion {
    const val MAJOR = 0
    const val MINOR = 0
    const val PATCH = 1

    val versionName: String
        get() = "$MAJOR.$MINOR.$PATCH"
}
```

To publish a new version, see `VERSIONING.md` for details on the automated release process.

## Troubleshooting

### "Could not find com.slackcat:slackcat:0.0.1"

- Ensure you have configured GitHub credentials correctly
- Verify your PAT has `read:packages` permission
- Check that you have access to the `konecnyna/slackcat-kt` repository
- Try running with `--refresh-dependencies` flag

### "401 Unauthorized" when publishing

- Verify your PAT has `write:packages` permission
- Check that `gpr.user` and `gpr.token` are correctly set
- Ensure the token hasn't expired

### "409 Conflict" when publishing

- This version already exists in GitHub Packages
- You cannot overwrite existing versions - increment the version number

## Notes

- GitHub Packages uses repository permissions, so only users with access to the repository can download packages
- Published packages cannot be deleted easily (GitHub limitation)
- Maven artifacts in GitHub Packages are immutable - you cannot overwrite a published version
