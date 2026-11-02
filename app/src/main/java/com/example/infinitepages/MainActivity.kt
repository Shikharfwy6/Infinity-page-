package com.example.infinitepages

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.StrikethroughS
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

// ======================= CONSTANTS =======================
const val PAGE_W = 420f      // dp  (A4 ratio 210:297)
const val PAGE_H = 594f      // dp
const val GRID = 20f         // dp
const val MIN_SCALE = 0.06f
const val MAX_SCALE = 5f
val PURPLE = Color(0xFF6C75E8)

val palette = listOf(
    Color.Black, Color.White, Color(0xFFE53935), Color(0xFFFB8C00),
    Color(0xFFFDD835), Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA),
    Color(0xFF6D4C41), Color.Gray, Color(0xFFFFF9C4), Color(0xFFBBDEFB),
    Color(0xFFC8E6C9), Color(0xFFFFCDD2), Color(0xFFE1BEE7), Color(0xFF263238)
)

enum class Tool { TEXT, LINE, PEN }

// ======================= TEXT FORMATTING MODEL =======================
@Immutable
data class TextSpan(
    val start: Int,
    val end: Int,
    val bold: Boolean? = null,
    val italic: Boolean? = null,
    val underline: Boolean? = null,
    val strike: Boolean? = null,
    val color: Color? = null,
    val bg: Color? = null,
    val size: Float? = null
)

data class Fmt(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strike: Boolean = false,
    val color: Color? = null,
    val bg: Color? = null,
    val size: Float? = null
)

fun Fmt.applySpan(s: TextSpan) = Fmt(
    bold = s.bold ?: bold,
    italic = s.italic ?: italic,
    underline = s.underline ?: underline,
    strike = s.strike ?: strike,
    color = s.color ?: color,
    bg = s.bg ?: bg,
    size = s.size ?: size
)

fun effective(spans: List<TextSpan>, pos: Int): Fmt {
    var f = Fmt()
    for (s in spans) {
        if (pos >= s.start && pos < s.end) f = f.applySpan(s)
    }
    return f
}

private fun cutPoints(len: Int, spans: List<TextSpan>): List<Int> {
    val cuts = sortedSetOf(0, len)
    spans.forEach { s ->
        cuts.add(s.start.coerceIn(0, len))
        cuts.add(s.end.coerceIn(0, len))
    }
    return cuts.toList()
}

fun buildStyled(text: String, spans: List<TextSpan>): AnnotatedString {
    if (spans.isEmpty() || text.isEmpty()) return AnnotatedString(text)
    val pts = cutPoints(text.length, spans)
    return buildAnnotatedString {
        append(text)
        for (k in 0 until pts.size - 1) {
            val a = pts[k]
            val b = pts[k + 1]
            if (a >= b) continue
            val f = effective(spans, a)
            if (f == Fmt()) continue
            val decos = buildList {
                if (f.underline) add(TextDecoration.Underline)
                if (f.strike) add(TextDecoration.LineThrough)
            }
            addStyle(
                SpanStyle(
                    fontWeight = if (f.bold) FontWeight.Bold else null,
                    fontStyle = if (f.italic) FontStyle.Italic else null,
                    textDecoration = if (decos.isEmpty()) null else TextDecoration.combine(decos),
                    color = f.color ?: Color.Unspecified,
                    background = f.bg ?: Color.Unspecified,
                    fontSize = f.size?.sp ?: TextUnit.Unspecified
                ),
                a, b
            )
        }
    }
}

fun flatten(text: String, spans: List<TextSpan>): List<TextSpan> {
    if (text.isEmpty()) return emptyList()
    val pts = cutPoints(text.length, spans)
    val out = ArrayList<TextSpan>()
    for (k in 0 until pts.size - 1) {
        val a = pts[k]
        val b = pts[k + 1]
        if (a >= b) continue
        val f = effective(spans, a)
        if (f == Fmt()) continue
        val span = TextSpan(a, b, f.bold, f.italic, f.underline, f.strike, f.color, f.bg, f.size)
        val last = out.lastOrNull()
        if (last != null && last.end == a &&
            last.copy(start = 0, end = 0) == span.copy(start = 0, end = 0)
        ) {
            out[out.size - 1] = last.copy(end = b)
        } else {
            out.add(span)
        }
    }
    return out
}

