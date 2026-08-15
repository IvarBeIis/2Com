# 12 – UX/UI Design Specification

**Projekt:** SecureWhisper
**Verze:** 1.0

---

## 1. Design principles

1. **Bezpečnost je viditelná, ne nápadná** — uživatel vidí, že komunikace je šifrovaná, ale UI ho tím nezahlcuje
2. **Privacy by default** — všechna citlivá nastavení mají bezpečnou výchozí hodnotu
3. **Žádné dark patterns** — uživatel nikdy nesouhlasí s něčím, co nechtěl
4. **Progressive disclosure** — pokročilé funkce dostupné, ale ne v cestě
5. **Konzistence s Material Design 3** — využívat platform conventions, kde to dává smysl
6. **Accessibility first** — WCAG 2.1 AA jako minimum

## 2. Design system

### 2.1 Brand identity

**Název aplikace:** SecureWhisper
**Tagline:** „Komunikace, která existuje jen mezi vámi dvěma."

**Logo:**
- Primární: stylizovaný chat bublinky se zámkem
- Mono varianta pro F-Droid badges
- App icon: adaptive icon (foreground + background)

### 2.2 Barvy

#### Dark theme (default)

```
Primary:        #6FE7BF  (mint green – „secure" association)
On Primary:     #00382A
Primary Cont.:  #00513E
On Prim. Cont.: #8AF8D6

Secondary:      #B0CCC1
Background:     #191C1B
On Background:  #E1E3E1
Surface:        #191C1B
Surface Var.:   #404944
On Surface:     #E1E3E1

Error:          #FFB4AB
Verified:       #58D68D  (badge color)
Warning:        #F39C12
```

#### Light theme

```
Primary:        #006B53
On Primary:     #FFFFFF
Primary Cont.:  #8AF8D6
On Prim. Cont.: #002117

Background:     #FBFDF9
Surface:        #FBFDF9
On Surface:     #191C1B

Error:          #BA1A1A
```

### 2.3 Typografie

- **Display Large:** Roboto Flex, 57sp, weight 400
- **Headline Medium:** Roboto Flex, 28sp, weight 500
- **Title Medium:** Roboto Flex, 16sp, weight 500
- **Body Large:** Roboto Flex, 16sp, weight 400
- **Body Medium:** Roboto Flex, 14sp, weight 400
- **Label Small:** Roboto Flex, 11sp, weight 500
- **Mono (for hex code):** JetBrains Mono, 14sp

### 2.4 Spacing

8dp grid:
- xs: 4dp
- sm: 8dp
- md: 16dp
- lg: 24dp
- xl: 32dp
- xxl: 48dp

### 2.5 Komponenty

Všechny komponenty z Material 3 + custom:
- `SafetyNumberDisplay` (mono font, grouped 5+5+5...)
- `EncryptionBadge` (verified/unverified states)
- `MessageBubble` (with status indicator, reply preview, reactions)
- `QrCodeView` (scannable, with brand watermark)
- `PeerStatusIndicator` (online dot, last seen)
- `ConnectionQualityBars` (during calls)

### 2.6 Ikonografie

Material Symbols (Outlined varianta).

Custom ikony:
- `ic_lock_verified` (zámek + checkmark)
- `ic_p2p_direct` (dvě tečky propojené)
- `ic_disappearing` (přesýpací hodiny)
- `ic_safety_number` (otisk prstu + čísla)

## 3. Klíčové obrazovky

### 3.1 Onboarding (4 obrazovky)

```
┌─────────────────────────┐  ┌─────────────────────────┐
│                         │  │                         │
│    [Logo animation]     │  │   [Lock illustration]   │
│                         │  │                         │
│  Vítejte v             │  │  Vaše komunikace       │
│  SecureWhisper         │  │  zůstane mezi vámi      │
│                         │  │                         │
│  Šifrovaná P2P         │  │  Žádný server, žádný    │
│  komunikace            │  │  prostředník.           │
│                         │  │                         │
│  ● ○ ○ ○                │  │  ○ ● ○ ○                │
│                         │  │                         │
│  [Pokračovat]           │  │  [Pokračovat]           │
│  [Přeskočit]            │  │  [Přeskočit]            │
└─────────────────────────┘  └─────────────────────────┘

┌─────────────────────────┐  ┌─────────────────────────┐
│                         │  │                         │
│   [QR illustration]     │  │  [Identity illustration]│
│                         │  │                         │
│  Identifikujete se      │  │  Důležité:              │
│  hexadecimálním kódem  │  │                         │
│                         │  │  Pokud ztratíte         │
│  Žádné telefonní       │  │  zařízení nebo         │
│  číslo, žádný e-mail.  │  │  aplikaci přeinstalujete│
│                         │  │  ztratíte všechny       │
│  Sdílíte ho jen         │  │  konverzace.            │
│  s lidmi, kterým chcete.│  │  Pokud chcete, vytvořte │
│                         │  │  zálohu v nastavení.    │
│  ○ ○ ● ○                │  │  ○ ○ ○ ●                │
│                         │  │                         │
│  [Pokračovat]           │  │  [Vytvořit identitu]    │
└─────────────────────────┘  └─────────────────────────┘
```

