# Reference Analysis: BlockerHero APK vs. EliteGuardian

## 1. Overview & Analysis Methodology
This analysis examines the reference application **BlockerHero** (version `1.5.0`, versionCode `190`, targetSdkVersion `35`, compileSdkVersion `35`) using static analysis via AAPT, JADX, and manifest inspection.

The primary APK analyzed is `base.apk` alongside its configuration splits (`split_config.arm64_v8a.apk`, `split_config.en.apk`, `split_config.xxhdpi.apk`).

---

## 2. Observable Architecture of BlockerHero

### 2.1 Core Architectural Approach
Contrary to apps that filter traffic at the network/VPN layer, **BlockerHero does not declare or implement an Android `VpnService`**. Instead, BlockerHero's filtering architecture is based entirely on user-interface inspection and accessibility event handling:

1. **Accessibility Service (`com.blockerhero.services.MyAccessibilityService`)**:
   - Subscribes to window state changes and accessibility events.
   - Inspects UI element hierarchies to scrape URLs from known browser address bars (e.g. Chrome, Samsung Internet, Firefox, Edge, Opera, DuckDuckGo, Brave, etc.) via known node IDs (such as `urlBarId`).
   - Scrapes text content on screen to match forbidden keywords.
   - Monitors target applications (e.g. Instagram search/reels, YouTube shorts, WhatsApp channels, Telegram search) using hardcoded package and activity names.
   - Enforces blocking by calling `performGlobalAction(GLOBAL_ACTION_BACK)` or presenting an overlay window (`WindowManager.addView` with `SYSTEM_ALERT_WINDOW`).

2. **Device Administration (`com.blockerhero.receiver.MyDeviceAdminReceiver`)**:
   - Registers as a Device Administrator (`android.app.action.DEVICE_ADMIN_ENABLED`).
   - Uses device administrator status to prevent users from casually uninstalling the application without first navigating through device admin deactivation.

3. **Remote Server Dependency & Commercialization**:
   - Uses Firebase Cloud Messaging (`FCMService`), Firebase Auth, and Google Play In-App Billing (`com.android.vending.BILLING` v8.0.0).
   - Syncs user blocklists and downloads global keyword lists from proprietary servers (`global_blocked_keywords`, `UserBlockedItem`).
   - Maintains a `BlockedDataLog` table with an `is_send` column to upload detection events and app usage to remote servers.
   - Enforces paid subscriptions (`UserSubscription`) for key features.

---

## 3. Structural Limitations of the Reference Architecture

1. **Fragile Browser URL Inspection**:
   - Because it inspects view hierarchies for specific `urlBarId` resource identifiers, any browser UI update or custom browser not explicitly whitelisted will bypass filtering entirely.
   - Private/Incognito modes and Chromium custom tabs often obscure or rename address bar view IDs, leading to leaks.

2. **Inability to Filter Non-Browser Network Traffic**:
   - Applications making background HTTP/HTTPS requests, embedded web views, or custom socket connections cannot be filtered by an AccessibilityService that relies on address-bar text scraping.

3. **Privacy Concerns in Logging**:
   - BlockerHero collects app package names, query texts, and sends event logs to remote servers (`is_send`).

4. **Performance & Battery Drain**:
   - Continuous parsing of `AccessibilityEvent` and deep accessibility node trees on every touch or scroll event introduces noticeable CPU overhead and UI latency.

5. **Monetization & Restrictions**:
   - Restricts features behind in-app purchases and account creation.

---

## 4. EliteGuardian Architectural Superiority

To achieve reliable, comprehensive, and privacy-preserving adult content blocking, **EliteGuardian** adopts a modern two-tier filtering strategy:

1. **Local DNS VPN Filtering Engine (Tier 1 - System-wide Protection)**:
   - Uses Android's official `VpnService` to capture DNS traffic (UDP/TCP port 53).
   - Intercepts DNS queries locally on the device (127.0.0.1) without routing user payloads through external proxies or TLS termination.
   - Resolves domains against an offline, high-performance Bloom filter / Trie / Set of verified adult domains.
   - Blocks adult domains by returning `NXDOMAIN` or `0.0.0.0` (Sinkhole).
   - Enforces SafeSearch at the DNS level by redirecting major search engine queries (Google, Bing, DuckDuckGo, YouTube) to their official SafeSearch CNAME/IP VIPs.
   - Works uniformly across **all** browsers, web views, and apps without requiring browser-specific view ID scraping.

2. **Application Restriction & Focus Shield (Tier 2 - App & Schedule Management)**:
   - Allows users to restrict specific applications during Focus Mode or schedule intervals.
   - Uses `UsageStatsManager` / foreground service / accessibility where configured to prevent access to distracting or unmoderated apps.

3. **Privacy-by-Design**:
   - Zero telemetry, zero mandatory cloud accounts, zero analytics.
   - All rules, allowlists, blocklists, and statistics remain strictly on the local device in an encrypted Room SQLite database and DataStore preferences.
   - Fully free, open-source Kotlin codebase with Material 3 UI.

