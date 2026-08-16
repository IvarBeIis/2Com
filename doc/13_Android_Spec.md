# 13 – Android-specific Documentation

**Projekt:** SecureWhisper
**Verze:** 1.0

---

## 1. Cílové verze a kompatibilita

### 1.1 SDK strategie

| Parametr | Hodnota | Důvod |
|----------|---------|-------|
| `minSdk` | 26 (Android 8.0) | Hardware-backed Keystore, BiometricPrompt foundations, ConnectivityManager.NetworkCallback |
| `targetSdk` | 36 (Android 16) | Compliance s Play Store požadavky a Android 16 deadline |
| `compileSdk` | 36 | Latest stable SDK |

### 1.2 Pokrytí zařízení

`minSdk = 26` pokrývá ~95 % aktivních Android zařízení v cílových trzích (EU, US).

### 1.3 ABI podpora

```gradle
android {
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }
}
```

---

## 2. AndroidManifest.xml

### 2.1 Permissions

```xml
<!-- Síť (vždy) -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

<!-- Foreground service pro spojení -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_PHONE_CALL" />
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- Notifikace -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.VIBRATE" />

<!-- Biometric / Keystore -->
<uses-permission android:name="android.permission.USE_BIOMETRIC" />

<!-- Kamera (QR scan, video calls) -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- Mikrofon (audio calls) -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />

<!-- Bluetooth (proximity discovery) -->
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" 
    android:usesPermissionFlags="neverForLocation" 
    tools:targetApi="s" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" 
    android:usesPermissionFlags="neverForLocation" 
    tools:targetApi="s" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" 
    tools:targetApi="s" />

<!-- WiFi Direct (proximity discovery) -->
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES" 
    android:usesPermissionFlags="neverForLocation"
    tools:targetApi="33" />

<!-- NFC (kontakt sharing) -->
<uses-permission android:name="android.permission.NFC" />

<!-- Storage (attachment export) -->
<!-- Scoped storage approach, no broad permission needed -->

<!-- Hardware features (declared but not required) -->
<uses-feature android:name="android.hardware.camera" android:required="false" />
<uses-feature android:name="android.hardware.microphone" android:required="false" />
<uses-feature android:name="android.hardware.bluetooth_le" android:required="false" />
<uses-feature android:name="android.hardware.nfc" android:required="false" />
```

### 2.2 Components

```xml
<application
    android:name=".SecureWhisperApp"
    android:allowBackup="false"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="false"
    android:icon="@mipmap/ic_launcher"
    android:label="@string/app_name"
    android:supportsRtl="true"
    android:theme="@style/Theme.SecureWhisper"
    android:enableOnBackInvokedCallback="true">

    <!-- Hlavní activity -->
    <activity
        android:name=".MainActivity"
        android:exported="true"
        android:launchMode="singleTask"
        android:theme="@style/Theme.SecureWhisper.Splash">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
        
        <!-- Deep link pro přidání kontaktu -->
        <intent-filter android:autoVerify="true">
            <action android:name="android.intent.action.VIEW" />
            <category android:name="android.intent.category.DEFAULT" />
            <category android:name="android.intent.category.BROWSABLE" />
            <data android:scheme="securewhisper" />
        </intent-filter>
        
        <!-- NFC NDEF přijem -->
        <intent-filter>
            <action android:name="android.nfc.action.NDEF_DISCOVERED" />
            <category android:name="android.intent.category.DEFAULT" />
            <data android:mimeType="application/vnd.securewhisper.contact" />
        </intent-filter>
    </activity>

    <!-- Foreground service pro DHT a aktivní spojení -->
    <service
        android:name=".service.ConnectionService"
        android:exported="false"
        android:foregroundServiceType="dataSync" />

    <!-- Foreground service pro hovory -->
    <service
        android:name=".service.CallService"
        android:exported="false"
        android:foregroundServiceType="phoneCall" />

    <!-- FCM service (Play varianta) -->
    <service
        android:name=".push.PushMessagingService"
        android:exported="false">
        <intent-filter>
            <action android:name="com.google.firebase.MESSAGING_EVENT" />
        </intent-filter>
    </service>

    <!-- Boot receiver pro auto-start (volitelné) -->
    <receiver
        android:name=".receiver.BootReceiver"
        android:exported="true"
        android:enabled="false">
        <intent-filter>
            <action android:name="android.intent.action.BOOT_COMPLETED" />
        </intent-filter>
    </receiver>

    <!-- File provider pro sdílení attachment -->
    <provider
        android:name="androidx.core.content.FileProvider"
        android:authorities="${applicationId}.fileprovider"
        android:exported="false"
        android:grantUriPermissions="true">
        <meta-data
            android:name="android.support.FILE_PROVIDER_PATHS"
            android:resource="@xml/file_provider_paths" />
    </provider>
</application>
```

