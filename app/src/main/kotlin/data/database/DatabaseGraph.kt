package data.database

import features.FeatureGraph
import features.common.StorageClient

object DatabaseGraph {
    val databaseClient = DatabaseClient()

    fun connectDatabase() {
        val databaseFeatures: List<StorageClient> = FeatureGraph.featureModules
            .filter { it is StorageClient }
            .map { it as StorageClient }
        databaseClient.initialize(databaseFeatures)
    }
}