# 2Com

**Encrypted peer-to-peer communication for Android**

2Com lets you chat with anyone using only a cryptographic hash — no phone number, no email, no central server. Messages are end-to-end encrypted. Connections are established via DHT (distributed hash table), local mDNS, or Bluetooth.

[![Release](https://img.shields.io/github/v/release/IvarBeIis/2Com)](https://github.com/IvarBeIis/2Com/releases)
[![CI](https://github.com/IvarBeIis/2Com/actions/workflows/pr-checks.yml/badge.svg)](https://github.com/IvarBeIis/2Com/actions/workflows/pr-checks.yml)
[![License: AGPL-3.0](https://img.shields.io/badge/License-AGPL%203.0-blue.svg)](LICENSE)

---

## How it works

1. **Identity** — on first launch, 2Com generates an Ed25519/X25519 key pair locally. Your identity is a BLAKE3 hash of your public keys (64 hex chars).
2. **Add contact** — share your hash via QR code, NFC, or paste it manually. No server involved.
3. **Connect** — the app discovers the other peer via:
   - **mDNS** — same local network (fastest, no internet)
   - **DHT** — internet, via bootstrap seed nodes + hardcoded fallback peers
4. **Encrypt** — all messages use AES-256-GCM with per-message nonces. Database stored via SQLCipher.

---

## Features (beta 1.0)

- Cryptographic identity (Ed25519 + X25519 + BLAKE3)
- QR code sharing
- Add contacts by hash
- Text messaging with delivery status
- Encrypted local storage (SQLCipher)
- Multi-transport: mDNS → DHT
- DHT bootstrap with 5-peer hardcoded fallback
- Dark theme

---

## Architecture

```
app/
├── :app                     # Main entry, navigation, DI wiring
├── :core:crypto             # Identity, Ed25519, BLAKE3, AES-GCM
├── :core:transport          # TransportManager, DhtTransport, MdnsTransport
├── :core:database           # Room + SQLCipher, DAOs, entities
├── :core:common             # Shared utilities, AppResult
├── :feature:onboarding      # Identity generation screen, QR display
├── :feature:contacts        # Add contact, contact list
└── :feature:chat            # Chat list, chat screen, message bubbles
```

**Tech stack:** Kotlin, Jetpack Compose, Hilt, Room, SQLCipher, BouncyCastle, OkHttp/Retrofit, ZXing

---

## Bootstrap Server

2Com uses a lightweight Node.js bootstrap server to help devices join the DHT network.

- Default: `http://80.211.207.41:3000`
- API: `GET /v1/seeds`, `POST /v1/announce`, `GET /v1/health`

If the bootstrap server is unreachable, the app falls back to 5 hardcoded DHT peers (multi-region).

### Run bootstrap locally

```bash
cd bootstrap-server
npm install
npm start
# or via Docker:
docker compose up -d
```

---

## Build

### Requirements
- Android Studio Hedgehog+
- JDK 17
- Android SDK 35

### Debug build
```bash
./gradlew assembleDebug
```

### Run tests
```bash
./gradlew testDebugUnitTest
```

### Release APK (signed)
```bash
export RELEASE_KEYSTORE_PATH=keystore/release.keystore
export RELEASE_KEYSTORE_PASSWORD=...
export RELEASE_KEY_ALIAS=...
export RELEASE_KEY_PASSWORD=...
./gradlew assembleRelease
```

---

## CI/CD

| Workflow | Trigger | Result |
|----------|---------|--------|
| `pr-checks.yml` | PR to main/develop | Tests + debug APK |
| `release.yml` | Push tag `v*` | Release APK + GitHub Release + bootstrap deploy |
| `nightly.yml` | Daily 03:00 UTC | Nightly debug APK |

### Secrets required for release

| Secret | Description |
|--------|-------------|
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded keystore file |
| `RELEASE_KEYSTORE_PASSWORD` | Keystore password |
| `RELEASE_KEY_ALIAS` | Key alias |
| `RELEASE_KEY_PASSWORD` | Key password |
| `VPS_SSH_PRIVATE_KEY` | SSH key for bootstrap server deploy |

---

## DHT Fallback Peers

Built into the APK — used when bootstrap HTTP API is unreachable:

| Host | Port | Region |
|------|------|--------|
| `80.211.207.41` | 49737 | Primary VPS |
| `45.76.100.42` | 49737 | US East (New York) |
| `95.179.200.11` | 49737 | EU (Frankfurt) |
| `139.162.55.73` | 49737 | Asia (Singapore) |
| `178.62.194.88` | 49737 | EU (Amsterdam) |

---

## Security

- All messages encrypted with AES-256-GCM (random nonce per message)
- Identity keys never leave the device
- Database encrypted with SQLCipher + random 256-bit key (stored in Android DataStore)
- Bootstrap server sees only IP + port (no content, no identity)
- Noise XX handshake planned for v1.1 (peer identity verification)

---

## Roadmap

| Version | Features |
|---------|----------|
| 1.0 (beta) | Identity, contacts, text chat, DHT/mDNS |
| 1.1 | Noise XX handshake, NAT hole punching, STUN |
| 1.5 | Voice calls (Opus), push notifications |
| 2.0 | Video calls, screen share |
| 2.x | Group chat, desktop client |

---

## License

[AGPL-3.0](LICENSE)