fun commonPrefix(a: String, b: String): Int {
    val n = minOf(a.length, b.length)
    var i = 0
    while (i < n && a[i] == b[i]) i++
    return i
}

fun commonSuffix(a: String, b: String, prefix: Int): Int {
    val n = minOf(a.length, b.length) - prefix
    var i = 0
    while (i < n && a[a.length - 1 - i] == b[b.length - 1 - i]) i++
    return i
}

fun shiftSpans(old: String, new: String, spans: List<TextSpan>, prefix: Int, suffix: Int): List<TextSpan> {
    val removed = old.length - prefix - suffix
    val added = new.length - prefix - suffix
    val delta = added - removed
    val oldTail = old.length - suffix
    fun adjStart(p: Int): Int = when {
        p < prefix -> p
        p >= oldTail -> p + delta
        else -> prefix + added
    }
    fun adjEnd(p: Int): Int = when {
        p <= prefix -> p
        p >= oldTail -> p + delta
        else -> prefix
    }
    return spans.mapNotNull { s ->
        val a = adjStart(s.start).coerceIn(0, new.length)
        val b = adjEnd(s.end).coerceIn(0, new.length)
        if (b > a) s.copy(start = a, end = b) else null
    }
}

fun currentFmt(p: PageData): Fmt {
    val sel = p.field.selection
    val len = p.field.text.length
    var f = Fmt()
    if (len > 0) {
        val pos = if (sel.collapsed) (sel.start - 1).coerceAtLeast(0) else minOf(sel.start, sel.end)
        f = effective(p.spans, pos.coerceAtMost(len - 1))
    }
    if (sel.collapsed) {
        val pd = p.pending
        if (pd != null) f = f.applySpan(pd)
    }
    return f
}

// ======================= PAGE MODEL =======================
@Immutable
data class Ink(val points: List<Offset>, val color: Color, val width: Float)

@Immutable
data class PageData(
    val id: Long,
    val col: Int,
    val row: Int,
    val color: Color = Color.White,
    val field: TextFieldValue = TextFieldValue(""),
    val spans: List<TextSpan> = emptyList(),
    val pending: TextSpan? = null,
    val align: TextAlign = TextAlign.Start,
    val font: Int = 0,
    val baseSize: Float = 16f,
    val bg: Int = 0,                       // 0 none, 1 grid, 2 lines, 3 dots
    val inks: List<Ink> = emptyList()
)

data class Cand(val col: Int, val row: Int, val fromId: Long)
data class CellRange(val c0: Int, val c1: Int, val r0: Int, val r1: Int)

fun cellKey(c: Int, r: Int): Long = (c.toLong() shl 32) xor (r.toLong() and 0xffffffffL)

// ======================= VIEWMODEL =======================
@Stable
class PagesViewModel : ViewModel() {
    private var nextId = 1L
    val pages = mutableStateListOf(PageData(id = nextId++, col = 0, row = 0))

    var selectedId by mutableStateOf(1L)
    var tool by mutableStateOf(Tool.TEXT)
    var penColor by mutableStateOf(Color.Black)
    var penWidth by mutableFloatStateOf(2.5f)

    // camera
    var scale by mutableFloatStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
    var viewport by mutableStateOf(IntSize.Zero)
    var initialized = false

    private fun idx(id: Long) = pages.indexOfFirst { it.id == id }

    private fun update(id: Long, f: (PageData) -> PageData) {
        val i = idx(id)
        if (i >= 0) pages[i] = f(pages[i])
    }

