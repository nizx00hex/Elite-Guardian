# Android Permissions Specification: EliteGuardian

## 1. Overview
EliteGuardian adheres to the principle of **least privilege**. Unlike invasive surveillance apps or ad-supported blockers, EliteGuardian requests only the permissions strictly required to perform local content blocking, scheduled protection, and tamper deterrence.

---

## 2. Declared & Required Permissions

| Permission | Protection Level | Purpose in EliteGuardian | Failure / Denial Handling |
| :--- | :--- | :--- | :--- |
| `android.permission.INTERNET` | Normal | Allows DNS forwarding for unblocked domains via upstream servers (Cloudflare 1.1.1.3 / CleanBrowsing / Quad9). | Network failure reported in Dashboard; DNS fallback. |
| `android.permission.ACCESS_NETWORK_STATE` | Normal | Monitors network changes (Wi-Fi <-> Cellular) to rebind the VPN tunnel seamlessly without dropping connections. | Tunnel maintains best-effort loop until state updates. |
| `android.permission.FOREGROUND_SERVICE` | Normal | Keeps the filtering engine alive as a persistent foreground service with a visible persistent notification. | Required for Android 9+ (API 28+). |
| `android.permission.FOREGROUND_SERVICE_SPECIAL_USE` / `FOREGROUND_SERVICE_CONNECTED_DEVICE` | Normal (Android 14+) | Required by Android 14 (API 34+) for foreground service type declarations. | Declared with specific type `specialUse` / `systemExempted`. |
| `android.permission.POST_NOTIFICATIONS` | Runtime (API 33+) | Displays protection status notification, focus session reminders, and blocked domain count alerts. | If denied, app functions silently without notification controls; prompt in Dashboard. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Normal | Automatically resumes DNS protection and active Focus Mode schedules upon device reboot. | User can manually start protection if boot receiver is delayed by OEM battery managers. |
| `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Normal / Intent | Guides user to exempt EliteGuardian from aggressive OEM battery killing (Doze mode) to prevent filter termination. | Optional onboarding prompt; app functions normally unless OS kills the process. |
| `android.permission.BIND_DEVICE_ADMIN` | Signature / Special | Allows the user to enable Device Administrator mode to prevent impulsive uninstallation of the app. | Entirely optional; user can skip during onboarding and toggle in Security Settings. |
| `android.permission.PACKAGE_USAGE_STATS` | Special App Access | (Optional for App Restriction feature) Checks the current foreground app package during Focus Mode without needing full accessibility. | Explains to user why App Usage access is required; if denied, app restrictions remain disabled. |

---

## 3. VpnService Authorization Flow

The `VpnService` permission does **not** use `ActivityCompat.requestPermissions`. Instead:
1. EliteGuardian prepares the VPN intent via `VpnService.prepare(context)`.
2. If `prepare()` returns a non-null `Intent`, Android presents the system-controlled VPN connection dialog: `"EliteGuardian wants to set up a VPN connection that allows it to monitor network traffic"`.
3. EliteGuardian handles the `ActivityResult` in `ActivityResultContracts.StartActivityForResult()`:
   - **`RESULT_OK`**: The VPN tunnel is established immediately.
   - **`RESULT_CANCELED`**: The user canceled the authorization. The UI reflects `Protection: Disabled`, explains why VPN authorization is necessary, and provides a "Retry Setup" action.
4. If another VPN is already active, Android automatically revokes the previous VPN when EliteGuardian connects. EliteGuardian listens for `onRevoke()` in the `VpnService` to cleanly release sockets, notify the UI, and avoid silent failures.

---

## 4. Device Administrator Flow

1. Registered via `DeviceAdminReceiver` in `AndroidManifest.xml` with `<meta-data android:name="android.app.device_admin" android:resource="@xml/device_admin" />`.
2. Policy file specifies minimal policies (e.g. no remote wipe or lock password policies, solely registered as an admin).
3. EliteGuardian launches `DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN` with an explicit explanation:
   > "Enabling Device Admin prevents impulsive uninstallation when cravings or urges strike. You can disable this setting anytime from EliteGuardian Security Settings."
4. If the user chooses to remove protection, they enter their local PIN (if configured) or wait out the cooldown timer, and EliteGuardian programmatically directs them to `DevicePolicyManager.removeActiveAdmin()`.

