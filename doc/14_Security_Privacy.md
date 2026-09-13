# 14 – Security & Privacy Document

**Projekt:** SecureWhisper
**Verze:** 1.0

---

## 1. Threat model

### 1.1 Aktiva, která chráníme

| Aktivum | Citlivost | Důsledek kompromitace |
|---------|-----------|------------------------|
| Privátní klíče uživatele | Kritická | Ztráta identity, MITM riziko |
| Obsah zpráv (text, media) | Vysoká | Únik soukromé komunikace |
| Metadata komunikace (kdo s kým) | Vysoká | Sociální graf, sledování |
| Lokální DB (kontakty, historie) | Vysoká | Kompletní obraz aktivity |
| Identita peera (hex hash) | Střední | Kontaktovatelnost, traffic analysis |

### 1.2 Adversariální modely

| Útočník | Schopnosti | Mitigace |
|---------|------------|----------|
| Pasivní pozorovatel sítě (ISP, vláda) | Sleduje veškerý traffic | E2E šifrování, padding, optional Tor |
| Aktivní MITM | Modifikuje pakety, falešné certifikáty | Noise XX handshake, key pinning, safety number |
| Kompromitovaný bootstrap node | Lže o peer locations | Šifrování odolné vůči routing tampering |
| Útočník s fyzickým přístupem k zařízení (krátkodobý) | Sleduje displej, vytahuje USB | App lock, snímání obrazovky off, hidden recents |
| Útočník s fyzickým přístupem (dlouhodobý) | Forenzní analýza | Hardware-backed keystore, SQLCipher, no cloud backup |
| Malicious upload do storu | Trojan v aplikaci | Reproducible builds, signing verification |
| Útočník s root přístupem k telefonu | Plné OS privileges | Limited mitigation, root detection warning |
| State-level attacker | Vše výše + zero-days | Defense-in-depth, audit, transparentní kód |

### 1.3 Out-of-scope

- Útoky na uživatele přes social engineering (phishing)
- Útoky vyžadující kompromitaci HW (chip-level)
- Compelled access (klíč pod nátlakem) – mimo scope MVP

---

## 2. Kryptografická specifikace

### 2.1 Stack

| Vrstva | Algoritmus | Klíčová délka |
|--------|------------|---------------|
| Identita – signing | Ed25519 | 256 bit |
| Identita – key exchange | X25519 (Curve25519) | 256 bit |
| Identity hash | BLAKE3 | 256 bit |
| Handshake + messaging | Signal Protocol (X3DH PreKeyBundle exchange + Double Ratchet), via `org.signal:libsignal` | – |
| Symetrické šifrování zpráv | ChaCha20-Poly1305 | 256 bit |
| Symetrické šifrování souborů | AES-256-GCM | 256 bit |
| Mediální stream | DTLS-SRTP (AES-128-GCM nebo AES-256-GCM) | 128/256 bit |
| Lokální DB | SQLCipher (AES-256-CBC + HMAC-SHA512) | 256 bit |
| Backup export | age (X25519 + ChaCha20-Poly1305) | – |
| KDF (passphrase → klíč) | Argon2id (m=64MB, t=3, p=4) | – |

### 2.2 Identity generation

```
1. Generate Ed25519 keypair using OS-secure CSPRNG (SecureRandom backed by /dev/urandom)
2. Generate X25519 keypair using same CSPRNG
3. Envelope-encrypt both private keys with an AES-256-GCM key generated natively inside
   Android Keystore (StrongBox-backed if available, see 3.2) before writing to disk — the
   raw Ed25519/X25519/libsignal key material itself cannot be generated *inside* Keystore
   (Keystore has no import/export path for these curves that libsignal's session code can
   use), so envelope encryption of externally-generated keys is the correct pattern, same
   as used by Signal's own Android client
4. Compute identity_hash = BLAKE3(
     "SecureWhisper-Identity-v1" ||
     ed25519_public ||
     x25519_public
   )
5. Display first 64 hex chars to user
```

### 2.3 Safety number derivace

```
fingerprint_self = BLAKE3(domain || self.ed25519_pub || self.x25519_pub)[0..30]
fingerprint_peer = BLAKE3(domain || peer.ed25519_pub || peer.x25519_pub)[0..30]

safety_number = sort([fingerprint_self, fingerprint_peer])
                |> join("")
                |> to_hex
                |> chunk_into_groups_of_5

domain = "SecureWhisper-SafetyNumber-v1"
```

Výsledek: 60 hex znaků zobrazených ve 12 skupinách po 5.

### 2.4 Forward secrecy a post-compromise security

