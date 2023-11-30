package features

import features.ping.PingFeature
import kotlin.reflect.KClass

object FeatureEntry {
    val features: Array<KClass<out ChatModule>> = arrayOf(
        PingFeature::class
    )
}