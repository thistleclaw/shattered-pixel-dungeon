# Shattered Pixel Dungeon — Adventure Mode

This branch is based on **v3.2.1**, the last Shattered Pixel Dungeon release that supports Android 4.0–4.4.

## What changed

- Separate Android application ID: `com.thistleclaw.shatteredpd.adventure`
- User-visible name: **Shattered Pixel Dungeon Adventure**
- Automatic rollback checkpoint on the first normal save of every floor/branch
- Three manual save slots
- Save/load menu available from the in-game menu, including after a normal permadeath
- Checkpoints are stored outside `gameN/`, so Shattered's normal death cleanup does not delete them

The auto checkpoint is intentionally not overwritten by ordinary saves on the same floor. Moving to a different floor or quest branch creates a new auto checkpoint.

## Build Android debug APK

Requires JDK 17.

```bash
git clone -b adventure-saves https://github.com/thistleclaw/shattered-pixel-dungeon.git
cd shattered-pixel-dungeon
./gradlew android:assembleDebug
```

APK output:

```text
android/build/outputs/apk/debug/
```

The debug build has an `.indev` application-ID suffix, so it can be installed separately from both official Shattered Pixel Dungeon and a future release build of this fork.

## Notes

This is an Adventure Mode fork rather than an attempt to preserve traditional roguelike permadeath. The normal dungeon generation, enemies, loot, hunger, bosses and combat balance are unchanged; only rollback saves are added.