### 2.3 Permission rationale (zdůvodnění)

Pro Play Store review a Data Safety form:

| Permission | Účel | Když se vyžaduje |
|------------|------|------------------|
| INTERNET | Síťová komunikace | Vždy |
| CAMERA | QR scan, video calls | Při skenování / video hovoru |
| RECORD_AUDIO | Audio/video calls | Při hovoru |
| BLUETOOTH_* | Proximity discovery | Když uživatel zvolí |
| NEARBY_WIFI_DEVICES | WiFi Direct discovery | Když uživatel zvolí |
| NFC | NFC kontakt sharing | Když uživatel klepne |
| POST_NOTIFICATIONS | Příchozí zprávy/hovory | Při onboardingu |

---

## 3. Strategie podpory zařízení

### 3.1 Kategorie zařízení

| Kategorie | Příklady | Strategy |
|-----------|----------|----------|
| Premium | Pixel 6+, Galaxy S21+ | Full features, hardware crypto, StrongBox |
| Mid-range | Galaxy A54, Pixel 7a | Full features, software crypto fallback |
| Budget | Galaxy A14, low-end | Reduced background DHT, optimized media |
| Tablets | Tab S9, Pixel Tablet | Adaptive layouts, multi-pane |
| Foldables | Galaxy Z Fold, Pixel Fold | Adaptive layouts, different states |
| AOSP-based | GrapheneOS, /e/OS, LineageOS | UnifiedPush variant, no Google Services |

### 3.2 Screen size adaptace

```kotlin
@Composable
fun AdaptiveLayout() {
    val windowSizeClass = calculateWindowSizeClass()
    
    when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> CompactLayout()      // phones portrait
        WindowWidthSizeClass.Medium -> MediumLayout()        // tablets portrait, foldables
        WindowWidthSizeClass.Expanded -> ExpandedLayout()    // tablets landscape, desktops
    }
}
```

**Conversation list / detail:**
- Compact: stack (single pane navigation)
- Medium/Expanded: side-by-side (master-detail)

### 3.3 Density buckets

Podpora všech density bucketů (mdpi až xxxhdpi):
- Vector drawables (XML) jako default
- PNG fallback jen pro complex assets

---

## 4. Background work strategie

### 4.1 WorkManager taska

| Worker | Trigger | Constraints |
|--------|---------|-------------|
| `ExpiringMessagesWorker` | Periodic 15 min | – |
| `KeyRotationWorker` | Periodic daily | Charging |
| `BackupWorker` | User-triggered | Wifi, charging (optional) |
| `DhtMaintenanceWorker` | Periodic 1h | NetworkAvailable |
| `WakeupWorker` | One-time, on FCM | – |

### 4.2 Foreground services

**ConnectionService** běží:
- Když má uživatel aktivní konverzaci (recently active)
- Když je app v popředí
- Když user explicitně zapnul "Always-on" mode

Notifikace foreground service: persistent, nízká priorita, „SecureWhisper – Připojeno" + tlačítko „Pause".

**CallService** běží:
- Pouze během aktivního hovoru
- High priority notifikace s call controls

### 4.3 Battery optimization

```kotlin
// Při onboardingu navrhnout uživateli vyloučit z battery optimization
fun checkBatteryOptimization(context: Context): Boolean {
    val pm = context.getSystemService(POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

// Request s explanation
fun requestBatteryOptimizationExemption(activity: Activity) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${activity.packageName}")
    }
    activity.startActivity(intent)
}
```

### 4.4 Doze mode handling

- High-priority FCM používán pro okamžité doručení
- Batched syncs během doze maintenance windows
- Documented limitations v UI ("Doručení může být zpožděno o až 15 minut během spánku zařízení")

---

## 5. Storage strategie

### 5.1 Adresářová struktura

```
/data/data/cz.securewhisper/
├── databases/
│   └── securewhisper.db (SQLCipher encrypted)
├── files/
│   ├── attachments/
│   │   └── {uuid}.enc (AES-256-GCM)
│   ├── thumbnails/
│   └── temp/
├── shared_prefs/
│   ├── encrypted_prefs.xml (EncryptedSharedPreferences)
│   └── settings.xml
└── cache/
    └── (volatile data)
```

### 5.2 Scoped storage

- Žádný `READ_EXTERNAL_STORAGE` ani `WRITE_EXTERNAL_STORAGE`
- Pro export attachments: SAF (Storage Access Framework)
- Pro import: SAF picker

### 5.3 Backup vyloučení

