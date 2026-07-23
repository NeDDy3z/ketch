package com.neddy.ketch.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * One rounded shape family from the M3 redesign (docs/design_document.md):
 * chips and badges are full pills, everything else shares this scale.
 *
 * Component mapping:
 *  - extraSmall -> icon tiles, small inner panels (14dp)
 *  - small      -> filled fields, inner info panels (16dp)
 *  - medium     -> grouped-row containers, buttons (20dp)
 *  - large      -> cards (26dp)
 *  - extraLarge -> sheets, dialogs, hero tiles (32dp)
 */
val KetchShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