### 3.2 Hlavní obrazovka (Conversations List)

```
┌─────────────────────────┐
│ SecureWhisper       ⋮  │ ← top bar
├─────────────────────────┤
│ 🔍 Hledat              │
├─────────────────────────┤
│ ◯ Marek    🔒    14:23 │
│   Ahoj, jak se máš?  ✓✓│
├─────────────────────────┤
│ ◯ Eva      🔒✓   12:01 │
│   Skvělé, díky!     ✓✓ │
├─────────────────────────┤
│ ◯ Alice    🔒✓✓  včera │
│   📷 Fotka            │
├─────────────────────────┤
│ ◯ Petr     ⚠     2.5.  │
│   Klíč se změnil       │
└─────────────────────────┘
                   ┌────┐
                   │ +  │ ← FAB pro nový kontakt
                   └────┘
```

**Indikátory:**
- 🔒 = šifrovaný kontakt (always)
- 🔒✓ = ověřený safety number
- 🔒✓✓ = ověřený + posíleně (např. via Bluetooth)
- ⚠ = varování (změna klíče, nedostupný)

### 3.3 Přidání kontaktu

```
┌─────────────────────────┐
│ ←  Přidat kontakt      │
├─────────────────────────┤
│                         │
│  ┌─────────────────┐   │
│  │                 │   │
│  │   [QR scanner]  │   │
│  │                 │   │
│  │   Zaměřte QR    │   │
│  │   kód kontaktu  │   │
│  │                 │   │
│  └─────────────────┘   │
│                         │
│  Nebo vyberte způsob:  │
│                         │
│  📱 Naskenovat QR kód  │
│  📲 NFC               │
│  ✍️  Zadat kód ručně   │
│  🔗 Otevřít odkaz      │
│  📡 Hledat v okolí     │
│                         │
│  ─────────────────     │
│                         │
│  [Sdílet můj kód]      │
└─────────────────────────┘
```

### 3.4 Můj profil / sdílení kódu

```
┌─────────────────────────┐
│ ← Moje identita        │
├─────────────────────────┤
│                         │
│       ┌─────────┐      │
│       │░▓░▓▒▓░░▓│      │
│       │▒██▓▓░▒▓░│      │
│       │░▓▒▓██▒█▓│      │
│       │▓░▒░░▓██▒│      │
│       │▒▓██▒░░▒░│      │
│       └─────────┘      │
│                         │
│   a3f4 e2c1 9d8b 6740  │
│   b218 5f9a 3c0e 7d12  │
│   8e4b 6a9f 2d10 c8e7  │
│   4f53 a619 2b8d 30ce  │
│                         │
│  [Kopírovat] [Sdílet]  │
│  [NFC] [Bluetooth]     │
│                         │
│  ─────────────────     │
│                         │
│  Zobrazené jméno:       │
│  Marek (změnit)         │
│                         │
│  Vytvořeno: 12.5.2025  │
└─────────────────────────┘
```

### 3.5 Konverzace

```
┌─────────────────────────┐
│ ← ◯ Eva 🔒✓     📞 📹 ⋮ │
├─────────────────────────┤
│  Včera 12:00            │
│                         │
│        ┌──────────────┐ │
│        │ Ahoj!        │ │
│        │       12:01 ✓✓│ │
│        └──────────────┘ │
│                         │
│ ┌──────────────┐        │
│ │ Ahoj, jak se │        │
│ │ máš?         │        │
│ │ 12:02        │        │
│ └──────────────┘        │
│                         │
│        ┌──────────────┐ │
│        │ Skvělé, díky!│ │
│        │       14:30 ✓✓│ │
│        └──────────────┘ │
│                         │
│        ⏱ 1 týden        │
├─────────────────────────┤
│ 📎 [Napsat zprávu...] ➤│
└─────────────────────────┘
```

### 3.6 Hovor (audio)

```
┌─────────────────────────┐
│                         │
│                         │
│         ◯               │
│        ◯◯◯              │
│       ◯◯◯◯◯             │
│                         │
│         Eva             │
│                         │
│      🔒 Šifrováno      │
│      ✓ Ověřeno          │
│                         │
│         02:34           │
│                         │
│   ▮▮▮▮▮▯  Kvalita      │
│                         │
│                         │
│   🎙   📢   🎬   ✋    │
│  mute spkr camera hold  │
│                         │
│       ┌─────────┐      │
│       │  ZAVĚSIT │      │
│       └─────────┘      │
└─────────────────────────┘
```

### 3.7 Safety number verification

