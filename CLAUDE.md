# Quill - Development Guidelines

## Release Process
- **Release notes must only contain changes from the current version.** Do NOT include changes from previous versions.
- **Always bump `versionCode` and `versionName` in `app/build.gradle.kts` before each release.** Also update the version string in `SettingsScreen.kt` (`stringResource(R.string.settings_version, "x.x.x")`).
- Version number bumps are internal and should NOT be listed in release notes.

## Build
- Build command: `gradle assembleDebug` (use Android Studio's bundled JBR and Gradle wrapper)
- APK output: `app/build/outputs/apk/debug/app-debug.apk`
- Upload releases via `gh release create`

## Architecture
- Kotlin + Jetpack Compose, MVVM + Repository pattern
- Dagger Hilt for DI, Room for database, OkHttp for networking
- MainActivity extends `ComponentActivity` (NOT AppCompatActivity - causes black screen)
- Theme: `android:Theme.Material.Light.NoActionBar`
- Language switching uses `AppCompatDelegate.setApplicationLocales()` without requiring AppCompatActivity
