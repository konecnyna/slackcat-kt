package app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object AppGraph {
    val globalScope = CoroutineScope(Dispatchers.IO)
}
