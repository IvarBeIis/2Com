# 16 – CI/CD a Release Management

**Projekt:** SecureWhisper
**Verze:** 1.0

---

## 1. Build pipeline

### 1.1 Stack

- **Build system:** Gradle 8.x s Version Catalogs (`libs.versions.toml`)
- **Language plugins:** Kotlin 2.x, KSP, Hilt
- **CI platform:** GitHub Actions (primary), self-hosted runners pro reproducible builds
- **Release automation:** Fastlane

### 1.2 Gradle struktura

```
SecureWhisper/
├── gradle/
│   └── libs.versions.toml          # Version catalog
├── build.gradle.kts                # Root build
├── settings.gradle.kts
├── app/
│   └── build.gradle.kts
├── core/
│   ├── crypto/
│   ├── transport/
│   ├── database/
│   ├── notifications/
│   └── common/
├── feature/
│   ├── onboarding/
│   ├── contacts/
│   ├── chat/
│   ├── calls/
│   └── settings/
└── buildSrc/                        # Build conventions
    └── src/main/kotlin/
        └── com.securewhisper.android.application.gradle.kts
```

### 1.3 Convention plugins

```kotlin
// buildSrc/src/main/kotlin/com.securewhisper.android.application.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    compileSdk = 35
    defaultConfig {
        minSdk = 26
        targetSdk = 35
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
```

---

## 2. CI pipeline

### 2.1 Trigger matrix

| Event | Workflow | Run on |
|-------|----------|--------|
| `push` na PR branch | `pr-checks.yml` | Linux |
| `push` na `main` | `main.yml` | Linux + macOS |
| Tag `v*` | `release.yml` | Linux (reproducible runner) |
| Cron `0 3 * * *` | `nightly.yml` | Linux |
| Manual dispatch | `manual-test-build.yml` | Linux |

### 2.2 Hlavní workflow (PR checks)

```yaml
# .github/workflows/pr-checks.yml
name: PR Checks

on:
  pull_request:
    branches: [main, develop]

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17
      
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          cache-read-only: ${{ github.ref != 'refs/heads/main' }}
      
      - name: Lint
        run: ./gradlew lintPlayDebug detekt
      
      - name: Spotless
        run: ./gradlew spotlessCheck
      
      - name: Unit tests
        run: ./gradlew testPlayDebugUnitTest testFdroidDebugUnitTest
      
      - name: Coverage
        run: ./gradlew jacocoTestReport
      
      - name: Upload coverage
        uses: codecov/codecov-action@v4
      
      - name: Dependency vulnerability scan
        run: ./gradlew dependencyCheckAnalyze
      
      - name: Secret scan
        uses: gitleaks/gitleaks-action@v2
      
      - name: Static analysis (Semgrep)
        uses: returntocorp/semgrep-action@v1
        with:
          config: p/owasp-top-ten p/android
      
      - name: Build debug APKs
        run: ./gradlew assemblePlayDebug assembleFdroidDebug
      
      - name: APK size check
        run: |
          PLAY_APK=app/build/outputs/apk/play/debug/*.apk
          SIZE=$(stat -c%s $PLAY_APK)
          if [ $SIZE -gt 52428800 ]; then exit 1; fi
```

### 2.3 Instrumentation testy

```yaml
  instrumentation:
    runs-on: ubuntu-latest
    needs: validate
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: 17 }
      
      - name: Build test APKs
        run: ./gradlew packagePlayDebugAndroidTest
      
      - name: Auth GCloud
        uses: google-github-actions/auth@v2
        with:
          credentials_json: ${{ secrets.FIREBASE_SA }}
      
      - name: Run on Firebase Test Lab
        run: |
          gcloud firebase test android run \
            --type instrumentation \
            --app app/build/outputs/apk/play/debug/app-play-debug.apk \
            --test app/build/outputs/apk/androidTest/play/debug/app-play-debug-androidTest.apk \
            --device model=oriole,version=33 \
            --device model=panther,version=34 \
            --device model=lynx,version=35 \
            --timeout 30m \
            --use-orchestrator
```

---

## 3. Versioning

### 3.1 Verzování schéma

**Semantic Versioning 2.0.0:** `MAJOR.MINOR.PATCH`

- **MAJOR:** breaking changes (wire protokol, DB schema bez migrace)
- **MINOR:** nové features
- **PATCH:** bug fixy, security fixy

### 3.2 versionCode strategie

```kotlin
android {
    defaultConfig {
        // Format: M MM PP B
        // M = major (1)
        // MM = minor (00-99)
        // PP = patch (00-99)
        // B = build number (0-9)
        versionCode = 1_00_03_0  // 1.0.3 build 0
        versionName = "1.0.3"
    }
}
```

### 3.3 Branch model

- `main` — stabilní production
- `develop` — integration branch
- `feature/*` — nové features
- `release/*` — stabilizace před release
- `hotfix/*` — urgent production fixes

