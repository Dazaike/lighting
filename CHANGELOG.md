# Changelog

## [v0.4.0] - 2026-10-31

First release published from a clean history; changes are described relative to the unreleased desktop/Android 0.3.19 builds.

### Added
- Android: Apple-style UI (`ui/Apple.kt`): frosted grouped cards, iOS-style slider, segmented control, switch, large titles and a floating Light/Settings tab bar.
- Android: Light screen layout built around a colour card showing the current output, a status pill, a saved-colour row with an add button, and the Wheel/RGB picker below.
- Android: `QuickPowerActivity` with `LightsOnActivity` / `LightsOffActivity` entry points and a transparent theme.
- Desktop: React/Vite web UI under `web/` (colour wheel, RGB sliders, segmented pill), a local `server.py`, and `logging_setup.py` with a rotating log file.
- Desktop: unit tests under `tests/` (`python -m unittest discover -s tests`).
- Desktop: inline error banner with plain-language BLE failure text, and a `Reset Bluetooth & Reconnect` recovery action.
- Desktop: `Lumora` branding (icon, splash, `.exe`).

### Changed
- Android: UI moved out of `MainActivity.kt` into `ui/LumoraUi.kt`; settings are now a tab instead of a bottom sheet.
- Android: `MainViewModel`, `SettingsRepository` and `MainUiState` extended (see diff); theme reworked in `ui/theme/Theme.kt`.
- Desktop: `service.py` reworked around tracked futures, busy flags and a single failure path; `controller.py`, `settings.py`, `ui.py` and `app.py` updated to match.
- Desktop requires `bleak>=3.0.1`.
- Version bumped to `0.4.0` (desktop `pyproject.toml`, Android `versionName` 0.4.0 / `versionCode` 23).

### Removed
- Software-animated effects and native effect presets (`effects.py`); they fought the device's own timing.
- Android: the `io.github.kyant0:backdrop` dependency and the liquid-glass components.

### Repository
- Repository history was reset for the public release; signed release APKs are now produced with a dedicated keystore (kept out of the repo).
- `.gitignore` covers `node_modules/`, `web/dist/`, `*.egg-info/`, `.claude/`, signing keys, `lighting-settings.json`, generated `.spec` files and UI screenshots.
