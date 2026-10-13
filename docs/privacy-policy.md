# EliteGuardian Privacy Policy

**Effective Date:** October 2026  
**Commitment:** 100% Privacy-by-Design, Free, and Open Source

EliteGuardian is dedicated to digital wellbeing and protection from adult content while strictly respecting user privacy and autonomy.

---

## 1. Zero Cloud Data Collection
EliteGuardian operates **100% offline and on-device**:
- **No User Accounts:** You do not need to register, provide an email address, or log in.
- **No Browsing History Logs:** EliteGuardian never logs, captures, or transmits visited URLs or domain histories.
- **No Analytics / Telemetry:** There are no third-party tracking SDKs, Firebase Analytics, Crashlytics trackers, or advertising IDs.
- **No Monetization Tracking:** EliteGuardian contains no advertisements, paywalls, or in-app billing telemetry.

---

## 2. Local DNS Filtering Architecture
- EliteGuardian utilizes Android's official `VpnService` strictly as a **local loopback interface** (`10.254.0.1`).
- Network packets for DNS (UDP port 53) are parsed on-device to match queried domains against local adult content blocklists.
- If a query matches an adult domain, a sinkhole answer (`0.0.0.0`) is synthesized locally without leaving the device.
- Permitted queries are forwarded to secure upstream family DNS resolvers (e.g. Cloudflare Family 1.1.1.3 or CleanBrowsing).
- EliteGuardian **never** intercepts HTTPS traffic, installs root certificates, or breaks TLS encryption.

---

## 3. Local Data Storage & Security
- **Custom Blocklists & Allowlists:** Stored locally in an SQLite database using Android Room (`elite_guardian.db`).
- **Preferences & Metrics:** Counters (e.g., total blocked queries) are stored locally using Android Jetpack DataStore.
- **Security PIN:** If configured, PINs are salted with cryptographically secure random bytes and hashed using SHA-256 before being stored. Plaintext PINs are never stored.

---

## 4. Permissions Usage
- `INTERNET` & `ACCESS_NETWORK_STATE`: Resolves non-blocked DNS queries via upstream servers and detects network handoffs.
- `FOREGROUND_SERVICE`: Keeps the local DNS filtering tunnel responsive with an ongoing status notification.
- `RECEIVE_BOOT_COMPLETED`: Resumes user-configured protection upon device startup.
- `POST_NOTIFICATIONS`: Displays active filter status and Focus Mode countdowns.
- `BIND_DEVICE_ADMIN` (Optional): Deterrent against impulsive uninstallation during moments of vulnerability.

---

## 5. Contact & Auditing
EliteGuardian is open source. You can audit the entire source code, build reproducible APKs, and verify that no network requests other than standard upstream DNS queries take place.