```xml
<!-- res/xml/data_extraction_rules.xml -->
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="root" />
        <exclude domain="database" />
        <exclude domain="file" />
        <exclude domain="sharedpref" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="root" />
        <exclude domain="database" />
        <exclude domain="file" />
        <exclude domain="sharedpref" />
    </device-transfer>
</data-extraction-rules>
```

`android:allowBackup="false"` v manifestu — žádné cloudové zálohy ani device-to-device transfer.

---

## 6. Lifecycle a state management

### 6.1 Process lifecycle

- Application class (`SecureWhisperApp`) inicializuje DI, crypto subsystem
- ProcessLifecycleOwner pro detekci app on/off foreground
- Při background → after timeout pause DHT participation (battery)

### 6.2 Activity lifecycle s Compose

- Single Activity architecture (`MainActivity`)
- Navigation via `androidx.navigation:navigation-compose`
- Deep link handling v MainActivity, dispatched do correct destination

### 6.3 Configuration changes

- ViewModels survive (default Compose pattern)
- Active calls survive rotation via foreground service binding

---

## 7. Build configuration

### 7.1 Build types

```gradle
buildTypes {
    debug {
        applicationIdSuffix = ".debug"
        versionNameSuffix = "-debug"
        isDebuggable = true
        isMinifyEnabled = false
    }
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        signingConfig = signingConfigs.getByName("release")
    }
}
```

### 7.2 Product flavors

```gradle
flavorDimensions += "distribution"
productFlavors {
    create("play") {
        dimension = "distribution"
        // FCM enabled
        buildConfigField("Boolean", "USE_FCM", "true")
        buildConfigField("Boolean", "USE_UNIFIED_PUSH", "false")
    }
    create("fdroid") {
        dimension = "distribution"
        // UnifiedPush
        buildConfigField("Boolean", "USE_FCM", "false")
        buildConfigField("Boolean", "USE_UNIFIED_PUSH", "true")
    }
}

// Hardcoded DHT fallback peery — sdílené oběma flavory
// Používají se při nedostupnosti bootstrap HTTP API
buildConfigField("String", "DHT_FALLBACK_PEERS",
    "\"45.76.100.42:49737,95.179.200.11:49737,139.162.55.73:49737,178.62.194.88:49737,[2a01:4f8:c0c:9abc::1]:49737\""
)
```

### 7.3 ProGuard rules

```proguard
# libsignal native
-keep class org.signal.libsignal.** { *; }
-keep class org.whispersystems.** { *; }

# WebRTC
-keep class org.webrtc.** { *; }

# Hyperswarm JNI
-keep class com.holepunch.** { *; }

# DHT fallback peer constants (nesmí být odstraněny ProGuardem)
-keepclassmembers class cz.securewhisper.core.transport.DhtBootstrapPeers { *; }

# Protobuf
-keep class * extends com.google.protobuf.GeneratedMessageLite { *; }

# Reflection-used classes
-keep @kotlinx.serialization.Serializable class * { *; }
```

---

## 8. Play Store specifika

### 8.1 Data Safety form

| Datum | Sbíráno? | Sdíleno? | Důvod | Volitelné? |
|-------|----------|----------|-------|------------|
| Identifikátory zařízení | Ne | – | – | – |
| Diagnostika (crashes) | Pouze on opt-in | Ano (vlastní endpoint) | Stabilita | Ano |
| Zprávy | Ne | Ne | – | – |
| Kontakty | Ne | Ne | – | – |

### 8.2 App Bundle

Distribuce via Android App Bundle (AAB):
- Per-device APK delivery
- Reduces download size

### 8.3 Play Integrity API

**Záměrně NEPOUŽÍVÁME** — narušilo by to F-Droid distribuci a možnost rootovaných zařízení.

---

## 9. F-Droid specifika

### 9.1 Inclusion criteria
- 100 % FOSS dependencies (no Google Play Services in fdroid flavor)
- Reproducible builds verified
- No anti-features

### 9.2 Anti-features deklarace
- `NonFreeNet` — DHT bootstrap nodes a optional FCM (in play flavor only)

### 9.3 Build metadata

```yaml
# metadata/cz.securewhisper.yml
Categories:
  - Internet
  - Security
License: AGPL-3.0-only
SourceCode: https://github.com/securewhisper/android
IssueTracker: https://github.com/securewhisper/android/issues
Translation: https://hosted.weblate.org/projects/securewhisper

AutoName: SecureWhisper
RepoType: git
Repo: https://github.com/securewhisper/android.git

Builds:
  - versionName: 1.0.0
    versionCode: 100
    commit: v1.0.0
    subdir: app
    gradle:
      - fdroid

AntiFeatures:
  - NonFreeNet
```
