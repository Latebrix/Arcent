package tech.arcent.stats

// stats screen placeholder -_-

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tech.arcent.R
import tech.arcent.crash.CrashReporting

@Composable
fun StatsScreen() {
    val nonFatalMsg = stringResource(id = R.string.sentry_test_nonfatal_event)
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = stringResource(id = R.string.stats_placeholder),
                style = MaterialTheme.typography.headlineSmall,
            )
            Button(onClick = {
                try {
                    throw RuntimeException("Manual test crash")
                } catch (e: Exception) {
                    CrashReporting.capture(e)
                }
            }) {
                Text(stringResource(id = R.string.sentry_test_crash))
            }
            Button(onClick = {
                CrashReporting.nonFatal(nonFatalMsg)
            }) {
                Text(stringResource(id = R.string.sentry_test_nonfatal))
            }
        }
    }
}

@Preview
@Composable
private fun StatsScreenPreview() {
    StatsScreen()
}
