package cz.twocom.core.transport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DhtBootstrapPeersTest {

    @Test
    fun `fallback list has at least 3 peers`() {
        assertTrue(DhtBootstrapPeers.HARDCODED_FALLBACK.size >= 3)
    }

    @Test
    fun `all peers have valid port`() {
        DhtBootstrapPeers.HARDCODED_FALLBACK.forEach { peer ->
            assertTrue("Port out of range: ${peer.port}", peer.port in 1..65535)
        }
    }

    @Test
    fun `all peers have non-empty host`() {
        DhtBootstrapPeers.HARDCODED_FALLBACK.forEach { peer ->
            assertTrue("Empty host for ${peer.region}", peer.host.isNotBlank())
        }
    }

    @Test
    fun `all node IDs are 64 hex chars`() {
        DhtBootstrapPeers.HARDCODED_FALLBACK.forEach { peer ->
            assertEquals("NodeId length for ${peer.region}", 64, peer.nodeId.length)
            assertTrue("NodeId not hex for ${peer.region}", peer.nodeId.matches(Regex("[0-9a-f]{64}")))
        }
    }
}
