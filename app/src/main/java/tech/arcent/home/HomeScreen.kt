package tech.arcent.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.tooling.preview.Preview
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.Surface
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import tech.arcent.stats.StatsScreen

@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val systemUi = rememberSystemUiController()
    LaunchedEffect(Unit) {
        systemUi.setStatusBarColor(Color(0xFF1C1C1E), darkIcons = false)
        systemUi.setNavigationBarColor(Color(0xFF2C2C2E), darkIcons = false)
    }
    var selectedTab by remember { mutableStateOf(0) }
    HomeScaffold(selectedTab = selectedTab, onTabChange = { selectedTab = it }) {
        if (selectedTab == 0) WinsContent(state) else StatsScreen()
    }
}

@Composable
private fun HomeScaffold(selectedTab: Int, onTabChange: (Int) -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        containerColor = Color(0xFF1C1C1E),
        bottomBar = {
            AppBottomNavigation(selectedIndex = selectedTab, onSelect = onTabChange)
        }
    ) { pv ->
        Box(Modifier.padding(pv)) { content() }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    WinsContent(HomeUiState(achievements = sampleAchievements(), streakDays = 7))
}
