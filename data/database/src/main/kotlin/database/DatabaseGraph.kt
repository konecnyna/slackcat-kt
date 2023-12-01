package database

object DatabaseGraph {
    val databaseClient = DatabaseClient()

    fun connectDatabase() {
//        val databaseFeatures: List<StorageClient> = FeatureGraph.featureModules
//            .filter { it is StorageClient }
//            .map { it as StorageClient }
//        databaseClient.initialize(databaseFeatures)
    }
}