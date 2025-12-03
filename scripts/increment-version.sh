#!/bin/bash

set -e

VERSION_FILE="buildSrc/src/main/kotlin/AppVersion.kt"

if [ ! -f "$VERSION_FILE" ]; then
    echo "Error: $VERSION_FILE not found"
    exit 1
fi

current_major=$(grep "const val MAJOR" "$VERSION_FILE" | sed 's/.*= \([0-9]*\).*/\1/')
current_minor=$(grep "const val MINOR" "$VERSION_FILE" | sed 's/.*= \([0-9]*\).*/\1/')
current_patch=$(grep "const val PATCH" "$VERSION_FILE" | sed 's/.*= \([0-9]*\).*/\1/')

echo "Current version: $current_major.$current_minor.$current_patch"

increment_type="${1:-patch}"

case "$increment_type" in
    major)
        new_major=$((current_major + 1))
        new_minor=0
        new_patch=0
        ;;
    minor)
        new_major=$current_major
        new_minor=$((current_minor + 1))
        new_patch=0
        ;;
    patch)
        new_major=$current_major
        new_minor=$current_minor
        new_patch=$((current_patch + 1))
        ;;
    *)
        echo "Error: Invalid increment type. Use 'major', 'minor', or 'patch'"
        exit 1
        ;;
esac

echo "New version: $new_major.$new_minor.$new_patch"

cat > "$VERSION_FILE" << EOF
object AppVersion {
    const val MAJOR = $new_major
    const val MINOR = $new_minor
    const val PATCH = $new_patch

    val versionName: String
        get() = "\$MAJOR.\$MINOR.\$PATCH"

    val versionCode: Int
        get() = MAJOR * 10000 + MINOR * 100 + PATCH
}
EOF

echo "Version incremented successfully!"
echo "$new_major.$new_minor.$new_patch"
