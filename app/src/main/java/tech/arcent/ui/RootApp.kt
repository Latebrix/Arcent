package tech.arcent.ui

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import tech.arcent.R
import tech.arcent.auth.AuthEvent
import tech.arcent.auth.AuthViewModel
import tech.arcent.auth.AuthScreen

/*
 * RootApp: Splash -> Auth/Home routing.
 */
@Composable
fun RootApp(externalVm: AuthViewModel? = null) {
    val vm = externalVm ?: viewModel<AuthViewModel>()
    val state by vm.uiState.collectAsState()


    if (state.isAuthenticated) {
        HomeScreen()
    } else {
        AuthScreen(onAuthenticated = { /* recomposition triggers home */ }, vm = vm)
    }
}

@Composable
private fun HomeScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Home", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
