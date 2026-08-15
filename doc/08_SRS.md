# 08 – Software Requirements Specification (SRS)

**Projekt:** SecureWhisper
**Verze:** 1.0

---

## 1. Úvod

### 1.1 Účel
Tento dokument specifikuje funkční a nefunkční požadavky na Android aplikaci SecureWhisper – plně šifrovaný P2P komunikátor.

### 1.2 Scope
SecureWhisper je nativní Android aplikace umožňující:
- Generování lokální kryptografické identity
- Discovery a navázání P2P spojení mezi zařízeními
- Výměnu šifrovaných textových zpráv, příloh a hlasových/video hovorů

### 1.3 Definice a zkratky

| Termín | Definice |
|--------|----------|
| E2E | End-to-end (encryption) |
| P2P | Peer-to-peer |
| DHT | Distributed Hash Table |
| MITM | Man-in-the-Middle |
| ICE | Interactive Connectivity Establishment |
| STUN | Session Traversal Utilities for NAT |
| TURN | Traversal Using Relays around NAT |
| SRTP | Secure Real-time Transport Protocol |
| FCM | Firebase Cloud Messaging |

---

## 2. Funkční požadavky

### 2.1 Identita uživatele

**FR-01:** Aplikace MUSÍ generovat klíčový pár (Ed25519 pro podpisy, X25519 pro výměnu klíčů) při prvním spuštění.

**FR-02:** Privátní klíč MUSÍ být uložen v Android Keystore s hardware-backed StrongBox (pokud je dostupný).

**FR-03:** Veřejný klíč MUSÍ být převeden na hexadecimální identifikátor pomocí BLAKE3 (32 bytů → 64 hex znaků).

**FR-04:** Identifikátor MUSÍ být zobrazitelný jako:
- 64-znakový hex řetězec
- QR kód (max ECC level)
- NFC tag payload
- Deep link `securewhisper://add?id={hash}&v={version}`

**FR-05:** Aplikace MUSÍ umožnit export identity do souboru chráněného passphrase (Argon2id KDF, AES-256-GCM).

**FR-06:** Aplikace MUSÍ podporovat smazání identity ("clean slate") s explicitním potvrzením.

### 2.2 Discovery a connection

**FR-10:** Aplikace MUSÍ podporovat tyto discovery mechanismy v tomto pořadí:
1. mDNS / Bonjour (lokální síť)
2. Bluetooth LE advertising
3. WiFi Direct
4. Hyperswarm DHT (vzdálené)

**FR-11:** Aplikace MUSÍ se pokusit o navázání spojení paralelně přes všechny dostupné transporty a použít první úspěšné.

**FR-12:** Aplikace MUSÍ implementovat NAT traversal pomocí STUN a hole punching s fallbackem na komunitní relay nodes.

**FR-13:** Aplikace MUSÍ při P2P handshaku použít Noise Protocol (XX pattern) nad navázaným transportem.

**FR-14:** Aplikace MUSÍ ověřit shodu mezi peerovým hex ID a klíčem prezentovaným při handshaku. Při neshodě MUSÍ spojení odmítnout.

### 2.3 Šifrování

**FR-20:** Veškerá obsahová komunikace MUSÍ používat Signal Protocol (Double Ratchet) nad libsignal knihovnou.

**FR-21:** Audio a video stream MUSÍ používat DTLS-SRTP s klíči odvozenými ze Signal session keys.

**FR-22:** Aplikace MUSÍ generovat a zobrazit safety number (60 hex znaků, skupiny po 5) odvozený z obou veřejných klíčů.

**FR-23:** Při změně klíče peera MUSÍ aplikace zobrazit varování a vyžadovat manuální re-verifikaci.

### 2.4 Chat

**FR-30:** Aplikace MUSÍ podporovat textové zprávy do 8 KB plain textu.

**FR-31:** Aplikace MUSÍ podporovat přílohy:
- Obrázky (JPEG, PNG, WebP) do 25 MB v MVP
- Video (MP4, WebM) do 25 MB v MVP
- Soubory (libovolný typ) do 25 MB v MVP

**FR-32:** Aplikace MUSÍ zobrazit stavy zpráv:
- Odesílá se (clock icon)
- Doručeno (single check)
- Přečteno (double check)
- Selhalo (warning icon + retry button)

**FR-33:** Aplikace MUSÍ podporovat indikátor "peer typing".

**FR-34:** Aplikace MUSÍ podporovat disappearing messages s nastavitelnou dobou (5s, 30s, 1min, 5min, 1h, 1den, 1týden).

