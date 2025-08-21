package tech.arcent.addachievement.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.getSelectedText
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import tech.arcent.R
import kotlin.math.min

/* mark line tokens for headings/quote/list present in stored markdown text */
private val heading2Token = "## "
private val heading1Token = "# "
private val quoteToken = "> "
private val bulletToken = "- "
private val numberedRegex = Regex("^\\d+\\. ")

/* Strip leading structural token returning pair(tokenLength, contentStartIndex) */
private fun detectLineToken(line: String): Pair<Int,String> {
    return when {
        line.startsWith(heading2Token) -> heading2Token.length to "h2"
        line.startsWith(heading1Token) -> heading1Token.length to "h1"
        line.startsWith(quoteToken) -> quoteToken.length to "quote"
        line.startsWith(bulletToken) -> bulletToken.length to "bullet"
        numberedRegex.find(line)?.let { it.range.first==0 } == true -> numberedRegex.find(line)!!.value.length to "number"
        else -> 0 to "none"
    }
}

/* Inline bold + italic markers */
private fun isBoldWrapped(text: String): Boolean = text.startsWith("**") && text.endsWith("**") && text.length>=4
private fun isItalicWrapped(text: String): Boolean = text.startsWith("*") && text.endsWith("*") && text.length>=2 && !(text.startsWith("**") && text.endsWith("**"))

