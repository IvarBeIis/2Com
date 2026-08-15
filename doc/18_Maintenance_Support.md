# 18 – Maintenance & Support Document

**Projekt:** SecureWhisper
**Verze:** 1.0

---

## 1. Support model

### 1.1 Kanály podpory

| Kanál | Cílový segment | Response SLA |
|-------|----------------|--------------|
| GitHub Issues | Power users, developers | Best effort, ~3 dny |
| support@securewhisper.org | Všichni Free uživatelé | 5 pracovních dní |
| Komunitní fórum (Discourse) | Všichni | Best effort, peer support |
| Mastodon @SecureWhisper | Všichni (public) | Best effort |
| Plus Priority Support | Plus předplatitelé | 3 pracovní dny |
| Business Support | B2B zákazníci | Per SLA |

### 1.2 Podpora dle severity

| Severity | Definice | Response | Resolution target |
|----------|----------|----------|-------------------|
| P0 (Critical) | Aktivní bezpečnostní incident, široký výpadek | 1h | 24h |
| P1 (High) | Major funkcionalita nefunkční | 4h | 72h |
| P2 (Medium) | Workaround dostupný | 1 pracovní den | 14 dní |
| P3 (Low) | Kosmetické, feature request | 5 prac. dní | Plánované |

---

## 2. SLA

### 2.1 Free tier SLA

**Žádný formální SLA** – aplikace je open-source a poskytována as-is.

**Reasonable best effort:**
- Bootstrap nodes uptime: 99 % cíl, ne garance
- Security patches: do 7 dní od potvrzení (P1 a vyšší)
- Bug fixes: per release schedule (měsíčně)

### 2.2 Plus tier SLA

| Položka | Hodnota |
|---------|---------|
| Bootstrap nodes uptime | 99,5 % měsíčně |
| Email response | 3 pracovní dny |
| Security patch (P1+) | 48h |
| Refund policy | Pro-rated cancellation kdykoliv |

### 2.3 Business tier SLA

| Položka | Hodnota |
|---------|---------|
| Self-hosted node uptime | 99,9 % (s redundantní infrastrukturou) |
| Email response | 1 pracovní den |
| Phone/video support | 4 hodiny pracovní doby |
| Security patch (P0/P1) | 24h |
| Custom feature deliverable | Per kontrakt |
| Onboarding training | Zahrnut |
| Compliance dokumentace | DPA, ISO 27001 SOA |

### 2.4 Service credits

Pro Plus a Business tier při nesplnění SLA:

| Měsíční uptime | Service credit |
|----------------|----------------|
| < 99,5 % (Plus) / < 99,9 % (Business) | 10 % |
| < 99 % | 25 % |
| < 95 % | 50 % |
| < 90 % | 100 % |

---

## 3. Hotfix proces

### 3.1 Kritéria pro hotfix

Hotfix release je vyhrazen pro:
- **Bezpečnostní zranitelnosti** (P0/P1)
- **Crash-loop bugy** zasahující > 1 % uživatelů
- **Data corruption** rizika
- **Regulatorní compliance** (urgentní právní požadavky)

NE pro: features, nice-to-have improvements (tyto čekají na regular release).

### 3.2 Hotfix workflow

```
[Issue identified]
    ↓
[Severity assessment by Tech Lead + Security Officer]
    ↓
[War room sestaven]
    ├── Tech Lead (decision maker)
    ├── Security Officer (pokud security)
    ├── On-call developer (implementation)
    └── Product Owner (communication)
    ↓
[Fix + targeted tests]
    ↓
[Branch hotfix/v1.0.X]
    ↓
[Expedited PR review (2 reviewers, < 1h)]
    ↓
[CI runs, smoke test on 3 reference devices]
    ↓
[Merge, tag, release]
    ↓
[Staged rollout: 1% → 10% → 50% → 100% over 24-72h]
    ↓
[Post-incident review within 7 days]
```

### 3.3 Hotfix komunikace

- **Interní:** Slack/Matrix war room channel
- **Pro Plus/Business:** Email + in-app notifikace s release notes
- **Veřejně:** Mastodon, GitHub Release, blog post (pokud security)

### 3.4 Security disclosure timeline

Standardní coordinated disclosure:

```
Day 0:  Researcher reports vulnerability
Day 1:  Acknowledgement, severity assessment
Day 2-7: Fix development
Day 8-14: Internal testing, staged rollout
Day 30: Public disclosure (CVE, blog post)
Day 90: Maximum pre-disclosure delay
```

Pro aktivně exploitovanou zranitelnost — okamžitá public coordination s reporterem.

---

## 4. Deprecation policy

### 4.1 Verze aplikace

| Verze status | Definice | Doba |
|--------------|----------|------|
| **Active** | Latest minor release | – |
| **Maintenance** | Předchozí minor | 6 měsíců po dalším minor release |
| **Deprecated** | Stále funkční, žádné updaty | Další 6 měsíců |
| **End-of-Life (EOL)** | Force upgrade nebo přestane fungovat | – |

