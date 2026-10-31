# HappyLighting Desktop + Android

Windows desktop controller for BLE LED devices that work with the HappyLighting or Triones mobile apps.

This repo now also includes a native Android client under [`app/`](./app) that ports the BLE control path directly to Kotlin/Jetpack Compose for on-phone control.

Current scope (desktop `0.4.0`):

- polished PySide6 desktop UI (colour wheel or RGB sliders)
- saved-device reconnect on launch
- on/off, static colour, brightness (RGB scaling)
- inline error banner with actionable BLE failure text — no modal dialogs
- stuck-connection recovery via `Reset Bluetooth & Reconnect`
- rotating log file for a windowed build that has no console
- Windows single-file `.exe` build path
- native Android Studio project with direct BLE control

## Requirements

- Windows 10 or 11 with Bluetooth Low Energy support
- Python 3.11+
- The light must not already be connected to your phone

## Install

```powershell
py -3.11 -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -e .
```

## Run

```powershell
lighting
```

Or:

```powershell
python -m happylighting
```

## Build EXE

```powershell
.\build.ps1
```

## Android App

- Open the repo in Android Studio.
- Let Android Studio install or locate the Android SDK if prompted.
- Connect your phone with `USB debugging` enabled.
- Run the `app` configuration to install the debug build directly.
- Build `release` for a sideloadable APK once BLE behavior is verified on-device.

Optional signing for release APKs:

- Create a `keystore.properties` file in the repo root with:

```properties
storeFile=path/to/your-keystore.jks
storePassword=your-store-password
keyAlias=your-key-alias
keyPassword=your-key-password
```

- If `keystore.properties` is present, the Android `release` build will sign with it.

## Notes

- Settings now live in `%APPDATA%\HappyLighting\settings.json`.
- Legacy `lighting-settings.json` is migrated automatically on first run.
- Logs are written to `%APPDATA%\HappyLighting\happylighting.log` (rotating, 3 files). Qt warnings and uncaught tracebacks land there too, since the shipped build is windowed and discards stderr.
- Some compatible controllers advertise under names like `QHM-*` instead of `Triones` or `HappyLighting`, so the scan view now shows nearby named BLE devices and tags likely matches first.
- The prototype uses the same Triones/HappyLighting command bytes documented by community reverse engineering and the Home Assistant integration.
- Supported write UUIDs:
  - `0000ffd5-0000-1000-8000-00805f9b34fb`
  - `0000ffd9-0000-1000-8000-00805f9b34fb`
  - `0000ffe5-0000-1000-8000-00805f9b34fb`
  - `0000ffe9-0000-1000-8000-00805f9b34fb`
- Supported read/notify UUIDs:
  - `0000ffd0-0000-1000-8000-00805f9b34fb`
  - `0000ffd4-0000-1000-8000-00805f9b34fb`
  - `0000ffe0-0000-1000-8000-00805f9b34fb`
  - `0000ffe4-0000-1000-8000-00805f9b34fb`
- The brightness slider is RGB scaling, not a separate device-native brightness command.
- Desktop requires `bleak>=3.0.1`; BLE failures are translated into plain sentences by `happylighting.controller.describe_exception` instead of surfacing empty `BleakError` reprs.
- Software-animated effects were removed in `0.4.0`: they fought the device's own timing and made every failure look like a disconnect.

## Tests

```powershell
python -m unittest discover -s tests
```

## License

MIT — see [LICENSE](LICENSE).
