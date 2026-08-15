# 09 – Technical Architecture (HLD & LLD)

**Projekt:** SecureWhisper
**Verze:** 1.0

---

## ČÁST A: HIGH-LEVEL DESIGN (HLD)

## 1. Architektonický přehled

SecureWhisper používá **Clean Architecture** s **MVVM** pattern v presentation vrstvě. Aplikace je modulární s jasným oddělením concerns mezi transportní, šifrovací, doménovou a prezentační vrstvou.

## 2. Architektonické vrstvy

```
┌────────────────────────────────────────────────────┐
│              PRESENTATION LAYER                    │
│  Jetpack Compose + ViewModel + StateFlow           │
│  (Activities, Composables, Navigation)             │
├────────────────────────────────────────────────────┤
│                DOMAIN LAYER                        │
│  Use Cases + Repository Interfaces                 │
│  (Pure Kotlin, no Android dependencies)            │
├────────────────────────────────────────────────────┤
│                  DATA LAYER                        │
│  Repository Implementations + Mappers              │
│  (Coordinates between local DB and network)        │
├────────────────────────────────────────────────────┤
│      LOCAL DB         │      NETWORK LAYER         │
│  Room over SQLCipher  │  Transport Manager         │
│                       │  ├── Hyperswarm DHT        │
│                       │  ├── mDNS Discovery        │
│                       │  ├── Bluetooth LE          │
│                       │  ├── WiFi Direct           │
│                       │  └── WebRTC Media          │
├────────────────────────────────────────────────────┤
│              CRYPTOGRAPHY LAYER                    │
│  libsignal (Double Ratchet) + Noise Protocol       │
│  Android Keystore wrapper                          │
└────────────────────────────────────────────────────┘
```

## 3. Klíčové moduly

### 3.1 `:app`
Application module s DI (Hilt), navigation, theme. Sjednocuje všechny ostatní moduly.

### 3.2 `:feature:onboarding`
Identity generation, onboarding flows.

### 3.3 `:feature:contacts`
Add contact (QR scan, NFC, manual), contact list, safety number verification.

### 3.4 `:feature:chat`
Konverzace, message list, attachments, typing indicators, reactions.

### 3.5 `:feature:calls`
Audio/video call UI, in-call controls, ringer.

### 3.6 `:feature:settings`
Preferences, app lock, backup/restore, advanced.

### 3.7 `:core:crypto`
Wrapper nad libsignal a Noise Protocol. Identity management.

### 3.8 `:core:transport`
Multi-transport manager (DHT, mDNS, BLE, WiFi Direct, WebRTC).

### 3.9 `:core:database`
Room nad SQLCipher, migrations, DAOs.

### 3.10 `:core:notifications`
FCM integration, local notifications, channel management.

### 3.11 `:core:common`
Shared utilities, extensions, error handling.

## 4. Datový tok

### 4.1 Odeslání zprávy

```
[User input]
    ↓
ChatViewModel.sendMessage(text)
    ↓
SendMessageUseCase
    ↓
MessageRepository.send()
    ├── Encrypt via SignalSession (libsignal)
    ├── Persist locally (encrypted_message + status='SENDING')
    ↓
TransportManager.send(peerId, ciphertext)
    ├── Try active connection
    ├── Fallback: establish new connection
    ↓
Notify peer device
    ↓
Update status='DELIVERED' (on ACK)
    ↓
ViewModel updates UI
```

### 4.2 Příchozí spojení

```
TransportManager listener
    ↓
Validate peer hash matches handshake key
    ↓
Run Noise XX handshake
    ↓
Establish Signal session (Double Ratchet)
    ↓
Process incoming frames:
    ├── Chat message → MessageRepository.receive()
    ├── Call invite → CallManager.handleIncoming()
    ├── Typing indicator → ConversationViewModel
    └── Reaction → MessageRepository.addReaction()
```

## 5. Klíčové externí závislosti

| Knihovna | Účel | Verze |
|----------|------|-------|
| libsignal-android | Double Ratchet, X3DH | nejnovější |
| Hyperswarm (přes JNI) | DHT discovery & connection | nejnovější |
| WebRTC Android | Media transport | M120+ |
| SQLCipher | Encrypted local DB | 4.x |
| Room | DB ORM | 2.6+ |
| Hilt | DI | 2.51+ |
| Compose | UI | 1.7+ |
| Noise Java | Handshake protocol | 1.x |
| ZXing | QR generation/scanning | 3.x |

## 6. Multi-transport strategie

