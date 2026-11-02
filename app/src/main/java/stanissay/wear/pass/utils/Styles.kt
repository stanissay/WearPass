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
    val GRAY = Color(0xFF686D73)
    val BORDER = Color(0xFF242629)
    val TRANSPARENT = Color(0x00000000)
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
    surfaceContainer = MainColors.GRAY,
    onBackground = MainColors.WHITE,
    outline = MainColors.BORDER
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
    val BUTTON_SIZE_L = 36.dp

    val NULL_PADDING = 0.dp
    val MAIN_PADDING = 8.dp
    val SPACE = 4.dp

    val THICKNESS = 1.dp
    val ELEVATION = 8.dp
}

object PaddingStyle {
    val xs = PaddingValues(4.dp)
    val xl = PaddingValues(16.dp)

    val horizontalXs = PaddingValues(horizontal = 6.dp)
    val horizontalS = PaddingValues(horizontal = 8.dp)
    val horizontalXl = PaddingValues(horizontal = 16.dp)

    val verticalXxs = PaddingValues(vertical = 2.dp)
    val verticalXs = PaddingValues(vertical = 4.dp)

    val none = PaddingValues(0.dp)
    val circularBar = PaddingValues(36.dp)
    val scrollBarV = PaddingValues(end = 2.dp)
}