    fun addPage(col: Int, row: Int, fromId: Long) {
        if (pages.any { it.col == col && it.row == row }) return
        val src = pages.firstOrNull { it.id == fromId }
        val p = PageData(
            id = nextId++, col = col, row = row,
            color = src?.color ?: Color.White,
            align = src?.align ?: TextAlign.Start,
            font = src?.font ?: 0,
            baseSize = src?.baseSize ?: 16f,
            bg = src?.bg ?: 0
        )
        pages.add(p)
        selectedId = p.id
    }

    fun deletePage(id: Long) {
        if (pages.size <= 1) return
        pages.removeAll { it.id == id }
        if (selectedId == id) selectedId = pages.first().id
    }

    fun onFieldChange(id: Long, nv: TextFieldValue, applyPending: Boolean = true) {
        update(id) { p ->
            val oldText = p.field.text
            if (nv.text == oldText) {
                val keep = if (nv.selection == p.field.selection) p.pending else null
                p.copy(field = nv, pending = keep)
            } else {
                val prefix = commonPrefix(oldText, nv.text)
                val suffix = commonSuffix(oldText, nv.text, prefix)
                val added = nv.text.length - prefix - suffix
                var spans = shiftSpans(oldText, nv.text, p.spans, prefix, suffix)
                val pend = p.pending
                if (applyPending && pend != null && added > 0) {
                    val ns = pend.copy(start = prefix, end = prefix + added)
                    val last = spans.lastOrNull()
                    spans = if (last != null && last.end == ns.start &&
                        last.copy(start = 0, end = 0) == ns.copy(start = 0, end = 0)
                    ) {
                        spans.dropLast(1) + last.copy(end = ns.end)
                    } else {
                        spans + ns
                    }
                }
                if (spans.size > 64) spans = flatten(nv.text, spans)
                p.copy(field = nv, spans = spans, pending = pend)
            }
        }
    }

    /** Bold / italic / colour / size etc. Selection par lagta hai, selection na ho to aage ke typing par. */
    fun format(id: Long, make: (Fmt) -> TextSpan) {
        val i = idx(id)
        if (i < 0) return
        val p = pages[i]
        val sel = p.field.selection
        if (!sel.collapsed) {
            val s = minOf(sel.start, sel.end)
            val e = maxOf(sel.start, sel.end)
            val t = make(effective(p.spans, s))
            var spans = p.spans + t.copy(start = s, end = e)
            if (spans.size > 64) spans = flatten(p.field.text, spans)
            pages[i] = p.copy(spans = spans)
        } else {
            val cur = currentFmt(p)
            val t = make(cur)
            val pd = p.pending
            val merged = TextSpan(
                0, 0,
                bold = t.bold ?: pd?.bold,
                italic = t.italic ?: pd?.italic,
                underline = t.underline ?: pd?.underline,
                strike = t.strike ?: pd?.strike,
                color = t.color ?: pd?.color,
                bg = t.bg ?: pd?.bg,
                size = t.size ?: pd?.size
            )
            pages[i] = p.copy(pending = merged)
        }
    }

    fun bullet(id: Long) {
        val p = pages.firstOrNull { it.id == id } ?: return
        val t = p.field.text
        val cur = p.field.selection.start.coerceIn(0, t.length)
        val lineStart = if (cur == 0) 0 else t.lastIndexOf('\n', cur - 1) + 1
        if (t.startsWith("• ", lineStart)) {
            val nt = t.substring(0, lineStart) + t.substring(lineStart + 2)
            onFieldChange(id, TextFieldValue(nt, TextRange((cur - 2).coerceAtLeast(lineStart))), false)
        } else {
            val nt = t.substring(0, lineStart) + "• " + t.substring(lineStart)
            onFieldChange(id, TextFieldValue(nt, TextRange(cur + 2)), false)
        }
    }

    fun setAlign(id: Long, a: TextAlign) = update(id) { it.copy(align = a) }
    fun cycleFont(id: Long) = update(id) { it.copy(font = (it.font + 1) % 4) }
    fun cycleBg(id: Long) = update(id) { it.copy(bg = (it.bg + 1) % 4) }
    fun setPageColor(id: Long, c: Color) = update(id) { it.copy(color = c) }
    fun addInk(id: Long, ink: Ink) = update(id) { it.copy(inks = it.inks + ink) }
    fun undoInk(id: Long) = update(id) { if (it.inks.isEmpty()) it else it.copy(inks = it.inks.dropLast(1)) }
}

