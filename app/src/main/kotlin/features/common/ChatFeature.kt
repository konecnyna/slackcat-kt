package features.common


import app.common.Router
import app.models.Message
import org.jetbrains.exposed.sql.Table


abstract class FeatureModule {
    abstract fun onInvoke(message: Message)
    abstract fun provideCommand(): String
}


interface StorageClient {
    fun provideTable(): Table
}