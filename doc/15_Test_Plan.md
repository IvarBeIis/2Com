# 15 – Test Plan & Test Cases

**Projekt:** SecureWhisper
**Verze:** 1.0

---

## 1. Cíle testování

- Verifikovat, že aplikace splňuje funkční a nefunkční požadavky ze SRS
- Zajistit absenci kritických bezpečnostních a stabilitních defektů před release
- Validovat UX flow s reálnými uživateli
- Pokrývat regresi při každém release

## 2. Strategie testování

### 2.1 Test pyramida

```
                    ┌──────────────────┐
                    │   Manual / E2E   │  ~5 %
                    │   Exploratory    │
                ┌───┴──────────────────┴───┐
                │     UI / Espresso        │  ~15 %
                │     Compose Tests        │
            ┌───┴──────────────────────────┴───┐
            │       Integration               │  ~20 %
            │       (Robolectric, JUnit)      │
        ┌───┴──────────────────────────────────┴───┐
        │            Unit Tests                    │  ~60 %
        │   (JUnit, MockK, Kotest, Turbine)        │
        └──────────────────────────────────────────┘
```

### 2.2 Coverage cíle

| Modul | Line coverage | Branch coverage |
|-------|---------------|------------------|
| `:core:crypto` | ≥ 90 % | ≥ 85 % |
| `:core:transport` | ≥ 80 % | ≥ 75 % |
| `:core:database` | ≥ 85 % | ≥ 80 % |
| `:feature:*` | ≥ 70 % | ≥ 65 % |
| `:app` | ≥ 60 % | ≥ 55 % |
| Project total | ≥ 75 % | ≥ 70 % |

---

## 3. Typy testů

### 3.1 Unit tests

**Frameworks:** JUnit 5, MockK, Kotest assertions, Turbine (pro Flow testing)

**Cíl:** Izolované testování business logiky bez Android dependencies.

**Příklady testovaných tříd:**
- `IdentityManager` — generování klíčů, hash derivace
- `SafetyNumberCalculator` — deterministická derivace
- `MessageEncoder/Decoder` — Protobuf serializace
- `MessageRepository` — orchestrace
- `ConnectionManager` — transport selection logic
- `DisappearingMessagesScheduler` — TTL handling

**Příklad:**

```kotlin
class SafetyNumberCalculatorTest {
    @Test
    fun `safety number is deterministic regardless of order`() {
        val keyA = TestData.aliceIdentity
        val keyB = TestData.bobIdentity
        
        val result1 = SafetyNumberCalculator.compute(keyA, keyB)
        val result2 = SafetyNumberCalculator.compute(keyB, keyA)
        
        result1 shouldBe result2
    }
    
    @Test
    fun `safety number is 60 hex chars in 12 groups of 5`() {
        val result = SafetyNumberCalculator.compute(TestData.alice, TestData.bob)
        
        result.length shouldBe 60
        result.chunked(5).size shouldBe 12
        result.matches(Regex("[0-9a-f]+")).shouldBeTrue()
    }
    
    @Test
    fun `safety number changes when key changes`() {
        val original = SafetyNumberCalculator.compute(TestData.alice, TestData.bob)
        val modified = SafetyNumberCalculator.compute(TestData.alice, TestData.bobRotated)
        
        original shouldNotBe modified
    }
}
```

### 3.2 Integration tests

**Frameworks:** JUnit 5 + Robolectric + in-memory DB

**Cíl:** Testování interakce mezi komponentami (DB + Repository, Crypto + Transport).

**Příklady scénářů:**
- Kompletní message send/receive flow s mocked transport
- DB migrace 1 → 2 → 3 s ověřením integrity dat
- Signal session establishment + encrypt/decrypt round-trip
- Foreground service lifecycle s WorkManager

