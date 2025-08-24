package tech.arcent.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import tech.arcent.R
import java.text.DateFormat
import java.util.Date

/*
 achievementItem used by home, search, and all screens
 */
@Composable
internal fun AchievementItem(
    achievement: Achievement,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Surface(
        color = Color(0xFF2C2C2E),
        shape = RoundedCornerShape(16.dp),
        onClick = { onClick?.invoke() },
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val placeholderRes = R.drawable.home_item_placeholder
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp)),
            ) {
                if (achievement.photoUrl != null) {
                    AsyncImage(
                        model = achievement.photoUrl,
                        contentDescription = achievement.title,
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = placeholderRes),
                        error = painterResource(id = placeholderRes),
                        fallback = painterResource(id = placeholderRes),
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Image(
                        painter = painterResource(id = placeholderRes),
                        contentDescription = achievement.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(achievement.title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(4.dp))
                Text(formatTimestamp(achievement.achievedAt), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    achievement.tags.forEach { tag ->
                        AchievementTag(tag)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}

private fun formatTimestamp(epoch: Long): String {
    val df = DateFormat.getDateInstance(DateFormat.SHORT)
    val tf = DateFormat.getTimeInstance(DateFormat.SHORT)
    val d = Date(epoch)
    return df.format(d) + " • " + tf.format(d)
}

@Composable
internal fun AchievementTag(text: String) {
    Surface(
        color = Color(0xFF3A3A3C),
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}
