package tech.arcent.home

/*
 HomeScreen orchestrates navigation between main wins, add, all, search, stats, details, settings screens.
 */

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import tech.arcent.addachievement.AddAchievementScreen
import tech.arcent.addachievement.AddAchievementViewModel
import tech.arcent.auth.AuthViewModel
import tech.arcent.home.detail.WinDetailsRoute
import tech.arcent.home.simple.SimpleAllScreen
import tech.arcent.home.simple.SimpleSearchScreen
import tech.arcent.settings.SettingsScreenHost
import tech.arcent.settings.SettingsViewModel
import tech.arcent.stats.StatsScreen
import tech.arcent.ui.LocalSystemBarController
import tech.arcent.ui.SystemBarStyle

private enum class HomeMode { MAIN, ADD, ALL, SEARCH, DETAIL, SETTINGS }

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(vm: HomeViewModel = hiltViewModel(), authVm: AuthViewModel = hiltViewModel()) {
    val state by vm.uiState.collectAsState()
    var mode by remember { mutableStateOf(HomeMode.MAIN) }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedAchievement by remember { mutableStateOf<Achievement?>(null) }
    val addVm: AddAchievementViewModel = hiltViewModel()
    val settingsVm: SettingsViewModel = hiltViewModel()
    val barController = LocalSystemBarController.current
    val baseColor = Color(0xFF1C1C1E)
    val addColor = Color(0xFF252525)

    /* reactive fallback update */
    LaunchedEffect(mode, selectedTab) {
        val color = if (mode == HomeMode.ADD) addColor else baseColor
        barController.set(SystemBarStyle(color, color, false))
    }

    BackHandler(enabled = true) {
        when {
            mode == HomeMode.SETTINGS -> mode = HomeMode.MAIN
            selectedTab == 1 -> selectedTab = 0
            mode == HomeMode.DETAIL -> { mode = HomeMode.MAIN; selectedAchievement = null }
            mode != HomeMode.MAIN -> mode = HomeMode.MAIN
        }
    }

    HomeScaffold(selectedTab = selectedTab, onTabChange = { idx ->
        selectedTab = idx
        if (idx == 0 && mode !in listOf(HomeMode.DETAIL, HomeMode.ADD, HomeMode.SETTINGS)) mode = HomeMode.MAIN
    }, showBottomBar = false) {
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
                            onAddNew = {
                                addVm.startNew()
                                barController.set(SystemBarStyle(addColor, addColor, false))
                                mode = HomeMode.ADD
                            },
                            onToggleAll = { mode = HomeMode.ALL },
                            onLoadMore = { vm.loadMore() },
                            onOpenSearch = { mode = HomeMode.SEARCH },
                            onSearchQueryChange = { vm.onSearchQueryChange(it) },
                            onOpenDetails = { a -> selectedAchievement = a; mode = HomeMode.DETAIL },
                            onOpenSettings = { mode = HomeMode.SETTINGS },
                        )
                    HomeMode.ADD ->
                        AddAchievementScreen(
                            vm = addVm,
                            onBack = {
                                addVm.startNew()
                                val returningToDetail = addVm.uiState.value.editingId != null && selectedAchievement != null
                                val targetMode = if (returningToDetail) HomeMode.DETAIL else HomeMode.MAIN
                                if (targetMode != HomeMode.ADD) barController.set(SystemBarStyle(baseColor, baseColor, false))
                                mode = targetMode
                            },
                            onSaved = { a ->
                                vm.onNewAchievement(a)
                                addVm.startNew()
                                val returningToDetail = selectedAchievement?.id == a.id
                                val targetMode = if (returningToDetail) HomeMode.DETAIL else HomeMode.MAIN
                                if (targetMode != HomeMode.ADD) barController.set(SystemBarStyle(baseColor, baseColor, false))
                                if (returningToDetail) selectedAchievement = a
                                mode = targetMode
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
                                    barController.set(SystemBarStyle(addColor, addColor, false))
                                    mode = HomeMode.ADD
                                },
                                onDeleted = { id ->
                                    vm.onDeleteAchievement(id)
                                    selectedAchievement = null
                                    mode = HomeMode.MAIN
                                },
                            )
                        }
                    HomeMode.SETTINGS ->
                        SettingsScreenHost.Screen(
                            vm = settingsVm,
                            authVm = authVm,
                            onBack = { mode = HomeMode.MAIN },
                            onLogout = { settingsVm.logoutAndReturn(authVm) { mode = HomeMode.MAIN } },
                            onAccountDeleted = { settingsVm.deleteAndReturn(authVm) { mode = HomeMode.MAIN } },
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
        },
    ) { pv ->
        Box(Modifier.padding(pv)) { content() }
    }
}
