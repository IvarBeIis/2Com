# 10 – API Specification

**Projekt:** SecureWhisper
**Verze:** 1.0

---

## 1. Přehled

SecureWhisper nemá tradiční REST/GraphQL API – aplikace je P2P. Existují však tři kategorie kontraktů:

1. **Wire protocol** – binární protokol mezi dvěma instancemi aplikace (přes Noise tunel + Signal Protocol)
2. **Bootstrap node API** – minimální REST API bootstrap nodů pro DHT seedy
3. **Push notification API** – integrace s FCM (resp. UnifiedPush)

---

## 2. Wire Protocol (P2P Communication)

### 2.1 Frame structure

Po navázání Noise + Signal session se veškerá komunikace odehrává přes binární framy:

```
+--------+--------+----------+-----------------+
| ver(1) | type(1)| len(4 BE)| payload (len)   |
+--------+--------+----------+-----------------+
```

**Verze:** `0x01` pro v1 protokolu.

**Typy framů:**

| Type | Kód | Popis |
|------|-----|-------|
| HANDSHAKE_HELLO | 0x01 | Inicializace handshake metadata |
| HANDSHAKE_ACK | 0x02 | Ack handshake |
| MESSAGE | 0x10 | Textová/multimedia zpráva |
| MESSAGE_ACK | 0x11 | Potvrzení doručení |
| MESSAGE_READ | 0x12 | Přečtení |
| TYPING | 0x13 | Indikátor psaní |
| REACTION | 0x14 | Emoji reakce |
| MESSAGE_DELETE | 0x15 | Smazání zprávy (oboustranné) |
| ATTACHMENT_OFFER | 0x20 | Nabídka přílohy |
| ATTACHMENT_ACCEPT | 0x21 | Příjemce souhlasí s příjmem |
| ATTACHMENT_CHUNK | 0x22 | Část přílohy |
| ATTACHMENT_DONE | 0x23 | Konec přílohy |
| CALL_OFFER | 0x30 | Inicializace hovoru (SDP offer) |
| CALL_ANSWER | 0x31 | Odpověď na hovor (SDP answer) |
| CALL_ICE_CANDIDATE | 0x32 | ICE kandidát |
| CALL_HANGUP | 0x33 | Ukončení hovoru |
| KEEPALIVE | 0xF0 | Ping pro udržení spojení |

### 2.2 Payload formáty (Protobuf)

#### MESSAGE
```protobuf
message ChatMessage {
    string message_id = 1;       // UUID
    int64 sent_at = 2;           // Unix milliseconds
    ContentType type = 3;
    oneof content {
        TextContent text = 4;
        AttachmentMetadata attachment = 5;
        ReactionContent reaction = 6;
    }
    string reply_to_id = 7;      // optional
    int32 expires_in_seconds = 8; // 0 = nikdy
}

enum ContentType {
    TEXT = 0;
    IMAGE = 1;
    VIDEO = 2;
    AUDIO = 3;
    FILE = 4;
    REACTION = 5;
}

message TextContent {
    string text = 1;
}

message AttachmentMetadata {
    string filename = 1;
    int64 size_bytes = 2;
    string mime_type = 3;
    bytes sha256 = 4;
    string thumbnail_b64 = 5;     // optional, pro images/videos
}

message ReactionContent {
    string target_message_id = 1;
    string emoji = 2;
    bool remove = 3;
}
```

#### MESSAGE_ACK
```protobuf
message MessageAck {
    string message_id = 1;
    int64 received_at = 2;
}
```

#### CALL_OFFER
```protobuf
message CallOffer {
    string call_id = 1;
    CallType type = 2;
    string sdp = 3;               // WebRTC SDP offer
    int64 timestamp = 4;
}

enum CallType {
    AUDIO = 0;
    VIDEO = 1;
}
```

#### ATTACHMENT_CHUNK
```protobuf
message AttachmentChunk {
    string attachment_id = 1;
    int32 chunk_index = 2;
    bytes data = 3;               // šifrované Signal Protocolem zvenčí
    bool is_last = 4;
}
```