```
                  ┌─────────────────┐
                  │ ConnectionManager│
                  └────────┬────────┘
                           │
         ┌─────────────────┼─────────────────┐
         ▼                 ▼                 ▼
   ┌──────────┐      ┌──────────┐      ┌──────────┐
   │  Local   │      │ Internet │      │ Proximity│
   └─────┬────┘      └─────┬────┘      └─────┬────┘
         │                 │                 │
    ┌────┴────┐       ┌────┴────┐       ┌────┴────┐
    │  mDNS   │       │ DHT     │       │ BLE     │
    │         │       │ STUN    │       │ WiFi-D  │
    │         │       │ Relay   │       │ NFC     │
    └─────────┘       └─────────┘       └─────────┘
```

**Selection priority** (configurable):
1. Existing active connection
2. mDNS (LAN, lowest latency)
3. WiFi Direct (proximity, high bandwidth)
4. DHT direct (with hole punching)
5. DHT through relay
6. Bluetooth LE (last resort, low bandwidth)

### 6.1 DHT bootstrap strategie

DHT vstup probíhá dvoustupňově:

1. **Primární:** Stažení seed listů z bootstrap HTTP API (`/v1/seeds`)
2. **Fallback:** Při nedostupnosti API se použijí hardcoded záložní peery

Hardcoded fallback DHT peery (zabudované v APK, aktualizovatelné pouze novou verzí):

```kotlin
object DhtBootstrapPeers {
    val HARDCODED_FALLBACK = listOf(
        DhtPeer(host = "45.76.100.42",         port = 49737, nodeId = "a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890", region = "us-east"),
        DhtPeer(host = "95.179.200.11",        port = 49737, nodeId = "b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1", region = "eu-frankfurt"),
        DhtPeer(host = "139.162.55.73",        port = 49737, nodeId = "c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2", region = "ap-singapore"),
        DhtPeer(host = "178.62.194.88",        port = 49737, nodeId = "d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3", region = "eu-amsterdam"),
        DhtPeer(host = "2a01:4f8:c0c:9abc::1", port = 49737, nodeId = "e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4e5f67890a1b2c3d4", region = "eu-helsinki-ipv6"),
    )
}
```

| Host | Port | Region | Provider |
|------|------|--------|----------|
| `45.76.100.42` | 49737 | US East (New York) | Vultr |
| `95.179.200.11` | 49737 | EU (Frankfurt) | Vultr |
| `139.162.55.73` | 49737 | Asia (Singapore) | Linode |
| `178.62.194.88` | 49737 | EU (Amsterdam) | DigitalOcean |
| `2a01:4f8:c0c:9abc::1` | 49737 | EU IPv6 (Helsinki) | Hetzner |

Logika výběru v `HyperswarmTransport`:
```
1. Fetch /v1/seeds → pokud OK, použij vrácené peery
2. Pokud HTTP selže nebo timeout (5 s) → použij HARDCODED_FALLBACK
3. Paralelní connect ke všem fallback peerům, použij první úspěšný
4. Po vstupu do DHT sítě bootstrap peery dále nepotřebujeme
```

---

## ČÁST B: LOW-LEVEL DESIGN (LLD)

## 7. Identity Management

### 7.1 Klíčový pár

```kotlin
data class Identity(
    val signingKeyPair: Ed25519KeyPair,    // Pro autentizaci
    val agreementKeyPair: X25519KeyPair,    // Pro key exchange
    val createdAt: Instant,
    val identityHashHex: String             // BLAKE3(signingPublicKey || agreementPublicKey)
)

class IdentityManager(
    private val keystore: AndroidKeystoreWrapper,
    private val secureRandom: SecureRandom
) {
    suspend fun generateIdentity(): Identity { /* ... */ }
    suspend fun loadIdentity(): Identity? { /* ... */ }
    suspend fun exportIdentity(passphrase: CharArray): ByteArray { /* ... */ }
    suspend fun importIdentity(data: ByteArray, passphrase: CharArray): Identity { /* ... */ }
}
```

### 7.2 Identity hash format

```
hash = BLAKE3(
    "SecureWhisper-Identity-v1" ||
    signingPublicKey (32 bytes) ||
    agreementPublicKey (32 bytes)
)

display: 64 hex characters, např.
"a3f4 e2c1 9d8b 6740 b218 5f9a 3c0e 7d12 8e4b 6a9f 2d10 c8e7 4f53 a619 2b8d 30ce"
```

## 8. Connection Establishment Flow

### 8.1 Handshake state machine

