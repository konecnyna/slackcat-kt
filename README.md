# slackcat-kt

Slackcat but strongly typed

## Note

The`*Graphs.kt` classes are a very simple global singleton dependency graph. 
For a real app, you would use something like Dagger instead.# slackcat-kt
Slackcat but strongly typed

## Run locally

You can run commands locally without using SlackRTM webscoket by adding your command to the program arguments in the run config

For instance if you add `?ping` to program arguments you should get

```shell
Cli is connected
Starting slackcat using Cli engine
Incoming message: ?ping
Outgoing message: OutgoingChatMessage(channelId=123456789, text=pong)
```


# Setup
## Add env vars

1. Edit run configuration
2. Open environmental variables
3. add
    * SLACK_APP_TOKEN
    * SLACK_BOT_TOKEN
4. Profit.


## Slack Workspace setup:

### Enable Event Subscriptions

1. Go to your Slack app's dashboard.
2. Navigate to **Features** > **Event Subscriptions**.
3. Toggle **Enable Events** to **On**.

### Subscribe to Message Events

1. In the **Event Subscriptions** page, scroll down to **Subscribe to bot events**.
2. Click on **Add Bot User Event**.
3. Add the following events based on your needs:
    - `message.channels` (for messages in public channels)
    - `message.im` (for direct messages)
    - `message.groups` (for private channels)
    - `message.mpim` (for group direct messages)

### Set Up OAuth Scopes

1. Navigate to **OAuth & Permissions** in your app's dashboard.
2. In the **Scopes** section, under **Bot Token Scopes**, add the following scopes:
    - `channels:history` (to read messages in channels)
    - `groups:history` (for private channels)
    - `im:history` (for direct messages)
    - `mpim:history` (for group direct messages)
    - `channels:join` (if your bot needs to join channels automatically)
    - `chat:write` (to send messages)

### Reinstall the App

1. After making changes to scopes and event subscriptions, you need to reinstall the app to your workspace for the changes to take effect.
2. Go to **OAuth & Permissions** and click **Install App to Workspace**.

### Add the Bot to Channels

1. Ensure that your bot is a member of the channels where it needs to listen to messages.
2. You can invite the bot to a channel by typing `/invite @YourBotName` in Slack.