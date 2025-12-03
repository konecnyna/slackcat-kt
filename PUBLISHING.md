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

### Option 1: Publish All Library Modules

```bash
./gradlew :library:slackcat:publish
./gradlew :library:slackcat-modules:publish
./gradlew :library:slackcat:core:common:publish
./gradlew :library:slackcat:core:database:publish
./gradlew :library:slackcat:core:network:publish
./gradlew :library:slackcat:core:server:publish
./gradlew :library:slackcat:data:chat:publish
```

### Option 2: Publish Specific Module

```bash
./gradlew :library:slackcat:publish
```

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
    implementation("com.slackcat:slackcat:0.0.1")
    implementation("com.slackcat:slackcat-modules:0.0.1")
    // Add other modules as needed
}
```

### 3. Configure Credentials

Same as publishing - create `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.token=YOUR_PERSONAL_ACCESS_TOKEN
```

## Available Modules

- `com.slackcat:slackcat:0.0.1` - Main library
- `com.slackcat:slackcat-modules:0.0.1` - Modules library
- `com.slackcat:common:0.0.1` - Common utilities
- `com.slackcat:database:0.0.1` - Database layer
- `com.slackcat:network:0.0.1` - Network layer
- `com.slackcat:server:0.0.1` - Server components
- `com.slackcat:chat:0.0.1` - Chat data models

## Versioning

The current version is set in the root `build.gradle.kts`:

```kotlin
allprojects {
    group = "com.slackcat"
    version = "0.0.1"
}
```

To publish a new version, update the version number and republish.

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
