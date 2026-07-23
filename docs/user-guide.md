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

1. On the Home screen, tap the add button in the bottom right corner.
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
   - Preferred connection: optionally prefer a vehicle type (train, bus,
     tram, metro, ferry). Ketch then picks a connection that uses that
     vehicle instead of the plain fastest one. Set "Max extra minutes vs.
     fastest" to cap how much slower the preferred connection may be before
     the fastest one is used instead; leave it empty to always prefer.
   - Enabled: turn the whole watcher on or off.
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

A watcher fires at most once per time window. After a notification it stays
quiet until the next window opens, so you do not get duplicates while moving
around the trigger area.

## Home screen

The Home screen lists all your watchers in your own order, each showing its
current fastest connection. While lookups run you see loading placeholders.
Disabled watchers appear as a muted resting card.

- Pull down to refresh all connections.
- Tap (or long press, depending on the Settings option) a watcher to edit it.
- Flip a watcher's switch to enable or disable it without opening the editor.
- Add a watcher with the button in the bottom right corner.

### Reordering and deleting

The three dots in the top right corner open the special tools:

- **Reorder**: the list switches to compact rows. Drag the handle on the
  right, or use the up and down arrows, to move a watcher. The three dots
  become a cancel button; tap it when you are done.
- **Delete**: each watcher gets a checkbox. Tick the ones you want to remove
  and tap the red Delete button at the bottom. The three dots become a
  cancel button to back out without deleting.

## Home screen widget

Long press your launcher home screen, open the widgets list, and drop the
Ketch widget. A configuration screen opens where you tick the watchers the
widget should show; you can pick several. The widget lists each selected
watcher with its name and the current fastest connection, scrollable when
they do not fit. The circle button in the top right corner re-fetches all
connections. Tapping a row opens Ketch.

## Settings

Each group of settings sits in its own category card:

- Theme: light, dark, or follow the system.
- Editing: whether tapping or holding a watcher on the Home screen opens it
  for editing.
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
