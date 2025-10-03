rootProject.name = "slackcat-kt"

includeBuild("slackcat-gradle-plugin")
include(
    ":app",
    ":library:slackcat",
    ":library:slackcat-modules",
    ":library:slackcat:data:chat",
    ":library:slackcat:core:common",
    ":library:slackcat:core:database",
    ":library:slackcat:core:network",
    ":library:slackcat:core:server"
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")