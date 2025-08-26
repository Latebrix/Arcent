package tech.arcent.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.graphics.Color
import tech.arcent.auth.AuthScreen
import tech.arcent.auth.AuthViewModel
import tech.arcent.home.HomeScreen

/*
 * RootApp: Splash -> Auth/Home routing with centralized system bar styling.
 */
@Composable
fun RootApp(externalVm: AuthViewModel? = null) {
    val vm = externalVm ?: viewModel<AuthViewModel>()
    val state by vm.uiState.collectAsState()
    val authenticated = state.isAuthenticated
    SystemBarHost {
        val barController = LocalSystemBarController.current
        if (authenticated) {
            barController.set(DefaultAuthenticatedBars())
            HomeScreen(authVm = vm)
        } else {
            barController.set(SystemBarStyle(Color.Transparent, Color.Transparent, false))
            AuthScreen(onAuthenticated = { }, vm = vm)
        }
    }
}
