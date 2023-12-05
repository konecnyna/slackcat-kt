package com.slackcat.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object SlackcatAppGraph {
    val globalScope = CoroutineScope(Dispatchers.IO)
}
