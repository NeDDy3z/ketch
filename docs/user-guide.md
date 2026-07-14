# Ketch User Guide

Ketch notifies you which public transport connection to take when you leave a
place you care about, typically home or work.

## First launch

1. Open the app. The Home screen shows a permissions card.
2. Tap "Grant basic" and allow location and notifications.
3. Tap "Allow background location" and choose "Allow all the time". This is
   required so leave triggers work while the app is closed.
4. Open Settings and paste your Google Maps Platform API key if it was not
   baked into the build.

## Creating a watcher

A watcher describes one commute, for example "Home to work". If you commute
both ways, create two watchers, one for each direction.

1. Open the Watchers tab and tap the add button in the bottom right corner.
2. Fill in the fields:
   - Name: anything that helps you recognize the watcher.
   - Icon: pick an icon for the watcher.
   - Destination stop: search and pick the stop you travel to.
   - Trigger location: the watcher fires when you leave this place. Pick it
     on the map, or tap "Current location" while at home or work to anchor
     it there. The leave radius controls how far you must move before it
     fires. The route itself always starts from wherever you are when the
     watcher fires, so there is no start stop to configure.
   - Active days: the days of the week the watcher is allowed to fire.
   - Time window: the watcher only fires between these times.
   - Limits: optionally cap the number of transfers and the total travel
     time. Connections above the limits are ignored.
   - Notifications: turn the notification for this watcher on or off.
3. Tap Save.

## How notifications work

When a watcher fires, Ketch looks up the current fastest connection from
your current position to the destination stop and posts a notification. The
title is the first boarding, the body continues with the transfers and the
arrival, one per line:

```
16:00 Praha hl.n. (R41) 🚆
16:30 Cesky Brod (660) 🚌
17:00 K.n.C.l, nam.
```

Read it as: board train R41 at Praha hl.n. at 16:00, transfer to bus 660 at
Cesky Brod at 16:30, arrive at Kostelec n.C. lesy at 17:00. The emoji shows
the vehicle type of each boarding. Walking to and from stops is never shown.

A watcher fires at most once per day. After a notification it stays quiet
until the next day, so you do not get duplicates while moving around the
trigger area.

## Home screen

The Home screen shows the current fastest connection for every enabled
watcher, refreshed on demand with the refresh button. Watchers whose trigger
location is closest to you are listed first. While lookups run you see
loading placeholders.

## Watchers list

The Watchers tab lists all watchers. From here you can:

- Toggle a watcher on or off with the switch
- Delete a watcher with the trash icon
- Tap a watcher to edit it

## Settings

- Theme: light, dark, or follow the system.
- API key: the Google Maps Platform key used for lookups.
- New watcher defaults: active days, time window, and leave radius that
  prefill the editor when you create a new watcher.

## Tips

- For "leaving work" and "leaving home" directions of the same commute,
  create two watchers and anchor each trigger location with "Use current
  location" while you are there.
- If notifications do not arrive, check that background location is set to
  "Allow all the time" and that battery optimization is not restricting the
  app.
