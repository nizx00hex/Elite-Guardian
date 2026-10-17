# Admin Protection Mode — Architecture & Implementation

## 1. Overview & Objective

**Admin Protection Mode** in EliteGuardian provides tamper resistance by registering the application as an Android Device Administrator. 

### Core Purpose
- Provides a conscious friction barrier against impulsive uninstallation during moments of vulnerability or relapse temptation.
- When enabled, Android prevents immediate uninstallation by requiring the user to explicitly deactivate device administrator privileges first.

---

## 2. Technical Architecture

### A. Android Device Administrator Integration
- **Receiver**: `com.eliteguardian.app.receiver.GuardianDeviceAdminReceiver` extends `DeviceAdminReceiver`.
- **Manifest Declaration**:
  - Permission: `android.permission.BIND_DEVICE_ADMIN`
  - Action Filters:
    - `android.app.action.ADD_DEVICE_ADMIN`
    - `android.app.action.DEVICE_ADMIN_ENABLED`
    - `android.app.action.DEVICE_ADMIN_DISABLED`
  - Meta-data: `@xml/device_admin`
- **Minimal Policy footprint**:
  - EliteGuardian uses an empty `<uses-policies></uses-policies>` block in `res/xml/device_admin.xml`.
  - **Zero invasive policies**: No password reset, no camera disabling, no screen locking, no remote wipe privileges.

### B. User Experience & Lifecycle Synchronization
- **Real System State Source of Truth**:
  - `DevicePolicyManager.isAdminActive(adminComponent)` is queried whenever the Settings screen resumes (`Lifecycle.Event.ON_RESUME`).
  - The switch and badge (`Enabled`, `Disabled`, or `Locked by Focus Mode`) strictly mirror the actual operating system state.
  - The UI **never** reports "Enabled" or "Protected" if administrator rights are inactive.
- **Deactivation Hook (`onDisableRequested`)**:
  - Implemented in `GuardianDeviceAdminReceiver`.
  - When the user initiates deactivation from Android settings, Android displays EliteGuardian's custom explanation:
    *"Disabling Admin Protection Mode removes the extra step required before uninstalling EliteGuardian."*

---

## 3. Activation Flow

1. User toggles the Admin Protection Mode switch to **ON**.
2. EliteGuardian displays an explanatory confirmation dialog detailing:
   - What the permission does (friction against impulsive uninstallation).
   - What the permission does NOT do (no data inspection, no device lockout, no invasive policies).
3. User confirms by clicking "Continue to Android Settings".
4. The official Android intent is launched:
   ```kotlin
   Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
       putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
       putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "EliteGuardian uses Device Administrator privileges solely to prevent accidental or impulsive uninstallation.")
   }
   ```
5. Android presents the system-level activation prompt.
6. Upon activation, `GuardianDeviceAdminReceiver.onEnabled()` updates DataStore, and `ON_RESUME` synchronizes the UI badge to **Enabled**.

---

## 4. Deactivation Flow

1. User toggles the switch to **OFF**.
2. If **Focus Mode** is active:
   - Deactivation is blocked: the user is notified with a lock toast (*"🔒 Locked during active Focus Mode. Cannot be deactivated until Focus Mode finishes."*).
3. If Focus Mode is inactive:
   - EliteGuardian displays an in-app deactivation dialog with step-by-step instructions:
     1. Open Android's official Device Administrators screen.
     2. Select EliteGuardian.
     3. Tap *Deactivate this device admin app*.
4. App fires an explicit intent to open the system Device Admin or Security Settings (`android.settings.DEVICE_ADMIN_SETTINGS` with fallback to `android.settings.SECURITY_SETTINGS`).
5. Android handles deactivation natively with OS confirmation dialogs.
6. When the user returns to EliteGuardian, `ON_RESUME` re-evaluates `dpm.isAdminActive()`:
   - If user cancelled: switch remains ON, status remains **Enabled**.
   - If user completed deactivation: switch turns OFF, status updates to **Disabled**.

---

## 5. Limitations & Transparency

| Aspect | Device Administrator (EliteGuardian) | MDM / Device Owner (Enterprise) |
|---|---|---|
| **User Control** | User retains full control and can deactivate in Android Settings | Managed by enterprise / cannot be removed by user |
| **Data Access** | Zero access to private personal data (photos, contacts, messages) | Can monitor network and enforce enterprise certificates |
| **Lockout Risk** | Zero lockout risk; never alters lockscreen PIN or blocks system settings | Can remotely wipe device or lock screen |
| **System Settings** | Android settings are always accessible | Can restrict access to settings |