// ======================= ACTIVITY =======================
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) { WorkspaceScreen() }
        }
    }
}

// ======================= WORKSPACE =======================
@Composable
fun WorkspaceScreen(vm: PagesViewModel = viewModel()) {
    val density = LocalDensity.current.density
    val pageWpx = PAGE_W * density
    val pageHpx = PAGE_H * density

    fun fitTo(col: Int, row: Int) {
        val vw = vm.viewport.width.toFloat()
        val vh = vm.viewport.height.toFloat()
        if (vw <= 0f || vh <= 0f) return
        val s = minOf(vw * 0.6f / pageWpx, vh * 0.6f / pageHpx).coerceIn(MIN_SCALE, MAX_SCALE)
        vm.scale = s
        val cx = (col + 0.5f) * pageWpx
        val cy = (row + 0.5f) * pageHpx
        vm.offset = Offset(vw / 2f - cx * s, vh / 2f - cy * s)
    }

    fun applyTransform(centroid: Offset, pan: Offset, zoom: Float) {
        val old = vm.scale
        val ns = (old * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
        val f = ns / old
        vm.offset = centroid - (centroid - vm.offset) * f + pan
        vm.scale = ns
    }

    LaunchedEffect(vm.viewport) {
        if (!vm.initialized && vm.viewport.width > 0) {
            fitTo(0, 0)
            vm.initialized = true
        }
    }

    val range by remember(pageWpx, pageHpx) {
        derivedStateOf {
            val s = vm.scale
            val o = vm.offset
            val vw = vm.viewport.width.toFloat()
            val vh = vm.viewport.height.toFloat()
            CellRange(
                floor(((0f - o.x) / s) / pageWpx).toInt(),
                floor(((vw - o.x) / s) / pageWpx).toInt(),
                floor(((0f - o.y) / s) / pageHpx).toInt(),
                floor(((vh - o.y) / s) / pageHpx).toInt()
            )
        }
    }

    val pages = vm.pages
    val cands = remember(pages.size) {
        val occupied = HashSet<Long>()
        pages.forEach { occupied.add(cellKey(it.col, it.row)) }
        val out = LinkedHashMap<Long, Cand>()
        pages.forEach { p ->
            val dirs = listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0)
            for ((dc, dr) in dirs) {
                val c = p.col + dc
                val r = p.row + dr
                val k = cellKey(c, r)
                if (!occupied.contains(k) && !out.containsKey(k)) out[k] = Cand(c, r, p.id)
            }
        }
        out.values.toList()
    }

    val r = range
    val visiblePages = pages.filter { it.col in r.c0..r.c1 && it.row in r.r0..r.r1 }
    val visibleCands = cands.filter { it.col in r.c0..r.c1 && it.row in r.r0..r.r1 }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .clipToBounds()
                .onSizeChanged { vm.viewport = it }
                // do ungli: zoom + pan
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val pressedCount = event.changes.count { it.pressed }
                            if (pressedCount >= 2) {
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val centroid = event.calculateCentroid(useCurrent = true)
                                if (zoom != 1f || pan != Offset.Zero) {
                                    applyTransform(centroid, pan, zoom)
                                }
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                // ek ungli: khali jagah par drag = pan
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        vm.offset = vm.offset + drag
                    }
                }
        ) {
            visiblePages.forEach { p -> key(p.id) { PageView(p, vm) } }
            visibleCands.forEach { c -> key(cellKey(c.col, c.row)) { AddButton(c, vm) } }
        }

        BottomBar(vm) {
            val p = vm.pages.firstOrNull { it.id == vm.selectedId }
            fitTo(p?.col ?: 0, p?.row ?: 0)
        }
    }
}