```kotlin
@RunWith(RobolectricTestRunner::class)
class MessageRepositoryIntegrationTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: MessageRepository
    
    @BeforeEach
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        repository = MessageRepositoryImpl(db, fakeTransport, fakeSessionManager)
    }
    
    @Test
    fun `send message persists with status SENDING then SENT`() = runTest {
        val conversationId = createTestConversation()
        
        val flowEvents = mutableListOf<List<Message>>()
        val job = launch { repository.observeConversation(conversationId).collect { flowEvents.add(it) } }
        
        repository.send(conversationId, MessageContent.Text("Hello"))
        advanceUntilIdle()
        
        flowEvents.last().last().status shouldBe MessageStatus.SENT
        job.cancel()
    }
}
```

### 3.3 Instrumentation tests (UI)

**Frameworks:** Espresso, Compose UI Test, AndroidX Test

**Cíl:** Testování UI na reálném zařízení/emulátoru.

**Klíčové flow:**
- Onboarding a generování identity
- QR scan flow
- Add contact via deep link
- Send message a state updates
- Initiate call
- App lock / unlock

```kotlin
@HiltAndroidTest
class OnboardingFlowTest {
    @get:Rule(order = 0) val hiltRule = HiltAndroidRule(this)
    @get:Rule(order = 1) val composeRule = createAndroidComposeRule<MainActivity>()
    
    @Test
    fun firstLaunch_completesOnboarding_andShowsIdentity() {
        composeRule.onNodeWithText("Vítejte").assertIsDisplayed()
        repeat(3) { composeRule.onNodeWithText("Pokračovat").performClick() }
        composeRule.onNodeWithText("Vytvořit identitu").performClick()
        
        composeRule.onNodeWithText("Moje identita").assertIsDisplayed()
        composeRule.onNodeWithTag("hex_code").assertTextMatches(Regex("[0-9a-f ]{79}"))
    }
}
```

### 3.4 Bezpečnostní testy

| Test | Tool | Frekvence |
|------|------|-----------|
| Static analysis | Detekt, Android Lint | Každý PR |
| Dependency vuln scan | OWASP Dependency-Check, Renovate | Týdně |
| Secret scanning | Gitleaks | Každý commit |
| SAST | Semgrep, MobSF | Týdně |
| DAST | MobSF dynamic | Před release |
| Penetration test | Externí | 2× ročně |
| Crypto audit | Externí (Cure53) | Ročně |
| Fuzzing (Protobuf parsing) | Jazzer | Průběžně v CI |

### 3.5 Performance tests

| Test | Tool | Acceptance |
|------|------|-----------|
| Cold start | Macrobenchmark | < 2s na Pixel 6 |
| Frame drops | Macrobenchmark Jank | < 1% janky frames |
| Memory leaks | LeakCanary | 0 leaks v happy path |
| DB query benchmark | Room benchmarks | < 50ms pro list 1000 messages |
| Battery drain | Battery Historian | < 8 % / 24h aktivní |
| APK size | Gradle reports | < 50 MB total |

### 3.6 Network resilience tests

Simulace různých síťových podmínek:
- High latency (500ms+)
- Packet loss (5%, 10%)
- Bandwidth limits (3G, 2G)
- NAT type variations (full cone, restricted, symmetric)
- Captive portals
- Network handover (WiFi → cellular)

**Tool:** Charles Proxy, Network Link Conditioner, Android Studio Network Profiler

### 3.7 Compatibility tests

| Test matrix | Devices |
|-------------|---------|
| Manufacturer | Samsung, Pixel, Xiaomi, OnePlus, Nothing |
| Android version | 8.0, 9, 10, 11, 12, 13, 14, 15 |
| Screen size | 4.7", 5.5", 6.7", 7.6" (foldable), 10" (tablet) |
| ROM | Stock, GrapheneOS, LineageOS, /e/OS |
| Cloud testing | Firebase Test Lab (matrix runs) |

---

## 4. Test cases (vybrané)

### 4.1 TC-001: Generování identity

| Pole | Hodnota |
|------|---------|
| ID | TC-001 |
| Priorita | Critical |
| Modul | Onboarding |
| Předpoklad | Aplikace prvně spuštěna, neexistuje identita |

