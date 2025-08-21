package tech.arcent.addachievement

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import tech.arcent.R

private val componentBackground = androidx.compose.ui.graphics.Color(0xFF3B3B3B)

/* Date picker dialog */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAchievementDatePicker(initialMillis: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    Dialog(onDismissRequest = onDismiss) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 3.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(componentBackground)
                    .padding(top = 12.dp, bottom = 8.dp)
            ) {
                DatePicker(
                    state = datePickerState,
                    colors = DatePickerDefaults.colors(
                        containerColor = componentBackground,
                        titleContentColor = LocalContentColor.current,
                        headlineContentColor = LocalContentColor.current
                    )
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text(text = stringResource(id = R.string.add_cancel)) }
                    TextButton(onClick = { datePickerState.selectedDateMillis?.let { onConfirm(it) } }) { Text(text = stringResource(id = R.string.add_ok)) }
                }
            }
        }
    }
}

/* Time picker */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAchievementTimePicker(hour: Int, minute: Int, onDismiss: () -> Unit, onConfirm: (Int, Int) -> Unit) {
    val timeState = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onConfirm(timeState.hour, timeState.minute) }) { Text(stringResource(id = R.string.add_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.add_cancel)) } },
        text = { TimePicker(state = timeState) }
    )
}