// ======================= + BUTTON =======================
@Composable
fun AddButton(c: Cand, vm: PagesViewModel) {
    val density = LocalDensity.current.density
    val pw = PAGE_W * density
    val ph = PAGE_H * density
    val btn = 56f * density
    Box(
        Modifier
            .graphicsLayer {
                val s = vm.scale
                val f = max(1f, 0.5f / s)
                scaleX = f
                scaleY = f
                translationX = vm.offset.x + (c.col + 0.5f) * pw * s - btn / 2f
                translationY = vm.offset.y + (c.row + 0.5f) * ph * s - btn / 2f
            }
            .size(56.dp)
            .clip(CircleShape)
            .clickable { vm.addPage(c.col, c.row, c.fromId) },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Add, contentDescription = "Add page", tint = PURPLE, modifier = Modifier.size(44.dp))
    }
}

// ======================= ONE PAGE =======================
private fun snapV(v: Float, on: Boolean): Float = if (on) (v / GRID).roundToInt() * GRID else v

private fun DrawScope.drawInk(points: List<Offset>, color: Color, widthDp: Float) {
    if (points.isEmpty()) return
    val d = density
    val w = widthDp * d
    if (points.size == 1) {
        drawCircle(color, w / 2f, points[0] * d)
        return
    }
    if (points.size == 2) {
        drawLine(color, points[0] * d, points[1] * d, w, StrokeCap.Round)
        return
    }
    val path = Path()
    path.moveTo(points[0].x * d, points[0].y * d)
    for (i in 1 until points.size) path.lineTo(points[i].x * d, points[i].y * d)
    drawPath(path, color, style = Stroke(width = w, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@Composable
fun PageView(page: PageData, vm: PagesViewModel) {
    val density = LocalDensity.current.density
    val pw = PAGE_W * density
    val ph = PAGE_H * density
    val selected = vm.selectedId == page.id
    val light = page.color.luminance() > 0.5f
    val textColor = if (light) Color.Black else Color.White
    var draft by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val spans = page.spans
    val transformation = remember(spans) {
        VisualTransformation { text ->
            TransformedText(buildStyled(text.text, spans), OffsetMapping.Identity)
        }
    }
    val family = when (page.font) {
        1 -> FontFamily.Serif
        2 -> FontFamily.Monospace
        3 -> FontFamily.Cursive
        else -> FontFamily.Default
    }

    Box(
        Modifier
            .wrapContentSize(Alignment.TopStart, unbounded = true)
            .graphicsLayer {
                val s = vm.scale
                transformOrigin = TransformOrigin(0f, 0f)
                scaleX = s
                scaleY = s
                translationX = vm.offset.x + page.col * pw * s
                translationY = vm.offset.y + page.row * ph * s
            }
            .size(PAGE_W.dp, PAGE_H.dp)
            .background(page.color)
    ) {
        // background: grid / lines / dots
        if (page.bg != 0) {
            Canvas(Modifier.matchParentSize()) {
                val lc = (if (light) Color.Black else Color.White).copy(alpha = 0.18f)
                val step = (if (page.bg == 2) 28f else GRID).dp.toPx()
                when (page.bg) {
                    1 -> {
                        var x = 0f
                        while (x <= size.width) {
                            drawLine(lc, Offset(x, 0f), Offset(x, size.height), 1f)
                            x += step
                        }
                        var y = 0f
                        while (y <= size.height) {
                            drawLine(lc, Offset(0f, y), Offset(size.width, y), 1f)
                            y += step
                        }
                    }
                    2 -> {
                        var y = step
                        while (y <= size.height) {
                            drawLine(lc, Offset(0f, y), Offset(size.width, y), 1f)
                            y += step
                        }
                    }
                    else -> {
                        var y = step
                        while (y < size.height) {
                            var x = step
                            while (x < size.width) {
                                drawCircle(lc, 1.5.dp.toPx(), Offset(x, y))
                                x += step
                            }
                            y += step
                        }
                    }
                }
            }
        }

        // text
        Box(
            Modifier
                .matchParentSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            BasicTextField(
                value = page.field,
                onValueChange = { vm.onFieldChange(page.id, it) },
                textStyle = TextStyle(
                    color = textColor,
                    fontSize = page.baseSize.sp,
                    fontFamily = family,
                    textAlign = page.align
                ),
                cursorBrush = SolidColor(textColor),
                visualTransformation = transformation,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = (PAGE_H - 48f).dp)
                    .onFocusChanged { if (it.isFocused) vm.selectedId = page.id },
                decorationBox = { inner ->
                    Box {
                        if (page.field.text.isEmpty()) {
                            Text(
                                "यहाँ लिखें…",
                                color = textColor.copy(alpha = 0.35f),
                                fontSize = page.baseSize.sp
                            )
                        }
                        inner()
                    }
                }
            )
        }

        // lines / pen strokes
        Canvas(Modifier.matchParentSize()) {
            page.inks.forEach { drawInk(it.points, it.color, it.width) }
            if (draft.size >= 2) drawInk(draft, vm.penColor, vm.penWidth)
        }

        // draw overlay (sirf Line / Pen tool me)
        if (vm.tool != Tool.TEXT) {
            val tool = vm.tool
            val snap = page.bg == 1
            Box(
                Modifier
                    .matchParentSize()
                    .pointerInput(tool, snap, page.id) {
                        val dens = density
                        fun conv(o: Offset): Offset {
                            val x = snapV((o.x / dens).coerceIn(0f, PAGE_W), snap)
                            val y = snapV((o.y / dens).coerceIn(0f, PAGE_H), snap)
                            return Offset(x, y)
                        }
                        detectDragGestures(
                            onDragStart = { o ->
                                vm.selectedId = page.id
                                draft = listOf(conv(o))
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                if (draft.isNotEmpty()) {
                                    val p = conv(change.position)
                                    draft = if (tool == Tool.LINE) listOf(draft.first(), p) else draft + p
                                }
                            },
                            onDragEnd = {
                                if (draft.size >= 2) {
                                    vm.addInk(page.id, Ink(draft, vm.penColor, vm.penWidth))
                                }
                                draft = emptyList()
                            },
                            onDragCancel = { draft = emptyList() }
                        )
                    }
            )
        }

        // border (selected page highlight)
        Box(
            Modifier
                .matchParentSize()
                .border(
                    if (selected) 2.dp else 0.5.dp,
                    if (selected) PURPLE else Color(0x33000000)
                )
        )
    }
}

// ======================= TOOLBAR =======================
@Composable
fun BarIcon(
    icon: ImageVector,
    active: Boolean = false,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (active) PURPLE.copy(alpha = 0.35f) else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorDialog(
    title: String,
    onPick: (Color) -> Unit,
    onDismiss: () -> Unit,
    extraLabel: String? = null,
    onExtra: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("बंद करें") } },
        dismissButton = {
            if (extraLabel != null) {
                TextButton(onClick = { onExtra(); onDismiss() }) { Text(extraLabel) }
            }
        },
        title = { Text(title) },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                palette.forEach { c ->
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(c, CircleShape)
                            .border(1.dp, Color.Gray, CircleShape)
                            .clickable { onPick(c); onDismiss() }
                    )
                }
            }
        }
    )
}

