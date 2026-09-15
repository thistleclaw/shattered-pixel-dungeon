# Shattered Pixel Dungeon: Adventure — Legacy

This branch is the **Android 4.x edition** of Adventure Mode. It is based on **Shattered Pixel Dungeon 3.2.1**, the last upstream line used by this fork that runs on Android 4.0–4.4.

For the current Shattered Pixel Dungeon 4.0.0 edition, use the [`adventure-v4`](https://github.com/thistleclaw/shattered-pixel-dungeon/tree/adventure-v4) branch.

## Adventure save system

- **Autosave** — one rollback checkpoint created on the first completed normal save after entering each floor or quest branch.
- **Manual 1–3** — three manual slots that preserve the exact current turn.
- **Recovery after death** — a valid Adventure checkpoint is kept recoverable after normal permadeath.
- **Separate storage** — checkpoints live outside `gameN/`, so Shattered's normal run cleanup does not destroy them.
- **Safe replacement** — checkpoint folders are copied through temporary/backup directories. The implementation deliberately avoids libGDX `FileHandle.moveTo()` on Android local storage because its directory fallback can nest the source folder inside the destination.

The ordinary dungeon generation, enemies, loot, hunger, bosses and combat balance are unchanged. Adventure Mode only adds rollback saves.

## Android identity

- App name: **Shattered Pixel Dungeon Adventure**
- Application ID: `com.thistleclaw.shatteredpd.adventure`
- Current legacy version: `3.2.1-adventure5`
- Minimum Android: **4.0 / API 14**

Debug builds add the usual `.indev` suffix to the application ID.

## Builds

The `Adventure legacy Android build` workflow produces:

- `shattered-pd-adventure-legacy-debug` — installable development APK.
- `shattered-pd-adventure-legacy-release-unsigned` — optimized release variant which still needs a persistent Android signing key before public distribution.

Local build:

```bash
git clone -b adventure-saves https://github.com/thistleclaw/shattered-pixel-dungeon.git
cd shattered-pixel-dungeon
./gradlew android:assembleDebug android:assembleRelease
```

## Upstream and license

Shattered Pixel Dungeon is created by Evan Debenham and is based on Pixel Dungeon by Watabou. Adventure Mode is an unofficial downstream modification.

The code remains licensed under **GPL-3.0-or-later**. See [`LICENSE`](LICENSE).
