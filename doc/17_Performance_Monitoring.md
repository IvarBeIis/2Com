# 17 – Performance & Monitoring Plan

**Projekt:** SecureWhisper
**Verze:** 1.0

---

## 1. Filozofie monitoringu

Privacy-first aplikace má specifické požadavky na monitoring: **nesmíme sledovat uživatele**. Monitoring je proto:
- **Opt-in** pro telemetrii (default OFF)
- **Lokální agregace** pokud možno (ne posílat raw events)
- **Anonymizovaný** s minimálními identifikátory
- **Transparentní** — uživatel vidí, co se sbírá

## 2. Performance cíle

### 2.1 Klíčové metriky

| Metric | Target P50 | Target P95 | Target P99 |
|--------|------------|------------|------------|
| Cold start | 1,2 s | 2,0 s | 3,0 s |
| Warm start | 300 ms | 600 ms | 1,0 s |
| Hot start | 100 ms | 200 ms | 400 ms |
| Time to first message screen | 2,0 s | 4,0 s | 6,0 s |
| Message send latency (active conn) | 200 ms | 500 ms | 1,0 s |
| Connection establishment (LAN) | 1,0 s | 5,0 s | 10 s |
| Connection establishment (DHT) | 3,0 s | 15 s | 30 s |
| Audio call setup | 2,0 s | 5,0 s | 10 s |
| Video call setup | 3,0 s | 7,0 s | 15 s |
| Audio one-way latency (good network) | 80 ms | 200 ms | 400 ms |

### 2.2 Resource footprint

| Resource | Idle | Active chat | During call |
|----------|------|-------------|-------------|
| RAM (peak) | 150 MB | 250 MB | 400 MB |
| CPU (avg) | <2 % | <8 % | <30 % |
| Battery / hour | <0.3 % | <2 % | <8 % |
| Data / hour idle | 50 KB | – | – |
| Data / minute call (audio) | – | – | ~200 KB |
| Data / minute call (video 720p) | – | – | ~5 MB |

### 2.3 Stabilitní cíle

| Metric | Target | Hard floor |
|--------|--------|------------|
| Crash-free user rate | ≥ 99,7 % | 99,5 % |
| Crash-free session rate | ≥ 99,8 % | 99,5 % |
| ANR rate | < 0,05 % | 0,1 % |
| Excessive wakelocks | 0 | 0 |
| Excessive battery drain (Android Vitals) | "Good" | – |

---

## 3. Crash a error monitoring

### 3.1 Volba nástroje

**Self-hosted Sentry** (sentry.io self-hosted) místo Firebase Crashlytics z důvodu:
- Plná kontrola nad daty
- Žádné posílání do Google
- Možnost on-prem pro B2B

**Backup option:** Bugsnag (s self-hosted variantou pro Enterprise).

### 3.2 Sentry konfigurace

```kotlin
// SecureWhisperApp.kt
override fun onCreate() {
    super.onCreate()
    
    if (BuildConfig.DEBUG || preferences.crashReportingEnabled) {
        SentryAndroid.init(this) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.environment = BuildConfig.BUILD_TYPE
            options.release = "${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
            options.tracesSampleRate = 0.1
            options.isAttachScreenshot = false       // Privacy
            options.isAttachViewHierarchy = false    // Privacy
            options.isSendDefaultPii = false         // Žádné PII
            options.beforeSend = SentryOptions.BeforeSendCallback { event, _ ->
                scrubSensitive(event)
            }
        }
    }
}

private fun scrubSensitive(event: SentryEvent): SentryEvent {
    // Odstranit message content, peer hashes, attachment paths
    event.breadcrumbs?.forEach { crumb ->
        crumb.message?.let { msg ->
            crumb.message = msg.replace(Regex("[0-9a-f]{32,}"), "[HEX_REDACTED]")
        }
    }
    event.contexts.remove("user")
    return event
}
```

### 3.3 Co se sbírá (jen po opt-in)

**Sbíráme:**
- Stack trace
- Device info (model, OS verze, RAM, screen size)
- App version, build type, locale
- Anonymous install ID (random UUID, regenerated on opt-in toggle)
- Breadcrumbs s scrubbed obsahem

**NEsbíráme:**
- IP adresa (Sentry must not log)
- User identity hash
- Peer hashes
- Message content
- File names, paths
- Phone number (nemáme), IMEI, advertising ID