**Forward secrecy:** Garantována Double Ratchet (každá zpráva má vlastní klíč odvozený z předchozího chain key).

**Post-compromise security:** Pokud je Diffie-Hellman ratchet nezkompromitován, future zprávy zůstávají bezpečné po další DH výměně.

**Limitace:** Pokud útočník kompromituje long-term identity klíč, může se vydávat za uživatele do té doby, než dojde k re-verifikaci safety number.

---

## 3. Klíčový management

### 3.1 Hierarchie klíčů

```
Android Keystore (StrongBox) — AES-256-GCM wrapping keys only, never leave Keystore
├── identity_wrap_v1   (wraps identity Ed25519 + X25519 private keys, envelope-encrypted on disk)
└── db_master_v1       (wraps the SQLCipher database passphrase)

In-memory (cleared on app close/lock)
├── Decrypted Signal session states
└── Active connection ephemeral keys

Local DB (SQLCipher encrypted, passphrase itself envelope-encrypted by db_master_v1)
├── Signal session state (per peer)
├── Pre-keys
└── Per-attachment encryption keys
```

### 3.2 Keystore parametry

```kotlin
val keyGenSpec = KeyGenParameterSpec.Builder(
    "identity_wrap_v1",
    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
    .setKeySize(256)
    .setIsStrongBoxBacked(true)               // Hardware security if available
    .setUserAuthenticationRequired(false)      // false – ratchet nesmí blokovat
    .setInvalidatedByBiometricEnrollment(false)
    .build()
```

Toto AES-GCM klíč jen *obaluje* (envelope-encrypts) externě vygenerovaný Ed25519/X25519
privátní materiál před uložením — Keystore nativně negeneruje klíče na křivkách, které
`libsignal` umí přímo použít pro X3DH/Double Ratchet.

**Database master key:**

```kotlin
val dbKeySpec = KeyGenParameterSpec.Builder("db_master_v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
    .setKeySize(256)
    .setIsStrongBoxBacked(true)
    .setUserAuthenticationRequired(true)       // App lock vyžadován pro DB
    .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
    .build()
```

### 3.3 Key rotation

| Klíč | Rotace |
|------|--------|
| Identity (Ed25519/X25519) | Pouze user-iniciovaná (znamená novou identitu) |
| Signed prekey | Každých 7 dní |
| One-time prekeys | Spotřebovány s každým novým session |
| Double Ratchet chain keys | Per zpráva |
| Database master key | Při změně PIN/biometrie |

### 3.4 Key compromise response

**Pokud je identity klíč kompromitován:**
1. User generuje novou identitu (nový hex kód)
2. Aplikace označí starý hash jako "abandoned"
3. Uživatel manuálně sdílí nový kód s kontakty
4. **Žádný revocation list neexistuje** (kompromis pro decentralizaci)

---

## 4. Síťová bezpečnost

### 4.1 Transport security

- **Bootstrap API:** HTTPS only, TLS 1.3, certificate pinning
- **Peer-to-peer:** Noise XX → Signal Protocol (no TLS at this layer)
- **Media (calls):** DTLS-SRTP over UDP

### 4.2 Certificate pinning (bootstrap)

```kotlin
val pinner = CertificatePinner.Builder()
    .add("bootstrap1.securewhisper.org", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
    .add("bootstrap2.securewhisper.org", "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
    .build()
```

Backup pin pro každý host. Pin update via in-app update.

### 4.3 DNS security

- DoH (DNS over HTTPS) pro bootstrap resolution (default: Cloudflare 1.1.1.1, configurable)
- Optional DNSSEC validation
- IP fallback hardcoded ve aplikaci pro případ DNS blokace

Hardcoded fallback IP adresy (odolné vůči DNS blokaci):

```kotlin
// Přímý DHT přístup bez DNS – při výpadku nebo blokaci DNS
val HARDCODED_FALLBACK_IPS = listOf(
    "45.76.100.42",         // US East (New York)    – Vultr
    "95.179.200.11",        // EU (Frankfurt)        – Vultr
    "139.162.55.73",        // Asia (Singapore)      – Linode
    "178.62.194.88",        // EU (Amsterdam)        – DigitalOcean
    "2a01:4f8:c0c:9abc::1", // EU IPv6 (Helsinki)   – Hetzner
)
// port: 49737 (DHT), přístupné přímo bez HTTP/DNS
```

Bezpečnostní vlastnost: přímé IP:port spojení obchází DNS, ale **stále podléhá Noise XX handshaku** — kompromitovaný IP nemůže podvrhnout identitu peera.

