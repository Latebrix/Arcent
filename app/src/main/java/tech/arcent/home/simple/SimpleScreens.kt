package tech.arcent.home.simple

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.arcent.R
import tech.arcent.home.AchievementItem
import tech.arcent.home.AllListItem
import tech.arcent.home.HomeUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// date header row
@Composable
private fun DateHeaderRow(dayStart: Long) {
    val fmt = remember { SimpleDateFormat("d MMM yyyy", Locale.getDefault()) }
    Text(
        text = fmt.format(Date(dayStart)),
        color = Color(0xFFBBBBBB),
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp, horizontal = 8.dp),
    )
}

@Composable
private fun SimpleTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(color = Color(0xFF1C1C1E)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(id = R.string.cd_back), tint = Color.White)
            }
            Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.weight(1f))
            actions()
        }
    }
}

@Composable
fun SimpleAllScreen(
    state: HomeUiState,
    onBack: () -> Unit,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()
    Column(Modifier.fillMaxSize().background(Color(0xFF1C1C1E))) {
        SimpleTopBar(title = stringResource(id = R.string.home_all), onBack = onBack)
        if (state.allListItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(id = R.string.all_empty), color = Color(0xFFAAAAAA))
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.allListItems, key = {
                    when (it) {
                        is AllListItem.DateHeader -> "d_" + it.dayStartMillis
                        is AllListItem.Entry -> it.achievement.id
                    }
                }) { item ->
                    when (item) {
                        is AllListItem.DateHeader -> DateHeaderRow(item.dayStartMillis)
                        is AllListItem.Entry -> {
                            AchievementItem(item.achievement, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(12.dp))
                    if (state.loadingMore) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF799C92), strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                        }
                    } else if (state.nextCursor != null) {
                        TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(id = R.string.home_load_more), color = Color(0xFF799C92))
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun SimpleSearchScreen(
    state: HomeUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
) {
    var internalQuery by remember(state.searchQuery) { mutableStateOf(state.searchQuery) }
    Column(Modifier.fillMaxSize().background(Color(0xFF1C1C1E))) {
        SimpleTopBar(title = stringResource(id = R.string.search_title_bar), onBack = onBack) {
            // inline search field
        }
        Box(Modifier.padding(16.dp)) {
            Surface(
                color = Color(0xFF2C2C2E),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                BasicTextField(
                    value = internalQuery,
                    onValueChange = {
                        internalQuery = it
                        onQueryChange(it)
                    },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(color = Color.White),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        if (internalQuery.isEmpty()) {
                            Text(stringResource(id = R.string.search_hint_inline), color = Color(0xFF777777))
                        }
                        inner()
                    },
                )
            }
        }
        if (state.searchLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Color(0xFF799C92)) }
        } else {
            if (internalQuery.isNotBlank() && state.searchResults.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(id = R.string.search_no_results), color = Color(0xFFAAAAAA))
                }
            } else {
                val listState = rememberLazyListState()
                LazyColumn(state = listState, contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxSize()) {
                    items(state.searchResults, key = { it.id }) { a ->
                        AchievementItem(a, modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}
