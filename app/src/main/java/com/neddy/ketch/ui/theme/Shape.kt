package com.neddy.ketch.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * A rounded, expressive shape scale. Everything in the app reads as rounded so
 * inputs, chips, cards and buttons live in the same visual family — no square
 * corners next to pill shaped controls.
 *
 * Component mapping (Material 3 defaults):
 *  - extraSmall -> text fields, menus, snackbars
 *  - small      -> chips
 *  - medium     -> cards
 *  - large      -> FABs, navigation drawer
 *  - extraLarge -> dialogs, bottom sheets, map preview
 */
val KetchShapes = Shapes(
    extraSmall = RoundedCornerShape(16.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
