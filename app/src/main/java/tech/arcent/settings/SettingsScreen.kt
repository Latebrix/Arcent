package tech.arcent.settings

/*
 Settings screen: account section (avatar change, logout, delete), crash reporting toggle, app version info
 */

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import tech.arcent.R
import tech.arcent.auth.AuthViewModel

object SettingsScreenHost {
    @Composable
    fun Screen(
        vm: SettingsViewModel = hiltViewModel(),
        authVm: AuthViewModel,
        onBack: () -> Unit,
        onLogout: () -> Unit,
        onAccountDeleted: () -> Unit,
    ) {
        SettingsScreen(vm = vm, authVm = authVm, onBack = onBack, onLogout = onLogout, onAccountDeleted = onAccountDeleted)
    }
}

@Composable
fun SettingsScreen(
    vm: SettingsViewModel = hiltViewModel(),
    authVm: AuthViewModel,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onAccountDeleted: () -> Unit,
) {
    val state by vm.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? -> vm.setAvatar(uri) }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val isLocal = state.provider == "local"
    LaunchedEffect(Unit) {
        vm.events.collect { ev ->
            val msg = when (ev) {
                SettingsEvent.NameSaved -> context.getString(R.string.settings_snackbar_name_saved)
                SettingsEvent.AvatarSaved -> context.getString(R.string.settings_snackbar_avatar_saved)
                is SettingsEvent.Error -> when (ev.resKey) {
                    "avatar_size" -> context.getString(R.string.settings_error_avatar_size)
                    "name_save" -> context.getString(R.string.settings_error_name_save)
                    else -> context.getString(R.string.settings_error_generic)
                }
            }
            snackbarHostState.showSnackbar(message = msg, withDismissAction = true)
        }
    }
    Scaffold(
        containerColor = Color(0xFF1C1C1E),
        topBar = { SettingsTopBar(onBack = onBack) },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { pv ->
        Box(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(pv)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp) /* removed bottom extra spacing to eliminate gap above system nav */,
            ) {
                SectionTitle(text = stringResource(id = R.string.settings_section_account))
                Spacer(Modifier.height(12.dp))
                val nameInitial = state.nameInput.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
                AvatarRow(avatarPath = state.avatarPath, nameInitial = nameInitial, onChange = { imagePicker.launch("image/*") })
                Spacer(Modifier.height(12.dp))
                NameEditBlock(state = state, onNameChange = { vm.onNameInput(it) }, onSave = { vm.saveName(authVm) })
                Spacer(Modifier.height(24.dp))
                SectionTitle(text = stringResource(id = R.string.settings_account_actions))
                Spacer(Modifier.height(12.dp))
                if (!isLocal) {
                    Button(
                        onClick = { onLogout() },
                        enabled = !state.loggingOut,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF555555)),
                    ) {
                        if (state.loggingOut) { CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
                        Text(stringResource(id = R.string.settings_logout))
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { showDeleteDialog = true },
                        enabled = !state.deleting,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5555)),
                    ) {
                        if (state.deleting) { CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
                        Text(stringResource(id = R.string.settings_delete_account_and_data))
                    }
                } else {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF2C2C2E), modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(id = R.string.settings_provider_local_note), color = Color(0xFFBBBBBB), modifier = Modifier.padding(16.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { showDeleteDialog = true },
                        enabled = !state.deleting,
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5555)),
                    ) {
                        if (state.deleting) { CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
                        Text(stringResource(id = R.string.settings_delete_local_data))
                    }
                }
                Spacer(Modifier.height(28.dp))
                SectionTitle(text = stringResource(id = R.string.settings_crash_reporting))
                Spacer(Modifier.height(8.dp))
                CrashToggleRow(enabled = state.crashEnabled, isDebug = state.isDebug, onToggle = { vm.toggleCrashReporting(it) })
                Spacer(Modifier.height(28.dp))
                SectionTitle(text = stringResource(id = R.string.settings_more))
                Spacer(Modifier.height(12.dp))
                VersionInfo(version = state.versionName)
                Spacer(Modifier.height(12.dp))
                val repoUrl = stringResource(id = R.string.settings_repo_url)
                RepoLinkRow(onOpen = { uriHandler.openUri(repoUrl) })
            }
            if (state.avatarUploading) {
                Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF1E1E1F)) {
                        Box(Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF799C92))
                        }
                    }
                }
            }
        }
    }
    if (showDeleteDialog) {
        val msgRes = if (isLocal) R.string.settings_delete_message_local else R.string.settings_delete_message_remote
        DeleteConfirmDialog(messageRes = msgRes, onDismiss = { showDeleteDialog = false }, onConfirm = { showDeleteDialog = false; onAccountDeleted() })
    }
}

@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = { onBack() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.settings_action_back), tint = Color.White)
        }
        Spacer(Modifier.width(8.dp))
        Text(stringResource(id = R.string.settings_title), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Color(0xFFEEEEEE), fontWeight = FontWeight.SemiBold)
}

