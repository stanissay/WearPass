/*
 * KeePass manager for Wear OS
 * Copyright (C) 2026 [stanissay]
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */

package stanissay.wear.pass.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedTextStyle
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.Shapes
import androidx.wear.compose.material3.Typography

object MainColors {
    val BACKGROUND = Color(0xFF000000)
    val FIRST_ACCENT = Color(0xFF527F89)
    val SECOND_ACCENT = Color(0xFF3E4348)
    val WHITE = Color(0xFFD5D8DC)
    val TRANSPARENT = Color(0x00000000)
    val NEGATIVE = Color(0xFF6C0808)
}

val TypographyStyle = Typography(
    arcMedium = CurvedTextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 12.sp
    ),

    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 14.sp
    ),

    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 13.sp
    ),

    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 12.sp
    ),

    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontSize = 10.sp
    )
)

val ColorStyle = ColorScheme(
    primary = MainColors.FIRST_ACCENT,
    secondary = MainColors.SECOND_ACCENT,
    background = MainColors.BACKGROUND,
    onBackground = MainColors.WHITE
)

val ShapesStyle = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

object UiConstants {
    val BASE_SIZE = 36.dp
    val BASE_SIZE_XXS = 8.dp

    val BUTTON_SIZE_S = 16.dp
    val BUTTON_SIZE = 32.dp

    val SPACE = 4.dp
    val THICKNESS = 1.dp

    val paddingH = PaddingValues(horizontal = SPACE)
}