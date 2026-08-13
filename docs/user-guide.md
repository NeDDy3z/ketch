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
   - Icon: pick an icon for the watcher. Besides the plain places and
     vehicles there are combinations such as "home by car" or "work on
     foot", which show the place with the transport badged over it.
   - Destination stop: search and pick the stop you travel to.
   - Trigger location: the watcher fires when you leave this place. Pick it
     on the map, or tap "Current location" while at home or work to anchor
     it there. The leave radius controls how far you must move before it
     fires. The route itself always starts from wherever you are when the
     watcher fires, so there is no start stop to configure.
   - Car start: optional. Search or pick the place you park at, for example
     a park and ride. When you leave the trigger fast enough to be driving
     (see the "Driving above" setting), Ketch routes from there instead of
     from your door. Leave it empty and every departure is treated as a
     walk to the nearest stop. Use "Clear" to remove it again.
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
- Tap a watcher to edit it.
- Long press a watcher to open its details.
- Double tap a watcher to open the route in Google Maps as public transport
  directions to its destination.
- Flip a watcher's switch to enable or disable it without opening the editor.
- Add a watcher with the button in the bottom right corner.

### Watcher details

Long pressing a watcher opens its details page:

- The connection it would notify you about if you left now.
- Under it, a quicker connection when one exists: a departure later than the
  first one that spends less time travelling. The subtitle says how much
  later it leaves and how much shorter it is, so waiting is a clear choice.
- Everything the watcher is set to: destination, active days and window, car
  start, preferred vehicle and limits.
- Edit and Delete at the bottom. Delete asks for confirmation here, since
  there is no undo bar behind it.

The refresh icon in the top right looks the connections up again.

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
widget should show; you can pick several. Resize the widget by long pressing
it and dragging its handles — the layout follows the size you give it.

The widget shows one watcher at a time: its name and the current fastest
connection, one boarding per line. The chevrons at the bottom move to the
previous or next watcher and the dots between them show where you are; tap a
dot to jump straight to that watcher. The circle button in the top right
corner re-fetches all connections, the logo tile in the top left reopens the
widget configuration, and tapping the connection opens Ketch.

## Settings

Each group of settings sits in its own category card:

- Color palette: Ketch is dark only, and the palette sets its tones.
- Journey:
  - "Walk faster than the map thinks" takes a percentage off the walking
    time the routing provider assumes, so a connection you can still catch
    is not written off as unreachable. 10% by default, off at 0%. It costs a
    second lookup per watcher, because the provider has to be asked again
    from an earlier departure.
  - "Driving above" is the speed from which a leave counts as a car journey
    rather than a walk. Above it, a watcher with a car start point routes
    from there.
- Gestures: whether a double tap opens the route in Google Maps.
- Refresh: whether pull to refresh looks up every watcher or only the ones
  inside their active day and window.
- Updates: whether Ketch watches its own GitHub release page, a "Check now"
  button, and a link to the latest release.
- API key: the Google Maps Platform key used for lookups.
- New watcher defaults: active days, time window, and leave radius that
  prefill the editor when you create a new watcher.

## Updates

Ketch is installed from an APK, not a store, so it watches its own GitHub
release page. A few times a day it asks GitHub for the newest release; when
that release is newer than the build you are running, a dialog appears on the
Home screen:

- **Update** opens the new APK download in your browser. Open the downloaded
  file to install it over the current version; your watchers and settings are
  kept.
- **Later** hides the dialog for a day.
- **Don't remind me** stops the checks entirely. Settings → Updates turns
  them back on, and it asks again straight away.

Settings → Updates also has a "Check now" button and links to the latest
release. Help → Updates links to the latest release and the full history.

## Tips

- For "leaving work" and "leaving home" directions of the same commute,
  create two watchers and anchor each trigger location with "Use current
  location" while you are there.
- If notifications do not arrive, check that background location is set to
  "Allow all the time" and that battery optimization is not restricting the
  app.
