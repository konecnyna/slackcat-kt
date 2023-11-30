package data.database

import features.FeatureGraph
import features.common.StorageClient

object DatabaseGraph {
    val databaseClient = DatabaseClient().apply {
        val databaseFeatures: List<StorageClient> = FeatureGraph.featureModules
            .filter { it is StorageClient }
            .map { it as StorageClient }
        initialize(databaseFeatures)
    }
}