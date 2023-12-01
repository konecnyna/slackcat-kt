package features

import features.chat.date.DateFeature
import features.chat.kudos.KudosFeature
import features.chat.ping.PingFeature
import features.chat.status.StatusFeature
import features.common.FeatureModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.reflect.KClass

object FeatureGraph {
    val featureCoroutineScope = CoroutineScope(Dispatchers.IO)
    val features: Array<KClass<out FeatureModule>> = arrayOf(
        DateFeature::class,
        KudosFeature::class,
        PingFeature::class,
        StatusFeature::class
    )
}