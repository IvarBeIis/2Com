# 05 – Monetization & Pricing Strategy

**Projekt:** SecureWhisper
**Verze:** 1.0

---

## 1. Filozofie monetizace

Privacy-first aplikace nemůže monetizovat tradičními cestami (reklamy, prodej dat). Naším cílem je udržitelný provoz bez kompromisu v ochraně soukromí uživatelů a bez vytváření perverzních pobídek (např. shromažďování dat pro reklamní cílení).

**Vodicí principy:**
- Žádná reklama uvnitř aplikace
- Žádný prodej dat třetím stranám
- Žádné dark patterns nebo umělé limity
- Transparentní financování
- Open-source kód zůstává volně dostupný

## 2. Doporučený model: Freemium + B2B + Donations

### 2.1 Free tier (pro 95 % uživatelů)
- Plná funkcionalita chatu, audio, video pro 1:1
- Neomezený počet kontaktů
- Bluetooth/LAN/DHT discovery
- Lokální export
- E2E šifrování beze změny

**Cíl:** Maximální adopce, žádné překážky pro privacy-conscious uživatele.

### 2.2 SecureWhisper Plus (€2,99/měsíc nebo €29/rok)

**Pro power users:**
- Větší soubory v chatu (do 1 GB místo 25 MB)
- Vlastní bootstrap nodes (přidání + export seznamu)
- Pokročilá témata a customizace UI
- Prioritní onboarding pro nové kontakty (rychlejší DHT lookup)
- Více současných aktivních konverzací bez performance dopadu (technically: víc keep-alive spojení)
- Cloud-encrypted backup do uživatelem zvolené úložiště (Mega, Filen, vlastní WebDAV)

**Cíl:** Pokrytí provozních nákladů (bootstrap nodes, vývoj).

### 2.3 SecureWhisper Business (€8/uživatel/měsíc, min. 5 uživatelů)

**Pro firmy:**
- Self-hosted bootstrap node + DHT overlay (jen pro firemní uživatele)
- Centralizovaný audit log (volitelný, pouze metadata typu „kdo se připojil")
- Custom branding (white-label varianta)
- Priority support s SLA
- Compliance dokumentace (GDPR, ISO 27001)
- Onboarding training

**Cíl:** Hlavní zdroj příjmu po dosažení product-market fit.

### 2.4 Donations / Sponzorství

- GitHub Sponsors / Liberapay / Patreon
- Cryptocurrency wallety (BTC, Monero)
- Bankovní převody pro transparentní účet
- Možnost firemního sponzorství (logo na webu, pokud si firma přeje)

**Cíl:** Doplňkový zdroj, signál komunitní podpory pro grant aplikace.

### 2.5 Granty

- **NLnet Foundation** – EU technologická infrastruktura
- **Open Technology Fund (OTF)** – tools pro digitální práva
- **Mozilla Foundation** – web/communication privacy
- **Sovereign Tech Fund** (DE) – open-source infrastruktura
- **Ford Foundation** – digitální práva

**Cíl:** Pokrytí konkrétních feature roadmap milestones (audity, lokalizace, iOS port).

## 3. Cenová strategie

### 3.1 Free → Plus konverze

| Metric | Cíl 12M | Cíl 24M |
|--------|---------|---------|
| Free → Plus konverze | 1,5 % | 3 % |
| Plus retention (12M) | 65 % | 70 % |
| Average revenue per Plus user (ARPU) | €25 | €28 |

### 3.2 Pricing benchmarking

| Aplikace | Free | Premium | Business |
|----------|------|---------|----------|
| Signal | Vše | – | – |
| Threema | – | €5 jednorázově | €2,5/uživatel/měsíc |
| Telegram | Vše | €4,99/měsíc (Premium) | – |
| Wickr | – | – | $5/uživatel/měsíc |

**Pozice SecureWhisper:** Mírně pod Threemou na B2B, srovnatelně s Telegram Premium na consumer.

## 4. Strategie vyhnutí se pasti

### 4.1 Co NEbudeme dělat
- **Reklamy:** Nikdy. Porušuje to základní hodnotovou propozici.
- **Prodej dat:** Nikdy (i když by to bylo „anonymizované").
- **Umělé limity:** Žádné limity typu „zdarma jen 100 zpráv denně".
- **Dark patterns:** Žádné nepopiratelné upselly nebo skryté předplatné.
- **In-app nákupy stickerů/emoji:** Banalizovalo by to vážné poslání.

### 4.2 Plánovaná obrana proti tlaku na monetizaci
- Veřejné finanční reporty (čtvrtletně)
- Komitment k open-source v stanovách (governance jako Signal Foundation)
- Možnost forku, pokud by někdy došlo ke kompromisu hodnot

## 5. Forecast příjmů (24 měsíců)

| Kvartál | Free MAU | Plus subs | Business účty | Donations | Granty | Total revenue/Q |
|---------|----------|-----------|---------------|-----------|--------|------------------|
| Q1 (launch) | 1 000 | 0 | 0 | €500 | €0 | €500 |
| Q2 | 5 000 | 50 | 0 | €1 500 | €30 000 | €33 000 |
| Q3 | 12 000 | 200 | 1 (10 users) | €2 500 | €0 | €5 800 |
| Q4 | 25 000 | 500 | 3 (40 users) | €4 000 | €50 000 | €62 000 |
| Q5 | 40 000 | 800 | 8 (100 users) | €5 000 | €0 | €15 000 |
| Q6 | 60 000 | 1 200 | 15 (200 users) | €6 000 | €0 | €25 000 |
| Q7 | 85 000 | 1 800 | 25 (350 users) | €7 500 | €60 000 | €105 000 |
| Q8 | 120 000 | 2 500 | 40 (550 users) | €9 000 | €0 | €50 000 |

**Kumulativní 24M revenue:** ~€296 000

**Hlavní závěr:** V prvních 24 měsících jsou granty a B2B kontrakty kritické. Plus subscriptions samy nepokryjí náklady do dosažení škály.

## 6. Měření úspěchu monetizace

### KPI

- **Plus konverze rate:** 1,5 % po 12M, 3 % po 24M
- **B2B churn rate:** < 10 % ročně
- **Revenue per active user (RPU):** €0,30 po 12M, €0,75 po 24M
- **Donor count growth:** 30 % YoY
- **Grant funding ratio:** > 30 % příjmů v prvních 18 měsících

### Negativní KPI (ke sledování)
- Stížnosti na monetizační praktiky v recenzích: < 1 %
- Uninstall rate spojená s upgrade prompts: < 0,5 %
