# EliteGuardian & BlockerHero Feature Matrix

The following matrix categorizes all observable and planned features according to the evaluation criteria:
- **Verified in the reference** (Present and verified via static/dynamic analysis of BlockerHero APK)
- **Inferred but not verified** (Suggested by code or manifest in BlockerHero, but unverified runtime effectiveness)
- **Planned for EliteGuardian** (Targeted feature for the EliteGuardian application)
- **Implemented and tested** (Completed, covered by unit/integration tests in EliteGuardian)
- **Unsupported or limited by Android** (Features constrained by modern Android security and sandboxing models)

---

| Feature Category | Specific Capability | Status in BlockerHero | Status in EliteGuardian | Technical Mechanism / Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Network & Content Filtering** | Adult Domain DNS Sinkholing | Not implemented (No VPN) | **Planned for EliteGuardian** | `VpnService` capturing UDP 53; returns `0.0.0.0` or `NXDOMAIN` locally. |
| | System-Wide Browser Filtering | Inferred (Accessibility scraping) | **Planned for EliteGuardian** | Works for all browsers via DNS rather than per-browser address-bar scraping. |
| | Subdomain & Wildcard Matching | Verified in the reference (String contains) | **Planned for EliteGuardian** | Robust Trie/Reverse domain segment matching (`x.adult.com` matches `adult.com`). |
| | Internationalized Domain Names (IDN) | Inferred but not verified | **Planned for EliteGuardian** | Punycode / `java.net.IDN.toASCII()` normalization. |
| | Custom User Blocklists | Verified in the reference | **Planned for EliteGuardian** | Room database with CRUD operations, search, import/export. |
| | Custom User Allowlists | Verified in the reference | **Planned for EliteGuardian** | Strict precedence: allowlist overrides blocklist. |
| | SafeSearch DNS Enforcement | Inferred (Keyword search blocking) | **Planned for EliteGuardian** | DNS redirection to SafeSearch VIPs (Google `forcesafesearch.google.com`, Bing, DuckDuckGo). |
| | TLS Decryption / MITM Certificate | Unsupported or limited by Android | Unsupported or limited by Android | Excluded by design: TLS interception violates user security and Android TLS trust store rules. |
| **Application Restrictions** | Restrict Selected Apps | Verified in the reference | **Planned for EliteGuardian** | User-selected installed package restriction with overlay/warning. |
| | Focus Mode Sessions | Verified in the reference (`FocusTime`) | **Planned for EliteGuardian** | Timed focus sessions blocking restricted apps & websites with WorkManager alarms. |
| | Scheduled Protection Windows | Verified in the reference | **Planned for EliteGuardian** | Recurring daily/weekly time ranges via WorkManager and AlarmManager. |
| **Tamper Protection** | Device Admin Protection | Verified in the reference (`MyDeviceAdminReceiver`) | **Planned for EliteGuardian** | Device Administration API to prevent casual app removal without deactivation. |
| | Settings PIN Lock | Inferred but not verified | **Planned for EliteGuardian** | Salted cryptographic SHA-256 / PBKDF2 hash stored securely in EncryptedSharedPreferences / DataStore. |
| | Cooldown Timers for Disabling | Inferred in the reference | **Planned for EliteGuardian** | Timed delayed unlocking to prevent impulsive disabling. |
| **Privacy & Infrastructure** | Accountless Operation | Unsupported in reference (Mandatory Firebase) | **Planned for EliteGuardian** | 100% offline, zero account creation, zero login. |
| | Zero Browsing History Telemetry | Unsupported in reference (Uploads `BlockedDataLog`) | **Planned for EliteGuardian** | Zero remote tracking; minimal local counters (total blocked count). |
| | No Ads / No Paywalls | Unsupported in reference (In-App Billing v8) | **Planned for EliteGuardian** | 100% free and open-source without subscriptions. |
| **UI & Experience** | Modern Material 3 UI | Inferred (Mix of View + Compose) | **Planned for EliteGuardian** | 100% Jetpack Compose with Dark-first Material 3 theme. |
| | Filter Diagnostic Test | Unsupported in reference | **Planned for EliteGuardian** | Built-in self-test verifying active DNS sinkholing (`test.adult.eliteguardian.local`). |
| | Onboarding & Guided Permissions | Verified in the reference | **Planned for EliteGuardian** | Interactive onboarding explaining permissions, VPN consent, and battery optimization. |

