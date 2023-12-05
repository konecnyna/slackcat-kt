package app.modules.status

//import data.network.NetworkGraph
//
//class StatusClient {
//    private val networkClient = NetworkGraph.networkClient
//
//    suspend fun fetch(): SlackStatusResponse {
//        return networkClient.fetch(
//            "https://status.slack.com/api/v2.0.0/current",
//            SlackStatusResponse.serializer(),
//        )
//    }
//}
//
//@Serializable
//data class SlackStatusResponse(
//    val status: String,
//    val date_created: String,
//    val date_updated: String,
//    val active_incidents: List<String?>?,
//)
