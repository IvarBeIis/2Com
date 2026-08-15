# 06 – Go-to-Market Plan

**Projekt:** SecureWhisper
**Verze:** 1.0

---

## 1. Launch strategie

### 1.1 Fáze launche

#### Fáze 0: Pre-launch (T-3 měsíce před launch)
- Closed alpha s 50 vybranými testery (privacy advokáti, novináři)
- Setup landing page s waiting listem
- Začátek security auditu (Cure53 nebo NCC Group)
- Buildování komunity na Mastodonu, Matrixu, Reddit /r/privacy

#### Fáze 1: Soft launch (T-0)
- F-Droid release (žádný gating, plně open)
- Pre-release na GitHub Releases (APK)
- Omezený launch v ČR a Německu
- Lokalizace: cs, de, en
- PR k vybraným médiím (Heise, Lupa, ZDNet, Ars Technica)

#### Fáze 2: Public launch (T+2 měsíce)
- Google Play Store release
- Rozšířený PR (Hacker News, Product Hunt, BoingBoing)
- Lokalizace: + fr, es, pt, pl, uk
- Sponsorship/partnership s privacy NGO (EFF, Privacy International, IuRe)

#### Fáze 3: Scale (T+6 měsíců)
- Globální dostupnost
- Lokalizace na 15+ jazyků
- Influencer outreach v privacy/tech komunitě
- B2B sales aktivity

## 2. Marketing strategie

### 2.1 Komunikační poselství

**Hlavní:** „Komunikace, která existuje jen mezi vámi dvěma."

**Sub-messages:**
- „Bez čísla. Bez serveru. Bez kompromisu."
- „Otevřený kód, uzavřená komunikace."
- „Identifikuje vás kód, ne vaše telefonní číslo."

### 2.2 Komunikační kanály

| Kanál | Cílová persona | Aktivity | Priorita |
|-------|----------------|----------|----------|
| Mastodon, Pixelfed | Tech-savvy, privacy aktivisté | Dev updates, transparency posts | Vysoká |
| Reddit /r/privacy, /r/privacytoolsio | Privacy enthusiasts | AMA, technické posty | Vysoká |
| Matrix komunity | Decentralizační komunita | Interakce, support | Střední |
| Twitter/X | Mainstream tech | Klíčové milníky | Střední |
| GitHub | Vývojáři | README, releases | Vysoká |
| Tech blogy (Heise, Ars) | Profesionálové | PR, deep-dive články | Vysoká |
| Podcasts (Darknet Diaries, IT-Sicherheit) | Tech-aware | Interview s týmem | Střední |
| YouTube (privacy kanály) | Mainstream privacy curious | Tutoriály, hodnocení | Nízká (později) |

### 2.3 Content strategie

**Měsíční rytmus:**
- 1× hluboký technický blogpost (architektura, security)
- 2× transparency report (audity, finance, governance)
- Nepravidelně: incident reports, postmortems
- Týdenní development updates na Mastodonu

**Témata blog postů (první rok):**
1. „Proč jsme zvolili Hyperswarm DHT"
2. „Jak funguje safety number"
3. „Přesnost metafory: Co znamená P2P v praxi"
4. „Threat model SecureWhisper"
5. „Porovnání s Briarem, Signalem, Sessionem"
6. „Naše governance struktura"
7. „Audit results: Co jsme zjistili a opravili"

### 2.4 Komunitní strategie

- **Bug bounty program** od měsíce 6 (HackerOne nebo self-hosted)
- **Open development:** veřejný roadmap na GitHubu, otevřené design diskuse
- **Příspěvky komunity:** překlady (Weblate), témata, reportované bugy
- **Annual conference talk:** CCC, FOSDEM, OHM/MCH

## 3. App Store Optimization (ASO)

### 3.1 Google Play Store

**App Title (50 char):**
„SecureWhisper – Privátní P2P chat"

**Short description (80 char):**
„Šifrovaný chat, hovory a video. Bez čísla. Bez serveru. Plně P2P."

**Long description (4000 char):**

