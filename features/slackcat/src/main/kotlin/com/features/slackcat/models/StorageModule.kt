package com.features.slackcat.models

import org.jetbrains.exposed.sql.Table


interface StorageModule {
    fun provideTable(): Table
}