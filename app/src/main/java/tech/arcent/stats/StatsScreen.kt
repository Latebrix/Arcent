package tech.arcent.stats

/*
 * Stats screen: simple placeholder + diagnostic actions (test crash, non fatal) and a demo counter
 * whose increments add breadcrumbs for Sentry Session Replay correlation.
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import tech.arcent.R

@Composable
fun StatsScreen(vm: StatsViewModel = hiltViewModel()) {
    val counter by vm.counter.collectAsState()
    val nonFatalMsg = stringResource(id = R.string.sentry_test_nonfatal_event)
    StatsScreenContent(
        counter = counter,
        onIncrement = { vm.increment() },
        onTestCrash = { vm.testCrash() },
        onTestNonFatal = { vm.testNonFatal(nonFatalMsg) },
    )
}

@Composable
private fun StatsScreenContent(
    counter: Int,
    onIncrement: () -> Unit,
    onTestCrash: () -> Unit,
    onTestNonFatal: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(id = R.string.stats_placeholder),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(id = R.string.stats_counter, counter),
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onIncrement) { Text(stringResource(id = R.string.stats_increment)) }
            Button(onClick = onTestCrash) { Text(stringResource(id = R.string.sentry_test_crash)) }
            Button(onClick = onTestNonFatal) { Text(stringResource(id = R.string.sentry_test_nonfatal)) }
        }
    }
}

@Preview
@Composable
private fun StatsScreenPreview() {
    StatsScreenContent(counter = 3, onIncrement = {}, onTestCrash = {}, onTestNonFatal = {})
}
