package tech.arcent.home

/*
 HomeScreen orchestrates navigation between main wins, add, all, search, stats, and details screens.
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
import tech.arcent.home.detail.WinDetailsRoute
import tech.arcent.home.simple.SimpleAllScreen
import tech.arcent.home.simple.SimpleSearchScreen
import tech.arcent.stats.StatsScreen

private enum class HomeMode { MAIN, ADD, ALL, SEARCH, DETAIL }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(vm: HomeViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsState()
    val systemUi = rememberSystemUiController()
    var mode by remember { mutableStateOf(HomeMode.MAIN) }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedAchievement by remember { mutableStateOf<Achievement?>(null) }
    val addVm: AddAchievementViewModel = hiltViewModel()

    LaunchedEffect(mode, selectedTab) {
        val navColor = when (mode) { HomeMode.ALL, HomeMode.SEARCH, HomeMode.DETAIL -> Color(0xFF1C1C1E); else -> Color(0xFF2C2C2E) }
        systemUi.setStatusBarColor(Color(0xFF1C1C1E), false)
        systemUi.setNavigationBarColor(navColor, false)
    }

    BackHandler(enabled = true) {
        when {
            selectedTab == 1 -> selectedTab = 0
            mode == HomeMode.DETAIL -> { mode = HomeMode.MAIN; selectedAchievement = null }
            mode != HomeMode.MAIN -> mode = HomeMode.MAIN
        }
    }

    HomeScaffold(selectedTab = selectedTab, onTabChange = { idx ->
        selectedTab = idx
        if (idx == 0 && mode != HomeMode.DETAIL) mode = HomeMode.MAIN
    }, showBottomBar = (selectedTab == 0 && (mode == HomeMode.MAIN)) || selectedTab == 1) {
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
                            onAddNew = { addVm.startNew(); mode = HomeMode.ADD },
                            onToggleAll = { mode = HomeMode.ALL },
                            onLoadMore = { vm.loadMore() },
                            onOpenSearch = { mode = HomeMode.SEARCH },
                            onSearchQueryChange = { vm.onSearchQueryChange(it) },
                            onOpenDetails = { a -> selectedAchievement = a; mode = HomeMode.DETAIL },
                        )
                    HomeMode.ADD ->
                        AddAchievementScreen(
                            vm = addVm,
                            onBack = {
                                // always clear form on exit
                                addVm.startNew()
                                if (addVm.uiState.value.editingId != null && selectedAchievement != null) {
                                    mode = HomeMode.DETAIL
                                } else {
                                    mode = HomeMode.MAIN
                                }
                            },
                            onSaved = { a ->
                                vm.onNewAchievement(a)
                                addVm.startNew() // clear after save
                                if (selectedAchievement?.id == a.id) {
                                    selectedAchievement = a
                                    mode = HomeMode.DETAIL
                                } else {
                                    mode = HomeMode.MAIN
                                }
                            },
                        )
                    HomeMode.ALL ->
                        SimpleAllScreen(
                            state = state,
                            onBack = { mode = HomeMode.MAIN },
                            onLoadMore = { vm.loadMore() },
                            onOpenDetails = { a -> selectedAchievement = a; mode = HomeMode.DETAIL },
                        )
                    HomeMode.SEARCH ->
                        SimpleSearchScreen(
                            state = state,
                            onBack = { mode = HomeMode.MAIN },
                            onQueryChange = { vm.onSearchQueryChange(it) },
                            onOpenDetails = { a -> selectedAchievement = a; mode = HomeMode.DETAIL },
                        )
                    HomeMode.DETAIL ->
                        selectedAchievement?.let { a ->
                            WinDetailsRoute(
                                achievement = a,
                                onBack = { mode = HomeMode.MAIN; selectedAchievement = null },
                                onEdit = { editA ->
                                    addVm.startEditing(editA)
                                    mode = HomeMode.ADD
                                },
                                onDeleted = { id ->
                                    vm.onDeleteAchievement(id)
                                    selectedAchievement = null
                                    mode = HomeMode.MAIN
                                },
                                onDuplicated = { dup ->
                                    vm.onNewAchievement(dup)
                                    selectedAchievement = dup
                                    mode = HomeMode.DETAIL
                                },
                            )
                        }
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
