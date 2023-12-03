package features.slackcat.models


interface StorageModule {
    fun provideTable(): TableEntity
}