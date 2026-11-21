package app.clearsms.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Radii by size, not one pill for everything: controls 6 to 10, cards 12,
 * sheets and dialogs 20. The previous 28dp large/extraLarge turned every
 * sheet, dialog and floating action button into a pill, which is the shape
 * Material reaches for when nobody chooses one.
 */
val ClearSmsShapes =
    Shapes(
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(10.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(20.dp),
    )
