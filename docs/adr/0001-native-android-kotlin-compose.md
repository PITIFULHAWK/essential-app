# Migrate to Native Android with Jetpack Compose in Subfolder

We decided to migrate the entire application from Flutter to 100% native Android using Kotlin and Jetpack Compose in an `android-app/` subdirectory.
This eliminates engine overhead, delivers true 120Hz native Nothing OS fluidity, and guarantees exact background alarms through Android AlarmManager.
Placing the native Android codebase in `android-app/` leaves the repository root structured cleanly for a future web application.

## Considered Options

- **Flutter at root**: Single cross-platform codebase, but carries a 15-20MB engine overhead and makes future multi-target repo organization cluttered.
- **Pure Native Kotlin in `android-app/`**: Full Android hardware integration, native Room database, zero runtime abstraction layer, and clean directory isolation.

## Consequences

- Android logic resides completely within `android-app/`.
- Flutter root files will be pruned once the native app is verified and installed on the Nothing Phone.
