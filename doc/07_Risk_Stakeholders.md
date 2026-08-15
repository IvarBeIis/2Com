# 07 – Risk & Stakeholder Register

**Projekt:** SecureWhisper
**Verze:** 1.0

---

## 1. Stakeholder Register

### 1.1 Interní stakeholders

| Role | Odpovědnost | Schvaluje | Komunikace |
|------|-------------|-----------|------------|
| Product Owner | Produktová vize, roadmapa, priority | PRD, release scope | Týdenně |
| Technical Lead | Architektura, technická řešení | SRS, HLD, LLD | Týdenně |
| Security Officer | Threat modeling, audity, response | Security Document, audit reports | Bi-weekly |
| UX Lead | Uživatelská zkušenost, design system | UX Design Spec | Bi-weekly |
| QA Lead | Test strategy, quality gates | Test Plan, release readiness | Týdenně |
| DevOps Lead | CI/CD, infrastruktura, monitoring | CI/CD doc, Performance Plan | Bi-weekly |
| Legal Counsel | GDPR, terms, compliance | Privacy policy, ToS | Měsíčně |

### 1.2 Externí stakeholders

| Role | Vztah | Vliv | Engagement strategie |
|------|-------|------|----------------------|
| Koncoví uživatelé | Spotřebitelé | Vysoký | In-app feedback, komunita |
| Privacy NGO | Advokáti, partneři | Vysoký | Pravidelné aktualizace, společné PR |
| Bezpečnostní auditoři | Validátoři kvality | Vysoký | Roční audit, follow-up |
| F-Droid maintainers | Distribuční kanál | Střední | Compliance s policy |
| Google (Play Store) | Distribuční kanál | Vysoký | Compliance, app review |
| Funding partneři (granty) | Finanční podpora | Vysoký | Pravidelný reporting |
| Komunita kontributorů | Kód, překlady | Střední | Open governance |
| Tech média | Public awareness | Střední | Press kit, briefingy |
| Regulátoři (DPC, ENISA) | Compliance | Vysoký (potenciálně) | Reactive, právní zástupce |
| Konkurence | Tržní dynamika | Nízký | Benchmark, ne aktivní engagement |

### 1.3 RACI matrix (klíčové aktivity)

| Aktivita | PO | TL | SO | UX | QA | DevOps |
|----------|----|----|----|----|----|--------|
| Schválení feature scope | A | C | C | C | I | I |
| Architektonická rozhodnutí | C | A | R | I | I | C |
| Threat model update | I | C | A | I | I | C |
| Release decision | A | R | R | I | R | R |
| UX research | C | I | I | A | I | – |
| Security incident response | C | C | A | I | C | R |
| Penetration test koordinace | I | C | A | – | C | C |

*A = Accountable, R = Responsible, C = Consulted, I = Informed*

---

## 2. Risk Register

### 2.1 Klasifikace rizik

**Pravděpodobnost (P):** 1 (velmi nízká) – 5 (velmi vysoká)
**Dopad (D):** 1 (zanedbatelný) – 5 (kritický)
**Skóre = P × D**

### 2.2 Technická rizika

| ID | Riziko | P | D | Skóre | Mitigace | Vlastník |
|----|--------|---|---|-------|----------|----------|
| TR-01 | NAT traversal selhává u mobilních operátorů (CGNAT) | 4 | 4 | 16 | Implementace TURN-like relay přes komunitní uzly | TL |
| TR-02 | Hyperswarm DHT bootstrap nodes se stanou cílem útoku | 3 | 4 | 12 | Multiple bootstrap, DNS seeds, comunita uzlů | TL |
| TR-03 | Vysoká spotřeba baterie odradí uživatele | 4 | 3 | 12 | Adaptivní DHT participace, FCM wake-up | TL |
| TR-04 | Prolomení použité kryptografické knihovny | 1 | 5 | 5 | libsignal s aktivním údržbou, monitoring CVE | SO |
| TR-05 | Memory leak v dlouho běžící aplikaci | 3 | 3 | 9 | Stress testy, LeakCanary, profiling | QA |
| TR-06 | Inkompatibilita s budoucími Android verzemi | 3 | 3 | 9 | Beta testy s Android Developer Preview | TL |
| TR-07 | DTLS-SRTP fingerprint mismatch v některých zařízeních | 2 | 3 | 6 | Široké device testing, fallback strategie | QA |
| TR-08 | Race conditions v Double Ratchet při rychlé výměně | 2 | 4 | 8 | Důsledné testování, rate limiting | TL |
| TR-09 | Hyperswarm dependency abandonware | 2 | 4 | 8 | Sledování upstream, plán B na libp2p | TL |
| TR-10 | DDoS na bootstrap nodes | 4 | 3 | 12 | Cloudflare, geo-distribuce, anycast | DevOps |

### 2.3 Bezpečnostní rizika

| ID | Riziko | P | D | Skóre | Mitigace | Vlastník |
|----|--------|---|---|-------|----------|----------|
| SR-01 | Únik privátního klíče přes side-channel | 2 | 5 | 10 | Android Keystore, hardware-backed keys | SO |
| SR-02 | Man-in-the-Middle při prvním kontaktu | 3 | 4 | 12 | Safety number, encouraged out-of-band verification | SO |
| SR-03 | Kompromitace bootstrap nodes (false routing) | 2 | 4 | 8 | DHT je untrusted, šifrování odolné vůči routing tampering | SO |
| SR-04 | Metadata leak přes DHT queries | 3 | 3 | 9 | Rotace lookup keys, optional Tor transport | SO |
| SR-05 | Replay útok na zachycené pakety | 1 | 4 | 4 | Ratchet protokol s pořadovými čísly | SO |
| SR-06 | Sledování přes traffic analysis | 3 | 3 | 9 | Padding, dummy traffic (volitelně), Tor support | SO |
| SR-07 | Malicious app store verze (supply-chain) | 2 | 5 | 10 | Reproducible builds, signing transparency | DevOps |
| SR-08 | Zranitelnost v native komponentách (WebRTC) | 2 | 4 | 8 | Aktualizace, monitoring CVE, fuzzing | SO |
| SR-09 | Únik dat při debugovacím režimu v produkci | 2 | 4 | 8 | Build pipeline kontroly, ProGuard/R8 | DevOps |