### 2.3 Protokol semantics

#### Doručení zprávy (happy path)

```
A → B:  MESSAGE { id="uuid-1", text="Ahoj" }
B → A:  MESSAGE_ACK { id="uuid-1" }
B → A:  MESSAGE_READ { id="uuid-1" }   (až když uživatel otevře chat)
```

#### Selhání spojení

```
A → B:  MESSAGE { id="uuid-1", text="Ahoj" }
        (timeout 30s, žádný ACK)
A:      Mark message status='UNDELIVERED', queue for retry
A:      Retry every 60s with exponential backoff up to 1h
```

#### Hovor flow

```
A → B:  CALL_OFFER { call_id="c1", type=AUDIO, sdp=... }
B:      Show incoming call UI
B → A:  CALL_ANSWER { call_id="c1", sdp=... }   (na accept)
A ↔ B:  CALL_ICE_CANDIDATE (multiple)
        WebRTC media stream begins
A → B:  CALL_HANGUP { call_id="c1" }
```

---

## 3. Bootstrap Node API (REST)

Bootstrap nodes poskytují minimální REST API pro discovery DHT peers.

### 3.1 Base URL

```
https://bootstrap1.securewhisper.org
https://bootstrap2.securewhisper.org
https://bootstrap3.securewhisper.org
```

### 3.0 Hardcoded fallback peery

Pokud jsou všechny bootstrap HTTP endpointy nedostupné (výpadek, cenzura, DNS blokace), aplikace přejde na hardcoded záložní DHT peery zabudované přímo v APK. Tato záloha zajistí vstup do DHT sítě bez závislosti na HTTP API.

| Host | Port | Node ID (prefix) | Region | Provider |
|------|------|-------------------|--------|----------|
| `45.76.100.42` | 49737 | `a1b2c3d4...` | US East (New York) | Vultr |
| `95.179.200.11` | 49737 | `b2c3d4e5...` | EU (Frankfurt) | Vultr |
| `139.162.55.73` | 49737 | `c3d4e5f6...` | Asia (Singapore) | Linode |
| `178.62.194.88` | 49737 | `d4e5f6a1...` | EU (Amsterdam) | DigitalOcean |
| `2a01:4f8:c0c:9abc::1` | 49737 | `e5f6a1b2...` | EU IPv6 (Helsinki) | Hetzner |

**Rozhodovací logika klienta:**
```
1. Pokus o GET /v1/seeds (timeout 5 s, retry 1×)
2. Při selhání → použít HARDCODED_FALLBACK list
3. Paralelní TCP connect ke všem fallback peerům (timeout 10 s)
4. Po úspěšném vstupu do DHT bootstrap peery dále nekontaktujeme
```

Fallback peery jsou shodné s hlavními bootstrap nodes (stejné servery), ale přistupují se přímo přes IP:port bez DNS a HTTP — odolné vůči DNS blokaci i výpadku web serveru.

### 3.2 Endpoints

#### `GET /v1/seeds`

Vrátí seznam aktivních DHT peerů pro bootstrap.

**Request:**
```
GET /v1/seeds HTTP/1.1
Accept: application/json
User-Agent: SecureWhisper/1.0 Android
```

**Response 200:**
```json
{
  "seeds": [
    {
      "host": "203.0.113.10",
      "port": 49737,
      "node_id": "8a1f..."
    },
    {
      "host": "[2001:db8::1]",
      "port": 49737,
      "node_id": "c3d2..."
    }
  ],
  "ttl_seconds": 300
}
```

**Rate limiting:** 10 req/min per IP, 1000 req/hour per IP.

#### `GET /v1/health`

```
GET /v1/health HTTP/1.1
```

**Response 200:**
```json
{
  "status": "ok",
  "version": "1.0.3",
  "active_peers": 12453,
  "uptime_seconds": 8294737
}
```

#### `POST /v1/relay-token` (volitelný, jen pro Plus uživatele)

Vyžádá si token pro použití komunitního TURN relay.

