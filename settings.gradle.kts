rootProject.name = "slackcat-kt"

includeBuild("slackcat-gradle-plugin")
include(
    ":app",
    ":library:slackcat",
    ":library:slackcat-modules",
    ":library:data:chat",
    ":library:core:common",
    ":library:core:database",
    ":library:core:network",
    ":library:core:server"
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")