### 4.2 Android verze support

| Android verze | API | Status | Plán |
|---------------|-----|--------|------|
| Android 8.0 / 8.1 | 26 / 27 | Supported (minSdk) | Drop 2027-06 |
| Android 9 | 28 | Supported | Drop 2027-06 |
| Android 10 | 29 | Supported | Drop 2028-01 |
| Android 11+ | 30+ | Supported | – |

**Princip:** minSdk se zvyšuje, když distribuce zařízení s nižším API klesne pod 5 % v cílových trzích.

### 4.3 Deprecation komunikační plán

**T-6 měsíců před EOL:**
- Deprecation oznámeno v release notes
- Blog post s vysvětlením
- In-app banner pro postižené uživatele

**T-3 měsíce:**
- Eskalovaná in-app warning
- Email pro Plus/Business uživatele
- Migration guide publikován

**T-1 měsíc:**
- Mandatory in-app warning
- Last chance email

**T-0:**
- Verze přestává být kompatibilní
- App store redirect na latest verzi
- Web banner

### 4.4 Wire protocol versioning

Wire protocol mezi peery se mění s opatrností:
- **Backward compatible changes:** Bez problému (nové optional fields)
- **Breaking changes:** Vyžadují feature flag, postupný rollout, min. 12 měsíců overlap

**Příklad postupu pro breaking wire change:**

```
v1.0.0: Wire v1 (current)
v1.5.0: Wire v1 + v2 (oba podporované, default v1)
v2.0.0: Wire v1 + v2 (default v2)
v3.0.0: Wire v2 only (drop v1)
```

Mezi v1.5.0 a v3.0.0 je minimum 12 měsíců.

### 4.5 DB schema migrace

Každá změna DB schema musí mít:
- Forward migration (mandatory)
- Test pro upgrade z předchozích N verzí
- Backup automatically created před migrací (fallback)

**Migration retention:** Podporujeme upgrade z verzí starších až 24 měsíců. Starší verze musí jít přes intermediate update.

---

## 5. Údržbové aktivity

### 5.1 Pravidelná údržba

| Aktivita | Frekvence | Vlastník |
|----------|-----------|----------|
| Dependency updates (minor/patch) | Týdně | Renovate Bot + review |
| Dependency updates (major) | Per release plánovaně | Tech Lead |
| Security audit (full) | Ročně | External auditor |
| Penetration test | 2× ročně | External |
| Bootstrap node maintenance | Měsíčně | DevOps |
| TLS certificate renewal | Auto via Let's Encrypt | DevOps |
| Bootstrap node OS updates | Měsíčně | DevOps |
| Backup testing | Kvartálně | DevOps |
| Disaster recovery drill | Pololetně | DevOps + Tech Lead |
| Privacy Policy review | Ročně + ad-hoc | Legal |
| ToS review | Ročně + ad-hoc | Legal |

### 5.2 Bootstrap node lifecycle

#### Nasazení nového nodu

1. Nový VPS/server provisioning (Ansible playbook)
2. OS hardening (CIS benchmarks)
3. SecureWhisper bootstrap binary deployment
4. Monitoring agent install (Prometheus node_exporter)
5. DNS A/AAAA record (po health check)
6. Soft launch — receive 10 % traffic for 24h
7. Full traffic + alert configuration

#### Decommissioning nodu

1. Remove DNS record (graceful: 1h předtím)
2. Drain traffic (no new connections, finish active)
3. Stop service
4. Wipe disk (DBAN nebo cryptographic erase)
5. Return hardware / decommission VPS

### 5.3 Disaster recovery

#### Scénáře

**Scenario A: Single bootstrap node failure**
- Auto-failover (DNS removes failed IP)
- Replacement node provisioned do 4h
- No user impact (multi-region setup)

**Scenario B: All bootstrap nodes failure**
- Aplikace automaticky přechází na hardcoded fallback DHT peery:
  - `45.76.100.42:49737` (US East, Vultr)
  - `95.179.200.11:49737` (EU Frankfurt, Vultr)
  - `139.162.55.73:49737` (Asia Singapore, Linode)
  - `178.62.194.88:49737` (EU Amsterdam, DigitalOcean)
  - `2a01:4f8:c0c:9abc::1:49737` (EU Helsinki IPv6, Hetzner)
- Aktivace emergency bootstrap (cloud-deployed instance)
- Communication via Mastodon, blog post
- Recovery target: 4h
- During outage: existing connections fungují; noví uživatelé používají hardcoded fallback peery

**Scenario C: Compromise of signing keys**
- Immediate key revocation komunikace
- New signing key generated v HSM
- Re-signed builds released
- Communication kanály: blog, Mastodon, email
- Recovery target: 7 dní pro plnou re-distribuci

**Scenario D: Critical zero-day v dependency**
- Hotfix process
- If exploitation widespread: temporary feature disable
- Communication: immediate

#### Backup strategy

