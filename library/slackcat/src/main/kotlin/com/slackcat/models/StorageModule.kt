package com.slackcat.models

import org.jetbrains.exposed.sql.Table

interface StorageModule {
    fun tables(): List<Table>
}