### 3.4 Symbolikace

- ProGuard/R8 mapping files automaticky uploadnuty po release build
- NDK debug symbols pro libsignal/WebRTC native crashes

---

## 4. Performance monitoring

### 4.1 Macrobenchmark v CI

```kotlin
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartup() = benchmarkRule.measureRepeated(
        packageName = "cz.securewhisper",
        metrics = listOf(StartupTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD
    ) {
        pressHome()
        startActivityAndWait()
    }
    
    @Test
    fun conversationListJank() = benchmarkRule.measureRepeated(
        packageName = "cz.securewhisper",
        metrics = listOf(FrameTimingMetric()),
        iterations = 10
    ) {
        // ... scroll through conversation list
    }
}
```

CI gate: Regression > 10 % oproti baseline → block PR.

### 4.2 Production performance metrics

**Anonymous, opt-in only:**

```kotlin
class PerformanceTracker(private val sentry: SentryHub) {
    fun trackOperation(name: String, block: () -> Unit) {
        if (!preferences.performanceTrackingEnabled) {
            block()
            return
        }
        val transaction = sentry.startTransaction(name, "operation")
        try {
            block()
            transaction.status = SpanStatus.OK
        } catch (e: Throwable) {
            transaction.status = SpanStatus.INTERNAL_ERROR
            throw e
        } finally {
            transaction.finish()
        }
    }
}
```

Sledované operace:
- `connection.establish` (s tagem transport type)
- `message.send`
- `db.query` (s tagem query type)
- `crypto.handshake`
- `media.call.setup`

### 4.3 Battery monitoring

**Lokální:** Battery Historian dump pro debug builds, dostupný přes Settings → Diagnostics → Export battery report.

**Production:** Android Vitals automaticky reportuje battery anomálie do Play Console. Sledujeme:
- Excessive wakelocks (> 1h v 24h)
- Excessive wakeups (> 10/h)
- Excessive partial wake locks during background

### 4.4 Network monitoring

```kotlin
class NetworkPerformanceInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val start = System.nanoTime()
        val response = chain.proceed(chain.request())
        val durationMs = (System.nanoTime() - start) / 1_000_000
        
        // Aggregate locally; flush percentiles weekly if opted-in
        localMetrics.recordHttpDuration(
            host = chain.request().url.host,
            duration = durationMs,
            statusCode = response.code
        )
        return response
    }
}
```

---

## 5. Custom telemetry (opt-in)

### 5.1 Filozofie

- Default OFF
- Granularní toggle (crash reporting separately from performance)
- Lokální agregace, posílání jen agregovaných percentilů, ne raw events

### 5.2 Sbírané metriky

| Metric | Účel | Granularita |
|--------|------|-------------|
| `app_session_duration` | Engagement | Histogram |
| `connection_success_rate` | Network health | Per-transport % |
| `dht_lookup_duration` | DHT performance | Histogram |
| `crypto_handshake_duration` | Crypto perf | Histogram |
| `db_query_duration_p95` | DB perf | Per query name |
| `screen_time_distribution` | UX | Per screen, % |

### 5.3 Co NE-sbíráme

- Identifikátory peerů
- Obsah zpráv
- Frekvenci komunikace s konkrétními peery
- Geografická location
- Network ID, BSSID, SSID

---

## 6. Bootstrap node monitoring

Bootstrap servery jsou vlastní infrastruktura, kde monitoring je odlišný.

### 6.1 Stack

- **Metrics:** Prometheus
- **Visualization:** Grafana
- **Alerting:** Alertmanager → PagerDuty
- **Log aggregation:** Loki (s 7denní retention, žádné IP adresy)

### 6.2 Sledované metriky

```
# Resources
node_cpu_usage_percent
node_memory_usage_bytes
node_disk_usage_percent
node_network_bytes_total

# Application
sw_bootstrap_active_peers
sw_bootstrap_lookups_per_second
sw_bootstrap_announces_per_second
sw_bootstrap_response_duration_seconds (histogram)
sw_bootstrap_uptime_seconds

# Errors
sw_bootstrap_errors_total{type=...}
```

### 6.3 SLO bootstrap nodů