**Request:**
```json
{
  "client_id": "<anonymized random ID>",
  "subscription_proof": "<signed token from licensing service>"
}
```

**Response 200:**
```json
{
  "turn_url": "turn:relay.securewhisper.org:3478",
  "username": "1715164800:user_abc",
  "password": "...",
  "expires_at": 1715168400
}
```

### 3.3 OpenAPI specifikace

```yaml
openapi: 3.1.0
info:
  title: SecureWhisper Bootstrap API
  version: 1.0.0
  description: Minimal API for DHT bootstrap and optional relay tokens
servers:
  - url: https://bootstrap1.securewhisper.org
paths:
  /v1/seeds:
    get:
      summary: Get list of DHT seed peers
      responses:
        '200':
          description: Seed list
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/SeedList'
        '429':
          description: Rate limited
  /v1/health:
    get:
      summary: Health check
      responses:
        '200':
          description: Healthy
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Health'
components:
  schemas:
    SeedList:
      type: object
      required: [seeds, ttl_seconds]
      properties:
        seeds:
          type: array
          items:
            $ref: '#/components/schemas/Seed'
        ttl_seconds:
          type: integer
    Seed:
      type: object
      required: [host, port, node_id]
      properties:
        host: { type: string }
        port: { type: integer }
        node_id: { type: string }
    Health:
      type: object
      properties:
        status: { type: string, enum: [ok, degraded] }
        version: { type: string }
        active_peers: { type: integer }
        uptime_seconds: { type: integer }
```

---

## 4. Push Notifications API

### 4.1 FCM (Google Play varianta)

Push notifikace **nikdy neobsahují obsah zprávy**. Slouží pouze k probuzení aplikace.

**Payload formát:**
```json
{
  "data": {
    "type": "wakeup",
    "v": "1"
  },
  "priority": "high"
}
```

Po obdržení FCM zprávy:
1. WorkManager spustí `WakeupWorker`
2. Worker připojí DHT
3. Začne přijímat příchozí spojení
4. Po 30 s nečinnosti se odpojí (battery optimization)

### 4.2 UnifiedPush (F-Droid varianta)

Pro F-Droid build se používá UnifiedPush — uživatelem zvolený distributor.

**Endpoint format:** custom HTTPS endpoint poskytnutý UP distributorem.

**Payload:**
```
POST {distributor_endpoint}
Content-Type: application/octet-stream

<32-byte opaque blob>
```

Aplikace registruje se u distributora a předá `endpoint` URL peerům přes Signal session jako "preferred wakeup channel".

---

## 5. Versioning a kompatibilita

### 5.1 Wire protocol versioning

Frame header obsahuje `ver` byte. Při setkání s vyšší verzí:
- Klient SHOULD pokusit se spojení s dohodou na nejvyšší společné verzi
- Klient MUSÍ odmítnout neznámé framy s `UNKNOWN_FRAME` errorem
- Backward compatibility se musí udržovat min. 24 měsíců

### 5.2 Bootstrap API versioning

- URL prefix `/v1/`, `/v2/` atd.
- Stará verze podporována min. 12 měsíců po release nové
- Klienti SHOULD posílat `User-Agent` pro telemetrii

### 5.3 Deprecation policy

- Deprecation oznámena v release notes 6 měsíců předem
- Soft warnings v aplikaci 3 měsíce předem
- Hard cutoff s nucenou aktualizací

---

## 6. Bezpečnostní úvahy API

### 6.1 Bootstrap API
- Veškerá komunikace přes HTTPS s certificate pinning
- Bootstrap odpovědi nesmí obsahovat žádné PII
- Rate limiting per IP a per fingerprint
- Neukládat IP adresy klientů (zero-log policy)

### 6.2 Wire protocol
- Veškerý payload (kromě header) je zašifrovaný Double Ratchet
- Replay protection přes Signal Protocol nonces
- Validation všech zpráv proti schema před zpracováním

### 6.3 Push notifications
- Žádný obsah zprávy v notifikaci
- Token je opaque, neobsahuje peer hash
- Při přihlášení nového zařízení se starý token invaliduje
