package features

import features.chat.date.DateFeature
import features.chat.ping.PingFeature
import features.chat.status.StatusFeature
import features.common.ChatModule
import kotlin.reflect.KClass

object FeatureEntry {
    val features: Array<KClass<out ChatModule>> = arrayOf(
        DateFeature::class,
        PingFeature::class,
        StatusFeature::class
    )
}