**FR-35:** Aplikace MUSÍ podporovat odpověď na zprávu (quote/reply).

**FR-36:** Aplikace MUSÍ podporovat reakce emoji na zprávu.

### 2.5 Hlasové a video hovory

**FR-40:** Aplikace MUSÍ podporovat 1:1 audio hovor (Opus codec, 48 kHz, VBR 16–64 kbps).

**FR-41:** Aplikace MUSÍ podporovat 1:1 video hovor (VP8/VP9, max 720p@30fps v MVP).

**FR-42:** Aplikace MUSÍ implementovat adaptivní bitrate na základě network conditions.

**FR-43:** Aplikace MUSÍ zobrazit indikátor kvality spojení během hovoru.

**FR-44:** Aplikace MUSÍ umožnit přepínání mezi přední a zadní kamerou během video hovoru.

**FR-45:** Aplikace MUSÍ podporovat ztlumení mikrofonu a vypnutí kamery během hovoru.

### 2.6 Notifikace

**FR-50:** Aplikace MUSÍ podporovat FCM push notifikace pro probuzení aplikace při přijetí zprávy.

**FR-51:** Push notifikace NESMÍ obsahovat obsah zprávy – pouze opaque identifikátor.

**FR-52:** Aplikace MUSÍ umožnit vypnutí push notifikací s jasným varováním o důsledcích (zprávy se doručí jen když je app v popředí).

**FR-53:** Aplikace MUSÍ podporovat per-konverzace nastavení notifikací (mute, custom sound).

### 2.7 Bezpečnost aplikace

**FR-60:** Aplikace MUSÍ podporovat zámek aplikace pomocí biometrie (Android BiometricPrompt) nebo PIN kódu.

**FR-61:** Aplikace MUSÍ skrýt obsah z thumbnail a recents view.

**FR-62:** Aplikace MUSÍ blokovat snímky obrazovky v citlivých obrazovkách (volitelně).

**FR-63:** Aplikace MUSÍ implementovat root/jailbreak detection s warning, ne hard block.

### 2.8 Lokální data

**FR-70:** Aplikace MUSÍ ukládat všechna data lokálně v SQLCipher databázi.

**FR-71:** Klíč pro databázi MUSÍ být odvozen z user passphrase (Argon2id) nebo Android Keystore klíče.

**FR-72:** Aplikace MUSÍ podporovat export lokální databáze (zašifrovaný backup).

**FR-73:** Aplikace MUSÍ podporovat selektivní mazání:
- Single message
- Single conversation
- Wipe all data (factory reset)

---

## 3. Nefunkční požadavky

### 3.1 Výkon

**NFR-01:** Cold start aplikace MUSÍ být do 2 sekund na referenčním zařízení (Pixel 6).

**NFR-02:** Latence odeslání textové zprávy v navázaném spojení MUSÍ být < 500 ms (P95).

**NFR-03:** Latence audia během hovoru MUSÍ být < 200 ms one-way (P95) při dobrém spojení.

**NFR-04:** Doba navázání nového P2P spojení MUSÍ být:
- < 5 s při LAN discovery (P95)
- < 15 s při DHT lookup (P95)
- < 30 s při fallback přes relay (P95)

**NFR-05:** Aplikace MUSÍ zvládat aktivní konverzaci s 10 000+ zprávami bez znatelného slow-down.

### 3.2 Spotřeba zdrojů

**NFR-10:** Battery drain v idle režimu (DHT participation off) MUSÍ být < 2 % za 24 hodin.

**NFR-11:** Battery drain v aktivním režimu (DHT participation on) MUSÍ být < 8 % za 24 hodin.

**NFR-12:** RAM footprint MUSÍ být < 200 MB v idle, < 400 MB během video hovoru.

**NFR-13:** APK velikost MUSÍ být < 50 MB.

**NFR-14:** Datový provoz v idle (DHT keep-alive) MUSÍ být < 5 MB/den.

### 3.3 Bezpečnost

**NFR-20:** Aplikace MUSÍ projít OWASP MASVS Level 2 verifikací.

**NFR-21:** Privátní klíče NESMÍ opustit Android Keystore (kromě explicitního exportu uživatelem).

**NFR-22:** Síťová komunikace MUSÍ použít forward secrecy.

**NFR-23:** Aplikace MUSÍ podporovat reproducible builds.

**NFR-24:** Aplikace NESMÍ obsahovat žádné analytické trackery nebo telemetrii bez opt-in.

### 3.4 Použitelnost

