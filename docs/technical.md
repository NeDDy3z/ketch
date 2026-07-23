# Ketch Technical Documentation

## Overview

Ketch is a single module Android app written in Kotlin with Jetpack Compose
and Material 3 (Material You). It targets Android 10+ (minSdk 29, targetSdk
36) and follows a lightweight layered architecture:

```
ui  ->  domain  <-  data
        ^
        |
     trigger (background)
```

- `ui` contains Compose screens, view models, navigation, and theming.
- `domain` contains pure models and logic (connection selection, formatting).
- `data` contains persistence (Room, DataStore) and remote transit providers.
- `trigger` contains everything that runs in the background: geofencing,
  time scheduling, WorkManager workers, and notifications.
- `di` contains `AppContainer`, a manual dependency container exposed through
  the `Application` subclass. The app deliberately avoids a DI framework to
  keep the build simple; the container centralizes construction so a
  framework could be introduced later without redesigning the code.

## Data layer

### Local persistence

- `KetchDatabase` (Room) stores watchers in the `watchers` table via
  `WatcherEntity` and `WatcherDao`. Mapping to the domain `Watcher` model is
  done by extension functions in `WatcherEntity.kt`. Active days are stored
  as comma separated ISO day numbers. Schema version 3 adds the preference
  and ordering columns with a `MIGRATION_2_3` that preserves existing rows;
  destructive fallback remains only as a safety net for other version gaps.
  `WatcherDao.applyOrder` writes a full reordering in one transaction.
- `SettingsRepository` (Preferences DataStore) stores the theme mode, the
  runtime API key override, and the defaults applied to new watchers.

### Transit provider

`TransitRepository` is the provider abstraction with two operations:

- `findConnections(origin, destination, departureTime)` returns alternative
  transit connections.
- `searchStops(query)` resolves free text to public transport stops.

`GoogleTransitRepository` implements it against two Google Maps Platform
services using Retrofit with kotlinx.serialization:

- Routes API v2 (`POST /directions/v2:computeRoutes`) with
  `travelMode=TRANSIT` and `computeAlternativeRoutes=true`. Only the fields
  named in the `X-Goog-FieldMask` header are returned. Transit steps are
  mapped to `TransitLeg` objects; walking steps are dropped.
- Places API New (`POST /v1/places:searchText`) restricted to
  `transit_station` results for the stop pickers.

The API key resolves at call time from DataStore, falling back to the
`GOOGLE_MAPS_API_KEY` value from `local.properties` compiled into
`BuildConfig.MAPS_API_KEY`. A missing key raises `MissingApiKeyException`
which surfaces in the UI.

The abstraction exists so a regional provider (for example Golemio for
Prague/PID data, which offers realtime vehicle positions and closures) can be
added later. The planned realtime disruption handling would plug in behind
the same interface.

## Domain layer

- `Watcher` models one configured commute, including the icon key, trigger
  location, active days, time window, optional limits, an optional preferred
  vehicle with a travel delta, and a `sortOrder` for the home ordering.
  `Watcher.isActiveAt` implements the day and window check applied when a
  trigger fires. Routes have no configured start; they begin at the current
  device position.
- `TransitConnection` and `TransitLeg` model a connection as transit
  boardings only. Transfers and total duration are derived properties.
  `VehicleCategory` collapses the many provider vehicle type strings into the
  handful of categories the user can prefer; `TransitLeg.category` and
  `TransitConnection.usesCategory` expose it.
- `ConnectionSelector.selectBest` filters connections by the optional limits
  and picks the earliest arrival, breaking ties by fewer transfers. When a
  `preferredVehicle` is set it prefers a connection using that category, but
  only while it is not slower than the fastest by more than
  `maxTravelDeltaMinutes`; past that gap the faster connection wins.
- `TriggerConfirmation.exitConfirmed` decides whether a geofence exit is real
  given the distance to the trigger center and the fix accuracy, dropping an
  exit only when the whole uncertainty circle sits inside the radius, and
  trusting the geofence when the fix is coarse or unavailable so a real
  departure is never missed.
- `ConnectionFormatter` renders the notification: the title is the first
  boarding as `"emoji stop (line) time"`, the body continues with the
  remaining boardings and the arrival, one per line in the expanded view.
  The emoji is derived from the transit vehicle type.

## Trigger layer

### Location trigger

All watchers are location triggered. The trigger location is picked on a map
(Maps SDK for Android via maps-compose) or set to the current position.

- `GeofenceManager.sync` replaces all registered geofences with one geofence
  per enabled watcher (request id `watcher_{id}`). Each tracks both ENTER and
  EXIT with an immediate notification responsiveness, and registration seeds
  the inside state with an initial ENTER trigger; tracking entry as well as
  exit makes a lone exit far more reliable. Registration is skipped without
  background location permission.