**Kroky:**
1. Otevřít aplikaci poprvé
2. Projít onboarding obrazovkami
3. Klepnout „Vytvořit identitu"
4. Počkat na dokončení

**Očekávaný výsledek:**
- Identita vygenerována do 10 sekund
- Hex kód zobrazen ve 64 znacích
- Klíče v Android Keystore (verifikováno přes ADB v test)
- Žádný požadavek na PII

**Acceptance criteria:** SRS FR-01, FR-02, FR-03, FR-04

---

### 4.2 TC-002: Přidání kontaktu přes QR

| Pole | Hodnota |
|------|---------|
| ID | TC-002 |
| Priorita | Critical |
| Předpoklad | Identita existuje, druhé zařízení má svůj QR připravený |

**Kroky:**
1. Hlavní obrazovka → FAB „+" → „Naskenovat QR kód"
2. Udělit oprávnění kamery (pokud poprvé)
3. Naskenovat QR kód druhého zařízení
4. Potvrdit přidání kontaktu
5. Zadat lokální jméno

**Očekávaný výsledek:**
- Kontakt přidán do seznamu
- Safety number generován
- Status: „Nový kontakt, neověřený"

---

### 4.3 TC-003: Odeslání první zprávy

| Pole | Hodnota |
|------|---------|
| ID | TC-003 |
| Priorita | Critical |
| Předpoklad | Kontakt přidaný, oba peeři online |

**Kroky:**
1. Otevřít kontakt
2. Napsat text „Test message"
3. Odeslat

**Očekávaný výsledek:**
- Stav „Odesílá se" → „Odesláno" → „Doručeno" (do 5s celkem)
- Zpráva persistována lokálně
- Peer obdrží zprávu
- DTLS/Noise handshake proběhne při prvním kontaktu

---

### 4.4 TC-010: Audio hovor – happy path

**Kroky:**
1. Otevřít kontakt
2. Klepnout ikonu hovoru
3. Druhé zařízení přijme hovor
4. Hovořit 30 sekund
5. Ukončit hovor

**Očekávaný výsledek:**
- Hovor naváže do 5s
- Audio kvalita ≥ 16 kbps Opus
- Latence < 200 ms one-way (měřeno)
- Po ukončení: záznam v call log s duration

---

### 4.5 TC-020: Safety number verification

**Kroky:**
1. Detail kontaktu → „Ověřit safety number"
2. Porovnat 60-znakový kód s druhým zařízením
3. Klepnout „Ověřit"

**Očekávaný výsledek:**
- Kontakt označen jako „Verified" (✓ badge)
- Při budoucí změně klíče peera se zobrazí warning

---

### 4.6 TC-030: Disappearing messages

**Kroky:**
1. Otevřít kontakt → nastavit disappearing 1 min
2. Odeslat zprávu
3. Ověřit synchronizaci nastavení s peerem
4. Počkat 1 min
5. Verifikovat smazání zprávy lokálně i u peera

**Očekávaný výsledek:**
- Zpráva zmizí na obou stranách
- Záznam v DB má `is_deleted = 1`
- Žádná residual data v message indexech

---

### 4.7 TC-040: App lock biometrií

**Kroky:**
1. Settings → App lock → Biometrie ON
2. Zavřít aplikaci, odemknout znovu
3. Verifikovat dialog BiometricPrompt
4. Authenticate

**Očekávaný výsledek:**
- DB nepřístupná do auth
- Po failed auth (3×) — fallback na PIN
- Při zamítnutí BiometricPrompt — aplikace se nezotevře

---

### 4.8 TC-050: Klíč peera se změnil

| Předpoklad | Kontakt verified, peer reinstaloval app |

**Kroky:**
1. Peer pošle zprávu z nové instalace
2. Self vidí warning v aplikaci

**Očekávaný výsledek:**
- Banner v konverzaci „⚠️ Klíč Evy se změnil"
- Verified badge odstraněn
- Re-verify flow nabízen

