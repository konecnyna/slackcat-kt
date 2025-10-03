package com.slackcat.modules

import com.slackcat.models.SlackcatModule
import com.slackcat.modules.simple.*
import com.slackcat.modules.storage.kudos.KudosModule
import com.slackcat.modules.storage.learn.LearnModule
import com.slackcat.modules.storage.jeopardy.JeopardyModule
import com.slackcat.modules.network.weather.WeatherModule
import com.slackcat.modules.network.translate.TranslateModule
import com.slackcat.modules.network.crypto.CryptoPriceModule
import com.slackcat.modules.network.emoji.EmojiModule
import com.slackcat.modules.network.status.StatusModule
import com.slackcat.modules.network.summon.SummonModule
import com.slackcat.modules.network.pokecat.PokeCatModule
import com.slackcat.modules.network.radar.RadarModule
import kotlin.reflect.KClass

/**
 * Convenience collections of pre-built Slackcat modules.
 * Consumers can use these collections to quickly set up their bot with common functionality.
 */
object SlackcatModules {
    /**
     * All available modules in the slackcat-modules library.
     */
    val all: List<KClass<out SlackcatModule>> = listOf(
        // Simple modules
        PingModule::class,
        DateModule::class,
        FlipModule::class,
        FrameModule::class,
        EmojiSentenceModule::class,

        // Storage modules
        KudosModule::class,
        LearnModule::class,
        JeopardyModule::class,

        // Network modules
        WeatherModule::class,
        TranslateModule::class,
        PokeCatModule::class,
        EmojiModule::class,
        SummonModule::class,
        StatusModule::class,
        CryptoPriceModule::class,
        RadarModule::class
    )

    /**
     * Simple modules with no external dependencies (no database, no API calls).
     * These are lightweight and always work out of the box.
     */
    val simple: List<KClass<out SlackcatModule>> = listOf(
        PingModule::class,
        DateModule::class,
        FlipModule::class,
        FrameModule::class,
        EmojiSentenceModule::class
    )

    /**
     * Fun/entertainment modules for playful interactions.
     */
    val `fun`: List<KClass<out SlackcatModule>> = listOf(
        PokeCatModule::class,
        EmojiModule::class,
        FlipModule::class,
        FrameModule::class,
        EmojiSentenceModule::class
    )

    /**
     * Utility modules providing practical functionality.
     */
    val utility: List<KClass<out SlackcatModule>> = listOf(
        PingModule::class,
        DateModule::class,
        WeatherModule::class,
        TranslateModule::class,
        StatusModule::class,
        CryptoPriceModule::class,
        RadarModule::class
    )

    /**
     * Gamification modules that track user engagement (requires database).
     */
    val gamification: List<KClass<out SlackcatModule>> = listOf(
        KudosModule::class,
        JeopardyModule::class,
        LearnModule::class
    )

    /**
     * Modules that require API keys or external service configuration.
     * These modules need additional setup before they can be used.
     */
    val requiresApiKeys: List<KClass<out SlackcatModule>> = listOf(
        WeatherModule::class,
        TranslateModule::class,
        CryptoPriceModule::class,
        RadarModule::class,
        PokeCatModule::class,
        EmojiModule::class,
        SummonModule::class,
        StatusModule::class
    )
}
