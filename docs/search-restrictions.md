# Search Restrictions & Search Bar Blocking — Specification & Limitations

## 1. Overview
The **Search Restrictions** feature in EliteGuardian allows users to restrict access to selected search engines, enforce SafeSearch VIP redirection, manage custom search domains, and enforce search lockouts during Focus Mode sessions.

---

## 2. Technical Architecture

### A. Restriction Modes

1. **Mode 1 — Domain-Level Search Engine Blocking (DNS Sinkhole)**
   - Utilizes EliteGuardian's local loopback VPN (`GuardianVpnService`).
   - Intercepts DNS queries on UDP Port 53.
   - When a search engine is restricted, all queries for its primary and regional domains (e.g., `google.com`, `www.google.com`, `google.co.*`, `bing.com`, `duckduckgo.com`, `search.yahoo.com`) are resolved locally to `0.0.0.0` (sinkholed).
   - Prevents browser searches and background network traffic for those search portals without decrypting or inspecting HTTPS packets.

2. **Mode 2 — SafeSearch VIP Enforcement**
   - Automatically directs queries to official search engine safe-mode VIPs at the DNS level:
     - Google SafeSearch VIP: `216.239.38.120` (`forcesafesearch.google.com`)
     - Bing SafeSearch VIP: `204.79.197.220` (`strict.bing.com`)
     - YouTube Restricted VIP: `216.239.38.119` (`restrict.youtube.com`)
     - DuckDuckGo SafeSearch VIP: `52.142.124.215` (`safe.duckduckgo.com`)
   - Guarantees strict adult-content filtering across web searches while allowing educational search access.

3. **Mode 3 — Focus Mode Search Restrictions**
   - Integrates with `FocusViewModel` and `AppPreferences.isFocusModeActive`.
   - When a scheduled or manual Focus Mode session is active, configured search engine restrictions are automatically enforced.
   - Once the focus session expires or finishes, search engine access automatically reverts to the user's standard configuration.

---

## 3. Supported Search Engines

The following engines are supported and independently verified:
- **Google Search** (`google.com`, `www.google.com`, `google.co.uk`, `google.ca`, `google.de`, `google.co.in`, etc.)
- **Microsoft Bing** (`bing.com`, `www.bing.com`, `cn.bing.com`)
- **DuckDuckGo** (`duckduckgo.com`, `www.duckduckgo.com`, `html.duckduckgo.com`, `lite.duckduckgo.com`)
- **Yahoo Search** (`search.yahoo.com`, `yahoo.com`, `www.yahoo.com`)
- **Yandex Search** (`yandex.com`, `yandex.ru`, `ya.ru`)
- **Brave Search** (`search.brave.com`)
- **Ecosia** (`ecosia.org`, `www.ecosia.org`)

Users can also add any arbitrary search domain using the **Custom Search Domains** manager.

---

## 4. Rule Precedence & Adult Blocker Integrity

- **Rule Precedence:**
  1. `Allowlist` (Explicit user allowlist overrides all blocks)
  2. `Blocklist` (Adult Seed Blocklist + Custom Blocked Domains)
  3. `Search Restrictions` (Selected search engine domains + custom search domains)
  4. `SafeSearch VIP` (If domain is not blocked and SafeSearch is enabled)
  5. `Forward to Upstream DNS` (`1.1.1.3` Cloudflare Family Resolver)
- **Zero Interference with Adult Blocking:**
  - Disabling search restrictions **never** disables the adult-content blocker.
  - The adult seed blocklist and user custom blocklists remain fully enforced.

---

## 5. Technical Limitations & Android Boundaries

1. **Launcher Search Widgets:**
   - On standard Android devices (without OEM Device Owner MDM enrollment or root), third-party applications **cannot** programmatically remove or hide home-screen search widgets (such as the Google Search bar widget on Pixel/AOSP launchers).
   - *Alternative solution provided to user:* Long-press the widget to remove it, or use custom launchers like Nova Launcher or Lawnchair.
2. **Dedicated Search Applications:**
   - While domain blocking prevents the Google App from fetching web results, users who want to prevent opening the app entirely should use EliteGuardian's **App Blocker** feature (`GuardianAccessibilityService`).
3. **Collateral Domain Impact:**
   - Blocking `google.com` blocks search traffic, but may also block other Google services hosted under that apex domain. SafeSearch VIP enforcement is recommended for users who need access to Google accounts or services while keeping adult content filtered.

