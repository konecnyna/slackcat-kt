rootProject.name = "slackcat-kt"

includeBuild("slackcat-gradle-plugin")
include(
    ":app",
    ":features:slackcat",
    ":data:chat",
    ":core:database",
    ":core:network",
    ":core:server"
)