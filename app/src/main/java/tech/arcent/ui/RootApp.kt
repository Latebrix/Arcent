package tech.arcent.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import tech.arcent.auth.AuthScreen
import tech.arcent.auth.AuthViewModel
import tech.arcent.home.HomeScreen

/*
 * RootApp: Splash -> Auth/Home routing.
 */
@Composable
fun RootApp(externalVm: AuthViewModel? = null) {
    val vm = externalVm ?: viewModel<AuthViewModel>()
    val state by vm.uiState.collectAsState()

    if (state.isAuthenticated) {
        HomeScreen(authVm = vm)
    } else {
        AuthScreen(onAuthenticated = { /* recomposition triggers home */ }, vm = vm)
    }
}
