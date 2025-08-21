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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import tech.arcent.stats.StatsScreen
import tech.arcent.addachievement.AddAchievementScreen
import tech.arcent.addachievement.AddAchievementViewModel
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.with
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(vm: HomeViewModel = viewModel()) {
    val state by vm.uiState.collectAsState()
    val systemUi = rememberSystemUiController()
    LaunchedEffect(Unit) {
        systemUi.setStatusBarColor(Color(0xFF1C1C1E), darkIcons = false)
        systemUi.setNavigationBarColor(Color(0xFF2C2C2E), darkIcons = false)
    }
    var selectedTab by remember { mutableStateOf(0) }
    var isAdding by remember { mutableStateOf(false) }
    val addVm: AddAchievementViewModel = viewModel()

    HomeScaffold(selectedTab = selectedTab, onTabChange = { selectedTab = it }, showBottomBar = !isAdding) {
        AnimatedContent(targetState = isAdding, transitionSpec = {
            if (targetState) {
                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it / 2 } + fadeOut()
            } else {
                slideInHorizontally { -it / 2 } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
            }
        }, label = "add_transition") { adding ->
            if (adding) {
                AddAchievementScreen(
                    vm = addVm,
                    onBack = { isAdding = false },
                    onSaved = { vm.addAchievement(it) }
                )
            } else {
                if (selectedTab == 0) WinsContent(state, onAddNew = { isAdding = true }) else StatsScreen()
            }
        }
    }
}

@Composable
private fun HomeScaffold(selectedTab: Int, onTabChange: (Int) -> Unit, showBottomBar: Boolean, content: @Composable () -> Unit) {
    Scaffold(
        containerColor = Color(0xFF1C1C1E),
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigation(selectedIndex = selectedTab, onSelect = onTabChange)
            }
        }
    ) { pv ->
        Box(Modifier.padding(pv)) { content() }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    WinsContent(HomeUiState(achievements = sampleAchievements(), streakDays = 7), onAddNew = {})
}