### 4.4 Tor support (Release 2.0+)

- Optional integration s Orbot (Android Tor proxy)
- Bootstrap a DHT queries přes Tor
- Latency increase ~3-5×, ale chrání metadata

---

## 5. Local data security

### 5.1 SQLCipher konfigurace

```kotlin
val passphrase = SQLiteDatabase.getBytes(dbKey)
val factory = SupportFactory(passphrase)

val db = Room.databaseBuilder(context, AppDatabase::class.java, "securewhisper.db")
    .openHelperFactory(factory)
    .build()
```

PRAGMA settings:
```sql
PRAGMA cipher_page_size = 4096;
PRAGMA kdf_iter = 256000;
PRAGMA cipher_hmac_algorithm = HMAC_SHA512;
PRAGMA cipher_kdf_algorithm = PBKDF2_HMAC_SHA512;
PRAGMA cipher_memory_security = ON;
```

### 5.2 EncryptedSharedPreferences

Pro non-DB sensitive data (FCM tokens, app lock state):

```kotlin
val masterKey = MasterKey.Builder(context)
    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
    .setUserAuthenticationRequired(false)
    .build()

val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "secure_prefs",
    masterKey,
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

### 5.3 Attachment encryption

```kotlin
fun encryptAttachment(plainBytes: ByteArray): EncryptedAttachment {
    val key = generateRandomKey(256)
    val iv = generateRandomIv(12)
    
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
    val ciphertext = cipher.doFinal(plainBytes)
    
    return EncryptedAttachment(ciphertext, iv, key)
    // key je uložen v messages.attachment_key v DB (chráněno SQLCipher)
}
```

### 5.4 Memory hygiene

- Sensitive ByteArrays: explicit `Arrays.fill(array, 0)` po použití
- CharArrays pro passphrases (na rozdíl od String — immutable)
- `BiometricPrompt.AuthenticationCallback` cleanup

---

## 6. Application-level security

### 6.1 App lock

- **Default:** Off (uživatel se rozhodne v onboardingu)
- **Mechanismy:** Biometrický (BiometricPrompt) nebo PIN (6+ digits)
- **Timeout:** Po 1/5/15 min nečinnosti (configurable)
- **Implementace:** Při locknutí se invaliduje DB master key cache → re-auth potřebná

### 6.2 Screenshot prevention

Aplikováno na obrazovkách:
- Konverzace (configurable per chat)
- Identity / safety number (always)
- App lock screen (always)
- Backup / export (always)

```kotlin
window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
```

### 6.3 Recents protection

```kotlin
// V Activity onCreate
window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

