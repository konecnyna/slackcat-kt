package com.slackcat.slackcat.models

import org.jetbrains.exposed.sql.Table


interface StorageModule {
    fun provideTable(): Table
}