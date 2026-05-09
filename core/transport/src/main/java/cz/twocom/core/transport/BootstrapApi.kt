package cz.twocom.core.transport

import retrofit2.http.GET

data class SeedResponse(val seeds: List<SeedNode>, val ttl_seconds: Int)
data class SeedNode(val host: String, val port: Int, val node_id: String)

interface BootstrapApi {
    @GET("/v1/seeds")
    suspend fun getSeeds(): SeedResponse

    @GET("/v1/health")
    suspend fun health(): Map<String, Any>
}
