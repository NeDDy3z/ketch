# Ketch

![Platform](https://img.shields.io/badge/platform-Android-3DDC84)
![Language](https://img.shields.io/badge/language-Kotlin-7F52FF)
![UI](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4)
![Design](https://img.shields.io/badge/design-Material%20You-6750A4)
![Min SDK](https://img.shields.io/badge/minSdk-29%20(Android%2010)-blue)
![License](https://img.shields.io/badge/license-private-lightgrey)

`platform: android` `language: kotlin` `ui: jetpack-compose` `design: material-you` `data: room` `scheduling: workmanager` `location: geofencing` `api: google-routes` `api: google-places`

Ketch is a simple private Android app that tells you which public transport
connection to catch the moment you leave home, work, or any other place you
configure. You set up a watcher with a destination stop, a trigger location,
and a time window. When you leave that location inside the window, Ketch looks
up the current fastest connection and sends a notification like:

```
16:00 Praha hl.n. (R41) 🚆
16:30 Cesky Brod (660) 🚌
17:00 K.n.C.l, nam.
```

The title is the nearest boarding stop with its line code, departure time,
and vehicle emoji. The body continues with transfers and ends with the final
stop and arrival time. Walking segments are always excluded.

## Features

- Watchers with a destination stop, active days, and a daily time window
- Leave trigger: a geofence exit around a location picked on a map or set to
  your current position; the route always starts from where you are.
  Transitions are confirmed against a fresh fix so GPS jitter does not fire a
  false departure
- Per watcher icon picker
- Optional limits per watcher: maximum transfers and maximum travel time
- Preferred connection per watcher: pick a vehicle type (train, bus, tram,
  metro, ferry) and Ketch chooses a connection using it, unless it is slower
  than the fastest by more than a configurable number of minutes
- Home screen listing every watcher with its current fastest connection, in
  your own order: pull to refresh, drag or arrow to reorder, and multi select
  to delete from the top right tools menu
- Home screen widget showing live connections for selected watchers with a
  manual refresh button
- Material You design with dynamic colors, light, dark, and system themes
- Skeleton loading states while lookups run
- Local persistence with Room, settings in DataStore
- Configurable defaults for new watchers

## Requirements

- Android 10 (API 29) or newer
- A Google Maps Platform API key with the Routes API, Places API (New), and
  Maps SDK for Android enabled (the SDK powers the trigger location picker)

## Setup

1. Clone the project and open it in Android Studio.
2. Provide the API key one of two ways:
   - Add `GOOGLE_MAPS_API_KEY=your_key_here` to `local.properties`, or
   - Build the app and paste the key into Settings at runtime.
3. Build and run the `app` configuration.

On first launch, grant the requested permissions from the Home screen:
location, background location (required for leave triggers), and
notifications.

## Documentation

- [User guide](docs/user-guide.md)
- [Technical documentation](docs/technical.md)

## Project layout

```
app/src/main/java/com/neddy/ketch/
  data/       Room database, DataStore settings, transit API clients, location
  domain/     Models, connection selection and formatting logic
  trigger/    Geofencing, time scheduling, WorkManager workers, notifications
  ui/         Compose screens, navigation, theme, shared components
  di/         Manual dependency container
docs/         User and technical documentation
```
