package com.slackcat.chat.engine.slack

import com.slack.api.model.block.DividerBlock
import com.slack.api.model.block.ImageBlock
import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.block.SectionBlock
import com.slack.api.model.block.composition.MarkdownTextObject
import com.slack.api.model.block.composition.PlainTextObject
import com.slack.api.model.block.element.ImageElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class JsonToBlockConverter {
    fun jsonObjectToBlocks(jsonString: String): List<LayoutBlock> {
        val jsonObject = Json.decodeFromString<JsonObject>(jsonString)
        val blocks = mutableListOf<LayoutBlock>()

        jsonObject["blocks"]?.jsonArray?.forEach { blockElement ->
            val block = blockElement.jsonObject
            when (block["type"]?.jsonPrimitive?.content) {
                "section" -> {
                    val textObject = block["text"]?.jsonObject
                    val text = textObject?.get("text")?.jsonPrimitive?.content
                    val textType = textObject?.get("type")?.jsonPrimitive?.content

                    val fields =
                        block["fields"]?.jsonArray?.map { field ->
                            val fieldText = field.jsonObject["text"]?.jsonPrimitive?.content ?: ""
                            MarkdownTextObject(fieldText, true)
                        }

                    val sectionBlockBuilder = SectionBlock.builder()

                    if (text != null && textType == "mrkdwn") {
                        sectionBlockBuilder.text(MarkdownTextObject(text, true))
                    } else if (text != null) {
                        sectionBlockBuilder.text(PlainTextObject(text, true))
                    }

                    fields?.let { sectionBlockBuilder.fields(it) }

                    block["block_id"]?.jsonPrimitive?.content?.let { sectionBlockBuilder.blockId(it) }

                    // Handle accessory if it exists
                    block["accessory"]?.jsonObject?.let { accessoryObject ->
                        if (accessoryObject["type"]?.jsonPrimitive?.content == "image") {
                            val imageUrl = accessoryObject["image_url"]?.jsonPrimitive?.content
                            val altText = accessoryObject["alt_text"]?.jsonPrimitive?.content
                            if (imageUrl != null && altText != null) {
                                sectionBlockBuilder.accessory(
                                    ImageElement.builder()
                                        .imageUrl(imageUrl)
                                        .altText(altText)
                                        .build(),
                                )
                            }
                        }
                    }

                    blocks.add(sectionBlockBuilder.build())
                }

                "divider" -> {
                    blocks.add(DividerBlock.builder().build())
                }

                "image" -> {
                    val imageUrl = block["image_url"]?.jsonPrimitive?.content
                    val altText = block["alt_text"]?.jsonPrimitive?.content
                    if (imageUrl != null && altText != null) {
                        blocks.add(
                            ImageBlock.builder()
                                .imageUrl(imageUrl)
                                .altText(altText)
                                .build(),
                        )
                    }
                }
            }
        }

        return blocks
    }
}
