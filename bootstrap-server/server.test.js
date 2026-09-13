import test from 'node:test';
import assert from 'node:assert/strict';
import * as ed from '@noble/ed25519';
import { blake3 } from '@noble/hashes/blake3.js';
import { bytesToHex } from '@noble/hashes/utils.js';
import { verifyAnnounce } from './server.js';

async function validAnnounceBody(overrides = {}) {
  const signingPriv = ed.utils.randomSecretKey();
  const signingPub = await ed.getPublicKeyAsync(signingPriv);
  const identityPub = crypto.getRandomValues(new Uint8Array(33));

  const node_id = bytesToHex(blake3(new Uint8Array([...signingPub, ...identityPub])));
  const port = 49737;
  const timestamp = Math.floor(Date.now() / 1000);
  const message = new TextEncoder().encode(`${node_id}:${port}:${timestamp}`);
  const signature = bytesToHex(await ed.signAsync(message, signingPriv));

  return {
    node_id,
    port,
    timestamp,
    signature,
    signing_public_key: bytesToHex(signingPub),
    identity_public_key: bytesToHex(identityPub),
    ...overrides,
  };
}

test('valid signed announce is accepted', async () => {
  const result = await verifyAnnounce(await validAnnounceBody());
  assert.equal(result.ok, true);
});

test('tampered signature is rejected', async () => {
  const body = await validAnnounceBody();
  body.signature = body.signature.slice(0, -2) + (body.signature.slice(-2) === '00' ? '01' : '00');
  const result = await verifyAnnounce(body);
  assert.equal(result.ok, false);
  assert.equal(result.error, 'invalid signature');
});

test('node_id not matching the provided public keys is rejected', async () => {
  const body = await validAnnounceBody({ node_id: 'a'.repeat(64) });
  const result = await verifyAnnounce(body);
  assert.equal(result.ok, false);
  assert.equal(result.error, 'node_id does not match provided public keys');
});

test('stale timestamp is rejected (replay protection)', async () => {
  const body = await validAnnounceBody();
  const result = await verifyAnnounce(body, body.timestamp + 3600);
  assert.equal(result.ok, false);
  assert.equal(result.error, 'timestamp out of range');
});

test('malformed node_id is rejected before any crypto work', async () => {
  const body = await validAnnounceBody({ node_id: 'not-hex' });
  const result = await verifyAnnounce(body);
  assert.equal(result.ok, false);
  assert.equal(result.error, 'invalid node_id');
});

test('missing fields are rejected', async () => {
  const result = await verifyAnnounce({ node_id: 'a'.repeat(64) });
  assert.equal(result.ok, false);
  assert.equal(result.error, 'missing required field');
});
