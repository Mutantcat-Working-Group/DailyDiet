# DailyDiet

Android calorie and diet tracker focused on a local-first food database, daily calorie
budget, exercise burn, and a transparent calorie deficit calculation.

## Current scope

- Mifflin-St Jeor BMR and TDEE calculation with deficit safety bounds.
- Energy and mass unit conversion (kcal, kJ, cal, g, kg, oz).
- Preloaded local food cache, with custom foods as fallback.
- Food and exercise logging.
- Pluggable vision model settings for photo-based food estimation.
- GitHub Actions build, unit-test, and APK artifact workflow.

## Build

Local Android SDK and JDK 21 are required. The CI workflow is the canonical build
entry point.

```bash
./gradlew testDebugUnitTest assembleDebug
```

## Versioning

The version format is `MAJOR.MINOR.yyyyMMdd`, for example `1.0.20261005`.
It is defined once in `gradle.properties`:

```properties
dailyDiet.versionName=1.0.20261005
```

`versionCode` is derived as `yyyyMMdd` plus a one-digit same-day revision, so
`1.0.20261005` becomes `202610050`. For a second release on the same day,
override it explicitly:

```properties
dailyDiet.versionCode=202610051
```

Release tags must use the same format with a `v` prefix, for example
`v1.0.20261005`. The tag value overrides `versionName` during the release build,
so the APK and the tag always match.

## Signing for CI

Add these repository secrets to publish a signed release APK on a `v*` tag:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

## Build artifacts

Every push and pull request uploads an Actions artifact containing:

- `DailyDiet-<version>-debug.apk`
- `checksums-md5.txt`
- `checksums-sha1.txt`

Pushing a tag such as `v1.0.20261005`, with the signing secrets configured, builds
the signed release APK and publishes a GitHub release with:

- `DailyDiet-1.0.20261005.apk`
- `checksums-md5.txt`
- `checksums-sha1.txt`

Each checksum file uses the standard `<hash>  <filename>` format and is verified
by the workflow with `md5sum -c` / `sha1sum -c` before upload.