**Bootstrap nodes:**
- Konfigurace v Git (Ansible)
- No persistent data (in-memory only)
- Recovery: re-deploy from Git

**Signing infrastructure:**
- Master keys v HSM s redundantním backup HSM
- Recovery procedure tested kvartálně
- 3-of-5 multi-sig pro klíčové operace

**Documentation a metadata:**
- GitHub repository (geo-distributed by GitHub)
- Mirror na GitLab
- Quarterly export do offline storage

---

## 6. Knowledge management

### 6.1 Runbooks

Pro každý on-call scénář existuje runbook v `/docs/runbooks/`:
- `bootstrap-node-down.md`
- `dht-degraded.md`
- `release-rollback.md`
- `security-incident-response.md`
- `play-store-rejection.md`
- `crashlytics-spike.md`

### 6.2 On-call rotation

- Týdenní rotace mezi seniorními inženýry (min. 3 lidi)
- Primary + Secondary
- Pager: PagerDuty (alebo Grafana OnCall)
- Compensation: 1 day off za týden on-call

### 6.3 Postmortem repository

Public-where-possible postmortemy v `/docs/postmortems/` s předem schváleným šablonem.

### 6.4 Architecture Decision Records (ADRs)

Klíčová rozhodnutí dokumentována v `/docs/adr/`:
- ADR-001: Volba Hyperswarm DHT
- ADR-002: Kotlin + Compose stack
- ADR-003: SQLCipher pro lokální DB
- ADR-004: Self-hosted Sentry vs. Crashlytics
- ADR-005: AGPL-3.0 license
- ADR-006: minSdk 26 strategie

---

## 7. Customer success

### 7.1 Onboarding nových B2B zákazníků

| Týden | Aktivita |
|-------|----------|
| 1 | Kick-off call, requirements gathering |
| 2 | Self-hosted node deployment |
| 3 | User onboarding training (záznam dostupný) |
| 4 | Pilot s 5-10 uživateli |
| 5-8 | Postupný rollout |
| 9 | Review meeting, success metrics |
| Quarterly | Business review |

### 7.2 Renewal proces

- T-90 dní: Customer success call
- T-60 dní: Renewal proposal
- T-30 dní: Final renewal discussion
- T-0: Renewal nebo offboarding

### 7.3 Churn prevention

- Health score per Business account (usage, support tickets, satisfaction)
- Proaktivní outreach pro at-risk accounts
- Roadmap input session pro největší zákazníky

---

## 8. Documentation maintenance

### 8.1 Doc types and ownership

| Doc | Vlastník | Update frequency |
|-----|----------|------------------|
| User docs (uživatelská) | Product + UX | Per release |
| API docs (bootstrap) | Tech Lead | Per API change |
| SDK docs (B2B) | Tech Lead | Per release |
| Architecture docs | Tech Lead | Per major change |
| Privacy Policy | Legal | Annually + ad-hoc |
| Security whitepaper | Security Officer | Annually |
| FAQ | Customer Success | Continuously |

### 8.2 Translation management

- Source language: Čeština (cs)
- Translations via Weblate (community)
- Profesionální překlad pro EN, DE
- Translation freeze 14 dní před release

### 8.3 Documentation changelog

Public documentation changes v `docs/CHANGELOG.md`, podobné kódovému changelogu.

---

## 9. End-user support

### 9.1 In-app help

- Kontextové tipy v onboardingu
- "?" ikony s vysvětlením u technických polí
- Settings → Help → FAQ (offline available)
- Settings → Help → Send diagnostics (opt-in)

### 9.2 FAQ kategorie

1. Začínáme (onboarding, identita, kontakty)
2. Bezpečnost (šifrování, safety number, app lock)
3. Síťování (proč nejde poslat zprávu, peer offline)
4. Hovory (nelze připojit, špatná kvalita)
5. Předplatné (Plus, Business)
6. Soukromí (jaká data sbíráme)

### 9.3 Customer feedback loop

- In-app feedback submission (anonymous možné)
- Quarterly user survey (NPS)
- Beta channel pro early feedback
- User research interviews (kvartálně)

---

## 10. End-of-life pro projekt (worst case)

Pokud by projekt musel skončit:

### 10.1 Sunset komunikace

- T-12 měsíců: Veřejné oznámení sunsetu
- T-6 měsíců: Bootstrap nodes přejdou do maintenance only
- T-3 měsíce: Final release s "sunset" mode
- T-0: Bootstrap nodes shutdown

### 10.2 Open source kontinuita

- Source code zůstává na GitHubu (Internet Archive backup)
- Maintainership announcement — komunita může převzít
- Documentation a build infrastructure předány komunitě
- Domain a značka mohou být převedeny na non-profit (např. CCC, EFF)

### 10.3 Uživatelská data

- Aplikace funguje lokálně i bez bootstrap nodů (LAN/Bluetooth)
- LAN/proximity discovery zůstává funkční navždy
- Final release umožní easy migration na alternativy (export do compatible formátu)
