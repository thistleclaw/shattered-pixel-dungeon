# Adventure Mode Android release signing

Adventure Mode uses its own Android application ID:

`com.thistleclaw.shatteredpd.adventure`

All public Android releases of this fork must be signed with the **same long-lived release key**. If the key changes, Android will reject an update over an existing installation and users will need to uninstall the old build first.

## Build

```bash
./gradlew android:assembleRelease
```

The unsigned APK is produced under:

```text
android/build/outputs/apk/release/
```

## Sign

Use the Android SDK `apksigner` tool:

```bash
apksigner sign \
  --ks /path/to/adventure-release-keystore.jks \
  --ks-key-alias adventure-release \
  --out Shattered-Pixel-Dungeon-Adventure.apk \
  android/build/outputs/apk/release/android-release-unsigned.apk
```

`apksigner` will ask for the keystore/key password unless they are supplied through a secure local mechanism.

Verify the result before publishing:

```bash
apksigner verify --verbose --print-certs Shattered-Pixel-Dungeon-Adventure.apk
```

Do **not** commit the release keystore or its password to Git. Keep at least two offline backups of the key; losing it means losing the ability to ship installable updates to existing users.