**NFR-30:** Aplikace MUSÍ podporovat tyto jazyky v MVP: cs, en, de.

**NFR-31:** Aplikace MUSÍ splňovat WCAG 2.1 Level AA pro accessibility.

**NFR-32:** Aplikace MUSÍ podporovat Android system text size scaling.

**NFR-33:** Aplikace MUSÍ podporovat dark mode (default) a light mode.

**NFR-34:** Aplikace MUSÍ být ovladatelná TalkBack screen readerem.

### 3.5 Kompatibilita

**NFR-40:** Aplikace MUSÍ podporovat Android 8.0 (API 26) až nejnovější Android verzi.

**NFR-41:** Aplikace MUSÍ podporovat tyto architektury: arm64-v8a, armeabi-v7a, x86_64.

**NFR-42:** Aplikace MUSÍ fungovat bez Google Play Services (s redukovanou funkcionalitou push).

**NFR-43:** Aplikace MUSÍ fungovat na zařízeních s 2 GB RAM a více.

**NFR-44:** Aplikace MUSÍ fungovat na obrazovkách 4,5"–10" (telefony i tablety).

### 3.6 Dostupnost a spolehlivost

**NFR-50:** Crash-free session rate MUSÍ být ≥ 99,5 %.

**NFR-51:** ANR (Application Not Responding) rate MUSÍ být < 0,1 %.

**NFR-52:** Bootstrap nodes MUSÍ mít 99,5 % uptime.

**NFR-53:** Aplikace MUSÍ implementovat graceful degradation při výpadku DHT.

**NFR-54:** Aplikace MUSÍ obsahovat hardcoded záložní DHT peery pro případ výpadku bootstrap HTTP API. Záložní peery jsou:

| Host | Port | Region |
|------|------|--------|
| `45.76.100.42` | 49737 | US East (New York) |
| `95.179.200.11` | 49737 | EU (Frankfurt) |
| `139.162.55.73` | 49737 | Asia (Singapore) |
| `178.62.194.88` | 49737 | EU (Amsterdam) |
| `2a01:4f8:c0c:9abc::1` | 49737 | EU IPv6 (Helsinki) |

**NFR-55:** Fallback na hardcoded peery MUSÍ proběhnout automaticky do 15 s od selhání bootstrap HTTP požadavku. Uživatel nesmí vidět chybové hlášení v tomto intervalu.

### 3.7 Údržba

**NFR-60:** Kód MUSÍ být dokumentovaný (KDoc) pro všechna veřejná API.

**NFR-61:** Test coverage MUSÍ být ≥ 70 % pro core moduly.

**NFR-62:** Aplikace MUSÍ podporovat in-app update notifikace pro kritické security patche.

**NFR-63:** Verze starší než 12 měsíců MUSÍ obdržet mandatory upgrade prompt.

### 3.8 Soukromí

**NFR-70:** Aplikace NESMÍ ukládat žádná uživatelská data na centrálních serverech (kromě FCM opaque tokenů).

**NFR-71:** Aplikace MUSÍ mít publikovanou Privacy Policy v souladu s GDPR.

**NFR-72:** Aplikace MUSÍ implementovat data minimization principle.

**NFR-73:** Aplikace MUSÍ poskytnout mechanismus pro export uživatelských dat (právo na přenositelnost).

---

## 4. Constraints

### 4.1 Technické

- Cílová platforma: Android 8.0+
- Programovací jazyk: Kotlin (s nutnou Java a C++ JNI pro libsignal/WebRTC)
- UI framework: Jetpack Compose
- Build systém: Gradle 8+

### 4.2 Regulační

- Compliance s GDPR (EU)
- Compliance s Google Play Developer Program Policies
- Compliance s F-Droid Inclusion Policy

### 4.3 Byznysové

- Open-source distribuce (GPL-3.0 nebo AGPL-3.0)
- Maximální závislost na proprietary services: pouze FCM jako optional

---

## 5. Acceptance criteria pro release

### 5.1 MVP (Release 1.0)
- Všechny FR-01 až FR-39 implementovány
- Všechny NFR-01 až NFR-04 splněny
- Crash-free rate ≥ 99 % v beta testu
- Security audit completed s opravenými P0/P1 nálezy
- Reproducible build verifikovaný F-Droid maintainerem

### 5.2 Release 1.5
- Všechny FR-40 až FR-53 implementovány
- Audio call quality test pass na 95 % testovacích zařízení

### 5.3 Release 2.0
- Video call podpora
- Group conversations (do 8 členů)
- Performance metrics splněny při skupinové komunikaci
