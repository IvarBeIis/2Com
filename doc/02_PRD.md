# 02 – Product Requirements Document (PRD)

**Projekt:** SecureWhisper
**Verze:** 1.0

---

## 1. Přehled produktu

SecureWhisper je Android aplikace pro plně šifrovanou peer-to-peer komunikaci. Uživatelé se identifikují hexadecimálním kódem odvozeným z lokálně generovaného veřejného klíče. Aplikace neukládá žádný uživatelský obsah na serverech a podporuje chat, hlasové a videohovory.

## 2. Klíčové funkce

### 2.1 MVP (Release 1.0)
- Lokální generování identity (Ed25519/X25519 klíčový pár)
- Sdílení kontaktního kódu přes QR, NFC, deep link, manuální zadání
- E2E šifrovaný chat (text, obrázky, soubory do 25 MB)
- Online doručení zpráv (žádná offline fronta)
- Bluetooth/WiFi Direct discovery v lokální síti
- DHT (Hyperswarm) pro vzdálené spojení
- Safety number ověření kontaktu

### 2.2 Release 1.5
- Hlasové hovory (1:1, Opus codec)
- Push notifikace (FCM, bez obsahu)
- Lokální záloha konverzací (zašifrovaný export)

### 2.3 Release 2.0
- Videohovory (1:1, VP8/VP9)
- Sdílení obrazovky
- Krátké hlasové zprávy
- Dark mode toggle (default dark)

### 2.4 Future (Release 2.x+)
- Skupinové chaty (max 8 členů, plně E2E)
- Skupinové hovory (přes SFU, ale šifrované)
- Self-hosted relay/bootstrap node
- Desktop klient (Linux/Windows/Mac)

## 3. User stories

### 3.1 Onboarding

**US-001:** Jako nový uživatel chci při prvním spuštění aplikace získat svou unikátní identitu, aniž bych musel zadávat osobní údaje.
- **Akceptační kritéria:**
  - Při prvním spuštění se vygeneruje klíčový pár lokálně
  - Uživateli se zobrazí jeho hex kód a QR
  - Žádný požadavek na telefonní číslo, e-mail nebo registraci
  - Proces nesmí trvat déle než 10 sekund

**US-002:** Jako nový uživatel chci být seznámen s principy fungování aplikace přes onboarding obrazovky.
- **Priorita:** Must-have
- **Akceptační kritéria:**
  - Maximálně 4 onboarding obrazovky
  - Možnost přeskočit
  - Vysvětlení: princip P2P, online-only doručení, význam safety number

### 3.2 Přidání kontaktu

**US-003:** Jako uživatel chci přidat kontakt naskenováním QR kódu.
- **Priorita:** Must-have
- **Akceptační kritéria:**
  - Kamera detekuje QR během 3 sekund
  - Po skenu zobrazení preview kontaktu (jméno + safety number)
  - Možnost potvrdit/zrušit přidání

**US-004:** Jako uživatel chci přidat kontakt zadáním hex kódu ručně, pokud QR nelze použít.
- **Priorita:** Should-have
- **Akceptační kritéria:**
  - Pole pro hex kód s validací formátu
  - Možnost vložit přes clipboard
  - Auto-formátování na čitelné skupiny po 4 znacích

**US-005:** Jako uživatel chci sdílet svůj kontaktní kód přes deep link.
- **Priorita:** Should-have
- **Akceptační kritéria:**
  - Generování linku ve formátu `securewhisper://add?id={hash}`
  - Možnost sdílet přes Android share sheet
  - Otevření linku v jiném zařízení s aplikací automaticky vyvolá dialog "Přidat kontakt"

### 3.3 Komunikace

**US-006:** Jako uživatel chci poslat textovou zprávu kontaktu, který je online.
- **Priorita:** Must-have
- **Akceptační kritéria:**
  - Zpráva se odešle do 3 sekund od stisku tlačítka
  - Indikace stavu: odesílá se / doručeno / přečteno
  - Pokud peer není online, zpráva čeká ve frontě s indikací "čeká na připojení peera"

**US-007:** Jako uživatel chci zahájit hlasový hovor s kontaktem.
- **Priorita:** Must-have (R1.5)
- **Akceptační kritéria:**
  - Tlačítko hovoru v detailu kontaktu
  - Příjemce vidí incoming call screen i při uzamčeném telefonu
  - Latence audia < 200 ms při dobrém spojení
  - Vizuální indikace kvality spojení během hovoru

**US-008:** Jako uživatel chci ověřit identitu kontaktu pomocí safety number.
- **Priorita:** Must-have
- **Akceptační kritéria:**
  - V detailu kontaktu zobrazení 60-znakového safety number ve skupinách po 5
  - Možnost porovnat zobrazením QR (oba uživatelé naskenují svůj QR navzájem)
  - Po ověření zobrazení odznaku "Verified"
  - Při změně klíče peera (reinstalace) jasné varování

### 3.4 Soukromí a bezpečnost

**US-009:** Jako uživatel chci aplikaci uzamknout PIN kódem nebo biometrií.
- **Priorita:** Must-have
- **Akceptační kritéria:**
  - Aktivace v nastavení
  - Po definovaném intervalu nečinnosti aplikace zamknuta
  - Biometrie přes Android BiometricPrompt API

**US-010:** Jako uživatel chci nastavit auto-mazání zpráv (disappearing messages).
- **Priorita:** Should-have
- **Akceptační kritéria:**
  - Per-konverzace nastavení (5s, 1min, 1h, 1den, 1týden, vypnuto)
  - Synchronizace nastavení s peerem
  - Vizuální countdown u zpráv

**US-011:** Jako uživatel chci exportovat svou identitu a konverzace zašifrovaně.
- **Priorita:** Should-have (R1.5)
- **Akceptační kritéria:**
  - Export do souboru chráněného heslem
  - Možnost importu na novém zařízení
  - Varování při importu o ztrátě dosavadní identity

## 4. Scope

### 4.1 V scope
- Android 8.0+ (API 26+)
- Plně E2E šifrovaná komunikace
- 1:1 chat, audio, video
- Lokální úložiště (SQLCipher)
- Open-source distribuce

### 4.2 Mimo scope (MVP)
- iOS aplikace
- Web client
- Skupinová komunikace
- Cloudové zálohy
- Centralizovaný onboarding
- Telefonní čísla / SMS verifikace

## 5. Priorizace (MoSCoW)

### Must have
- Generování identity
- Sdílení kódu (QR, manuální)
- E2E šifrovaný chat
- DHT discovery
- Safety number
- App lock

### Should have
- Hlasové hovory
- Disappearing messages
- Lokální export/import
- NFC sdílení

### Could have
- Témata aplikace
- Vlastní wallpapery v chatech
- Custom notification sounds

### Won't have (this release)
- Skupinová komunikace
- Cross-platform synchronizace
- Cloudové zálohy

## 6. Závislosti

- Hyperswarm DHT knihovna (Holepunch ekosystém)
- WebRTC for Android (Google build)
- libsignal (Signal Protocol)
- SQLCipher
- Firebase Cloud Messaging (volitelně, pouze pro probuzení app)

## 7. Otevřené otázky

- Použijeme vlastní DHT overlay nebo Mainline DHT?
- Implementace Tor transportu pro paranoidní uživatele – v MVP nebo později?
- Strategie pro device-to-device migraci identity bez kompromitace forward secrecy
