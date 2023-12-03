rootProject.name = "slackcat-kt"

includeBuild("slackcat-gradle-plugin")
include(
    ":app",
    ":features:slackcat-modules",
    ":features:slackcat-bot",
    ":data:chat",
    ":core:database",
    ":core:network",
    ":core:server"
)