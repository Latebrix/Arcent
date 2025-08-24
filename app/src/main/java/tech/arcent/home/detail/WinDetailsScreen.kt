package tech.arcent.home.detail

/*
 * Detail screen for viewing a single achievement: shows image, title, date,
 * optional category, tags, and markdown-formatted details with edit & delete actions. Share action replaces duplication this version.
 */

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.FileProvider
import tech.arcent.R
import tech.arcent.addachievement.markdown.buildStyledMarkdown
import tech.arcent.home.Achievement
import tech.arcent.share.ShareFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WinDetailsRoute(
    achievement: Achievement,
    onBack: () -> Unit,
    onEdit: (Achievement) -> Unit,
    onDeleted: (String) -> Unit,
    vm: WinDetailsViewModel = hiltViewModel(),
) {
    val state by vm.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val ctx = LocalContext.current
    LaunchedEffect(achievement.id) { vm.setAchievement(achievement) }
    LaunchedEffect(Unit) { vm.deleted.collect { onDeleted(it) } }
    state.achievement?.let { a ->
        val accent = Color(0xFF799C92)
        val markdown = remember(a.details) { a.details?.let { buildStyledMarkdown(it, accent) } }
        WinDetailsScreen(
            achievement = a,
            markdown = markdown,
            isDeleting = state.isDeleting,
            onBack = onBack,
            onEdit = { onEdit(a) },
            onDeleteRequest = { vm.requestDelete() },
            onShare = { ach ->
                val shareData = ShareFormatter.formatText(ach)
                val intent = Intent(Intent.ACTION_SEND)
                var hasImage = false
                if (!ach.photoUrl.isNullOrBlank()) {
                    val file = ShareFormatter.prepareImageFile(ctx, ach.photoUrl!!)
                    if (file != null) {
                        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file)
                        intent.putExtra(Intent.EXTRA_STREAM, uri)
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        intent.type = "image/*"
                        hasImage = true
                    }
                }
                if (!hasImage) intent.type = "text/plain"
                intent.putExtra(Intent.EXTRA_TEXT, shareData)
                runCatching { ctx.startActivity(Intent.createChooser(intent, ctx.getString(R.string.win_details_share))) }
            },
            snackbarHost = snackbarHost,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun WinDetailsScreen(
    achievement: Achievement,
    markdown: AnnotatedString?,
    isDeleting: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleteRequest: () -> Unit,
    onShare: (Achievement) -> Unit,
    snackbarHost: SnackbarHostState,
) {
    val accent = Color(0xFF799C92)
    var showActions by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    BackHandler { onBack() }
    val scroll = rememberScrollState()
    val hasDetails = !achievement.details.isNullOrBlank()
    val hasTags = achievement.tags.isNotEmpty()
    val category = achievement.categories.firstOrNull()
    val hasCategory = !category.isNullOrBlank()
    val showMetaBox = hasCategory || hasTags

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.win_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.cd_back), tint = Color.White) }
                },
                actions = { IconButton(onClick = { showActions = true }) { Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1C1C1E), titleContentColor = Color.White, navigationIconContentColor = Color.White, actionIconContentColor = Color.White),
                windowInsets = WindowInsets(0),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHost) },
        containerColor = Color(0xFF1C1C1E),
        contentWindowInsets = WindowInsets(0),
    ) { pv ->
        CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    modifier =
                        Modifier
                            .padding(pv)
                            .fillMaxSize()
                            .verticalScroll(scroll)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    TitleRow(achievement.title)
                    Spacer(Modifier.height(6.dp))
                    DateRow(achievement.achievedAt)
                    Spacer(Modifier.height(12.dp))
                    AchievementImage(photoUrl = achievement.photoUrl, title = achievement.title)
                    if (showMetaBox) {
                        Spacer(Modifier.height(20.dp))
                        MetaBox(category = if (hasCategory) category else null, tags = if (hasTags) achievement.tags else emptyList())
                    }
                    if (hasDetails && markdown != null) {
                        Spacer(Modifier.height(20.dp))
                        DetailsBox(markdown)
                    }
                    Spacer(Modifier.height(32.dp))
                    EditButton(onEdit = onEdit)
                    Spacer(Modifier.height(48.dp))
                }
                if (isDeleting) {
                    val msg = stringResource(id = R.string.win_details_deleting)
                    Box(Modifier.fillMaxSize().background(Color(0x88000000)), contentAlignment = Alignment.Center) {
                        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF2C2C2E)) {
                            Row(Modifier.padding(horizontal = 28.dp, vertical = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 3.dp, color = accent)
                                Spacer(Modifier.width(14.dp))
                                Text(msg, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showActions) {
        ActionSheet(
            onDismiss = { showActions = false },
            onDelete = { showActions = false; showDeleteConfirm = true },
            onShare = { showActions = false; onShare(achievement) },
        )
    }
    if (showDeleteConfirm) {
        DeleteConfirmDialog(onDismiss = { showDeleteConfirm = false }, onConfirm = { showDeleteConfirm = false; onDeleteRequest() })
    }
}

@Composable
private fun TitleRow(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DateRow(epoch: Long) {
    val now = System.currentTimeMillis()
    val diff = now - epoch
    val oneDay = java.util.concurrent.TimeUnit.DAYS.toMillis(1)
    val dfDate = java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM)
    val tf = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT)
    val timePart = tf.format(java.util.Date(epoch))
    val calA = java.util.Calendar.getInstance().apply { timeInMillis = epoch }
    val calB = java.util.Calendar.getInstance().apply { timeInMillis = now }
    fun sameDay(): Boolean = calA.get(java.util.Calendar.YEAR) == calB.get(java.util.Calendar.YEAR) && calA.get(java.util.Calendar.DAY_OF_YEAR) == calB.get(java.util.Calendar.DAY_OF_YEAR)
    val text = when {
        diff < oneDay && sameDay() -> stringResource(id = R.string.win_details_today) + " • " + timePart
        diff < 2 * oneDay && sameDay().not() && sameDay(epoch + oneDayInternal(), now) -> stringResource(id = R.string.win_details_yesterday) + " • " + timePart
        else -> dfDate.format(java.util.Date(epoch)) + " • " + timePart
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(painter = painterResource(id = R.drawable.icons_date), contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.Gray, fontSize = 14.sp)
    }
}

