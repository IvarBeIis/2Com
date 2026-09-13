'use strict';

import express from 'express';
import cors from 'cors';
import * as ed from '@noble/ed25519';
import { blake3 } from '@noble/hashes/blake3.js';
import { bytesToHex, hexToBytes } from '@noble/hashes/utils.js';

const app = express();
const PORT = process.env.PORT || 3000;
const START_TIME = Date.now();
const ANNOUNCE_MAX_SKEW_SECONDS = 60;
const HEX_64 = /^[0-9a-f]{64}$/;
const HEX_ANY = /^[0-9a-f]+$/;

app.use(cors());
app.use(express.json());

// Active peers: Map<nodeId, { host, port, lastSeen }>
const activePeers = new Map();
const PEER_TTL_MS = 5 * 60 * 1000; // 5 minutes

// Cleanup stale peers every minute. unref() so importing this module (e.g. from tests)
// doesn't keep the process alive with no server actually listening.
setInterval(() => {
  const now = Date.now();
  for (const [id, peer] of activePeers.entries()) {
    if (now - peer.lastSeen > PEER_TTL_MS) activePeers.delete(id);
  }
}, 60_000).unref();

// GET /v1/seeds — return list of active DHT peers
app.get('/v1/seeds', (req, res) => {
  const now = Date.now();
  const seeds = [];
  for (const [nodeId, peer] of activePeers.entries()) {
    if (now - peer.lastSeen <= PEER_TTL_MS) {
      seeds.push({ host: peer.host, port: peer.port, node_id: nodeId });
    }
  }
  // Always include self as a seed
  seeds.unshift({
    host: process.env.PUBLIC_IP || '80.211.207.41',
    port: 49737,
    node_id: 'a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890',
  });
  res.json({ seeds: seeds.slice(0, 50), ttl_seconds: 300 });
});

/**
 * Validates an announce body: field shape, node_id == BLAKE3(signing_public_key ||
 * identity_public_key), timestamp freshness, and the Ed25519 signature over
 * "node_id:port:timestamp" by signing_public_key. Pure/exported for unit testing —
 * 2COM_AUDIT.md #6: before this, POST /v1/announce accepted any node_id unauthenticated.
 */
export async function verifyAnnounce(body, nowSeconds = Math.floor(Date.now() / 1000)) {
  const { node_id, port, timestamp, signature, signing_public_key, identity_public_key } = body ?? {};

  if (!node_id || !port || !timestamp || !signature || !signing_public_key || !identity_public_key) {
    return { ok: false, error: 'missing required field' };
  }
  if (!HEX_64.test(node_id)) return { ok: false, error: 'invalid node_id' };
  if (!HEX_ANY.test(signature) || !HEX_ANY.test(signing_public_key) || !HEX_ANY.test(identity_public_key)) {
    return { ok: false, error: 'invalid hex field' };
  }
  if (!Number.isInteger(port) || port < 1 || port > 65535) {
    return { ok: false, error: 'invalid port' };
  }
  if (!Number.isInteger(timestamp) || Math.abs(nowSeconds - timestamp) > ANNOUNCE_MAX_SKEW_SECONDS) {
    return { ok: false, error: 'timestamp out of range' };
  }

  let signingPublicKeyBytes;
  let identityPublicKeyBytes;
  try {
    signingPublicKeyBytes = hexToBytes(signing_public_key);
    identityPublicKeyBytes = hexToBytes(identity_public_key);
  } catch {
    return { ok: false, error: 'malformed hex field' };
  }

  // node_id MUST be BLAKE3(signing_public_key || identity_public_key) — see
  // IdentityManager.identityHash on the client. Otherwise a valid Ed25519 signature over a
  // *different* claimed node_id would still pass.
  const expectedNodeId = bytesToHex(blake3(new Uint8Array([...signingPublicKeyBytes, ...identityPublicKeyBytes])));
  if (expectedNodeId !== node_id) {
    return { ok: false, error: 'node_id does not match provided public keys' };
  }

  const message = new TextEncoder().encode(`${node_id}:${port}:${timestamp}`);
  let signatureValid = false;
  try {
    signatureValid = await ed.verifyAsync(hexToBytes(signature), message, signingPublicKeyBytes);
  } catch {
    signatureValid = false;
  }
  if (!signatureValid) {
    return { ok: false, error: 'invalid signature' };
  }

  return { ok: true, node_id, port };
}

// POST /v1/announce — peer announces itself.
app.post('/v1/announce', async (req, res) => {
  const result = await verifyAnnounce(req.body);
  if (!result.ok) {
    return res.status(result.error === 'invalid signature' || result.error.includes('node_id does not match') ? 401 : 400)
      .json({ error: result.error });
  }

  const ip = req.headers['x-forwarded-for']?.split(',')[0].trim() || req.socket.remoteAddress;
  activePeers.set(result.node_id, { host: ip, port: result.port, lastSeen: Date.now() });
  res.json({ ok: true, your_ip: ip });
});

// GET /v1/health
app.get('/v1/health', (req, res) => {
  res.json({
    status: 'ok',
    version: '2.0.0',
    active_peers: activePeers.size,
    uptime_seconds: Math.floor((Date.now() - START_TIME) / 1000),
  });
});

// GET / — simple info page
app.get('/', (req, res) => {
  res.json({
    service: '2Com Bootstrap Node',
    endpoints: ['/v1/seeds', '/v1/announce', '/v1/health'],
  });
});

// Only bind a socket when run directly (`node server.js`) — importing this module from tests
// must not also start a real listener.
if (process.argv[1] && import.meta.url === `file://${process.argv[1]}`) {
  app.listen(PORT, '0.0.0.0', () => {
    console.log(`2Com bootstrap server running on port ${PORT}`);
  });
}

export { app };
