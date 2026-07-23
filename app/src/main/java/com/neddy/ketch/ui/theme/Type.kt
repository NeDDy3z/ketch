package com.neddy.ketch.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight

// Start from the Material 3 default type scale (correct Roboto metrics, sizes
// and tracking) and lean into a more expressive, modern feel by emphasizing the
// weight of headlines, titles and labels. This lets screens rely on the scale
// instead of sprinkling manual FontWeight.Bold overrides everywhere.
private val Default = Typography()

val Typography = Default.copy(
    headlineLarge = Default.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
    headlineMedium = Default.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
    headlineSmall = Default.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
    titleLarge = Default.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = Default.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = Default.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Default.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = Default.labelMedium.copy(fontWeight = FontWeight.SemiBold),
)
