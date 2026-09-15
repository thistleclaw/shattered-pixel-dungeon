# Shattered Pixel Dungeon: Adventure 3.2.1-adventure7

Legacy Adventure Mode build for old Android devices, based on Shattered Pixel Dungeon 3.2.1.

### Adventure additions

- One automatic floor-entry checkpoint.
- Three manual save slots that capture the exact current turn.
- Recovery from valid Adventure checkpoints after final death.
- Checkpoint storage outside the normal `gameN/` run directory.
- Safe explicit directory copying for Android local storage, avoiding the libGDX `FileHandle.moveTo()` directory nesting issue.
- English and Russian Adventure Mode interface strings, with English fallback for other languages.
- Separate application ID: `com.thistleclaw.shatteredpd.adventure`.

### Fixes in adventure7

- Fixed localized save-menu parameters appearing literally as `%s` and `%d` instead of floor/slot values.
- Added a CI check that rejects printf-style placeholders in Adventure localization bundles; libGDX `I18NBundle` requires MessageFormat placeholders such as `{0}` and `{1}`.

### Compatibility

This edition supports **Android 4.0 (API 14) and newer**, including Android 4.4 devices.

For the current Shattered Pixel Dungeon 4.0.0 edition, use the `adventure-v4` branch.

---

internal version number: 867

Android 4.0 (API 14)+ Devices
