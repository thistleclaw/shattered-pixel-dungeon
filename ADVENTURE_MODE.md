# Shattered Pixel Dungeon: Adventure

Adventure Mode is an unofficial fork of [Shattered Pixel Dungeon](https://github.com/00-Evan/shattered-pixel-dungeon) that keeps the original dungeon, combat, items, hunger, bosses and procedural generation, while adding rollback saves for players who do not want a run to disappear permanently after one death.

## Editions

| Branch | Base game | Android | Purpose |
| --- | --- | --- | --- |
| `adventure-v4` | Shattered Pixel Dungeon 4.0.0 | 5.0+ (API 21+) | Current Adventure build |
| `adventure-saves` | Shattered Pixel Dungeon 3.2.1 | 4.0+ (API 14+) | Legacy build for old devices, including Android 4.4 |

Both editions use the application ID `com.thistleclaw.shatteredpd.adventure`, separate from the official game. Debug builds add the usual `.indev` suffix.

## Save system

Adventure Mode adds a **Saves** entry to the in-game menu:

- **Autosave** — one floor-entry checkpoint. The first completed normal save after entering a floor or side branch becomes the rollback point for that location. It is not overwritten while you remain on the same floor.
- **Manual 1–3** — three manual slots which capture the exact current turn.
- **Load after death** — if the hero dies and a valid Adventure checkpoint exists, the run remains recoverable instead of being permanently discarded.
- **Safe replacement** — checkpoint folders are written through a temporary copy and backup. The code intentionally avoids libGDX `FileHandle.moveTo()` for local Android storage because its directory-copy fallback can add an extra path component.

Starting a genuinely new run in a reused game slot clears checkpoints from the previous run. Finishing/erasing a run clears its rollback data as well.

## Localization

Adventure Mode UI additions ship in English and Russian. Other game languages fall back to the English Adventure strings.

## Android builds

The `Adventure v4 Android build` GitHub Actions workflow produces two APK artifacts:

- `shattered-pd-adventure-v4-debug` — installable development APK, signed with the Android debug key.
- `shattered-pd-adventure-v4-release-unsigned` — optimized release APK produced by the release Gradle variant. It still needs signing with a persistent private Android signing key before public distribution.

A permanent signing key should be kept outside the public repository. Do not commit a keystore or its passwords. See [`docs/adventure-release-signing.md`](docs/adventure-release-signing.md) for the release procedure.

## Upstream and license

Shattered Pixel Dungeon is created by Evan Debenham and is based on Pixel Dungeon by Watabou. Adventure Mode is a downstream modification and is not an official Shattered Pixel Dungeon release.

The code remains licensed under **GPL-3.0-or-later**, matching the upstream project. See [`LICENSE.txt`](LICENSE.txt) and the original project for full licensing and attribution information.