### 3.4 Release tag konvence

- Stable: `v1.0.0`
- Pre-release: `v1.0.0-rc1`, `v1.0.0-beta3`
- Internal: `v1.0.0-internal.20260108`

---

## 4. Signing

### 4.1 Keystore management

**Production keystore:**
- Uloženo v HSM (Hardware Security Module) provozovatele
- Backup na šifrovaném USB v bankovním sejfu
- Klíč chráněn passphrase + PGP smartcard

**CI signing:**
- Keystore cesta v secrets, nikdy v repo
- Secrets v GitHub Actions: `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`

```kotlin
// app/build.gradle.kts
android {
    signingConfigs {
        create("release") {
            val keystoreFile = file(System.getenv("RELEASE_KEYSTORE_PATH") ?: "../keystore/release.keystore")
            if (keystoreFile.exists()) {
                storeFile = keystoreFile
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }
}
```

### 4.2 App signing key vs. upload key (Play Console)

- Používáme Play App Signing — Google drží app signing key
- My držíme upload key
- F-Droid varianta: my držíme signing key (F-Droid může také re-podepisovat své vlastní buildem)

---

## 5. Reproducible builds

### 5.1 Důvody

- Verifikace, že distribuovaný APK odpovídá zdrojovému kódu
- F-Droid inclusion requirement
- Důvěra komunity

### 5.2 Implementace

```dockerfile
# reproducible-build/Dockerfile
FROM eclipse-temurin:17-jdk-jammy

ENV ANDROID_HOME=/opt/android-sdk
RUN apt-get update && apt-get install -y unzip wget git
RUN wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
RUN unzip -q commandlinetools-linux-11076708_latest.zip -d $ANDROID_HOME/cmdline-tools
RUN mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest

# Pin to specific SDK versions
RUN yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager \
    "platforms;android-35" \
    "build-tools;34.0.0" \
    "ndk;26.1.10909125"

WORKDIR /workspace
ENTRYPOINT ["./gradlew"]
```

### 5.3 Build script

```bash
#!/bin/bash
# scripts/reproducible-build.sh
set -euo pipefail

VERSION=$1
docker build -t securewhisper-builder:$VERSION reproducible-build/

docker run --rm \
    -v $(pwd):/workspace \
    -v gradle-cache:/root/.gradle \
    -e SOURCE_DATE_EPOCH=$(git log -1 --format=%ct) \
    securewhisper-builder:$VERSION \
    assembleFdroidRelease

sha256sum app/build/outputs/apk/fdroid/release/app-fdroid-release.apk > apk.sha256
```

### 5.4 Verifikace

- Komunitní verifier spustí stejný script s pinned commit
- Porovná SHA256 distribuovaného APK s lokálně vybudovaným
- Reporty publikované na `https://github.com/securewhisper/reproducible-builds`

---

## 6. Distribuce

### 6.1 Release channels

| Channel | Audience | Update frequency |
|---------|----------|------------------|
| `internal` | Tým | Per build |
| `alpha` | Beta testers (closed) | Týdně |
| `beta` | Beta testers (open) | Měsíčně |
| `production` | Všichni | Měsíčně (regular) / urgent (hotfix) |

### 6.2 Google Play Console (Fastlane)

```ruby
# fastlane/Fastfile
default_platform(:android)

platform :android do
  desc "Promote to internal track"
  lane :internal do
    gradle(task: "bundlePlayRelease")
    upload_to_play_store(
      track: 'internal',
      aab: 'app/build/outputs/bundle/playRelease/app-play-release.aab',
      skip_upload_metadata: false,
      skip_upload_changelogs: false,
      release_status: 'completed'
    )
  end

  desc "Promote internal → beta"
  lane :promote_to_beta do
    upload_to_play_store(
      track: 'internal',
      track_promote_to: 'beta',
      skip_upload_apk: true,
      skip_upload_aab: true,
      skip_upload_metadata: true
    )
  end

  desc "Promote beta → production"
  lane :promote_to_production do
    upload_to_play_store(
      track: 'beta',
      track_promote_to: 'production',
      rollout: '0.1',  # Staged rollout 10%
      skip_upload_apk: true,
      skip_upload_aab: true
    )
  end

  desc "Build and upload F-Droid APK to GitHub releases"
  lane :fdroid_release do
    sh "../scripts/reproducible-build.sh #{ENV['VERSION']}"
    set_github_release(
      repository_name: "securewhisper/android",
      api_token: ENV["GITHUB_TOKEN"],
      name: "v#{ENV['VERSION']}",
      tag_name: "v#{ENV['VERSION']}",
      description: File.read("../CHANGELOG.md").split("##")[1],
      upload_assets: ["../app/build/outputs/apk/fdroid/release/app-fdroid-release.apk", "../apk.sha256"]
    )
  end
end
```

