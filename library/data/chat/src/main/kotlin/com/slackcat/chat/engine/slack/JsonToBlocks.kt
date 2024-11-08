package com.slackcat.chat.engine.slack;

import com.slack.api.model.block.LayoutBlock
import com.slack.api.model.block.SectionBlock
import com.slack.api.model.block.DividerBlock
import com.slack.api.model.block.ImageBlock
import com.slack.api.model.block.composition.MarkdownTextObject
import com.slack.api.model.block.composition.PlainTextObject
import com.slack.api.model.block.element.ImageElement
import kotlinx.serialization.json.*

class JsonToBlockConverter {
    fun jsonObjectToBlocks(jsonObject: JsonObject): List<LayoutBlock> {
        val blocks = mutableListOf<LayoutBlock>()

        jsonObject["blocks"]?.jsonArray?.forEach { blockElement ->
            val block = blockElement.jsonObject
            when (block["type"]?.jsonPrimitive?.content) {
                "section" -> {
                    val textObject = block["text"]?.jsonObject
                    val text = textObject?.get("text")?.jsonPrimitive?.content
                    val textType = textObject?.get("type")?.jsonPrimitive?.content

                    val fields = block["fields"]?.jsonArray?.map { field ->
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
                                        .build()
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
                                .build()
                        )
                    }
                }
            }
        }

        return blocks
    }
}


fun main() {
    val json = """
        {
            "blocks": [
                {
                    "type": "section",
                    "text": {
                        "type": "mrkdwn",
                        "text": "Danny Torrence left the following review for your property:"
                    }
                },
                {
                    "type": "section",
                    "block_id": "section567",
                    "text": {
                        "type": "mrkdwn",
                        "text": "<https://example.com|Overlook Hotel> \n :star: \n Doors had too many axe holes, guest in room 237 was far too rowdy, whole place felt stuck in the 1920s."
                    },
                    "accessory": {
                        "type": "image",
                        "image_url": "https://is5-ssl.mzstatic.com/image/thumb/Purple3/v4/d3/72/5c/d3725c8f-c642-5d69-1904-aa36e4297885/source/256x256bb.jpg",
                        "alt_text": "Haunted hotel image"
                    }
                },
                {
                    "type": "section",
                    "block_id": "section789",
                    "fields": [
                        {
                            "type": "mrkdwn",
                            "text": "*Average Rating*\n1.0"
                        }
                    ]
                }
            ]
        }
    """
    val jsonObject = Json.parseToJsonElement(json).jsonObject
    val jsonToBlockConverter = JsonToBlockConverter()
    val blocks = jsonToBlockConverter.jsonObjectToBlocks(jsonObject)
    println(blocks)
}