object AppVersion {
    const val MAJOR = 0
    const val MINOR = 0
    const val PATCH = 8

    val versionName: String
        get() = "$MAJOR.$MINOR.$PATCH"

    val versionCode: Int
        get() = MAJOR * 10000 + MINOR * 100 + PATCH
}
