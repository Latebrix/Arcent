package tech.arcent.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import tech.arcent.BuildConfig
import tech.arcent.R
import tech.arcent.theme.AppTheme

@Composable
fun AuthScreen(onAuthenticated: () -> Unit = {}, vm: AuthViewModel = viewModel()) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.onEvent(AuthEvent.CheckSession, context) }

    if (state.isAuthenticated) {
        // Notify parent and exit
        LaunchedEffect(Unit) { onAuthenticated() }
        return
    }

    //styles system bars
    val systemUiController = rememberSystemUiController()
    DisposableEffect(systemUiController) {
        systemUiController.setStatusBarColor(Color.Transparent, darkIcons = false)
        systemUiController.setNavigationBarColor(Color.Transparent, darkIcons = false)
        onDispose { }
    }

    /*
     * Google sign-in
     */
    val googleSignInOptions = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.google_server_client_id)
            .requestEmail()
            .build()
    }
    val googleClient = remember { GoogleSignIn.getClient(context, googleSignInOptions) }

    /*
     * Launcher for google sign intent
     */
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            runCatching { task.getResult(ApiException::class.java) }
                .onSuccess { account ->
                    val idToken = account.idToken
                    if (idToken != null) vm.onEvent(AuthEvent.GoogleToken(idToken), context) else vm.onEvent(AuthEvent.GoogleFailed(-1, "null_token"))
                }
                .onFailure { ex ->
                    val code = if (ex is ApiException) ex.statusCode else -2
                    vm.onEvent(AuthEvent.GoogleFailed(code, ex.message))
                }
        } else {
            vm.onEvent(AuthEvent.GoogleFailed(result.resultCode, "canceled_or_failed"))
        }
    }
    fun launchGoogle() { googleLauncher.launch(googleClient.signInIntent) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(487f / 718f).weight(0.6f),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(R.drawable.office_illustrated_last)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.title_celebrate),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 24.sp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!state.localMode) {
                        LandingSection(
                            onGoogle = { launchGoogle() },
                            onLocal = { vm.onEvent(AuthEvent.LocalRequested) }
                        )
                    } else {
                        LocalNameSection(
                            name = state.localName,
                            error = state.localNameError,
                            onNameChange = { vm.onEvent(AuthEvent.LocalNameChanged(it)) },
                            onSubmit = { vm.onEvent(AuthEvent.LocalSubmit, context) }
                        )
                    }
                    state.error?.let { err ->
                        Spacer(Modifier.height(12.dp))
                        Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(24.dp))
                    if (state.localMode) {
                        Text(
                            text = stringResource(id = R.string.local_mode_info),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp, textAlign = TextAlign.Center, color = Color(0xFFB0B0B0)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            if (state.isLoading) {
                Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF1E1E1F), tonalElevation = 8.dp, shadowElevation = 8.dp) {
                        Box(Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF799C92))
                        }
                    }
                }
            }
        }
    }
}

//my preview :)
@Preview(name = "Auth Simple", widthDp = 360, heightDp = 640)
@Composable
private fun PreviewAuthSimple() { AppTheme { AuthScreen() } }

@Composable
private fun LocalNameSection(
    name: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(id = R.string.label_your_name)) },
        placeholder = { Text(stringResource(id = R.string.placeholder_enter_your_name)) },
        singleLine = true,
        isError = error != null,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    )
    if (error != null) {
        Spacer(Modifier.height(4.dp))
        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
    }
    Spacer(Modifier.height(20.dp))
    Button(
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF789C93))
    ) {
        Text(stringResource(id = R.string.button_continue), fontSize = 16.sp, color = Color.White)
    }
}
