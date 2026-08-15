# 04 – User Personas & User Journey Maps

**Projekt:** SecureWhisper
**Verze:** 1.0

---

## 1. Persony

### 1.1 Persona A: Investigativní novinářka „Eva"

| Atribut | Detail |
|---------|--------|
| Věk | 34 |
| Lokace | Praha, ČR |
| Profese | Investigativní novinářka |
| Tech literacy | Vysoká |
| Zařízení | Pixel 8, Android 14 |

**Cíle:**
- Komunikovat se zdroji bez rizika identifikace
- Mít jistotu, že žádný operátor nemá přístup ke zprávám
- Snadno ověřit identitu zdroje

**Bolestivá místa (pain points):**
- Většina aplikací vyžaduje telefonní číslo, které ji identifikuje
- Signal vyžaduje obě strany online v určitý moment pro setup
- Strach z metadata leaků – kdo s kým mluví

**Jak SecureWhisper pomáhá:**
- Žádné telefonní číslo; identita je jen klíč
- Možnost ověření přes safety number osobně před prvním kontaktem
- DHT discovery bez znalosti centrálního providera

---

### 1.2 Persona B: IT specialista „Marek"

| Atribut | Detail |
|---------|--------|
| Věk | 28 |
| Lokace | Brno, ČR |
| Profese | DevOps engineer |
| Tech literacy | Velmi vysoká |
| Zařízení | OnePlus 12, GrapheneOS |

**Cíle:**
- Mít komunikační nástroj, kterému kompletně rozumí
- Možnost auditovat kód
- Nezávislost na Google službách

**Bolestivá místa:**
- Většina aplikací neběží bez Google Play Services
- Nemožnost provozovat vlastní infrastrukturu u většiny řešení
- Closed-source komponenty u jinak open-source aplikací

**Jak SecureWhisper pomáhá:**
- Distribuce přes F-Droid, fungování bez Google Services
- Plně open-source včetně všech závislostí
- Možnost provozovat vlastní bootstrap node

---

### 1.3 Persona C: Aktivistka „Alice"

| Atribut | Detail |
|---------|--------|
| Věk | 22 |
| Lokace | Berlín, DE |
| Profese | Studentka, dobrovolnice v NGO |
| Tech literacy | Střední |
| Zařízení | Samsung Galaxy A54 |

**Cíle:**
- Bezpečně komunikovat v rámci skupiny aktivistů
- Nebýt vystopovatelná během demonstrací
- Pohodlí použití

**Bolestivá místa:**
- Briar je technicky náročný a UX odpuzuje
- Bojí se, že její komunikace může být sledována
- Potřebuje rychle vyměnit kontakt s lidmi na akcích

**Jak SecureWhisper pomáhá:**
- Bluetooth/NFC sdílení kódů na akcích
- Lokální discovery v rámci akce funguje i bez internetu
- Moderní UX podobné běžným aplikacím

---

### 1.4 Persona D: Manažer „Petr"

| Atribut | Detail |
|---------|--------|
| Věk | 45 |
| Lokace | Praha, ČR |
| Profese | Vedoucí oddělení v advokátní kanceláři |
| Tech literacy | Nízká až střední |
| Zařízení | Samsung Galaxy S24 |

**Cíle:**
- Bezpečně komunikovat s klienty o citlivých záležitostech
- Splnit požadavky GDPR a profesního tajemství
- Vyhnout se cloudovým úložištím

**Bolestivá místa:**
- Nerozumí kryptografii, ale ví, že ji potřebuje
- Bojí se, že nastavení bude příliš složité
- Potřebuje něco, co používá i klient bez tech znalostí

**Jak SecureWhisper pomáhá:**
- Onboarding navržený pro netechnické uživatele
- QR kód jako primární metoda sdílení (klient naskenuje)
- Vizuální indikace bezpečnosti (Verified badge, zámeček)

---

## 2. User Journey Maps

### 2.1 Journey: První spuštění aplikace (Persona D – Petr)