### 6.3 Staged rollout strategie

| Phase | % users | Duration | Trigger advance |
|-------|---------|----------|-----------------|
| Phase 1 | 1 % | 24h | Crash-free > 99,5% |
| Phase 2 | 10 % | 48h | Crash-free > 99,3% |
| Phase 3 | 50 % | 72h | Crash-free > 99,0% |
| Phase 4 | 100 % | – | – |

Při zhoršení metrik halt + investigation.

### 6.4 F-Droid distribuce

- Tag push → metadata aktualizace v F-Droid repo
- F-Droid build server vytvoří APK z source
- Verifikace reproducibility (community + maintainer)
- Publikace v F-Droid stable repo (typicky 1-3 týdny po push)

### 6.5 Direct APK distribution

- GitHub Releases s podepsanými APK
- SHA256 checksums + GPG podpis
- Optional: signed via cosign / sigstore

---

## 7. Release process

### 7.1 Release checklist

**T-7 dní:**
- [ ] Feature freeze
- [ ] Translation strings finalized
- [ ] Smoke test na reference devices

**T-3 dny:**
- [ ] Code freeze (jen P0/P1 fixes)
- [ ] Release notes draft
- [ ] Privacy Policy review (pokud změna)

**T-1 den:**
- [ ] Final build vytvořen
- [ ] Reproducible build verified
- [ ] Beta tester sign-off
- [ ] Release notes finalized

**T-0 (release day):**
- [ ] Tag created
- [ ] CI verifies tag
- [ ] Upload to internal track (Play)
- [ ] Reproducible APK pushed do F-Droid metadata
- [ ] GitHub Release vytvořen
- [ ] Discord/Mastodon announcement

**T+1 den:**
- [ ] Promote to beta (10 %)
- [ ] Crash rate monitoring

**T+3 dny:**
- [ ] Promote to production (10 %)

**T+7 dní:**
- [ ] Promote to 100 % (pokud metrics OK)
- [ ] Retrospective

### 7.2 Hotfix process

1. Branch `hotfix/v1.0.x` z `main`
2. Fix + targeted unit/integration tests
3. PR review (zrychlený, 2 reviewers)
4. Merge → tag → release
5. Cherry-pick fix do `develop`

**SLA:** Critical security hotfix do 24h od potvrzení.

---

## 8. Release notes formát

### 8.1 Šablona

```markdown
# SecureWhisper v1.0.3

**Released:** 2026-05-15

## ✨ Nové funkce
- Reakce emoji na zprávy
- Disappearing messages s 1 minutou intervalem

## 🐛 Opravy
- Opraven crash při příchozím hovoru během uzamčené obrazovky (#234)
- Opravena synchronizace stavu „přečteno" při slabém signálu (#287)

## 🔒 Bezpečnost
- Aktualizovaná libsignal-android na verzi X.Y.Z
- Posílena validace incoming Protobuf messages

## ⚡ Performance
- Cold start o 15 % rychlejší
- Snížena spotřeba baterie v idle režimu o 8 %

## 🌐 Lokalizace
- Přidaná podpora francouzštiny

## 🔧 Pro technické uživatele
- Wire protocol verze: 1 (beze změny)
- DB schema verze: 3 (migrace 2 → 3 automaticky)
- minSdk: 26, targetSdk: 35

## ⚠️ Známé problémy
- ANR při scrollování velmi dlouhých konverzací (>5000 zpráv) — fix v 1.0.4

---

Plný changelog: https://github.com/securewhisper/android/compare/v1.0.2...v1.0.3
```

### 8.2 Play Store changelog (kratší forma)

Maximálně 500 znaků:
```
Co je nového v 1.0.3:
• Reakce emoji a disappearing messages
• Opraveny problémy s hovory na uzamčené obrazovce
• 15% rychlejší spuštění aplikace
• Přidaná francouzština
```

---

## 9. Versioning starých verzí

### 9.1 Support matrix

| Verze | Status | Support do |
|-------|--------|------------|
| 1.0.x (aktuální) | Active | – |
| 0.9.x | Bug fixes only | +6 měsíců |
| 0.8.x a starší | Deprecated | EOL |

### 9.2 Forced upgrade

Aplikace 12+ měsíců stará:
- Soft warning v UI po dobu 60 dní
- Hard prompt s odkazem na update
- Po dalších 30 dnech: aplikace nebude fungovat se znovu-vyjednávajícími peery (kompatibilita protokolu)

---

## 10. Rollback strategie

Pokud release způsobuje vážné problémy:

1. **Halt rollout** v Play Console
2. **Promote previous stable** zpět na 100 %
3. **Investigate** root cause
4. **Hotfix** s explicit version bump

**Pozor:** Play Store neumí "downgrade" — uživatelé s novou verzí ji budou mít, dokud nepřijde fix. Mitigace: gradual rollout zachycuje problém před širokou expozicí.
