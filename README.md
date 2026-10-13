# EliteGuardian — Android Adult Content Blocker & Focus Shield

[![Android CI](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-brightgreen.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-purple.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Privacy](https://img.shields.io/badge/Privacy-100%25%20On--Device-success.svg)](#privacy-guarantee)

**EliteGuardian** is a 100% free, private, open-source Android application designed to block adult content, enforce SafeSearch, and provide distraction-free focus sessions.

Unlike commercial blocker apps (such as BlockerHero) that enforce subscriptions and rely on brittle screen-scraping accessibility services, EliteGuardian operates as an independent, local DNS filtering engine powered by Android's official `VpnService`.

---

## 🛡️ Key Features

- **Local DNS Sinkholing (System-Wide Protection):** Intercepts DNS queries locally without routing user traffic through external proxies. Blocked domains resolve immediately to `0.0.0.0` or `::` (Sinkhole).
- **Enforced SafeSearch:** Maps Google, Bing, DuckDuckGo, and YouTube to their strict SafeSearch Virtual IPs at the DNS level.
- **Offline Seed Adult Blocklist:** Pre-bundles high-impact adult content domains directly in app assets, functioning without any network connection.
- **Custom Blocklist & Allowlist Management:** Full Room database support for user-managed custom domain rules with strict allowlist precedence, search, and import/export capabilities.
- **Focus Mode & Scheduling:** Time-boxed focus sessions (15m, 30m, 1h, 2h, 4h) and automated night-shield scheduling.
- **Accidental Change Deterrence:** Optional PIN protection with salted SHA-256 hashes and Device Administration (`DeviceAdminReceiver`) to deter impulsive uninstallation.
- **Built-in Diagnostic Self-Test:** Immediate local verification using `test.adult.eliteguardian.local`.
- **100% Privacy by Design:** Zero accounts, zero analytics, zero browsing history logs, zero ads.

---

## 🏗️ Architecture & Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Language** | Kotlin 1.9.24 |
| **User Interface** | Jetpack Compose with Material 3 (Dark-First Theme) |
| **Architecture** | MVVM with Repository Pattern & Coroutine Flows |
| **Local Database** | Room 2.6.1 with KSP (Kotlin Symbol Processing) |
| **Preferences** | Android Jetpack DataStore Preferences 1.1.1 |
| **Filtering Engine** | Android `VpnService` with RFC 1035 UDP DNS packet handling |
| **Security** | Salted SHA-256 with `SecureRandom` constant-time verification |
| **Minimum SDK** | Android 8.0 (API 26) |
| **Target SDK** | Android 15 (API 35) |

---

## 📁 Repository Structure

```
EliteGuardian/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/seed_blocklist.txt
│       │   ├── java/com/eliteguardian/app/
│       │   │   ├── EliteGuardianApp.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── navigation/Screen.kt
│       │   │   ├── ui/
│       │   │   │   ├── theme/
│       │   │   │   ├── onboarding/
│       │   │   │   ├── dashboard/
│       │   │   │   ├── blocklist/
│       │   │   │   ├── focus/
│       │   │   │   └── settings/
│       │   │   ├── data/
│       │   │   │   ├── database/
│       │   │   │   ├── preferences/
│       │   │   │   └── repository/
│       │   │   ├── domain/filtering/
│       │   │   ├── vpn/
│       │   │   ├── receiver/
│       │   │   └── security/
│       │   └── res/
│       └── test/
├── docs/
│   ├── reference-analysis.md
│   ├── feature-matrix.md
│   ├── android-permissions.md
│   ├── implementation-plan.md
│   ├── privacy-policy.md
│   ├── known-limitations.md
│   └── testing-report.md
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## 🚀 Building & Running

### Prerequisites
- JDK 17 or JDK 21
- Android SDK (API 34 or 35 installed)

### Run Unit Tests
```bash
./gradlew test
```
*Test reports are generated at:* `app/build/reports/tests/testDebugUnitTest/index.html`

### Assemble Debug APK
```bash
./gradlew assembleDebug
```
*Output APK path:*
```
app/build/outputs/apk/debug/app-debug.apk
```

### Install onto Device via ADB
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔐 Generating a Signed Release APK

1. Generate a release keystore:
   ```bash
   keytool -genkey -v -keystore eliteguardian.keystore -alias eliteguardian \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Build the release APK:
   ```bash
   ./gradlew assembleRelease
   ```
3. Sign and align the APK:
   ```bash
   zipalign -v -p 4 app/build/outputs/apk/release/app-release-unsigned.apk app-release-aligned.apk
   apksigner sign --ks eliteguardian.keystore --out EliteGuardian-v1.0.0.apk app-release-aligned.apk
   ```

---

## 📚 Technical Documentation

- [Reference Analysis (BlockerHero APK Breakdown)](docs/reference-analysis.md)
- [Feature Matrix](docs/feature-matrix.md)
- [Android Permissions Specification](docs/android-permissions.md)
- [Implementation Plan](docs/implementation-plan.md)
- [Privacy Policy](docs/privacy-policy.md)
- [Known Limitations & Android Boundaries](docs/known-limitations.md)
- [Verification & Test Report](docs/testing-report.md)

---

## ⚖️ License
EliteGuardian is distributed under the [Apache License 2.0](LICENSE).