@Composable
private fun AvatarRow(avatarPath: String?, nameInitial: String, onChange: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2C2C2E))
            .clickable { onChange() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val size = 56.dp
        Surface(shape = CircleShape, color = if (avatarPath.isNullOrBlank()) Color(0xFF1E88E5) else Color(0xFF3A3A3C), modifier = Modifier.size(size)) {
            if (!avatarPath.isNullOrBlank()) {
                AsyncImage(
                    model = avatarPath,
                    contentDescription = stringResource(id = R.string.settings_change_avatar),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(nameInitial, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(id = R.string.settings_change_avatar), color = Color.White)
            Text(stringResource(id = R.string.settings_avatar_picker_title), color = Color(0xFFAAAAAA), style = MaterialTheme.typography.labelSmall)
        }
        Icon(
            painter = painterResource(id = R.drawable.icons_settings),
            contentDescription = null,
            tint = Color(0xFF888888),
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun NameEditBlock(state: SettingsUiState, onNameChange: (String) -> Unit, onSave: () -> Unit) {
    Column {
        Text(stringResource(id = R.string.settings_name_label), color = Color.White)
        Spacer(Modifier.height(6.dp))
        TextField(
            value = state.nameInput,
            onValueChange = onNameChange,
            singleLine = true,
            placeholder = { Text(stringResource(id = R.string.settings_name_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF2C2C2E),
                unfocusedContainerColor = Color(0xFF2C2C2E),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
            ),
        )
        state.nameError?.let {
            val errTxt = when (it) { "short" -> R.string.settings_name_error_short; "long" -> R.string.settings_name_error_too_long; else -> R.string.settings_error_generic }
            Text(stringResource(id = errTxt), color = Color(0xFFFF7777), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(8.dp))
        val nameChanged = state.nameInput.trim() != state.savedName.trim()
        val canSave = nameChanged && state.nameInput.trim().length >= 2 && !state.savingName
        val activeColor = Color(0xFF789C93)
        val inactiveColor = Color(0xFF555555)
        Button(
            onClick = onSave,
            enabled = canSave,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (nameChanged) activeColor else inactiveColor, disabledContainerColor = inactiveColor),
            modifier = Modifier.height(44.dp).fillMaxWidth(),
        ) {
            if (state.savingName) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(id = R.string.settings_name_save))
        }
    }
}

/* duplication suffix block commented out
@Composable
private fun DuplicationSuffixBlock(state: SettingsUiState, onChange: (String) -> Unit, onSave: () -> Unit) {
    Column {
        Text(stringResource(id = R.string.settings_duplication_prefix_label), color = Color.White)
        Spacer(Modifier.height(6.dp))
        TextField(
            value = state.dupSuffixEditing,
            onValueChange = onChange,
            singleLine = true,
            placeholder = { Text(stringResource(id = R.string.settings_duplication_prefix_placeholder)) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF2C2C2E),
                unfocusedContainerColor = Color(0xFF2C2C2E),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
            ),
        )
        state.dupSuffixError?.let {
            Text(
                stringResource(id = R.string.settings_duplication_suffix_error_empty),
                color = Color(0xFFFF7777),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        val suffixChanged = state.dupSuffixEditing.trim() != state.dupSuffix.trim()
        val canSave = suffixChanged && state.dupSuffixEditing.trim().isNotEmpty() && !state.savingSuffix
        val activeColor = Color(0xFF789C93)
        val inactiveColor = Color(0xFF555555)
        Button(
            onClick = onSave,
            enabled = canSave,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = if (suffixChanged) activeColor else inactiveColor, disabledContainerColor = inactiveColor),
            modifier = Modifier.height(44.dp).fillMaxWidth(),
        ) {
            if (state.savingSuffix) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(stringResource(id = R.string.settings_duplication_prefix_save))
        }
    }
}
*/

@Composable
private fun ActionRow(text: String, color: Color, onClick: () -> Unit, loading: Boolean) {
    Surface(
        onClick = { if (!loading) onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2C2C2E),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text, color = color, modifier = Modifier.weight(1f))
            if (loading) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp), color = color)
            }
        }
    }
}

@Composable
private fun CrashToggleRow(enabled: Boolean, isDebug: Boolean, onToggle: (Boolean) -> Unit) {
    val descBase = if (isDebug) stringResource(id = R.string.settings_crash_reporting_desc_debug) else stringResource(id = R.string.settings_crash_reporting_desc)
    val buildTypeWord = if (isDebug) stringResource(id = R.string.settings_build_type_debug_word) else stringResource(id = R.string.settings_build_type_release_word)
    val fullDesc = descBase + " " + stringResource(id = R.string.settings_build_type_note, buildTypeWord)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2C2C2E))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(fullDesc, color = Color.White, modifier = Modifier.weight(1f))
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF789C93),
                    checkedTrackColor = Color(0xFF4A6B62),
                ),
            )
        }
    }
}

@Composable
private fun VersionInfo(version: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2C2C2E),
        modifier = Modifier.fillMaxWidth(),
    ) { Column(Modifier.padding(16.dp)) { Text(stringResource(id = R.string.settings_app_version_label), color = Color.White); Spacer(Modifier.height(4.dp)); Text(version, color = Color(0xFFAAAAAA), style = MaterialTheme.typography.labelSmall) } }
}

@Composable
private fun RepoLinkRow(onOpen: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2C2C2E),
        modifier = Modifier.fillMaxWidth().clickable { onOpen() },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(id = R.string.settings_repo), color = Color.White, modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(id = R.drawable.icons_settings),
                contentDescription = null,
                tint = Color(0xFF888888),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun DeleteConfirmDialog(messageRes: Int, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.settings_delete_confirm_title)) },
        text = { Text(stringResource(id = messageRes)) },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5555))) { Text(stringResource(id = R.string.settings_delete_confirm_yes), color = Color.White) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.settings_delete_confirm_no)) } },
    )
}