| SLO | Target | Window |
|-----|--------|--------|
| Availability | 99,5 % | 30 dní |
| Response latency P95 | < 200 ms | 7 dní |
| Error rate | < 0,1 % | 7 dní |

### 6.4 Alerts

| Alert | Severity | Action |
|-------|----------|--------|
| Node down > 5 min | P1 | Page on-call |
| All bootstrap nodes degraded | P0 | Wake CTO |
| Latency P95 > 500ms for 15min | P2 | Investigate |
| Error rate > 1% for 10min | P2 | Investigate |
| Disk > 85% | P3 | Schedule cleanup |

### 6.5 No-log policy

Bootstrap nodes:
- **NEUKLÁDAJÍ logy IP adres**
- Aplikační logy obsahují pouze metriky a chyby (anonymous)
- Retence 7 dní (Loki rotation)
- Pravidelný audit (kvartálně) potvrzující compliance s no-log

---

## 7. Health checks

### 7.1 In-app health screen

Skrytá obrazovka pro debugging (Settings → Advanced → Diagnostics):

```
┌─────────────────────────┐
│ Diagnostika             │
├─────────────────────────┤
│                         │
│ Stav aplikace           │
│ ✓ DB dostupná           │
│ ✓ Identita načtena      │
│ ✓ Keystore přístupný    │
│                         │
│ Síťová konektivita      │
│ ✓ Internet dostupný     │
│ ✓ Bootstrap node 1: OK  │
│ ✓ Bootstrap node 2: OK  │
│ ⚠ Bootstrap node 3:     │
│   Vysoká latence (820ms)│
│ ✓ DHT připojeno (12 peerů)│
│                         │
│ Aktivní spojení: 3      │
│ Probíhá hovor: ne       │
│                         │
│ [Export diagnostics]    │
│ [Reset connections]     │
└─────────────────────────┘
```

### 7.2 Diagnostic export

Generuje `.zip` se:
- App version, device info
- Connection stats (anonymized)
- Recent log lines (scrubbed)
- DB schema version
- Crash log (z Sentry pokud dostupný)

NEobsahuje:
- Identitu, peer hashes
- Obsah zpráv
- Attachments
- Klíče

Uživatel sám rozhoduje, zda export pošle do supportu.

---

## 8. Alerting

### 8.1 Production alerty (Play Store metrics)

| Alert | Trigger | Action |
|-------|---------|--------|
| Crash-free rate < 99,5 % | Across past 7 dní | Investigate, possibly hold rollout |
| ANR rate > 0,1 % | Past 7 dní | Performance review |
| Average rating drop > 0,3 | Past 14 dní | Review feedback, plan response |
| Spike in 1-star reviews | > 5 / day | Customer success engagement |

### 8.2 Sentry alerty

| Alert | Trigger | Action |
|-------|---------|--------|
| New error type | First occurrence | Notify on-call |
| Error spike | > 10× baseline 1h | Page on-call |
| Regression | New error in latest version | Block rollout |

---

## 9. Dashboards

### 9.1 Production dashboard (vnitřní, Grafana)

**Panels:**
- App crashes (Sentry → Grafana datasource)
- Bootstrap nodes health
- DAU/MAU trend (Play Console API, only if user opt-in tracking)
- Top 10 crash types
- Connection success rate by transport

### 9.2 Release dashboard

Vytvořen pro každý release, automaticky komparují metriky s předchozí verzí:
- Crash rate
- ANR rate
- Cold start time P50/P95
- New error types

---

## 10. Postmortem proces

Pro P0 incidents:

### 10.1 Šablona postmortemu

```markdown
# Postmortem: [Incident name]

**Date:** 2026-XX-XX
**Severity:** P0/P1
**Duration:** XX hours
**Authors:** [Names]

## Summary
Brief description of what happened.

## Timeline
- HH:MM — Detection
- HH:MM — Initial response
- HH:MM — Mitigation
- HH:MM — Resolution

## Root cause
Technical explanation.

## Impact
- # users affected
- # crashes
- # connections failed

## What went well
- ...

## What went wrong
- ...

## Action items
- [ ] Fix root cause
- [ ] Add monitoring for...
- [ ] Document...
- [ ] Update runbook...
```

### 10.2 Blameless culture

- Postmortemy fokusují na systémové chyby, ne na osoby
- Sdíleno interně, agregované findings publikovány v transparency report
