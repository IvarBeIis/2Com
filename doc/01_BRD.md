# 01 – Business Requirements Document (BRD)

**Projekt:** SecureWhisper
**Verze:** 1.0
**Status:** Draft

---

## 1. Účel dokumentu

Tento dokument definuje obchodní cíle, přínosy, cílovou skupinu a měřitelné kritéria úspěchu projektu SecureWhisper – Android aplikace pro plně šifrovanou P2P komunikaci.

## 2. Vize projektu

Vytvořit komunikační nástroj, který poskytuje silnou ochranu soukromí na úrovni protokolu, bez závislosti na centrální infrastruktuře a bez nutnosti registrace pomocí osobních údajů (telefonní číslo, e-mail). Spojení se navazuje pouze na základě sdíleného hexadecimálního identifikátoru.

## 3. Obchodní cíle

| ID | Cíl | Časový horizont |
|----|-----|------------------|
| G1 | Uvedení MVP se základní chat funkcionalitou na Google Play | 9 měsíců |
| G2 | Doplnění audio a video hovorů | 12 měsíců |
| G3 | Dosáhnout 50 000 aktivních uživatelů (MAU) | 18 měsíců |
| G4 | Etablovat aplikaci jako důvěryhodnou alternativu pro privacy-conscious uživatele | 24 měsíců |
| G5 | Provést nezávislý bezpečnostní audit a publikovat výsledky | 15 měsíců |

## 4. Obchodní přínos

### 4.1 Pro uživatele
- Komunikace bez zanechání digitální stopy u poskytovatele
- Žádné PII (telefonní číslo, e-mail) nutné pro registraci
- Plná kontrola nad daty (lokální úložiště, žádné cloudové zálohy by default)
- Open-source kód umožňuje nezávislý audit

### 4.2 Pro provozovatele
- Nízké provozní náklady (žádný centrální backend pro obsah)
- Diferenciace na trhu komunikačních aplikací
- Potenciál pro B2B nabídku (firemní self-hosted varianta)
- Etický pozicioning posiluje značku

## 5. Cílová skupina

### 5.1 Primární segmenty
- **Privacy-aware jednotlivci** – novináři, aktivisté, právníci, lékaři
- **Technicky orientovaní uživatelé** – vývojáři, security profesionálové
- **Uživatelé v rizikových regionech** – kde je dohled na komunikaci běžný

### 5.2 Sekundární segmenty
- Malé firmy hledající bezpečnou interní komunikaci
- Skupiny vyžadující anonymitu (whistleblowers, terapeutické skupiny)

## 6. Klíčové metriky úspěchu (KPI)

### 6.1 Akviziční metriky
| KPI | Cíl 6 měsíců | Cíl 12 měsíců | Cíl 18 měsíců |
|-----|--------------|---------------|---------------|
| Total downloads | 5 000 | 25 000 | 100 000 |
| Monthly Active Users (MAU) | 2 000 | 12 000 | 50 000 |
| Daily Active Users (DAU) | 500 | 4 000 | 18 000 |
| DAU/MAU ratio (stickiness) | 25 % | 33 % | 36 % |

### 6.2 Engagement metriky
- Průměrný počet zpráv na uživatele/den: 15+
- Průměrný počet aktivních konverzací na uživatele: 3+
- Retention D1 / D7 / D30: 60 % / 35 % / 20 %

### 6.3 Kvalitativní metriky
- Crash-free sessions: ≥ 99,5 %
- ANR rate: < 0,1 %
- Průměrné hodnocení v Play Store: ≥ 4,3
- Doba navázání spojení (P95): < 15 sekund
- Úspěšnost navázání P2P spojení: ≥ 85 %

### 6.4 Bezpečnostní metriky
- Počet kritických zranitelností v produkci: 0
- Doba odezvy na hlášenou zranitelnost (P1): < 24 hodin
- Frekvence security auditů: 1× ročně

## 7. Předpoklady

- Existuje poptávka po privacy-first komunikační aplikaci nezávislé na telefonním čísle
- Cílová skupina je ochotná akceptovat určité kompromisy v UX (latence, online-only doručení) výměnou za soukromí
- Open-source distribuce zvyšuje důvěryhodnost a nezhoršuje monetizaci

## 8. Omezení

- Nelze garantovat doručení zpráv při offline příjemci v MVP
- Závislost na bootstrap uzlech DHT pro počáteční discovery
- Apple App Store není v plánu MVP (kvůli omezením P2P frameworků)
- Aplikace nesmí porušovat lokální legislativu o šifrování (Čína, Rusko, některé arabské státy nepokryté)

## 9. Schvalování

| Role | Jméno | Datum | Podpis |
|------|-------|-------|--------|
| Product Owner | – | – | – |
| Technical Lead | – | – | – |
| Security Officer | – | – | – |
