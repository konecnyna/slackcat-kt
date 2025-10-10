package com.slackcat.models

import com.slackcat.database.DatabaseTable

interface StorageModule {
    fun tables(): List<DatabaseTable>
}
