# Versioning and Release Guide

This project uses automated versioning and release management through GitHub Actions.

## Version Management

Versions are managed through the `AppVersion.kt` file located at:
```
buildSrc/src/main/kotlin/AppVersion.kt
```

This file contains the version components:
```kotlin
object AppVersion {
    const val MAJOR = 0
    const val MINOR = 0
    const val PATCH = 1

    val versionName: String
        get() = "$MAJOR.$MINOR.$PATCH"

    val versionCode: Int
        get() = MAJOR * 10000 + MINOR * 100 + PATCH
}
```

All modules in the project automatically use this version through `build.gradle.kts`:
```kotlin
allprojects {
    version = AppVersion.versionName
}
```

## Manual Version Increment

You can manually increment the version using the script:

```bash
# Increment patch version (0.0.1 -> 0.0.2)
./scripts/increment-version.sh patch

# Increment minor version (0.0.1 -> 0.1.0)
./scripts/increment-version.sh minor

# Increment major version (0.0.1 -> 1.0.0)
./scripts/increment-version.sh major
```

## Automated Release Process

The GitHub Actions workflow `.github/workflows/release-and-publish.yml` automates the entire release process:

### How to Release

1. Go to your repository on GitHub
2. Click on **Actions** tab
3. Select **Release and Publish** workflow
4. Click **Run workflow** button
5. Choose the version increment type for the NEXT version:
   - `patch` - Bug fixes (0.0.1 -> 0.0.2)
   - `minor` - New features (0.0.1 -> 0.1.0)
   - `major` - Breaking changes (0.0.1 -> 1.0.0)
6. Click **Run workflow**

### What Happens Automatically

The workflow will:

1. ✅ **Get Current Version** - Reads version from `AppVersion.kt`
2. ✅ **Create GitHub Release** - Creates a tagged release (e.g., `v0.0.1`)
3. ✅ **Publish to GitHub Packages** - Publishes all library modules:
   - `com.slackcat:slackcat`
   - `com.slackcat:slackcat-modules`
   - `com.slackcat:common`
   - `com.slackcat:database`
   - `com.slackcat:network`
   - `com.slackcat:server`
   - `com.slackcat:chat`
4. ✅ **Increment Version** - Bumps version for next release
5. ✅ **Commit Changes** - Commits new version back to repository with `[skip ci]`

### Workflow Triggers

The workflow is **manual only** (`workflow_dispatch`), meaning it only runs when you explicitly trigger it. This gives you full control over when releases happen.

## Version Numbering Strategy

Follow [Semantic Versioning](https://semver.org/):

- **MAJOR** version - Breaking changes or major rewrites
- **MINOR** version - New features (backwards compatible)
- **PATCH** version - Bug fixes and small improvements

### Examples

- `0.0.1` -> `0.0.2` - Fixed a bug
- `0.0.2` -> `0.1.0` - Added new module or feature
- `0.1.0` -> `1.0.0` - First stable release or breaking API changes

## Consuming Released Versions

After a release, other projects can use your library:

```kotlin
dependencies {
    implementation("com.slackcat:slackcat:0.0.1")
}
```

See `PUBLISHING.md` for complete instructions on consuming the library.

## Pre-release Checklist

Before triggering a release, ensure:

- [ ] All tests pass locally
- [ ] Code is reviewed and merged to main
- [ ] CHANGELOG is updated (if you maintain one)
- [ ] Breaking changes are documented
- [ ] You have the correct version increment type selected

## Troubleshooting

### "Version already exists"

If a release fails because the version already exists:
1. The version was already released - check GitHub Releases
2. Manually increment the version in `AppVersion.kt`
3. Commit and push the change
4. Try the release workflow again

### "Publish failed"

If publishing to GitHub Packages fails:
1. Check that `GITHUB_TOKEN` has correct permissions
2. Verify the workflow has `packages: write` permission
3. Check the Actions logs for specific errors

### "Cannot push to repository"

If the version bump commit fails:
1. Check repository settings allow GitHub Actions to push
2. Go to Settings → Actions → General
3. Enable "Allow GitHub Actions to create and approve pull requests"
4. Set workflow permissions to "Read and write permissions"

## Files Created

- `buildSrc/src/main/kotlin/AppVersion.kt` - Version source of truth
- `buildSrc/build.gradle.kts` - BuildSrc configuration
- `scripts/increment-version.sh` - Version increment script
- `.github/workflows/release-and-publish.yml` - Release automation workflow