/* helper to compute one day */
private fun oneDayInternal(): Long = java.util.concurrent.TimeUnit.DAYS.toMillis(1)

private fun sameDay(a: Long, b: Long): Boolean {
    val ca = java.util.Calendar.getInstance().apply { timeInMillis = a }
    val cb = java.util.Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(java.util.Calendar.YEAR) == cb.get(java.util.Calendar.YEAR) && ca.get(java.util.Calendar.DAY_OF_YEAR) == cb.get(java.util.Calendar.DAY_OF_YEAR)
}

@Composable
private fun AchievementImage(photoUrl: String?, title: String) {
    val placeholder = R.drawable.ic_splash
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(shape),
    ) {
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = placeholder),
                error = painterResource(id = placeholder),
                fallback = painterResource(id = placeholder),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Image(painter = painterResource(id = placeholder), contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun MetaBox(category: String?, tags: List<String>) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF2C2C2E), shape = RoundedCornerShape(16.dp))
                .padding(16.dp),
    ) {
        if (!category.isNullOrBlank()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(id = R.string.win_details_category), color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(end = 24.dp))
                CategoryChip(text = category)
            }
        }
        if (tags.isNotEmpty()) {
            if (!category.isNullOrBlank()) Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(id = R.string.win_details_tags), color = Color.Gray, fontSize = 14.sp, modifier = Modifier.padding(end = 24.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    tags.forEach { tag -> TagChip(tag) }
                }
            }
        }
    }
}

@Composable
private fun TagChip(text: String) {
    Surface(color = Color(0xFF3A3A3C), shape = CircleShape) {
        Text(text, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
private fun CategoryChip(text: String) {
    Surface(color = Color(0xFF3A3A3C), shape = CircleShape) {
        Text(text, color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
    }
}

@Composable
private fun DetailsBox(markdown: AnnotatedString) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF2C2C2E), shape = RoundedCornerShape(16.dp))
                .padding(16.dp),
    ) {
        Text(stringResource(id = R.string.win_details_details), color = Color.Gray, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Text(markdown, color = Color.White, fontSize = 16.sp, lineHeight = 22.sp)
    }
}

@Composable
private fun EditButton(onEdit: () -> Unit) {
    Button(
        onClick = onEdit,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B8D7D)),
    ) {
        Icon(painter = painterResource(id = R.drawable.icons_edit), contentDescription = stringResource(id = R.string.win_details_edit), tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(id = R.string.win_details_edit), color = Color.White, fontSize = 16.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionSheet(onDismiss: () -> Unit, onDelete: () -> Unit, onShare: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2E2E2E),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 40.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Button(onClick = onShare, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B8D7D)), shape = RoundedCornerShape(16.dp)) { Text(stringResource(id = R.string.win_details_share), color = Color.White, fontSize = 16.sp) }
            Button(onClick = onDelete, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9A3A42)), shape = RoundedCornerShape(16.dp)) { Text(stringResource(id = R.string.win_details_delete), color = Color.White, fontSize = 16.sp) }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(id = R.string.win_details_delete_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(id = R.string.win_details_delete_cancel)) } },
        title = { Text(stringResource(id = R.string.win_details_delete)) },
        text = null,
    )
}
