# WearPass

![Logo](assets/logo.jpg)

KeePass (KDBX) database viewer for Wear OS. Read-only, standalone, no phone app required, no network access.

> Not affiliated with the KeePass or KeePassXC or KeePassDx projects.

### Requirements

Wear OS 5 or newer (Android 14, API 34).

### Features

- Opens KDBX databases up to version 4.1. Master password only, key files are not supported.
- Built-in file browser (folders and `.kdbx` files). The selected file is copied into private app storage.
- Source file tracking: the original path is remembered. On every unlock the source file is compared (SHA-256) with the stored copy; if it changed and opens with the saved master password, the copy is updated. If the source is missing or unreadable, the stored copy is used.
- Groups and entries browser. The Recycle Bin and Templates groups are hidden.
- Entry view: username, URL, notes, custom fields, attachment names (no download).
  - Password and protected custom fields are decrypted only on tap and hidden again automatically.
  - Revealed passwords are colored by character type (letters, digits, symbols).
- Entry and group icons: custom icons from the database and the built-in KeePass icon set.
- Ambient mode: content is replaced by a black screen, app state is preserved.
- Swipe left opens Settings from anywhere. Swipe right is "back".
- Settings: import a new file, change the unlock method, delete vault.

### Unlock methods

The master password is entered once; after that one of three methods is used.

| Method        | Details                                                                                                                                                                                                                   |
|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Classic PIN   | Numeric keypad.                                                                                                                                                                                                           |
| Circular dial | Rotary dial: turn the bezel/crown, change the rotation direction to enter a digit, tap the center to enter the last digit, long-press the center to clear. Touch dragging is a fallback for watches without rotary input. |
| System lock   | Uses the watch screen lock. The key is usable only while the watch is unlocked (with wrist detection: while it is worn). Anyone wearing the unlocked watch can open the vault.                                            |

- The master password is stored encrypted with an Android Keystore key. For the classic PIN and the circular dial, a key derived from the code (PBKDF2-HMAC-SHA256) adds a second layer.
- Too many wrong attempts erase the stored secret and require the master password again.
- The unlock method can be switched in Settings after confirming the current one.

### Auto-lock and privacy

- The vault locks shortly after the app is closed, minimized, the screen turns off or the watch enters ambient mode.
- It locks immediately when the watch is taken off (off-body sensor) or when the screen lock engages.
- Decrypted data lives only in memory and is dropped on lock.
- Screenshots are blocked (`FLAG_SECURE`).
- App data backup is disabled.
- The app has no `INTERNET` permission.
- "Delete vault" removes the database copy, saved secrets and Keystore keys. The source file is not touched.

### Credential provider (autofill)

Credential Manager provider for **passwords**.

- An app is matched to an entry by `androidapp://<package name>` in the entry's URL field.
- If the vault is locked, the provider offers an unlock prompt first.
- Saving new credentials and passkeys are not supported.
- Select WearPass as the credentials provider in the watch system settings.

### Permissions

| Permission                                   | Why                                                                           |
|----------------------------------------------|-------------------------------------------------------------------------------|
| `MANAGE_EXTERNAL_STORAGE` (All files access) | Read the `.kdbx` file from shared storage (file browser and source tracking). |
| `WAKE_LOCK`                                  | Ambient mode.                                                                 |
| `SUBSCRIBE_TO_KEYGUARD_LOCKED_STATE`         | Lock the vault when the watch locks.                                          |

The credential provider service is protected by the system permission `BIND_CREDENTIAL_PROVIDER_SERVICE`.

### Setup

1. Install the app on the watch.
2. Grant all files access (the permission screen is usually missing on Wear OS):
   ```
   adb shell appops set stanissay.wear.pass MANAGE_EXTERNAL_STORAGE allow
   ```
3. Put the `.kdbx` file on the watch, for example:
   ```
   adb push database.kdbx /sdcard/Documents/
   ```
4. Open WearPass, pick the file, enter the master password, create a PIN.

If several devices are connected, add `-s <serial>` to the `adb` commands.

### Not supported

Editing or creating databases, key files, TOTP, passkeys, saving credentials from other apps, attachment download.

### Security notes

- Classic PIN and circular dial codes are short by design. The attempt limit protects against guessing on the watch. An attacker with code execution on the device could brute-force a short code using the device's own Keystore. The real secret is the master password.
- System lock mode is convenient but weaker: it only relies on the watch being locked.
- Strings returned by the KDBX library cannot be zeroed, so decrypted values may stay in memory until garbage collection.

### Built with

Kotlin, Jetpack Compose for Wear OS (Material 3), Navigation 3, [kotpass](https://github.com/keemobile/kotpass), Android Keystore, Credential Manager.

### Credits

- **Built-in KeePass icons**: taken from the [KeePassXC](https://github.com/keepassxreboot/keepassxc) repository, folder [`share/icons/database`](https://github.com/keepassxreboot/keepassxc/tree/develop/share/icons/database). Converted from SVG to PNG (96x96) and bundled as `assets/kp_icons/`. Licensing as stated in KeePassXC's `COPYING`:
  - MIT: [icons8/flat-color-icons](https://github.com/icons8/flat-color-icons)
  - CC0: [paomedia/small-n-flat](https://github.com/paomedia/small-n-flat)
  - GPL-2+: [simple-icons/simple-icons](https://github.com/simple-icons/simple-icons/)
  - MIT: KeePassXC Team
- **[kotpass](https://github.com/keemobile/kotpass)** (MIT license): KDBX reading.
- **[KeePass](https://keepass.info/)** and **[KeePassXC](https://keepassxc.org/)**: KDBX format and icon set.

### License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

---

*Built for convenience and safety on Wear OS.*