```
┌─────────────────────────┐
│ ← Ověření identity      │
├─────────────────────────┤
│                         │
│  Tento kód musí být     │
│  shodný u obou stran:   │
│                         │
│  ┌───────────────────┐ │
│  │  3a8f1   c4e2d    │ │
│  │  9b7e0   f2a85    │ │
│  │  16cd3   4e9bf    │ │
│  │  20a7c   8de14    │ │
│  │  5f6b9   3e0ad    │ │
│  │  c81f2   794ed    │ │
│  └───────────────────┘ │
│                         │
│  [Skenovat QR od Evy]   │
│                         │
│  Pokud je kód shodný   │
│  na obou zařízeních,    │
│  vaše komunikace je     │
│  zabezpečená.          │
│                         │
│  ┌─────────────────┐   │
│  │ ✓ Ověřit        │   │
│  └─────────────────┘   │
│                         │
│  [Co když se neshoduje?]│
└─────────────────────────┘
```

## 4. Interakční vzory

### 4.1 Stavy spojení (vizualizace)

| Stav | Indikátor | Komunikace |
|------|-----------|------------|
| Není kontakt | – | „Přidejte kontakt" |
| Spojeno přímo (LAN) | 🟢 přímo | „Přímé spojení" |
| Spojeno přes DHT | 🟢 P2P | „P2P spojení" |
| Spojeno přes relay | 🟡 relay | „Spojení přes relay (E2E šifrováno)" |
| Hledá se peer | ⏳ animace | „Hledám {jméno}..." |
| Peer offline | 🔴 offline | „Čeká na připojení" |

### 4.2 Doručení zprávy

```
[Odesílám]   ⏳ ← jen ikona, vlevo
[Odesláno]   ✓
[Doručeno]   ✓✓
[Přečteno]   ✓✓ (modré)
[Selhalo]    ⚠️ Klepněte pro opakování
```

### 4.3 Změna klíče (warning)

Když peer reinstaloval app, jeho klíč se změnil:

```
┌─────────────────────────┐
│ ⚠️  Bezpečnostní upozornění
│                         │
│ Klíč Evy se změnil. To   │
│ může znamenat:           │
│                         │
│ • Eva přeinstalovala app │
│ • Někdo se za ni vydává  │
│                         │
│ Doporučujeme znovu       │
│ ověřit safety number.   │
│                         │
│ [Ověřit teď] [Později]   │
└─────────────────────────┘
```

## 5. Accessibility

### 5.1 WCAG 2.1 AA požadavky

- **Kontrast:** min. 4,5:1 pro text, 3:1 pro UI komponenty
- **Velikost dotykové oblasti:** min. 48×48dp
- **Text scaling:** podporovat sp škálování až 200 %
- **Screen reader:** všechny ikony a stavy mají content descriptions
- **Klávesová navigace:** focusable order logical, no traps

### 5.2 Specifické considerations

- Hex kódy mít alternativní zvukové popisy pro screen reader
- QR scanner mít alternativní fallback (manuální zadání)
- Hovory: vibrace + světelné indikátory pro deaf/hard-of-hearing
- High contrast mode pro slabozraké

## 6. Animace a micro-interactions

### 6.1 Principy
- Nikdy nepoužívat animace delší než 300ms
- Respektovat „Reduce motion" system setting
- Animace musí mít smysl (feedback, orientace), ne jen estetický

### 6.2 Klíčové animace
- Onboarding logo: 800ms entry
- Message bubble entry: 200ms slide-up + fade
- Connection establishment: spinner with state changes
- Successful verification: subtle haptic + checkmark animation

## 7. Notifikace

### 7.1 Channely (Android Notification Channels)

| Channel | Importance | Default sound | Behavior |
|---------|------------|---------------|----------|
| Messages | DEFAULT | Yes | Show preview (configurable) |
| Calls | HIGH | Custom ringtone | Heads-up + full screen intent |
| Background | LOW | No | Persistent (DHT participation) |
| Security | HIGH | Yes | Verification needed, key changes |

### 7.2 Notifikace privacy

- Default: nezobrazovat preview obsahu
- Notification action: „Open" (žádné Reply z notifikace v MVP – security)
- Lock screen: jen „Nová zpráva od {jméno}" nebo úplně skryto

## 8. Lokalizace

### 8.1 Podporované jazyky (MVP)
- Čeština (cs) – primární
- Angličtina (en)
- Němčina (de)

### 8.2 Plán pro Release 2.0
- Francouzština, Španělština, Portugalština, Polština, Ukrajinština

### 8.3 RTL support
- Plánovaný pro Release 2.5 (Arabština, Hebrejština)

## 9. Design files

- **Figma master:** `figma.com/file/securewhisper-design-system`
- **Symbol library:** Components, Icons, Color tokens
- **Prototyp:** Hi-fi prototype pro user testing

## 10. User testing

### 10.1 Testovací protokol
- 5 účastníků na release (Nielsen)
- Mix person (Eva, Marek, Alice, Petr)
- Tasks: onboarding, přidání kontaktu, ověření, první zpráva, video hovor

### 10.2 Klíčové metriky
- Task completion rate ≥ 90 %
- Time to first message ≤ 3 minuty (od stažení)
- SUS score ≥ 75