### 2.4 Produktová rizika

| ID | Riziko | P | D | Skóre | Mitigace | Vlastník |
|----|--------|---|---|-------|----------|----------|
| PR-01 | Nedostatečná uživatelská základna pro DHT efektivitu | 3 | 4 | 12 | Komunitní bootstrap, partnership s privacy NGO | PO |
| PR-02 | Příliš složité UX pro netechnické uživatele | 3 | 4 | 12 | UX research, iterativní onboarding, A/B testy | UX |
| PR-03 | Online-only doručení frustruje uživatele | 4 | 3 | 12 | Jasná komunikace, push notifikace, plán pro store-and-forward | PO |
| PR-04 | Chybí klíčové funkce vs. konkurence (skupiny) | 4 | 3 | 12 | Roadmap se skupinami v R3, jasná pozice MVP | PO |
| PR-05 | Negativní feedback kvůli battery drain | 3 | 3 | 9 | Pre-launch optimalizace, battery profiling | TL |

### 2.5 Byznysová rizika

| ID | Riziko | P | D | Skóre | Mitigace | Vlastník |
|----|--------|---|---|-------|----------|----------|
| BR-01 | Nedostatečné financování po vyčerpání seed | 3 | 5 | 15 | Diversifikované zdroje (granty, B2B, donations) | PO |
| BR-02 | Konkurence (Signal) přidá podobné funkce | 3 | 3 | 9 | Diferenciace přes plné P2P, B2B vertical | PO |
| BR-03 | Nízká konverze na Plus/Business | 3 | 4 | 12 | Iterativní pricing, customer development | PO |
| BR-04 | Klíčový stakeholder odejde z týmu | 2 | 4 | 8 | Knowledge sharing, dokumentace, governance | PO |

### 2.6 Regulatorní a právní rizika

| ID | Riziko | P | D | Skóre | Mitigace | Vlastník |
|----|--------|---|---|-------|----------|----------|
| LR-01 | EU Chat Control / „Going Dark" legislativa | 4 | 5 | 20 | Právní zástupce, advokační zapojení, EU sídlo | PO + Legal |
| LR-02 | Zablokování v některých zemích (RU, CN, IR) | 5 | 2 | 10 | Akceptace, fokus na svobodné trhy | PO |
| LR-03 | Google Play de-listing | 2 | 5 | 10 | F-Droid jako primární kanál, GitHub releases | PO |
| LR-04 | Apple App Store odmítnutí (budoucí iOS) | 4 | 2 | 8 | Akceptace pro MVP, právní review pro iOS | PO |
| LR-05 | GDPR porušení (i přes design) | 1 | 4 | 4 | DPIA, právní review, by-design dokumentace | Legal |
| LR-06 | Patent troll žaloba | 2 | 3 | 6 | Open Invention Network členství, prior art | Legal |

### 2.7 Operační rizika

| ID | Riziko | P | D | Skóre | Mitigace | Vlastník |
|----|--------|---|---|-------|----------|----------|
| OR-01 | Bootstrap nodes outage | 3 | 3 | 9 | Multi-region, monitoring, automated failover | DevOps |
| OR-02 | Klíčový dependency CVE bez patche | 3 | 3 | 9 | Security monitoring, fork/patch capability | TL |
| OR-03 | Únik podpisového klíče aplikace | 1 | 5 | 5 | HSM, key rotation procedure, signing transparency | DevOps |
| OR-04 | Loss of FCM access (Google sankce) | 1 | 3 | 3 | Self-hosted push (UnifiedPush) jako fallback | DevOps |

---

## 3. Risk monitoring

### 3.1 Risk review proces
- **Týdenní:** Tech Lead review nových technických rizik
- **Bi-weekly:** Security Officer review bezpečnostních rizik
- **Měsíční:** Full risk register review s Product Ownerem
- **Kvartální:** Strategic risk review s investory/grant funders

### 3.2 Trigger metriky pro escalaci

| Metric | Trigger | Akce |
|--------|---------|------|
| Crash-free rate | < 99 % | Eskalace na TL, hotfix |
| ANR rate | > 0,3 % | Performance review |
| P2P connection success | < 80 % | NAT/network analysis |
| Battery complaints | > 5 % recenzí | Optimization sprint |
| Negative reviews | > 10 % | Customer development |
| Security CVE v dependency | severity ≥ HIGH | Patch within 48h |
| Bootstrap node downtime | > 5 min | DevOps on-call |

### 3.3 Eskalační matice

| Úroveň | Kritérium | Příjemce | Reakce |
|--------|-----------|----------|--------|
| L1 | Skóre 1-6 | Owner risku | Standard mitigation |
| L2 | Skóre 7-12 | Owner + TL/PO | Plánovaná akce v sprint |
| L3 | Skóre 13-19 | Steering committee | Okamžitá pozornost |
| L4 | Skóre 20-25 | Board / všichni stakeholders | Krizový režim |