- `GeofenceBroadcastReceiver` receives exit transitions and enqueues a
  `ConnectionLookupWorker` per affected watcher. Entry transitions are used
  only to keep the platform's inside/outside state warm.

### Lookup worker

`ConnectionLookupWorker`:

1. Loads the watcher and validates enabled state, notification toggle, and
   the day/time window.
2. Fires at most once per window occurrence:
   `Watcher.hasFiredInCurrentWindow` gates on whether `lastTriggeredAt`
   falls after the current window opened, so the gate resets at the next
   window start rather than after a full day.
3. Confirms the exit with `TriggerConfirmation.exitConfirmed` using a fresh
   fix, so a spurious exit while still sitting inside the radius is dropped.
4. Resolves the route origin from the current device position, falling back
   to the trigger location when no fix is available.
4. Fetches connections, selects the best one, formats it, and posts a high
   priority notification via `NotificationHelper`.
5. Marks the watcher as triggered. Network failures retry up to 3 times.

### Resilience

- `BootCompletedReceiver` enqueues `TriggerSyncWorker` after reboot because
  neither geofences nor scheduled work survive one.
- `TriggerSyncWorker` re-syncs geofences from the database. It also runs
  after every watcher create, update, delete, or toggle.

## UI layer

- `MainActivity` installs the splash screen, observes the theme mode, and
  hosts `KetchApp`: a `Scaffold` with a Material 3 `NavigationBar` (Home,
  Settings) and a `NavHost` that also contains the editor route.
- Home (`HomeScreen`, `HomeViewModel`): permission onboarding cards, then
  every watcher in the user defined `sortOrder`, each with its current
  fastest connection (disabled watchers show a muted resting card). Each card
  carries an enable/disable switch that saves and re-syncs geofences in place.
  Pull to refresh re-runs the lookups. A FAB adds a watcher and a tap or long
  press (per the edit gesture setting) opens one for editing. Reordering only
  reshuffles cards without re-running lookups. The top right tools
  menu switches the list into a reorder mode (drag handle plus up and down
  arrows, persisted through `WatcherRepository.reorder`) or a delete mode
  (per row checkboxes and a red delete button); in either mode the menu
  becomes a cancel button. Shows skeleton cards while loading.
- Editor (`WatcherEditScreen`, `WatcherEditViewModel`): icon picker,
  debounced destination search (400 ms, minimum 3 characters), full screen
  map picker dialog for the trigger location, day chips, Material 3 time
  pickers, limit fields, a preferred vehicle chooser with its travel delta,
  and enabled and notifications toggles. Search and lookup failures are
  translated to actionable messages by `userMessageFor`. Validation errors
  are shown inline.
- Settings (`SettingsScreen`, `SettingsViewModel`): one category card per
  group, holding the theme selector, the edit gesture (tap or hold), the API
  key field, and the new watcher defaults.
- Theming: dynamic color on Android 12+, otherwise a teal and amber fallback
  palette, with light, dark, and system modes.

## Permissions

| Permission | Reason |
| --- | --- |
| `INTERNET` | Routes and Places API calls |
| `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` | Current position for the Home screen and trigger anchoring |
| `ACCESS_BACKGROUND_LOCATION` | Geofence exit triggers while the app is closed |
| `POST_NOTIFICATIONS` | Connection notifications (Android 13+) |
| `RECEIVE_BOOT_COMPLETED` | Re-registering triggers after reboot |

## Build

- Gradle 9.3.1, AGP 9.1.1, Kotlin 2.2.10 with built in Kotlin support,
  KSP for Room, kotlinx.serialization for JSON.
- Version catalog in `gradle/libs.versions.toml`.
- `GOOGLE_MAPS_API_KEY` in `local.properties` becomes
  `BuildConfig.MAPS_API_KEY`.
- Release builds enable code and resource shrinking.

## Testing

Unit tests cover the pure logic:

- `ConnectionFormatterTest` verifies the notification line format.
- `ConnectionSelectorTest` verifies best connection selection, limits, and the
  preferred vehicle logic with its travel delta.
- `TriggerConfirmationTest` verifies the geofence exit confirmation.
- `WatcherTest` verifies the fire once per window gate.

Run with `./gradlew test`.

## Future work

- Realtime tracking of closures and disruptions on watched routes, acting on
  them by re-planning and re-notifying. The `TransitRepository` abstraction
  and the worker pipeline are the intended integration points.
- Map based trigger location picking.
- Per watcher notification channels.
