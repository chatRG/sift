package app.clearsms.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Fixed semantic colors for money amounts: debit is always red, credit is
 * always green and a balance-only figure is always blue.
 *
 * These deliberately bypass [androidx.compose.material3.MaterialTheme]'s
 * `colorScheme`: Material You derives every scheme role from the user's
 * wallpaper, so `error`/`tertiary`/`primary` can land on hues that no longer
 * read as red/green/blue. Financial direction must not shift meaning with
 * the wallpaper, so these values are constants - only a light/dark variant
 * exists, chosen for WCAG AA (≥ 4.5:1) contrast against the app's light and
 * dark surfaces and the system notification shade.
 *
 * The same values are duplicated as `amount_debit` / `amount_credit` /
 * `amount_balance` in `res/values/colors.xml` (+ `values-night`) for the
 * custom notification layout; a unit test keeps both copies identical.
 */
@Immutable
data class SemanticAmountColors(
    val debit: Color,
    val credit: Color,
    val balance: Color,
) {
    companion object {
        /** Variant for light surfaces (paper background and white-ish shade). */
        val Light =
            SemanticAmountColors(
                debit = Color(0xFFA33A2B),
                credit = Color(0xFF2F6B3F),
                balance = Color(0xFF33577D),
            )

        /** Variant for dark surfaces (ink background and dark shade). */
        val Dark =
            SemanticAmountColors(
                debit = Color(0xFFF0A79C),
                credit = Color(0xFFA9CFA4),
                balance = Color(0xFFA9C4E8),
            )
    }
}

/**
 * The resolved [SemanticAmountColors] for the current theme; provided by
 * [ClearSmsTheme] (dark variant whenever the theme itself is dark).
 */
val LocalSemanticAmountColors = staticCompositionLocalOf { SemanticAmountColors.Light }
