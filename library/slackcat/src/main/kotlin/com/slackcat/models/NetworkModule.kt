package com.slackcat.models

import com.slackcat.network.NetworkClient

/**
 * Marker interface indicating that this module requires network access.
 * Modules implementing this interface typically need to make HTTP requests
 * to external APIs.
 */
interface NetworkModule {
    var networkClient: NetworkClient
}
