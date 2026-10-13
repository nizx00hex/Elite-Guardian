# EliteGuardian Known Limitations & Platform Boundaries

To maintain absolute transparency and adhere to Android security principles, the following technical limitations are documented:

---

## 1. Android Private DNS (DoT / DoH)
- **Behavior:** Starting with Android 9 (API 28), Android includes a native "Private DNS" setting that routes system DNS queries over TLS (DNS-over-TLS on port 853).
- **Impact:** If a user configures a custom third-party Private DNS provider (such as custom NextDNS or AdGuard DoT), Android will bypass local UDP port 53 queries.
- **Remediation / Guidance:** For EliteGuardian to provide maximum filtering accuracy, Private DNS in Android Settings -> Network -> Private DNS should be set to **Automatic** or **Off**. EliteGuardian then upstream-routes permitted queries to family-safe resolvers.

---

## 2. Hardcoded IP Connections & Custom Sockets
- **Behavior:** If an application bypasses standard hostname resolution and connects directly to raw IP addresses (e.g. `http://198.51.100.1`), DNS filtering does not see a domain lookup.
- **Impact:** Websites or apps accessed directly by IP will not trigger domain sinkholing. However, 99.9% of modern web traffic and CDN-hosted adult platforms require SNI / hostname resolution for routing and SSL certificates.

---

## 3. Absence of TLS Interception (MITM)
- **Behavior:** EliteGuardian does **not** install user root CA certificates or perform man-in-the-middle TLS decryption.
- **Rationale:** Intercepting encrypted HTTPS traffic exposes sensitive personal passwords, banking details, and violates Android platform security policies. Furthermore, modern apps with certificate pinning (HPKP) immediately break when subjected to TLS interception.
- **Impact:** URL paths on multi-purpose websites (e.g., `reddit.com/r/porn`) cannot be filtered via DNS alone unless the entire root domain (`reddit.com`) or media CDN is blocked.

---

## 4. Uninstall Prevention Boundaries
- **Behavior:** No non-system Android application can make itself 100% impossible to uninstall on an unmanaged consumer device.
- **EliteGuardian Approach:** Uses Android's official Device Administration API (`DeviceAdminReceiver`) as an effective **anti-relapse deterrent**. This prevents quick uninstallation from the launcher, requiring the user to intentionally open system settings and deactivate admin mode.

