# EliteGuardian Implementation Plan

## 1. Architectural Blueprint

EliteGuardian is architected following Clean MVVM principles, separating domain logic from Android framework APIs:

```
EliteGuardian
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/eliteguardian/app/
│       │   │   ├── EliteGuardianApp.kt
│       │   │   ├── MainActivity.kt
│       │   │   ├── navigation/
│       │   │   │   └── Screen.kt
│       │   │   ├── ui/
│       │   │   │   ├── theme/
│       │   │   │   ├── onboarding/
│       │   │   │   ├── dashboard/
│       │   │   │   ├── blocklist/
│       │   │   │   ├── focus/
│       │   │   │   └── settings/
│       │   │   ├── data/
│       │   │   │   ├── database/
│       │   │   │   │   ├── AppDatabase.kt
│       │   │   │   │   ├── BlocklistDao.kt
│       │   │   │   │   └── BlocklistEntity.kt
│       │   │   │   ├── preferences/
│       │   │   │   │   └── AppPreferences.kt
│       │   │   │   └── repository/
│       │   │   │       ├── FilterRepository.kt
│       │   │   │       └── SettingsRepository.kt
│       │   │   ├── domain/
│       │   │   │   ├── filtering/
│       │   │   │   │   ├── DomainMatcher.kt
│       │   │   │   │   ├── DomainValidator.kt
│       │   │   │   │   └── SafeSearchRules.kt
│       │   │   │   └── model/
│       │   │   ├── vpn/
│       │   │   │   ├── GuardianVpnService.kt
│       │   │   │   ├── DnsPacketHandler.kt
│       │   │   │   └── VpnState.kt
│       │   │   ├── receiver/
│       │   │   │   ├── BootReceiver.kt
│       │   │   │   └── GuardianDeviceAdminReceiver.kt
│       │   │   └── security/
│       │   │       └── PinManager.kt
│       │   └── res/
│       └── test/
│           └── java/com/eliteguardian/app/
│               ├── DomainMatcherTest.kt
│               ├── DomainValidatorTest.kt
│               ├── DnsPacketHandlerTest.kt
│               └── PinManagerTest.kt
```

---

## 2. Milestone Roadmap

### Milestone 1: Environment & Reference Analysis (COMPLETED)
- Inspect host Linux environment, JDK 21, Gradle 8.9, Android SDK (API 34-36).
- Decompile reference APK `base.apk` via JADX & AAPT.
- Document reference architecture (`docs/reference-analysis.md`).
- Generate feature matrix (`docs/feature-matrix.md`) and permissions specification (`docs/android-permissions.md`).

### Milestone 2: Project Setup, Material 3 Dark Theme, Navigation, & Onboarding
- Initialize Gradle Kotlin DSL project (`settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`) with Android Gradle Plugin 8.5.2, Kotlin 1.9.24, Compose Material 3, Room, DataStore.
- Implement `MainActivity`, Navigation graph with modern Compose routes.
- Build dark-first aesthetic theme and responsive screens:
  - Onboarding flow (Welcome, How it works, Permissions guide, VPN authorization).
  - Main Dashboard (Status card, quick toggle, filter stats, diagnostic test launcher).
  - Persistent DataStore preferences.

### Milestone 3: Domain Matching Engine & Blocklist Management
- Implement `DomainMatcher`:
  - Canonical domain normalization (lowercasing, Punycode/IDN conversion via `IDN.toASCII()`).
  - Strict hierarchical subdomain matching (`sub.evil.com` matches `evil.com`, but `good-evil.com` does not).
  - Explicit allowlist override precedence (Allowlist > Custom Blocklist > Global Adult Blocklist).
  - High-performance Set/Trie lookup.
- Room database integration for custom blocklists and allowlists.
- Import/Export functionality for text/hosts lists with robust syntax validation.
- Unit test suite for domain matching, malicious input, IDN, and precedence.

### Milestone 4: Android VpnService DNS Filtering Engine
- Implement `GuardianVpnService`:
  - Captures local DNS port 53 traffic via `VpnService.Builder`.
  - Parses incoming DNS packets (query header, question name, type A / AAAA).
  - If query matches blocked domain: immediately returns synthesized DNS response with `0.0.0.0` / `::` / `NXDOMAIN`.
  - If query is clean: forwards query upstream to secure DNS resolvers (e.g. Cloudflare Family 1.1.1.3 / CleanBrowsing) via DatagramSocket and returns response.
  - Intercepts SafeSearch domains (Google, Bing, DuckDuckGo) and resolves SafeSearch VIP addresses.
  - Manages VPN lifecycle, `onRevoke()`, connectivity changes, and error recovery.
  - Integration diagnostic: `test.adult.eliteguardian.local` tests active sinkholing.

### Milestone 5: App Restrictions, Focus Mode, & Accidental Change Protection
- Focus Mode session scheduler using Android WorkManager / AlarmManager.
- App restriction manager with optional UsageStats monitoring.
- PIN protection (`PinManager`) using SHA-256 with random salt to protect settings and filter termination.
- Anti-relapse cooldown timer for disabling protection.
- Optional Device Admin integration (`GuardianDeviceAdminReceiver`).

### Milestone 6: Verification, Unit Testing & Reproducible Build
- Execute all unit tests (`./gradlew test`).
- Compile debug APK (`./gradlew assembleDebug`).
- Document physical device verification checklist (Wi-Fi, mobile data, DNS leaks, browser compatibility).

### Milestone 7: Final Delivery & Release Documentation
- Deliver complete, clean codebase.
- Provide step-by-step instructions for installation, testing, and release signing.

