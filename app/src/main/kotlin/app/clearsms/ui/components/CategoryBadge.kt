package app.clearsms.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.clearsms.domain.model.Category
import app.clearsms.ui.theme.ClearSmsTheme

/**
 * Tray marking for a message's category: a 6dp ink dot and the category's
 * name, set in muted text. No filled container and no second palette.
 *
 * The old badge painted Important in the primary container, OTP in tertiary
 * and everything else in secondary, so a list of twenty rows carried three
 * competing tints before the sender names had a chance to read. The dot is
 * the only colour left, and it is small enough to scan without shouting.
 */
@Composable
fun CategoryBadge(
    category: Category,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(categoryDotColor(category)),
        )
        Text(
            text = category.displayName(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The one dot colour each category earns; everything else stays muted. */
@Composable
private fun categoryDotColor(category: Category): Color =
    when (category) {
        Category.IMPORTANT -> MaterialTheme.colorScheme.primary
        Category.OTP -> MaterialTheme.colorScheme.tertiary
        Category.PERSONAL -> MaterialTheme.colorScheme.secondary
        Category.SPAM -> MaterialTheme.colorScheme.error
        Category.PROMOTIONAL, Category.UNKNOWN -> MaterialTheme.colorScheme.outline
    }

@Preview
@Composable
private fun CategoryBadgePreview() {
    ClearSmsTheme {
        CategoryBadge(category = Category.IMPORTANT)
    }
}
