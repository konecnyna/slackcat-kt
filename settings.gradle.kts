rootProject.name = "slackcat-kt"

includeBuild("slackcat-gradle-plugin")
include(
    ":app",
    ":features",
    ":data:chat",
    ":data:database",
    ":data:network"
)