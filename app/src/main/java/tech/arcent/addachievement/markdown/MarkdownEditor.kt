package tech.arcent.addachievement.markdown

// MarkdownEditor composable refactored: logic moved to MarkdownEditorState and MarkdownStyler for lower complexity; comments relocated to function headers only.

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.arcent.R

@Composable
fun MarkdownEditor(
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    placeholder: String,
    maxLength: Int,
) {
    val focusRequester = remember { FocusRequester() }
    val scroll = rememberScrollState()
    var state by remember { mutableStateOf(MarkdownEditorState(raw = value, selection = TextRange(value.length))) }
    LaunchedEffect(value) { if (value != state.raw) state = state.copy(raw = value, selection = TextRange(value.length)) }
    val annotated = remember(state.raw) { buildStyledMarkdown(state.raw, accent) }
    val suppressedToolbar =
        remember {
            object : TextToolbar {
                override val status = TextToolbarStatus.Hidden

                override fun showMenu(
                    rect: androidx.compose.ui.geometry.Rect,
                    onCopyRequested: (() -> Unit)?,
                    onPasteRequested: (() -> Unit)?,
                    onCutRequested: (() -> Unit)?,
                    onSelectAllRequested: (() -> Unit)?,
                ) {
                }

                override fun hide() {}
            }
        }
    CompositionLocalProvider(LocalTextToolbar provides suppressedToolbar) {
        Column(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().background(Color(0xFF3B3B3B)).padding(bottom = 8.dp)) {
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp).heightIn(min = 170.dp).pointerInput(annotated) {
                        detectTapGestures(onTap = { focusRequester.requestFocus() })
                    },
                ) {
                    BasicTextField(
                        value = TextFieldValue(annotated, state.selection),
                        onValueChange = { tfv ->
                            val limited = tfv.text.take(maxLength)
                            val newSel = tfv.selection
                            state = state.onExternalEdit(limited, newSel)
                            onValueChange(limited)
                        },
                        cursorBrush = SolidColor(Color.White),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = 16.sp, lineHeight = 22.sp),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )
                    if (state.raw.isEmpty()) Text(placeholder, color = Color(0xFFAAAAAA), fontSize = 16.sp)
                }
                Divider(color = Color(0xFF2D2D2D), thickness = 1.dp)
                Row(
                    Modifier.horizontalScroll(scroll).padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MdAction(stringResource(id = R.string.add_markdown_bold), accent) { state = state.toggleBold(onValueChange) }
                    MdAction(stringResource(id = R.string.add_markdown_italic), accent) { state = state.toggleItalic(onValueChange) }
                    MdAction(stringResource(id = R.string.add_markdown_heading1), accent) { state = state.toggleHeading(1, onValueChange) }
                    MdAction(stringResource(id = R.string.add_markdown_heading2), accent) { state = state.toggleHeading(2, onValueChange) }
                    MdAction(stringResource(id = R.string.add_markdown_quote), accent) { state = state.toggleQuote(onValueChange) }
                    MdAction(stringResource(id = R.string.add_markdown_bullet_label), accent) { state = state.toggleBullet(onValueChange) }
                    MdAction(
                        stringResource(id = R.string.add_markdown_numbered_label),
                        accent,
                    ) { state = state.toggleNumbered(onValueChange) }
                }
            }
            Text(
                "${state.raw.length}/$maxLength",
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.align(Alignment.End).padding(top = 6.dp),
            )
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
    }
}

@Composable
private fun MdAction(
    label: String,
    accent: Color,
    onClick: () -> Unit,
) {
    Text(
        label,
        color = accent,
        modifier =
            Modifier.background(Color.Transparent).clickable {
                onClick()
            }.padding(horizontal = 10.dp, vertical = 4.dp),
        maxLines = 1,
        overflow = TextOverflow.Clip,
    )
}
