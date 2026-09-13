package cz.twocom.core.transport

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class SeedResponse(val seeds: List<SeedNode>, val ttl_seconds: Int)
data class SeedNode(val host: String, val port: Int, val node_id: String)
data class AnnounceRequest(
    val node_id: String,
    val port: Int,
    val timestamp: Long,
    val signature: String,
    val signing_public_key: String,
    val identity_public_key: String,
)
data class AnnounceResponse(val ok: Boolean, val your_ip: String?)

interface BootstrapApi {
    @GET("/v1/seeds")
    suspend fun getSeeds(): SeedResponse

    @POST("/v1/announce")
    suspend fun announce(@Body body: AnnounceRequest): AnnounceResponse

    @GET("/v1/health")
    suspend fun health(): Map<String, Any>
}