| Fáze | Akce uživatele | Myšlenky / pocity | Pain points | Příležitosti |
|------|----------------|-------------------|-------------|--------------|
| Discovery | Slyší o aplikaci od kolegy | „Zní to dobře, ale bude to fungovat?" | Pochybnosti o legitimitě | Důvěryhodný onboarding |
| Stažení | Najde a stáhne z Play Store | „Je to ta správná aplikace?" | Strach z fake apps | Verified developer badge |
| První spuštění | Otevře aplikaci | „Co po mě bude chtít?" | Nemá rád dlouhé registrace | Žádná registrace! |
| Onboarding | Projde 4 obrazovky | „Aha, žádné telefonní číslo nepotřebuji" | Možná příliš tech jazyk | Jednoduchý jazyk + ilustrace |
| Identita | Aplikace mu zobrazí jeho QR | „Co mám dělat dál?" | Neví, jak přidat klienta | Tlačítko „Pozvat někoho" prominentní |
| První kontakt | Sejde se s klientem, ten naskenuje QR | „Funguje to?" | Strach, že to neudělá správně | Vizuální feedback („Spojeno!") |

**Kritické momenty:** Onboarding (musí pochopit princip) a první přidání kontaktu (musí být úspěšné).

---

### 2.2 Journey: Posílání zprávy v terénu (Persona A – Eva)

| Fáze | Akce | Pocity | Pain points | Příležitosti |
|------|------|--------|-------------|--------------|
| Plánování | Schůzka se zdrojem | Napětí, opatrnost | Strach ze sledování | Aplikace nesmí budit pozornost |
| Setkání | Vymění QR kódy osobně | Soustředění na bezpečnost | Strach z chyby | Jasný feedback ověření |
| Verifikace | Porovnají safety number | Důvěra | Rušné prostředí | Velký, čitelný safety number |
| Komunikace | Pošle dotaz | Nervozita | Co když je sledována | Indikace E2E v UI |
| Doručení | Vidí „doručeno" | Úleva | Co když to nezašifrovalo | Vizualizace šifrování (zámeček) |
| Hovor | Domluví videohovor | Důvěra | Bojí se falešného peera | Kontrola fingerprint během hovoru |

---

### 2.3 Journey: Navázání spojení přes hex kód (Persona C – Alice)

```
[Discovery]
   ↓
Slyší o aplikaci na demonstraci
   ↓
[Acquisition]
   ↓
Stáhne z F-Droid (žije v Linux ekosystému)
   ↓
[Activation]
   ↓
První spuštění → vygeneruje identitu (5s)
   ↓
[Connection]
   ↓
Otevře QR kód → kamarádka skenuje
   ↓
[First message]
   ↓
Posílá „Ahoj, funguje to?" → doručeno za 2s
   ↓
[Retention]
   ↓
Používá denně, doporučuje kolegům
   ↓
[Advocacy]
   ↓
Píše blog post o aplikaci, učí kolegy
```

---

## 3. Empathy maps (vybrané persony)

### 3.1 Empathy map – Eva (novinářka)

**ŘÍKÁ:** „Potřebuju nástroj, který nemůže být donucen předat moje data."
**MYSLÍ SI:** „Můžu té aplikaci skutečně věřit? Kdo to napsal?"
**DĚLÁ:** Studuje audit reporty, ptá se kolegů, testuje na vedlejších zdrojích.
**CÍTÍ:** Trvalou nervozitu z odpovědnosti za zdroje.

### 3.2 Empathy map – Petr (manažer)

**ŘÍKÁ:** „Potřebuju to mít stejně jednoduché jako WhatsApp."
**MYSLÍ SI:** „Bude tomu klient rozumět?"
**DĚLÁ:** Hledá doporučení, čte recenze.
**CÍTÍ:** Stres z nesplnění GDPR.

---

## 4. Klíčové insighty pro design

1. **Onboarding musí být přístupný i netechnickým uživatelům** (Petr) — ale s možností „Show me more" pro tech-savvy (Marek).
2. **Sdílení kontaktu musí být extrémně rychlé** — typický scénář (Alice) je výměna na ulici během 30 sekund.
3. **Verifikace musí být vizuálně přesvědčivá** — uživatel musí cítit „je to skutečně on" (Eva).
4. **Aplikace nesmí budit pozornost** — neutrální ikona, generický název v notifikacích (volba) (Eva, Alice).
5. **Onboarding musí jasně komunikovat omezení** — online-only doručení nesmí být překvapení.