@Composable
fun BottomBar(vm: PagesViewModel, onFit: () -> Unit) {
    val page = vm.pages.firstOrNull { it.id == vm.selectedId } ?: vm.pages.first()
    val fmt = currentFmt(page)
    var dialog by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF1C1C1E))
            .padding(vertical = 4.dp)
    ) {
        if (vm.tool == Tool.TEXT) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BarIcon(Icons.Default.FormatBold, fmt.bold) {
                    vm.format(page.id) { f -> TextSpan(0, 0, bold = !f.bold) }
                }
                BarIcon(Icons.Default.FormatItalic, fmt.italic) {
                    vm.format(page.id) { f -> TextSpan(0, 0, italic = !f.italic) }
                }
                BarIcon(Icons.Default.FormatUnderlined, fmt.underline) {
                    vm.format(page.id) { f -> TextSpan(0, 0, underline = !f.underline) }
                }
                BarIcon(Icons.Default.StrikethroughS, fmt.strike) {
                    vm.format(page.id) { f -> TextSpan(0, 0, strike = !f.strike) }
                }
                BarIcon(Icons.Default.FormatColorText, false, fmt.color ?: Color.White) { dialog = "text" }
                BarIcon(Icons.Default.Highlight, fmt.bg != null && fmt.bg != Color.Transparent, fmt.bg ?: Color.White) {
                    dialog = "hl"
                }
                Spacer(Modifier.width(6.dp))
                BarIcon(Icons.Default.Remove) {
                    vm.format(page.id) { f -> TextSpan(0, 0, size = ((f.size ?: page.baseSize) - 2f).coerceAtLeast(8f)) }
                }
                Text(
                    "${(fmt.size ?: page.baseSize).toInt()}",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                BarIcon(Icons.Default.Add) {
                    vm.format(page.id) { f -> TextSpan(0, 0, size = ((f.size ?: page.baseSize) + 2f).coerceAtMost(96f)) }
                }
                Spacer(Modifier.width(6.dp))
                BarIcon(Icons.Default.FormatAlignLeft, page.align == TextAlign.Start) { vm.setAlign(page.id, TextAlign.Start) }
                BarIcon(Icons.Default.FormatAlignCenter, page.align == TextAlign.Center) { vm.setAlign(page.id, TextAlign.Center) }
                BarIcon(Icons.Default.FormatAlignRight, page.align == TextAlign.End) { vm.setAlign(page.id, TextAlign.End) }
                BarIcon(Icons.Default.FormatListBulleted) { vm.bullet(page.id) }
                BarIcon(Icons.Default.TextFields, page.font != 0) { vm.cycleFont(page.id) }
            }
        }
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BarIcon(Icons.Default.Edit, vm.tool == Tool.TEXT) { vm.tool = Tool.TEXT }
            BarIcon(Icons.Default.HorizontalRule, vm.tool == Tool.LINE) { vm.tool = Tool.LINE }
            BarIcon(Icons.Default.Brush, vm.tool == Tool.PEN) { vm.tool = Tool.PEN }
            if (vm.tool != Tool.TEXT) {
                BarIcon(Icons.Default.Palette, false, vm.penColor) { dialog = "pen" }
                BarIcon(Icons.Default.Undo) { vm.undoInk(page.id) }
            }
            Spacer(Modifier.width(6.dp))
            BarIcon(Icons.Default.FormatColorFill, false, page.color) { dialog = "page" }
            BarIcon(Icons.Default.GridOn, page.bg != 0) { vm.cycleBg(page.id) }
            BarIcon(Icons.Default.Delete) { vm.deletePage(page.id) }
            BarIcon(Icons.Default.CenterFocusStrong) { onFit() }
        }
    }

    when (dialog) {
        "text" -> ColorDialog(
            "टेक्स्ट का रंग",
            onPick = { c -> vm.format(page.id) { TextSpan(0, 0, color = c) } },
            onDismiss = { dialog = "" }
        )
        "hl" -> ColorDialog(
            "हाइलाइट रंग",
            onPick = { c -> vm.format(page.id) { TextSpan(0, 0, bg = c) } },
            onDismiss = { dialog = "" },
            extraLabel = "हटाएँ",
            onExtra = { vm.format(page.id) { TextSpan(0, 0, bg = Color.Transparent) } }
        )
        "pen" -> ColorDialog(
            "लाइन / पेन का रंग",
            onPick = { c -> vm.penColor = c },
            onDismiss = { dialog = "" }
        )
        "page" -> ColorDialog(
            "पेज का रंग",
            onPick = { c -> vm.setPageColor(page.id, c) },
            onDismiss = { dialog = "" }
        )
    }
}
