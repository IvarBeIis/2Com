'use strict';

const express = require('express');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 3000;
const START_TIME = Date.now();

app.use(cors());
app.use(express.json());

// Active peers: Map<nodeId, { host, port, lastSeen }>
const activePeers = new Map();
const PEER_TTL_MS = 5 * 60 * 1000; // 5 minutes

// Cleanup stale peers every minute
setInterval(() => {
  const now = Date.now();
  for (const [id, peer] of activePeers.entries()) {
    if (now - peer.lastSeen > PEER_TTL_MS) activePeers.delete(id);
  }
}, 60_000);

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

// POST /v1/announce — peer announces itself
app.post('/v1/announce', (req, res) => {
  const { node_id, port } = req.body;
  if (!node_id || !port) return res.status(400).json({ error: 'node_id and port required' });
  if (!/^[0-9a-f]{64}$/.test(node_id)) return res.status(400).json({ error: 'invalid node_id' });

  const ip = req.headers['x-forwarded-for']?.split(',')[0].trim() || req.socket.remoteAddress;
  activePeers.set(node_id, { host: ip, port: Number(port), lastSeen: Date.now() });
  res.json({ ok: true, your_ip: ip });
});

// GET /v1/health
app.get('/v1/health', (req, res) => {
  res.json({
    status: 'ok',
    version: '1.0.0',
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

app.listen(PORT, '0.0.0.0', () => {
  console.log(`2Com bootstrap server running on port ${PORT}`);
});