```
[INITIATING] → [TRANSPORT_DISCOVERY] → [TRANSPORT_CONNECTING]
    ↓                                        ↓
[FAILED]    ←   [HANDSHAKE_FAILED]   ←   [NOISE_HANDSHAKE]
                                             ↓
                                     [SIGNAL_SESSION_INIT]
                                             ↓
                                     [VERIFIED] → [ACTIVE]
                                             ↓
                                        [CLOSED]
```

### 8.2 Noise XX handshake nad TCP/QUIC

```
Initiator                                Responder
   |                                         |
   |--- e -------------------------------> |   (ephemeral key)
   |                                         |
   |   <--- e, ee, s, es ----------------- |   (responder identity)
   |                                         |
   |--- s, se ---------------------------> |   (initiator identity)
   |                                         |
   |   <-- transport mode ----------------- |
```

Po handshaku se ověří, že hash(static_key) odpovídá očekávanému `peerHash`. Při neshodě → reject.

### 8.3 Signal session bootstrap

Po Noise handshaku se naváže Signal session:
1. Initiator vygeneruje `PreKeyBundle` (signed prekey + one-time prekey)
2. Vymění bundles oboustranně přes Noise tunel
3. Initiator provede X3DH → vytvoří session
4. Od této chvíle veškerá data jdou přes Double Ratchet

## 9. Database schema

### 9.1 Hlavní tabulky

```sql
-- Identity (1 řádek pouze)
CREATE TABLE identity (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    public_signing_key BLOB NOT NULL,
    public_agreement_key BLOB NOT NULL,
    keystore_alias TEXT NOT NULL,
    identity_hash TEXT NOT NULL UNIQUE,
    created_at INTEGER NOT NULL
);

-- Kontakty
CREATE TABLE contacts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    peer_hash TEXT NOT NULL UNIQUE,
    public_signing_key BLOB NOT NULL,
    public_agreement_key BLOB NOT NULL,
    display_name TEXT,
    safety_number TEXT NOT NULL,
    is_verified INTEGER DEFAULT 0,
    is_blocked INTEGER DEFAULT 0,
    last_seen_at INTEGER,
    created_at INTEGER NOT NULL,
    notes TEXT
);

-- Konverzace
CREATE TABLE conversations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    contact_id INTEGER NOT NULL,
    last_message_at INTEGER,
    unread_count INTEGER DEFAULT 0,
    is_archived INTEGER DEFAULT 0,
    is_muted_until INTEGER,
    disappearing_messages_seconds INTEGER DEFAULT 0,
    FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE CASCADE
);

-- Zprávy
CREATE TABLE messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    conversation_id INTEGER NOT NULL,
    sender_is_self INTEGER NOT NULL,
    content_type TEXT NOT NULL, -- TEXT, IMAGE, VIDEO, FILE, AUDIO, REACTION, REPLY
    content TEXT,                -- TEXT content nebo JSON metadata
    attachment_path TEXT,        -- Lokální cesta k zašifrované příloze
    attachment_size INTEGER,
    attachment_mime TEXT,
    reply_to_message_id INTEGER,
    status TEXT NOT NULL,        -- SENDING, SENT, DELIVERED, READ, FAILED
    sent_at INTEGER NOT NULL,
    delivered_at INTEGER,
    read_at INTEGER,
    expires_at INTEGER,          -- pro disappearing
    is_deleted INTEGER DEFAULT 0,
    FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE,
    FOREIGN KEY (reply_to_message_id) REFERENCES messages(id) ON DELETE SET NULL
);

CREATE INDEX idx_messages_conv ON messages(conversation_id, sent_at DESC);
CREATE INDEX idx_messages_expires ON messages(expires_at) WHERE expires_at IS NOT NULL;

-- Signal session state (per peer)
CREATE TABLE signal_sessions (
    contact_id INTEGER PRIMARY KEY,
    session_state BLOB NOT NULL, -- libsignal serialized state
    last_updated INTEGER NOT NULL,
    FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE CASCADE
);

-- Hovor history
CREATE TABLE call_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    contact_id INTEGER NOT NULL,
    call_type TEXT NOT NULL, -- AUDIO, VIDEO
    direction TEXT NOT NULL, -- INCOMING, OUTGOING
    state TEXT NOT NULL,     -- COMPLETED, MISSED, REJECTED, FAILED
    started_at INTEGER NOT NULL,
    ended_at INTEGER,
    duration_seconds INTEGER,
    FOREIGN KEY (contact_id) REFERENCES contacts(id) ON DELETE CASCADE
);

-- Reactions
CREATE TABLE reactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    message_id INTEGER NOT NULL,
    emoji TEXT NOT NULL,
    is_self INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    UNIQUE(message_id, emoji, is_self)
);
```

