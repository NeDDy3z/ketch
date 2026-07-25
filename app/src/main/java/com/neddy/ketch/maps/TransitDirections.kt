package com.neddy.ketch.maps

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.neddy.ketch.domain.model.StopPlace

/**
 * Hands a watcher route over to Google Maps as public transport directions.
 *
 * The origin is deliberately left out so Maps routes from the current
 * position, the same way the in-app lookup does.
 */
object TransitDirections {

    private const val GOOGLE_MAPS_PACKAGE = "com.google.android.apps.maps"

    /**
     * Directions URL with the destination pinned to the watcher stop and the
     * travel mode fixed to public transport. Coordinates are used instead of
     * the stop name so the pin lands where the lookup routes to, and the
     * origin is left out so Maps starts from the current position.
     *
     * The legacy `daddr`/`dirflg=r` form is deliberate: the Maps app ignores
     * `travelmode=transit` on the newer `maps/dir/?api=1` links and opens in
     * driving mode, while `dirflg=r` reliably lands on public transport.
     */
    fun url(destination: StopPlace): String =
        "https://www.google.com/maps?daddr=" +
            "${destination.latitude},${destination.longitude}&dirflg=r"

    /** Opens the directions, preferring Google Maps over other map apps. */
    fun open(context: Context, destination: StopPlace) {
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(url(destination)))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // Google Maps is the only app guaranteed to honour travelmode=transit,
        // so try it first and fall back to whatever handles maps links.
        try {
            context.startActivity(Intent(view).setPackage(GOOGLE_MAPS_PACKAGE))
        } catch (e: ActivityNotFoundException) {
            try {
                context.startActivity(view)
            } catch (e2: ActivityNotFoundException) {
                Toast.makeText(context, "No maps app installed", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