---

### 4.9 TC-060: NAT traversal selhání

| Předpoklad | Symetrický NAT na obou stranách |

**Kroky:**
1. Pokus o odeslání zprávy (bez aktivního spojení)
2. DHT lookup, hole punching selže
3. Aplikace fallbackne na komunitní relay

**Očekávaný výsledek:**
- Spojení naváženo přes relay do 30s
- UI indikuje „Spojeno přes relay"
- Šifrování plně funkční (peer-to-peer E2E, relay vidí jen ciphertext)

---

### 4.10 TC-070: Wipe all data

**Kroky:**
1. Settings → Privacy → Wipe all data
2. Potvrdit warning dialog
3. Re-confirm zadáním "DELETE"

**Očekávaný výsledek:**
- DB smazána
- Keystore aliasy smazány
- Attachments smazány
- App restart → onboarding

---

## 5. Regression test suite

Před každým release:
- 100 % critical test cases
- 100 % high priority test cases
- Smoke test na 5 reference devices
- Performance benchmark vs. previous release

### 5.1 Smoke test (15 minut)

1. Cold start
2. Navigate all primary screens
3. Send a message (test peer)
4. Receive a message
5. Make a call (test peer)
6. App lock cycle
7. Settings tour

---

## 6. Test data management

### 6.1 Fixtures

```kotlin
object TestData {
    val aliceIdentity = Identity(
        signingKeyPair = Ed25519KeyPair(/* fixed test keys */),
        agreementKeyPair = X25519KeyPair(/* fixed test keys */),
        identityHashHex = "0000...alice"
    )
    
    val bobIdentity = Identity(/* ... */)
    
    fun freshConversation(): Conversation = /* ... */
    fun mockedSignalSession(): SignalSession = /* ... */
}
```

### 6.2 Test peer infrastructure

Pro integration tests:
- Standalone CLI tool `securewhisper-test-peer` — minimální Hyperswarm peer pro testing
- Lze spustit lokálně, simuluje peer behavior
- Vytváří konkrétní conditions: slow, offline, key-changed

---

## 7. Test environment

### 7.1 Local

- Android Studio + Android Emulator (Pixel 6 API 34)
- Test peer running locally (`./scripts/run-test-peer.sh`)
- In-memory Room DB

### 7.2 CI (GitHub Actions)

```yaml
jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '17' }
      - name: Unit tests
        run: ./gradlew testPlayDebugUnitTest jacocoTestReport
      - name: Upload coverage
        uses: codecov/codecov-action@v4

  instrumentation-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run on Firebase Test Lab
        run: gcloud firebase test android run --type instrumentation \
             --app app/build/outputs/apk/playDebug/app-play-debug.apk \
             --test app/build/outputs/apk/androidTest/playDebug/app-play-debug-androidTest.apk \
             --device model=oriole,version=33,locale=en,orientation=portrait \
             --device model=panther,version=34,locale=cs,orientation=portrait
```

### 7.3 Pre-release manual testing

- 5 testers, mix of personas (Eva, Marek, Alice, Petr archetypes)
- 1 týden testovacího okna
- Bug bash ve poslední den

---

## 8. Acceptance gating

### 8.1 Definice „Ready for Release"

Všechny musí být splněny:
- [ ] All P0/P1 bugs closed
- [ ] No new P2 bugs in past 7 days
- [ ] Code coverage targets met
- [ ] Security scan: no HIGH/CRITICAL findings
- [ ] Performance benchmarks within target
- [ ] Smoke test passed on 5 reference devices
- [ ] Beta tester sign-off (pro major releases)
- [ ] Release notes reviewed
- [ ] Privacy Policy aktuální

### 8.2 Eskalační proces

Pokud release není ready a má fixed datum:
- Tech Lead + Product Owner rozhodují o partial release / delay
- P0/P1 critical bugs jsou hard blockers (vždy delay)
