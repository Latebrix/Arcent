package tech.arcent.home

/*
 HomeScreen orchestrates navigation between main wins, add, all, search, and stats tabs.
 */

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import tech.arcent.addachievement.AddAchievementScreen
import tech.arcent.addachievement.AddAchievementViewModel
import tech.arcent.home.simple.SimpleAllScreen
import tech.arcent.home.simple.SimpleSearchScreen
import tech.arcent.stats.StatsScreen

private enum class HomeMode { MAIN, ADD, ALL, SEARCH }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(vm: HomeViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsState()
    val systemUi = rememberSystemUiController()
    var mode by remember { mutableStateOf(HomeMode.MAIN) }
    var selectedTab by remember { mutableStateOf(0) }
    val addVm: AddAchievementViewModel = hiltViewModel()

    LaunchedEffect(mode, selectedTab) {
        // Update system bar colors based on current mode
        val navColor = if (mode == HomeMode.ALL || mode == HomeMode.SEARCH) Color(0xFF1C1C1E) else Color(0xFF2C2C2E)
        systemUi.setStatusBarColor(Color(0xFF1C1C1E), false)
        systemUi.setNavigationBarColor(navColor, false)
    }

    BackHandler(enabled = true) {
        when {
            selectedTab == 1 -> {
                selectedTab = 0
            }
            mode != HomeMode.MAIN -> {
                mode = HomeMode.MAIN
            }
            else -> { }
        }
    }

    HomeScaffold(selectedTab = selectedTab, onTabChange = { idx ->
        selectedTab = idx
        if (idx == 0) {
            mode = HomeMode.MAIN
        }
    }, showBottomBar = (selectedTab == 0 && mode == HomeMode.MAIN) || selectedTab == 1) {
        if (selectedTab == 1) {
            StatsScreen()
        } else {
            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    val forward = initialState == HomeMode.MAIN && targetState != HomeMode.MAIN
                    if (forward) {
                        (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
                    } else {
                        (slideInHorizontally { -it } + fadeIn()) togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
                    }
                },
                label = "home-mode",
            ) { m ->
                when (m) {
                    HomeMode.MAIN ->
                        WinsContent(
                            state = state,
                            onAddNew = { mode = HomeMode.ADD },
                            onToggleAll = { mode = HomeMode.ALL },
                            onLoadMore = { vm.loadMore() },
                            onOpenSearch = { mode = HomeMode.SEARCH },
                            onSearchQueryChange = { vm.onSearchQueryChange(it) },
                        )
                    HomeMode.ADD ->
                        AddAchievementScreen(
                            vm = addVm,
                            onBack = { mode = HomeMode.MAIN },
                            onSaved = { a ->
                                vm.onNewAchievement(a)
                                mode = HomeMode.MAIN
                            },
                        )
                    HomeMode.ALL ->
                        SimpleAllScreen(
                            state = state,
                            onBack = { mode = HomeMode.MAIN },
                            onLoadMore = { vm.loadMore() },
                        )
                    HomeMode.SEARCH ->
                        SimpleSearchScreen(
                            state = state,
                            onBack = { mode = HomeMode.MAIN },
                            onQueryChange = { vm.onSearchQueryChange(it) },
                        )
                }
            }
        }
    }
}

@Composable
private fun HomeScaffold(
    selectedTab: Int,
    onTabChange: (Int) -> Unit,
    showBottomBar: Boolean,
    content: @Composable () -> Unit,
) {
    Scaffold(
        containerColor = Color(0xFF1C1C1E),
        bottomBar = {
            if (showBottomBar) {
                AppBottomNavigation(selectedIndex = selectedTab, onSelect = onTabChange)
            }
        },
    ) { pv ->

        Box(Modifier.padding(pv)) { content() }
    }
}