// Custom task description
val taskDesc = ActivityManager.TaskDescription("SecureWhisper", null, ContextCompat.getColor(this, R.color.primary))
setTaskDescription(taskDesc)
```

### 6.4 Root / debugger detection

- Detection via `RootBeer` knihovna nebo equivalent
- **Akce:** Soft warning v UI ("Vaše zařízení je rootnuto, což snižuje bezpečnost"), NE hard block
- Důvod: Privacy-conscious uživatelé často používají GrapheneOS/LineageOS nebo root pro legitimní důvody

### 6.5 Anti-tampering

- APK signing v2/v3
- Reproducible builds enable verification
- Při detekci modifikovaného binárního: warning, ne block

---

## 7. GDPR compliance

### 7.1 Datové role

- **Provozovatel (controller):** Provozovatel (vývojářský subjekt) pouze pro:
  - Bootstrap nodes (ephemeral data)
  - FCM token (opaque, nemapovatelné na uživatele)
- **Provozovatel (controller) NEUKLÁDÁ:**
  - Žádný uživatelský obsah
  - Žádné kontakty
  - Žádná identita uživatele
  - Žádná metadata komunikace

### 7.2 Právní základy zpracování

| Data | Účel | Právní základ |
|------|------|---------------|
| Bootstrap connection log (5 min TTL) | Funkčnost služby | Oprávněný zájem (čl. 6/1/f) |
| FCM token | Push notifikace | Souhlas (čl. 6/1/a) |
| Crash reporty (jen on opt-in) | Stabilita | Souhlas (čl. 6/1/a) |

### 7.3 Práva subjektů

| Právo | Naplnění |
|-------|----------|
| Právo na informace (čl. 13) | Privacy Policy v aplikaci a na webu |
| Právo na přístup (čl. 15) | Veškerá data jsou lokální – uživatel je vlastní |
| Právo na opravu (čl. 16) | Uživatel sám edituje svá data |
| Právo na výmaz (čl. 17) | „Wipe all data" funkce + odpojení FCM |
| Právo na omezení (čl. 18) | Disable FCM, opt-out z analytics |
| Právo na přenositelnost (čl. 20) | Export do age-encrypted backup |
| Právo vznést námitku (čl. 21) | Odhlášení z FCM, smazání aplikace |

### 7.4 Privacy by design

- **Data minimization:** Sbíráme jen minimum nutné pro funkčnost
- **Purpose limitation:** Bootstrap nodes nemohou vidět content
- **Storage limitation:** Bootstrap data 5min TTL, žádné logy IP adres
- **Pseudonymization:** Identita = veřejný klíč, ne PII

### 7.5 DPIA (Data Protection Impact Assessment)

DPIA proveden před launchem (interní dokument). Závěry:
- Hlavní riziko: bootstrap node compromise → metadata leak
- Mitigace: multi-region bootstrap, optional Tor
- Residual risk: akceptovatelný

### 7.6 Data Processing Agreements

Pro B2B zákazníky: připravený DPA šablona (čl. 28 GDPR), self-hosted varianta eliminuje nutnost data sharing.

---

## 8. Google Play Data Safety

### 8.1 Declared data collection

| Datum | Sbírá se? | Účel | Volitelné? |
|-------|-----------|------|------------|
| Personal info (name, email, phone) | NE | – | – |
| Photos and videos | NE (lokální only) | – | – |
| Audio files | NE (lokální only) | – | – |
| Files and docs | NE (lokální only) | – | – |
| Calendar | NE | – | – |
| Contacts | NE | – | – |
| App activity (interactions) | NE (default) / ANO (opt-in only) | Stabilita | Ano |
| App info and performance (crashes) | ANO (opt-in only) | Stabilita | Ano |
| Device or other IDs | NE | – | – |

### 8.2 Data sharing

| S kým? | Co? | Proč? |
|--------|-----|-------|
| Žádné třetí strany | – | – |

(FCM token jde do Google FCM, ale je opaque a nemapovatelný na uživatele.)

### 8.3 Encryption in transit
**Yes, all user data is encrypted in transit.**

### 8.4 Data deletion request
**Yes, users can request that their data be deleted.**
Proces: User → Settings → Wipe all data + odhlášení z FCM (auto)

---

## 9. Bezpečnostní processes

### 9.1 Vulnerability disclosure

- **Security email:** security@securewhisper.org
- **PGP klíč:** Publikovaný na webu
- **Response SLA:** 24h initial response, 7 dní pre-fix communication
- **Bug bounty:** Plánováno od měsíce 6 (HackerOne nebo self-hosted)

### 9.2 Security audit

- **Frekvence:** Roční (full audit), průběžné code reviews
- **Auditor:** Cure53, NCC Group nebo Trail of Bits
- **Publikování:** Public report do 30 dní po fix kritických nálezů

### 9.3 Incident response

| Severity | Trigger | Response time | Action |
|----------|---------|---------------|--------|
| P0 | Active exploitation, data leak | 1h | War room, public statement |
| P1 | Critical vuln, no exploitation | 4h | Hot fix release |
| P2 | High vuln | 24h | Next regular release |
| P3 | Medium vuln | 7 dní | Backlog priority |
| P4 | Low / informational | 30 dní | Backlog |

### 9.4 Reproducible builds

- Build environment: Docker image se zafixovanými verzemi
- Build script: deterministic, no timestamps
- Verification: F-Droid maintainer + community member musí build ověřit
- Rebuild každý release: archived in `https://github.com/securewhisper/reproducible-builds`

### 9.5 Supply chain security

- Dependency pinning (specific versions, no `+`)
- Renovate Bot pro aktualizace
- `gradle dependencyCheckAnalyze` v CI
- SBOM (Software Bill of Materials) generován při každém release
- Signed releases na GitHubu (cosign / sigstore)

---

## 10. Bezpečnostní limitace

Transparentně dokumentováno pro uživatele:

1. **Endpoint security:** Pokud je telefon kompromitován (root, malware), aplikace nemůže garantovat soukromí
2. **Visual eavesdropping:** Soukromí v okolí (rameno) je odpovědnost uživatele
3. **Bootstrap nodes mohou pozorovat:** kdo kdy DHT používá (ne s kým)
4. **Traffic analysis:** Bez Toru jsou IP-level metadata viditelná
5. **Social engineering:** Aplikace nechrání před lidskou chybou
6. **Quantum computing:** Curve25519/Ed25519 nejsou post-quantum bezpečné. Plán: migration na Kyber/Dilithium před 2030
