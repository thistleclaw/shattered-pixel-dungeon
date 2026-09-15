# Shattered Pixel Dungeon: Adventure 4.0.0-adventure2

Adventure Mode brings rollback saves to Shattered Pixel Dungeon 4.0.0 while keeping the normal dungeon rules and balance intact.

### Adventure additions

- One automatic checkpoint from the moment you enter each floor or side branch.
- Three manual save slots that capture the exact current turn.
- Checkpoints remain recoverable after final death instead of the run being permanently discarded.
- Safe checkpoint replacement with temporary/backup copies to protect existing saves if a write fails.
- English and Russian Adventure Mode interface strings, with English fallback for other languages.
- Separate application ID: `com.thistleclaw.shatteredpd.adventure`.
- Adventure update checks point to this fork rather than the upstream Shattered Pixel Dungeon release feed.

### Fixes in adventure2

- Fixed Adventure save-menu parameters appearing literally as `%s` and `%d` instead of floor/slot values.
- Added a CI check that rejects printf-style placeholders in Adventure localization bundles; libGDX `I18NBundle` requires MessageFormat placeholders such as `{0}` and `{1}`.

### Compatibility

This edition is based on **Shattered Pixel Dungeon 4.0.0** and requires **Android 5.0 (API 21) or newer**.

For Android 4.x devices, use the legacy `adventure-saves` branch based on Shattered Pixel Dungeon 3.2.1.

---

internal version number: 914

Android 5.0 (API 21)+ Devices
