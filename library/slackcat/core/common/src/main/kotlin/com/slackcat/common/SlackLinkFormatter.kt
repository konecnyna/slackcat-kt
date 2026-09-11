package com.slackcat.common

/**
 * Link handling for Slack message text.
 *
 * Slack sends inbound links as `mrkdwn`: `<url>` or `<url|label>`. Slack also accepts standard
 * Markdown (`[label](url)`) on outbound `markdown` blocks, so users and modules produce both.
 * These helpers read either format and emit the `mrkdwn` form that section blocks render.
 */
object SlackLinkFormatter {
    // Slack entities `<@U1>`, `<#C1|general>` and `<!here>` are not links. The lookahead skips them.
    private val SLACK_LINK = Regex("""<(?![@#!])([^|<>\s]+)(?:\|([^<>]*))?>""")

    private val MARKDOWN_LINK = Regex("""!?\[([^\[\]\n]*)]\(\s*<?([^\s)<>]+)>?\s*\)""")

    private val BARE_URL = Regex("""https?://[^\s<>|)\]]+""")

    private val CODE_SPAN = Regex("""(?s)```.*?```|`[^`\n]*`""")

    private val PRIVATE_SLACK_FILE_PATH = Regex("""^/(files|files-pri|files-tmb|docs|lists|canvas)(/|$)""")

    private val IMAGE_EXTENSIONS =
        setOf("jpg", "jpeg", "png", "gif", "bmp", "svg", "webp", "apng", "avif", "ico", "tiff")

    /**
     * Replaces every link with its bare URL, in both `<url|label>` and `[label](url)` form.
     * Slack entities such as user mentions and channel links stay intact.
     */
    fun toBareUrls(text: String): String =
        mapOutsideCodeSpans(text) { segment ->
            MARKDOWN_LINK.replace(SLACK_LINK.replace(segment) { it.groupValues[1] }) { it.groupValues[2] }
        }

    /**
     * Rewrites standard Markdown links into Slack `mrkdwn` links. Slack renders a Markdown image
     * as a hyperlink, so `![alt](url)` becomes a link too. Existing `mrkdwn` is left alone.
     */
    fun toSlackMrkdwn(text: String): String =
        mapOutsideCodeSpans(text) { segment ->
            MARKDOWN_LINK.replace(segment) { match ->
                toSlackLink(url = match.groupValues[2], label = match.groupValues[1])
            }
        }

    /** Builds the `mrkdwn` link Slack section blocks render. Falls back to the URL as the label. */
    fun toSlackLink(
        url: String,
        label: String?,
    ): String = "<$url|${label?.takeIf { it.isNotBlank() } ?: url}>"

    /** Returns the first http or https URL in the text, in any link format, or null. */
    fun extractFirstUrl(text: String): String? = BARE_URL.find(toBareUrls(text))?.value

    /** Reports whether the URL points at an image Slack can render in an image block. */
    fun isImageUrl(url: String): Boolean {
        val path = url.substringBefore('?').substringBefore('#')
        val extension = path.substringAfterLast('.', missingDelimiterValue = "")
        return extension.lowercase() in IMAGE_EXTENSIONS
    }

    /**
     * Reports whether the URL is a Slack-hosted private file. Slack requires a bearer token to
     * fetch these, so an image block pointing at one renders as a broken image.
     */
    fun isPrivateSlackFileUrl(url: String): Boolean {
        if (hasPublicSecret(url)) return false
        val host = url.substringAfter("://", missingDelimiterValue = "").substringBefore('/').lowercase()
        if (host == "files.slack.com") return true
        if (!host.endsWith(".slack.com") && host != "slack.com") return false
        val path = "/" + url.substringAfter("://", missingDelimiterValue = "").substringAfter('/', "")
        return PRIVATE_SLACK_FILE_PATH.containsMatchIn(path)
    }

    // files.sharedPublicURL grants anonymous read through this query parameter.
    private fun hasPublicSecret(url: String): Boolean =
        url.substringAfter('?', missingDelimiterValue = "")
            .split('&')
            .any { it.startsWith("pub_secret=") && it.length > "pub_secret=".length }

    private fun mapOutsideCodeSpans(
        text: String,
        transform: (String) -> String,
    ): String {
        val result = StringBuilder()
        var cursor = 0
        CODE_SPAN.findAll(text).forEach { span ->
            result.append(transform(text.substring(cursor, span.range.first)))
            result.append(span.value)
            cursor = span.range.last + 1
        }
        result.append(transform(text.substring(cursor)))
        return result.toString()
    }
}
