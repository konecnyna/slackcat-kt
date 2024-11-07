rootProject.name = "slackcat-kt"

includeBuild("slackcat-gradle-plugin")
include(
    ":app",
    ":library:features:slackcat",
    ":library:data:chat",
    ":library:core:database",
    ":library:core:network",
    ":library:core:server"
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")