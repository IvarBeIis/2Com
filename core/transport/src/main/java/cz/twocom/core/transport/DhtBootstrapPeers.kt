package cz.twocom.core.transport

data class DhtPeer(val host: String, val port: Int, val nodeId: String, val region: String = "")

object DhtBootstrapPeers {
    val HARDCODED_FALLBACK = listOf(
        DhtPeer("80.211.207.41", 49737, "a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890", "vps-primary"),
        DhtPeer("45.76.100.42", 49737, "b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1", "us-east"),
        DhtPeer("95.179.200.11", 49737, "c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2", "eu-frankfurt"),
        DhtPeer("139.162.55.73", 49737, "d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3", "ap-singapore"),
        DhtPeer("178.62.194.88", 49737, "e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4", "eu-amsterdam"),
    )
}
