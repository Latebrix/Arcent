package tech.arcent.addachievement

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.arcent.R
import kotlinx.coroutines.launch

/* bottom sheet displaying achievement tips */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAchievementTipsSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF2E2E2E),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = stringResource(id = R.string.tips_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            val tips = listOf(
                stringResource(id = R.string.tip_1),
                stringResource(id = R.string.tip_2),
                stringResource(id = R.string.tip_3),
                stringResource(id = R.string.tip_4),
                stringResource(id = R.string.tip_5)
            )
            tips.forEachIndexed { idx, tip ->
                Surface(
                    color = Color(0xFF3A3A3A),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${idx + 1}. $tip",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { scope.launch { sheetState.hide(); onDismiss() } },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B8D7D))
            ) {
                Text(
                    text = stringResource(id = R.string.add_add_mine),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
