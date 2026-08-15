# 03 – Market & Competitive Analysis

**Projekt:** SecureWhisper
**Verze:** 1.0

---

## 1. Tržní přehled

Trh se zabezpečenými messaging aplikacemi roste tempem ~15 % ročně, taženo zvýšeným povědomím o ochraně osobních údajů, regulatorními změnami (GDPR, EU Digital Services Act) a opakovanými incidenty s úniky dat u velkých platforem.

### 1.1 Velikost trhu (odhad)
- Globální trh secure messaging: ~5 mld. USD (2025)
- Privacy-first segment (alternativy k WhatsApp/Telegramu): ~800 mil. USD
- Roční růst: 12–18 %

### 1.2 Tržní trendy
- Posun od telefonního čísla jako identity směrem k pseudonymním identifikátorům
- Rostoucí poptávka po decentralizovaných řešeních (Matrix, SimpleX, Briar, Session)
- Tlak na E2E šifrování ze strany regulátorů (proti i pro)
- Rostoucí zájem o open-source a auditovatelné aplikace
- Privacy washing u velkých hráčů – uživatelé hledají "skutečně" privátní alternativy

## 2. Konkurenční analýza

### 2.1 Přímí konkurenti

#### Signal
- **Silné stránky:** Zlatý standard E2E šifrování, široká uživatelská základna (~70M MAU), open-source
- **Slabé stránky:** Vyžaduje telefonní číslo, centralizovaný server, problémy s odolností proti cenzuře
- **Pozicování:** Mainstream privacy

#### Session
- **Silné stránky:** Bez telefonního čísla, decentralizovaná síť (Lokinet), onion routing
- **Slabé stránky:** Vyšší latence kvůli onion routingu, žádná forward secrecy v některých režimech
- **Pozicování:** Hardcore privacy

#### Briar
- **Silné stránky:** Plně P2P, funguje i bez internetu (Tor, Bluetooth, WiFi), open-source
- **Slabé stránky:** Pouze Android, omezené UX, žádné video, pomalé doručení
- **Pozicování:** Aktivisté, krizové situace

#### SimpleX Chat
- **Silné stránky:** Žádné identifikátory uživatelů, queue-based architektura, open-source
- **Slabé stránky:** Náročnější UX, závislost na SMP serverech (i když vyměnitelných)
- **Pozicování:** Maximum metadata privacy

#### Threema
- **Silné stránky:** Žádné telefonní číslo (volitelné), švýcarská jurisdikce, audited
- **Slabé stránky:** Placená aplikace, centralizovaný server, méně známá
- **Pozicování:** B2B a privacy-focused

### 2.2 Nepřímí konkurenti
- **WhatsApp / Telegram / iMessage** – mainstream, ale problémy s privacy
- **Wire** – B2B zaměření
- **Matrix klienti (Element)** – federovaná architektura, ale složitější setup

### 2.3 Srovnávací tabulka

| Funkce | SecureWhisper | Signal | Session | Briar | SimpleX |
|--------|---------------|--------|---------|-------|---------|
| Bez tel. čísla | ✓ | ✗ | ✓ | ✓ | ✓ |
| P2P (bez serveru pro obsah) | ✓ | ✗ | částečně | ✓ | částečně |
| Audio/video hovory | ✓ | ✓ | ✓ | ✗ | ✓ |
| Forward secrecy | ✓ | ✓ | ✗ (částečně) | ✓ | ✓ |
| Open-source | ✓ | ✓ | ✓ | ✓ | ✓ |
| Funguje offline (LAN/BT) | ✓ | ✗ | ✗ | ✓ | ✗ |
| iOS klient | ✗ (MVP) | ✓ | ✓ | ✗ | ✓ |
| Skupinové chaty | ✗ (MVP) | ✓ | ✓ | ✓ | ✓ |
| Latence handshake | střední | nízká | vysoká | vysoká | střední |

## 3. Pozicování na trhu

### 3.1 Unique Value Proposition (UVP)
**„Komunikace, která existuje jen mezi vámi dvěma. Bez čísla, bez serveru, bez kompromisu."**

### 3.2 Diferenciace
- **Vs. Signal:** Žádné telefonní číslo, plně P2P architektura
- **Vs. Session:** Nižší latence, podpora video hovorů od začátku
- **Vs. Briar:** Moderní UX, podpora hovorů, přístupné běžnému uživateli
- **Vs. SimpleX:** Jednodušší mentální model (jeden kontaktní kód = jeden kontakt)

### 3.3 Tržní mezera
SecureWhisper cílí do mezery mezi „mainstream s kompromisy" (Signal) a „extrémně privátní, ale nepřístupné" (Briar). Kombinuje silné soukromí Briaru s přístupností Signalu.

## 4. SWOT analýza

### 4.1 Strengths (silné stránky)
- Plně decentralizovaná architektura bez single point of failure
- Open-source kód umožňující nezávislý audit
- Žádná závislost na PII pro identitu
- Moderní stack (Kotlin, Compose, WebRTC)
- Možnost B2B nabídky (self-hosted bootstrap)

### 4.2 Weaknesses (slabé stránky)
- Vyšší latence při navázání spojení (DHT lookup)
- Vyšší spotřeba baterie pro DHT účast
- Online-only doručení v MVP
- Nejistota ohledně masového user adoption
- Bez iOS klienta na startu

### 4.3 Opportunities (příležitosti)
- Rostoucí privacy povědomí (zejména v EU po DMA/DSA)
- Zhoršování důvěry v centralizované platformy
- Regulační podpora E2E šifrování v EU
- Potenciál pro grant funding (NLnet, OTF, Mozilla)
- Komunitní rozšíření (F-Droid, Reproducible Builds)

### 4.4 Threats (hrozby)
- Anti-encryption legislativa (UK Online Safety Bill, EU Chat Control)
- Možné zablokování ze strany některých států
- Konkurenční tlak ze strany Signalu (přidání usernames)
- Riziko, že Apple App Store odmítne přijmout app v budoucí iOS verzi
- Závislost na Google Play – riziko de-listingu

## 5. Tržní strategie

### 5.1 Segmentace (TAM/SAM/SOM)
- **TAM (Total Addressable Market):** ~3 mld. uživatelů messaging apps globálně
- **SAM (Serviceable Addressable Market):** ~150 mil. privacy-conscious Android uživatelů
- **SOM (Serviceable Obtainable Market):** ~500 tis. v prvních 24 měsících

### 5.2 Geografické priority
1. **Tier 1:** EU (Německo, Francie, ČR, NL, skandinávské země) – vysoké privacy povědomí
2. **Tier 2:** Severní Amerika (USA, Kanada) – tech-savvy segmenty
3. **Tier 3:** Latinská Amerika, jihovýchodní Asie – aktivistické komunity

### 5.3 Vstupní strategie
- Měkký launch v ČR a Německu (lokalizace cs, de, en)
- PR mezi privacy advokáty (EFF, Privacy International, IUVENTA, Iuridicum Remedium)
- Postupné rozšiřování po validaci product-market fit