## 10. Klíčové třídy

### 10.1 TransportManager

```kotlin
class TransportManager(
    private val transports: List<Transport>,
    private val identityManager: IdentityManager
) {
    suspend fun connect(peerHash: String): ConnectionResult
    suspend fun disconnect(peerHash: String)
    fun activeConnections(): Flow<List<Connection>>
    fun incomingConnections(): Flow<IncomingConnection>
}

interface Transport {
    val name: String
    val priority: Int
    suspend fun isAvailable(): Boolean
    suspend fun connect(peerHash: String): Connection?
    fun listen(): Flow<IncomingConnection>
}

class HyperswarmTransport(
    private val bootstrapApiUrl: String = "https://bootstrap1.securewhisper.org",
    private val fallbackPeers: List<DhtPeer> = DhtBootstrapPeers.HARDCODED_FALLBACK
) : Transport {
    override suspend fun connect(peerHash: String): Connection? {
        val seeds = fetchSeeds() ?: fallbackPeers  // HTTP fallback na hardcoded
        return joinDht(seeds).lookup(peerHash)
    }
    private suspend fun fetchSeeds(): List<DhtPeer>? = runCatching {
        withTimeout(5_000) { bootstrapApi.getSeeds() }
    }.getOrNull()
}
class MdnsTransport : Transport { /* ... */ }
class BleTransport : Transport { /* ... */ }
class WifiDirectTransport : Transport { /* ... */ }
```

### 10.2 MessageRepository

```kotlin
class MessageRepositoryImpl(
    private val db: AppDatabase,
    private val transportManager: TransportManager,
    private val sessionManager: SignalSessionManager
) : MessageRepository {
    override suspend fun send(conversationId: Long, content: MessageContent): Result<Message>
    override suspend fun receive(peerHash: String, ciphertext: ByteArray): Message
    override fun observeConversation(conversationId: Long): Flow<List<Message>>
    override suspend fun markRead(conversationId: Long)
    override suspend fun delete(messageId: Long)
}
```

### 10.3 CallManager

```kotlin
class CallManager(
    private val webRtcManager: WebRtcManager,
    private val sessionManager: SignalSessionManager
) {
    val currentCall: StateFlow<Call?>
    suspend fun initiateCall(peerHash: String, type: CallType): Call
    suspend fun acceptCall(call: Call)
    suspend fun rejectCall(call: Call)
    suspend fun endCall()
    fun toggleMute()
    fun toggleVideo()
    fun switchCamera()
}
```

## 11. Threading model

- **UI thread:** pouze Compose rendering a state observation
- **Default Dispatcher:** computation (crypto, parsing)
- **IO Dispatcher:** network, disk
- **Single-thread Crypto context:** sériové operace s Signal session (kvůli ratchet state)
- **WebRTC threads:** managed internally by WebRTC native

## 12. Error handling strategie

### 12.1 Vrstvené error types

```kotlin
sealed class AppError {
    sealed class Network : AppError() {
        object NoConnection : Network()
        object PeerUnreachable : Network()
        data class Timeout(val transport: String) : Network()
    }
    sealed class Crypto : AppError() {
        object InvalidSignature : Crypto()
        object SessionCorrupted : Crypto()
        data class HandshakeFailed(val reason: String) : Crypto()
    }
    sealed class Storage : AppError() {
        object DatabaseLocked : Storage()
        object DiskFull : Storage()
    }
    data class Unknown(val cause: Throwable) : AppError()
}
```

### 12.2 Recovery strategies

| Error | Strategie |
|-------|-----------|
| Network.NoConnection | Retry s exponential backoff, queue zprávu |
| Network.PeerUnreachable | Mark "čeká na připojení peera", periodic retry |
| Crypto.SessionCorrupted | Reset session, vyžadovat re-handshake |
| Crypto.HandshakeFailed | Upozornit uživatele na možný MITM |
| Storage.DatabaseLocked | Wait + retry, max 3× |
| Storage.DiskFull | Show error, navigate to storage management |

## 13. Build varianty

| Varianta | Distribuce | FCM | Reproducible |
|----------|------------|-----|--------------|
| `playRelease` | Google Play | ✓ | ✓ |
| `fdroidRelease` | F-Droid | ✗ (UnifiedPush) | ✓ |
| `playDebug` | Internal testing | ✓ | – |
| `fdroidDebug` | Internal testing | ✗ | – |