```
SecureWhisper je komunikační aplikace navržená pro lidi, kteří berou své soukromí vážně.

🔒 PLNĚ ŠIFROVANÉ
Každá zpráva, hovor i videohovor je end-to-end šifrovaný protokolem Signal. Nikdo, ani my, nemůže do vaší komunikace nahlédnout.

🌐 PEER-TO-PEER
Žádný centrální server neukládá vaše zprávy. Spojujete se přímo s druhou osobou.

🆔 ŽÁDNÉ TELEFONNÍ ČÍSLO
Vaše identita je hexadecimální kód odvozený z lokálně generovaného klíče. Nepotřebujete zadávat telefonní číslo, e-mail ani jiné osobní údaje.

✅ OVĚŘITELNÁ IDENTITA
Pomocí safety number můžete kdykoliv ověřit, že komunikujete se správnou osobou.

💬 FUNKCE
- Textový chat se šifrovanými přílohami
- Hlasové hovory v HD kvalitě
- Videohovory s adaptivní kvalitou
- Sdílení kontaktu přes QR, NFC, deep link
- Lokální discovery přes Bluetooth a WiFi
- Disappearing messages
- App lock biometrií
- Plně lokální úložiště (žádné cloudové zálohy)

🛡️ DŮVĚRYHODNOST
- 100 % open-source kód
- Reproducible builds
- Pravidelné nezávislé audity
- F-Droid distribuce dostupná
- Transparentní financování

[...]
```

**Keywords (research):**
- privacy messenger
- p2p chat
- encrypted chat
- secure communication
- no phone number messenger
- decentralized messaging
- private video call
- end-to-end encryption

**Vizualizace:**
- 8 screenshotů pro Play Store (chat, hovor, QR sharing, settings, safety number, dark mode, onboarding, video call)
- Krátké promo video (30s) demonstrující QR exchange + první zprávu
- Feature graphic 1024×500

### 3.2 F-Droid

- Dodržení F-Droid inclusion policy (žádné anti-features, čistě FOSS dependencies)
- Reproducible build skripty
- Důsledné dokumentování v `metadata/cs.cz.securewhisper/`
- Antifeature: NonFreeNet (kvůli FCM jako optional, default off)

## 4. PR strategie

### 4.1 Klíčoví novináři / outlety

**ČR:**
- Lupa.cz (Karel Choc)
- Živě.cz
- E15 / Hospodářské noviny tech sekce
- DSL.cz

**Mezinárodní:**
- Heise Online (DE)
- Ars Technica (US)
- The Register (UK)
- Wired
- 404 Media
- TechCrunch (sekundární)

### 4.2 Press kit (na webu)

- Logo (vektor + raster, dark/light)
- Screenshots ve vysokém rozlišení
- Founder bios + photos
- Whitepaper (technical specification)
- One-pager (executive summary)
- Boilerplate (3 odstavce o aplikaci)

## 5. Partnership strategie

### 5.1 Cílové NGO partneři
- Iuridicum Remedium (CZ)
- Privacy International (UK)
- Electronic Frontier Foundation (US)
- Reporters Without Borders
- Access Now

### 5.2 Cílové akademické partnery
- CTU FIT (Praha) – security výzkum
- TU Dresden – privacy research
- TU Munich – cryptography

### 5.3 Cílové komerční partnery
- VPN providery (Mullvad, ProtonVPN) – cross-promotion
- Privacy-focused hostingové firmy (Mythic Beasts, Greenhost)
- Hardware výrobci (Pine64, Volla Phone)

## 6. KPI marketing aktivity

| Metric | 3M | 6M | 12M | 18M |
|--------|----|----|----|------|
| Website unikátní návštěvy | 5k | 25k | 100k | 250k |
| Mastodon followers | 200 | 800 | 3 000 | 8 000 |
| Twitter followers | 500 | 2 000 | 8 000 | 20 000 |
| GitHub stars | 200 | 1 000 | 5 000 | 15 000 |
| F-Droid downloads | 1k | 8k | 40k | 120k |
| Play Store downloads | 0 | 5k | 30k | 100k |
| PR mentions (tier 1 outlets) | 5 | 15 | 40 | 80 |

## 7. Rozpočet (první rok)

| Položka | Roční náklad |
|---------|--------------|
| Security audit (Cure53) | €40 000 |
| Vývoj (3 FTE) | €180 000 |
| Marketing & PR | €30 000 |
| Hosting & infra | €5 000 |
| Právní (GDPR, app store agreements) | €8 000 |
| Lokalizace (15 jazyků) | €15 000 |
| Konference & travel | €12 000 |
| Total | €290 000 |

**Krytí:** Granty (€110k), B2B early customers (€40k), donations (€20k), zbytek z investice/equity.
