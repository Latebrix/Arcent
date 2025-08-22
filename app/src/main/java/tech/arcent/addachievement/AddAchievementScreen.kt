package tech.arcent.addachievement

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import tech.arcent.R
import tech.arcent.addachievement.markdown.MarkdownEditor
import java.text.DateFormat
import java.util.Calendar

/* Colors (kept local) */
private val darkBackground = Color(0xFF252525)
private val componentBackground = Color(0xFF3B3B3B)
private val chipBackground = Color(0xFF4A4A4A)
private val primaryGreen = Color(0xFF6B8D7D)
private val textPrimary = Color.White
private val textSecondary = Color(0xFFAAAAAA)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AddAchievementScreen(
    vm: AddAchievementViewModel,
    onBack: () -> Unit,
    onSaved: (tech.arcent.home.Achievement) -> Unit,
) {
    val state by vm.uiState.collectAsState()
    val accent = Color(0xFF799c92)
    val systemUi = rememberSystemUiController()
    var initialized by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        systemUi.setStatusBarColor(darkBackground, darkIcons = false)
        systemUi.setNavigationBarColor(darkBackground, darkIcons = false)
        if (!initialized) {
            initialized = true
        }
        vm.saved.collect { achievement ->
            onSaved(achievement)
            onBack()
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            systemUi.setStatusBarColor(Color(0xFF1C1C1E), false)
            systemUi.setNavigationBarColor(Color(0xFF2C2C2E), false)
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        vm.onEvent(AddAchievementEvent.ImagePicked(uri))
    }
    Scaffold(
        containerColor = darkBackground,
        topBar = { AddAchievementTopBar(accent = accent, onBack = onBack) { vm.onEvent(AddAchievementEvent.ToggleTips) } },
        bottomBar = {
            AddAchievementBottomBar(
                state = state,
                onClear = { vm.onEvent(AddAchievementEvent.Clear) },
                onSave = { vm.onEvent(AddAchievementEvent.Save) },
            )
        },
    ) { innerPadding ->
        CompositionLocalProvider(LocalOverscrollFactory provides null) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                AchievementTextField(
                    label = stringResource(id = R.string.add_field_title_label),
                    placeholder = stringResource(id = R.string.add_field_title_placeholder),
                    value = state.title,
                    onValueChange = { vm.onEvent(AddAchievementEvent.TitleChanged(it)) },
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(text = stringResource(id = R.string.add_field_details_label), color = textPrimary, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                MarkdownEditor(
                    value = state.details,
                    onValueChange = { vm.onEvent(AddAchievementEvent.DetailsChanged(it)) },
                    accent = accent,
                    placeholder = stringResource(id = R.string.add_field_details_placeholder),
                    maxLength = 4750,
                )
                Spacer(modifier = Modifier.height(14.dp))
                CategorySection(accent = accent, state = state, vm = vm)
                Spacer(modifier = Modifier.height(14.dp))
                TagsSection(accent = accent, state = state, vm = vm)
                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DateTimeBlock(
                        label = stringResource(id = R.string.add_date),
                        value = formatDate(state.dateMillis),
                        modifier = Modifier.weight(1f),
                    ) { vm.onEvent(AddAchievementEvent.ToggleDatePicker) }
                    DateTimeBlock(
                        label = stringResource(id = R.string.add_time),
                        value = formatTime(state.hour, state.minute),
                        modifier = Modifier.weight(1f),
                    ) { vm.onEvent(AddAchievementEvent.ToggleTimePicker) }
                }
                Spacer(modifier = Modifier.height(14.dp))
                AddAchievementImagePicker(
                    state = state,
                    componentBackground = componentBackground,
                    textSecondary = textSecondary,
                    imagePicker = { imagePicker.launch("image/*") },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
    AddAchievementOverlays(state = state, vm = vm)
}

@Composable
private fun AddAchievementTopBar(
    accent: Color,
    onBack: () -> Unit,
    onToggleTips: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(darkBackground)
            .padding(start = 2.dp, end = 6.dp, top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = Color.Transparent, modifier = Modifier.size(40.dp)) {
            Box(Modifier.fillMaxSize().clickable { onBack() }, contentAlignment = Alignment.Center) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.cd_back),
                    tint = accent,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Surface(
            onClick = onToggleTips,
            shape = CircleShape,
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            modifier = Modifier.height(40.dp),
        ) {
            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.icons_stars),
                    contentDescription = stringResource(id = R.string.add_tips),
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(stringResource(id = R.string.add_tips), color = accent, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun AddAchievementBottomBar(
    state: AddAchievementUiState,
    onClear: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(darkBackground)
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedButton(
            onClick = onClear,
            modifier = Modifier.size(height = 50.dp, width = 56.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = darkBackground),
            border = BorderStroke(1.dp, Color(0xFF555555)),
            contentPadding = PaddingValues(0.dp),
        ) {
            Icon(
                painter = painterResource(id = R.drawable.icons_trash),
                contentDescription = stringResource(id = R.string.cd_clear),
                tint = Color(0xFFBBBBBB),
                modifier = Modifier.size(20.dp),
            )
        }
        Button(
            onClick = onSave,
            modifier = Modifier
                .weight(1f)
                .height(50.dp),
            shape = RoundedCornerShape(16.dp),
            enabled = state.title.isNotBlank() && !state.isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(stringResource(id = R.string.add_save), color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
private fun AddAchievementImagePicker(
    state: AddAchievementUiState,
    componentBackground: Color,
    textSecondary: Color,
    imagePicker: () -> Unit,
) {
    Text(stringResource(id = R.string.add_add_photo_optional), color = textSecondary, fontSize = 15.sp)
    Spacer(modifier = Modifier.height(6.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(componentBackground)
            .clickable { imagePicker() },
        contentAlignment = Alignment.Center,
    ) {
        if (state.imageUri != null) {
            AsyncImage(
                model = state.imageUri,
                contentDescription = stringResource(id = R.string.cd_add_image),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(id = R.drawable.icons_add_image),
                    contentDescription = stringResource(id = R.string.cd_add_image),
                    tint = textSecondary,
                    modifier = Modifier.size(38.dp),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(stringResource(id = R.string.add_add_image), color = textSecondary, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun AddAchievementOverlays(
    state: AddAchievementUiState,
    vm: AddAchievementViewModel,
) {
    if (state.showDatePicker) {
        AddAchievementDatePicker(
            initialMillis = state.dateMillis,
            onDismiss = { vm.onEvent(AddAchievementEvent.ToggleDatePicker) },
            onConfirm = { vm.onEvent(AddAchievementEvent.DateChanged(it)) },
        )
    }
    if (state.showTimePicker) {
        AddAchievementTimePicker(
            hour = state.hour,
            minute = state.minute,
            onDismiss = { vm.onEvent(AddAchievementEvent.ToggleTimePicker) },
            onConfirm = { h, m -> vm.onEvent(AddAchievementEvent.TimeChanged(h, m)) },
        )
    }
    if (state.showTipsSheet) {
        AddAchievementTipsSheet(onDismiss = { vm.onEvent(AddAchievementEvent.ToggleTips) })
    }
    if (state.showAddCategoryDialog) {
        AddTextItemDialog(
            stringResource(id = R.string.add_new_category_dialog_title),
            stringResource(id = R.string.add_input_hint_category),
            { vm.onEvent(AddAchievementEvent.AddCategory(it)) },
            onDismiss = { vm.onEvent(AddAchievementEvent.ToggleAddCategoryDialog) },
        )
    }
    if (state.showAddTagDialog) {
        AddTextItemDialog(
            stringResource(id = R.string.add_new_tag_dialog_title),
            stringResource(id = R.string.add_input_hint_tag),
            { vm.onEvent(AddAchievementEvent.AddTag(it)) },
            onDismiss = { vm.onEvent(AddAchievementEvent.ToggleAddTagDialog) },
        )
    }
    if (state.isSaving) {
        Box(Modifier.fillMaxSize().background(Color(0x99000000)), contentAlignment = Alignment.Center) {
            Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF1E1E1F), tonalElevation = 8.dp, shadowElevation = 8.dp) {
                Row(Modifier.padding(horizontal = 28.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Color(0xFF799C92), strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(id = R.string.add_saving), color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

/* Remaining helper composableS */
@Composable
private fun AchievementTextField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp = 56.dp,
) {
    Column {
        Text(text = label, color = textPrimary, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = textSecondary) },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = minHeight),
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = componentBackground,
                unfocusedContainerColor = componentBackground,
                disabledContainerColor = componentBackground,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
            ),
            singleLine = singleLine,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(
    accent: Color,
    state: AddAchievementUiState,
    vm: AddAchievementViewModel,
) {
    val base = listOf(
        stringResource(id = R.string.cat_health),
        stringResource(id = R.string.cat_learning),
        stringResource(id = R.string.cat_productivity),
        stringResource(id = R.string.cat_wellness),
        stringResource(id = R.string.cat_personal),
    )
    val categories = base + state.userCategories
    Text(text = stringResource(id = R.string.add_category), color = textPrimary, fontSize = 16.sp)
    Spacer(Modifier.height(10.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { cat ->
            val selected = state.selectedCategory == cat
            Surface(
                onClick = { vm.onEvent(AddAchievementEvent.CategorySelected(cat)) },
                shape = CircleShape,
                color = if (selected) primaryGreen else chipBackground,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Text(cat, color = textPrimary, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontSize = 14.sp)
            }
        }
        Surface(
            onClick = { vm.onEvent(AddAchievementEvent.ToggleAddCategoryDialog) },
            shape = CircleShape,
            color = accent.copy(alpha = .18f),
        ) {
            Text(
                stringResource(id = R.string.add_plus),
                color = accent,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                fontSize = 16.sp,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(
    accent: Color,
    state: AddAchievementUiState,
    vm: AddAchievementViewModel,
) {
    val base = listOf(
        stringResource(id = R.string.tag_milestone),
        stringResource(id = R.string.tag_habit),
        stringResource(id = R.string.tag_pr),
        stringResource(id = R.string.tag_fitness),
        stringResource(id = R.string.tag_mindset),
        stringResource(id = R.string.tag_career),
    )
    val tags = base + state.userTags
    Text(text = stringResource(id = R.string.add_tags), color = textPrimary, fontSize = 16.sp)
    Spacer(Modifier.height(10.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tags.forEach { tag ->
            val selected = tag in state.selectedTags
            Surface(
                onClick = { vm.onEvent(AddAchievementEvent.TagToggled(tag)) },
                shape = CircleShape,
                color = if (selected) primaryGreen else chipBackground,
            ) {
                Text(tag, color = textPrimary, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontSize = 14.sp)
            }
        }
        Surface(
            onClick = { vm.onEvent(AddAchievementEvent.ToggleAddTagDialog) },
            shape = CircleShape,
            color = accent.copy(alpha = .18f),
        ) {
            Text(
                stringResource(id = R.string.add_plus),
                color = accent,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun AddTextItemDialog(
    title: String,
    hint: String,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onAdd(text.trim()) }) { Text(stringResource(id = R.string.add_ok)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.add_cancel)) } },
        title = { Text(title) },
        text = { TextField(value = text, onValueChange = { if (it.length <= 70) text = it }, placeholder = { Text(hint) }) },
    )
}

/* date time formatting helpers */
private fun formatDate(millis: Long): String {
    val df = DateFormat.getDateInstance(DateFormat.MEDIUM)
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return df.format(cal.time)
}

private fun formatTime(hour: Int, minute: Int): String {
    val tf = DateFormat.getTimeInstance(DateFormat.SHORT)
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return tf.format(cal.time)
}

/* date time selection block */
@Composable
private fun DateTimeBlock(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(componentBackground)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, color = textSecondary, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        Text(value, color = textPrimary, fontSize = 16.sp)
    }
}