@Composable
fun MarkdownEditor(
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
    placeholder: String,
    maxLength: Int
) {
    var raw by remember { mutableStateOf(value) }
    var selection by remember { mutableStateOf(TextRange(value.length)) }
    val focusRequester = remember { FocusRequester() }
    val scroll = rememberScrollState()

    /* Sync Clear */
    LaunchedEffect(value) { if(value!=raw){ raw=value; selection = TextRange(raw.length) } }

    fun lines(): MutableList<String> = raw.split('\n').toMutableList()
    fun rebuild(newLines: List<String>, newSel: TextRange?=null){
        val newText = newLines.joinToString("\n")
        raw = newText
        onValueChange(newText)
        if(newSel!=null) selection = newSel
        else selection = TextRange(newText.length)
    }
    fun selectionLineRange(): IntRange {
        val startLine = raw.substring(0, selection.start).count { it=='\n' }
        val endLine = raw.substring(0, selection.end).count { it=='\n' }
        return startLine..endLine
    }

    /* Line prefix utilities */
    fun removePrefixes(target: IntRange, prefixes: List<String>){
        val ls = lines()
        target.forEach { i ->
            if(i < ls.size){
                var l=ls[i]
                if(l.startsWith(heading2Token)) l = l.removePrefix(heading2Token)
                if(l.startsWith(heading1Token)) l = l.removePrefix(heading1Token)
                if(l.startsWith(quoteToken)) l = l.removePrefix(quoteToken)
                if(l.startsWith(bulletToken)) l = l.removePrefix(bulletToken)
                numberedRegex.find(l)?.let { if(it.range.first==0) l = l.removeRange(it.range) }
                ls[i]=l
            }
        }
        rebuild(ls)
    }
    fun toggleHeading(level:Int){
        val r = selectionLineRange()
        val ls = lines()
        val token = if(level==1) heading1Token else heading2Token
        val allSet = r.all { i -> i<ls.size && ls[i].startsWith(token) }
        r.forEach { i -> if(i<ls.size){
            var l=ls[i]
            /* remove any existing heading tokens first */
            if(l.startsWith(heading2Token)) l=l.removePrefix(heading2Token)
            if(l.startsWith(heading1Token)) l=l.removePrefix(heading1Token)
            if(!allSet) l = token + l
            ls[i]=l
        } }
        rebuild(ls)
    }
    fun toggleQuote(){
        val r=selectionLineRange(); val ls=lines(); val all=r.all{ i-> i<ls.size && ls[i].startsWith(quoteToken) }
        r.forEach { i-> if(i<ls.size){ var l=ls[i]; if(l.startsWith(quoteToken)) l=l.removePrefix(quoteToken) else if(!all) l=quoteToken+l; ls[i]=l } }
        rebuild(ls)
    }
    fun toggleBullet(){
        val r=selectionLineRange(); val ls=lines(); val all=r.all{ i-> i<ls.size && ls[i].startsWith(bulletToken) }
        r.forEach { i-> if(i<ls.size){ var l=ls[i];
            numberedRegex.find(l)?.let { if(it.range.first==0) l=l.removeRange(it.range) }
            if(l.startsWith(bulletToken)) l=l.removePrefix(bulletToken) else if(!all) l= bulletToken + l
            ls[i]=l }
        }
        rebuild(ls)
    }
    fun toggleNumbered(){
        val r=selectionLineRange(); val ls=lines(); val all=r.all{ i-> i<ls.size && numberedRegex.find(ls[i])?.range?.first==0 }
        /* remove bullets first */
        r.forEach { i-> if(i<ls.size){ var l=ls[i]; if(l.startsWith(bulletToken)) l=l.removePrefix(bulletToken); ls[i]=l } }
        if(all){
            r.forEach { i-> if(i<ls.size){ val m=numberedRegex.find(ls[i]); if(m!=null && m.range.first==0) ls[i]=ls[i].removeRange(m.range) } }
        } else {
            r.forEach { i-> if(i<ls.size){ val m=numberedRegex.find(ls[i]); if(m==null || m.range.first!=0) ls[i] = "1. "+ls[i] } }
        }
        /* renumber sequentially overall */
        var num=1; ls.indices.forEach { i-> val m=numberedRegex.find(ls[i]); if(m!=null && m.range.first==0){ val rest = ls[i].removeRange(m.range); ls[i] = "${num}. "+rest; num++ } }
        rebuild(ls)
    }

    fun toggleBold(){ if(selection.collapsed) return; val selText = raw.substring(selection.start, selection.end); val src=raw; val wrapped = isBoldWrapped(selText); val new = if(wrapped) src.substring(0,selection.start)+ selText.removePrefix("**").removeSuffix("**") + src.substring(selection.end) else src.substring(0,selection.start)+"**"+selText+"**"+src.substring(selection.end); val delta = if(wrapped) -4 else 4; raw=new; onValueChange(new); selection = TextRange(selection.start, selection.end+delta) }
    fun toggleItalic(){ if(selection.collapsed) return; val selText= raw.substring(selection.start, selection.end); val wrapped = isItalicWrapped(selText); if(isBoldWrapped(selText)) return; val src=raw; val new = if(wrapped) src.substring(0,selection.start)+ selText.removePrefix("*").removeSuffix("*") + src.substring(selection.end) else src.substring(0,selection.start)+"*"+selText+"*"+src.substring(selection.end); val delta = if(wrapped) -2 else 2; raw=new; onValueChange(new); selection = TextRange(selection.start, selection.end+delta) }

    /* build annotated string: keep tokens in raw text for persistence; hide their characters via transparent color while styling content */
    val annotated = remember(raw) {
        buildAnnotatedString {
            val lines = raw.split('\n')
            lines.forEachIndexed { idx, line ->
                val (tokLen, kind) = detectLineToken(line)
                val lineStart = length
                append(line)
                val lineEnd = length
                /* Hide only structural tokens for headings / quote; leave list tokens visible */
                if(tokLen > 0 && (kind=="h1" || kind=="h2" || kind=="quote")) addStyle(SpanStyle(color = Color.Transparent), lineStart, lineStart + tokLen)
                when(kind){
                    "h1" -> addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp), lineStart + tokLen, lineEnd)
                    "h2" -> addStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 19.sp), lineStart + tokLen, lineEnd)
                    "quote" -> addStyle(SpanStyle(color = accent.copy(alpha=.85f), fontStyle = FontStyle.Italic), lineStart + tokLen, lineEnd)
                }
                /* Bold markers ** ** */
                "\\*\\*(.+?)\\*\\*".toRegex().findAll(line).forEach { m ->
                    val s = lineStart + m.range.first
                    val e = lineStart + m.range.last + 1
                    if(e - s >= 4) {
                        addStyle(SpanStyle(color = Color.Transparent), s, s+2)
                        addStyle(SpanStyle(color = Color.Transparent), e-2, e)
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold), s+2, e-2)
                    }
                }
                /* Italic markers */
                val italicRegex = Regex("(?<!\\*)\\*([^*]+?)\\*(?!\\*)")
                italicRegex.findAll(line).forEach { m ->
                    val segment = line.substring(m.range)
                    if(segment.startsWith("**") || segment.endsWith("**")) return@forEach
                    val s = lineStart + m.range.first
                    val e = lineStart + m.range.last + 1
                    if(e - s >= 2) {
                        addStyle(SpanStyle(color = Color.Transparent), s, s+1)
                        addStyle(SpanStyle(color = Color.Transparent), e-1, e)
                        addStyle(SpanStyle(fontStyle = FontStyle.Italic), s+1, e-1)
                    }
                }
                if(idx < lines.lastIndex) append('\n')
            }
        }
    }

    val suppressedToolbar = object: TextToolbar { override val status = TextToolbarStatus.Hidden; override fun showMenu(rect: androidx.compose.ui.geometry.Rect, onCopyRequested: (() -> Unit)?, onPasteRequested: (() -> Unit)?, onCutRequested: (() -> Unit)?, onSelectAllRequested: (() -> Unit)?) {}; override fun hide() {} }

    CompositionLocalProvider(LocalTextToolbar provides suppressedToolbar) {
        Column(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().background(Color(0xFF3B3B3B)).padding(bottom=8.dp)) {
                Box(Modifier.fillMaxWidth().padding(horizontal=12.dp, vertical=12.dp).heightIn(min=170.dp).pointerInput(annotated){ detectTapGestures(onTap={ focusRequester.requestFocus() }) }) {
                    BasicTextField(
                        value = TextFieldValue(annotated, selection),
                        onValueChange = { tfv ->
                            /* allow direct edits mapping since tokens remain present (but hidden :) */
                            val newRaw = tfv.text.take(maxLength)
                            raw = newRaw
                            onValueChange(newRaw)
                            selection = tfv.selection
                        },
                        cursorBrush = SolidColor(Color.White),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color=Color.White,fontSize=16.sp,lineHeight=22.sp),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                    )
                    if(raw.isEmpty()) Text(placeholder, color = Color(0xFFAAAAAA), fontSize = 16.sp)
                }
                Divider(color=Color(0xFF2D2D2D), thickness=1.dp)
                Row(Modifier.horizontalScroll(scroll).padding(horizontal=10.dp, vertical=6.dp), horizontalArrangement=Arrangement.spacedBy(12.dp), verticalAlignment=Alignment.CenterVertically) {
                    MdAction(stringResource(id = R.string.add_markdown_bold), accent){ toggleBold() }
                    MdAction(stringResource(id = R.string.add_markdown_italic), accent){ toggleItalic() }
                    MdAction(stringResource(id = R.string.add_markdown_heading1), accent){ toggleHeading(1) }
                    MdAction(stringResource(id = R.string.add_markdown_heading2), accent){ toggleHeading(2) }
                    MdAction(stringResource(id = R.string.add_markdown_quote), accent){ toggleQuote() }
                    MdAction(stringResource(id = R.string.add_markdown_bullet_label), accent){ toggleBullet() }
                    MdAction(stringResource(id = R.string.add_markdown_numbered_label), accent){ toggleNumbered() }
                }
            }
            Text("${raw.length}/$maxLength", fontSize=12.sp, color = Color.Gray, modifier=Modifier.align(Alignment.End).padding(top=6.dp))
            LaunchedEffect(Unit){ focusRequester.requestFocus() }
        }
    }
}

@Composable
private fun MdAction(label:String, accent:Color, onClick:()->Unit){
    Text(label, color=accent, modifier= Modifier.clip(MaterialTheme.shapes.small).background(Color.Transparent).clickable{ onClick() }.padding(horizontal=10.dp, vertical=4.dp), maxLines=1, overflow= TextOverflow.Clip)
}
