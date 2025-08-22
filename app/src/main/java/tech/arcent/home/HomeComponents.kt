package tech.arcent.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.arcent.R
import tech.arcent.auth.data.UserProfileStore
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.ExperimentalFoundationApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun WinsContent(
    state: HomeUiState,
    onAddNew: () -> Unit,
    onToggleAll: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    val listState = rememberLazyListState()
    Column(Modifier.fillMaxSize()) {
        TopBar(modifier = Modifier.padding(horizontal = 16.dp), onOpenSearch = onOpenSearch)
        CompositionLocalProvider(LocalOverscrollFactory provides null) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                item { RecentWinCard(modifier = Modifier.fillMaxWidth()) }
                item { Spacer(Modifier.height(16.dp)) }
                item { AddNewAchievementButton(modifier = Modifier.fillMaxWidth(), onClick = onAddNew) }
                item { Spacer(Modifier.height(16.dp)) }
                item { FirstWinPrompt(modifier = Modifier.fillMaxWidth()) }
                item { Spacer(Modifier.height(24.dp)) }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(id = R.string.home_recent_achievements),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(onClick = onToggleAll, shape = CircleShape, color = Color(0xFF2C2C2E)) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(id = R.string.home_all), color = Color.White, fontSize = 14.sp)
                                Icon(Icons.Default.ChevronRight, contentDescription = stringResource(id = R.string.cd_all), tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
                if (state.achievements.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                            Text(text = stringResource(id = R.string.home_empty_recent), color = Color(0xFFAAAAAA), fontSize = 14.sp)
                        }
                    }
                } else {
                    items(state.achievements, key = { it.id }) { achievement ->
                        AchievementItem(achievement, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun DateHeaderRow(dayStart: Long) {
    val fmt = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    val text = fmt.format(Date(dayStart))
    Text(text = text, color = Color(0xFFBBBBBB), fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
}

@Composable
internal fun TopBar(modifier: Modifier = Modifier, onOpenSearch: () -> Unit) {
    val context = LocalContext.current
    val profile = remember { UserProfileStore.load(context) }
    val nameInitial = profile?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp)
        ,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF1E88E5),
            modifier = Modifier.size(40.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = nameInitial,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        val actionBg = Color(0xFF2C2C2E)
        IconButton(onClick = { onOpenSearch() }) {
            Surface(color = actionBg, shape = CircleShape) {
                Icon(
                    painter = painterResource(id = R.drawable.icons_search),
                    contentDescription = stringResource(id = R.string.cd_search),
                    tint = Color.White,
                    modifier = Modifier.padding(10.dp).size(20.dp)
                )
            }
        }
        IconButton(onClick = { }) {
            Surface(color = actionBg, shape = CircleShape) {
                Icon(
                    painter = painterResource(id = R.drawable.icons_settings),
                    contentDescription = stringResource(id = R.string.cd_settings),
                    tint = Color.White,
                    modifier = Modifier.padding(10.dp).size(20.dp)
                )
            }
        }
    }
}

@Composable
internal fun RecentWinCard(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        color = Color.Transparent,
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(shape)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_splash),
            contentDescription = stringResource(id = R.string.cd_featured_win),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
internal fun AddNewAchievementButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF789C93))
    ) {
        Icon(Icons.Default.Add, contentDescription = stringResource(id = R.string.cd_add))
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(id = R.string.home_action_add_new_achievement), fontSize = 16.sp)
    }
}

@Composable
internal fun FirstWinPrompt(modifier: Modifier = Modifier) {
    Surface(
        color = Color(0xFF2C2C2E),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.smile),
                contentDescription = "Smile",
                tint = Color.Unspecified,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(stringResource(id = R.string.home_first_win_title), color = Color.White, fontWeight = FontWeight.Bold)
                Text(stringResource(id = R.string.home_first_win_subtitle), color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
internal fun AppBottomNavigation(selectedIndex: Int, onSelect: (Int) -> Unit) {
    NavigationBar(containerColor = Color(0xFF2C2C2E)) {
        val items = listOf(
            Triple(stringResource(id = R.string.nav_wins), R.drawable.icons_stars, 0),
            Triple(stringResource(id = R.string.nav_stats), R.drawable.icons_graph, 1)
        )
        items.forEach { (label, iconRes, idx) ->
            NavigationBarItem(
                icon = { Icon(painter = painterResource(id = iconRes), contentDescription = label, modifier = Modifier.size(24.dp)) },
                label = { Text(label) },
                selected = selectedIndex == idx,
                onClick = { onSelect(idx) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    selectedTextColor = Color.White,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color(0xFF3A3A3C)
                )
            )
        }
    }
}
