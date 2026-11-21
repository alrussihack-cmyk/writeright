package com.writeright.ai

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import java.util.Locale

// ------------------------------------------------------------------ look & feel

private val Mint = Color(0xFF2EE3B8)
private val Dark = Color(0xFF10201B)
private val Yellow = Color(0xFFFFD84A)
private val PaleRed = Color(0xFFFFE3E3)
private val Red = Color(0xFFD9273C)
private val PaleTile = Color(0xFFDDF6EE)
private val TipYellow = Color(0xFFFFF4C7)
private val Scrim = Color(0x5A10231D)

@Immutable
private data class Palette(
    val bg: Color, val ink: Color, val edge: Color, val shadow: Color, val rule: Color,
    val surface: Color, val sheet: Color, val muted: Color, val good: Color
)

private val LightPalette = Palette(
    bg = Color.White, ink = Dark, edge = Dark, shadow = Dark, rule = Color(0xFFD0E8E0),
    surface = Color.White, sheet = Color(0xFFF1FFFA), muted = Color(0xFF4A665E), good = Color(0xFF0B7A5E)
)
private val DarkPalette = Palette(
    bg = Color(0xFF13201C), ink = Color(0xFFEAF7F2), edge = Color(0xFFEAF7F2), shadow = Color(0xFF000000),
    rule = Color(0xFF2A4039), surface = Color(0xFF1C2D27), sheet = Color(0xFF182823),
    muted = Color(0xFF9DB8AF), good = Color(0xFF4EE8B8)
)
private val LocalPalette = staticCompositionLocalOf { LightPalette }

private val Cairo = FontFamily(
    listOf(400, 600, 700, 900).map { w ->
        Font(R.font.cairo, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)
private val Hand = FontFamily(
    Font(R.font.kalam_regular, FontWeight.Normal),
    Font(R.font.kalam_bold, FontWeight.Bold)
)

/** Hard offset shadow + thick outline: the "sticker" look used across the app. */
private fun Modifier.brutal(
    shape: Shape, fill: Color, edge: Color, shadow: Color,
    width: Dp = 2.5.dp, offset: Dp = 4.dp
): Modifier = this
    .drawBehind {
        if (offset > 0.dp) {
            val o = offset.toPx()
            val outline = shape.createOutline(size, layoutDirection, this)
            translate(o, o) { drawOutline(outline, color = shadow) }
        }
    }
    .background(fill, shape)
    .border(width, edge, shape)

private enum class Mode { WRITE, RESULTS, EXPLAIN }
private enum class Tool { PEN, ERASER }

private fun mistakesTitle(n: Int): String = when (n) {
    1 -> "استنى! فيه غلطة"
    2 -> "استنى! فيه غلطتين"
    in 3..10 -> "استنى! فيه ${toArabicDigits(n)} غلطات"
    else -> "استنى! فيه ${toArabicDigits(n)} غلطة"
}

private fun mistakesChip(n: Int): String = if (n == 1) "١ غلطة" else "${toArabicDigits(n)} غلطات"

// ------------------------------------------------------------------ activity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { WriteRightApp() }
    }
}

@Composable
private fun WriteRightApp() {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val rowGap = ROW_GAP_DP * density

    var dark by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var tool by remember { mutableStateOf(Tool.PEN) }
    var strokes by remember { mutableStateOf(emptyList<StrokeData>()) }
    var demo by remember { mutableStateOf<Demo?>(null) }
    var analysis by remember { mutableStateOf<Analysis?>(null) }
    var mode by remember { mutableStateOf(Mode.WRITE) }
    var selected by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("") }
    var statusIsError by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var lastAnalyzed by remember { mutableStateOf<List<StrokeData>?>(null) }

    val handRegular = remember { context.resources.getFont(R.font.kalam_regular) }
    val ttsHolder = remember { arrayOfNulls<TextToSpeech>(1) }
    DisposableEffect(Unit) {
        ttsHolder[0] = TextToSpeech(context) { code ->
            if (code == TextToSpeech.SUCCESS) ttsHolder[0]?.language = Locale.US
        }
        onDispose { ttsHolder[0]?.shutdown() }
    }

    fun clearBoard() {
        strokes = emptyList()
        demo = null
        analysis = null
        mode = Mode.WRITE
        status = ""
        lastAnalyzed = null
    }

    fun dismissResults() {
        demo = null
        analysis = null
        mode = Mode.WRITE
    }

    fun runAnalysis() {
        if (strokes.isEmpty() || busy) return
        val snapshot = strokes
        busy = true
        statusIsError = false
        status = "جاري تجهيز القراءة..."
        lastAnalyzed = snapshot
        analyzeInk(
            context, snapshot, density, rowGap,
            onStatus = { statusIsError = false; status = it },
            onSuccess = { result ->
                busy = false
                status = ""
                if (snapshot === strokes) {
                    analysis = result
                    selected = 0
                    mode = Mode.RESULTS
                }
            },
            onError = { message ->
                busy = false
                statusIsError = true
                status = message
            }
        )
    }

    // "الـ AI بيراقب": read the board by itself once the writer pauses.
    LaunchedEffect(strokes, mode, busy) {
        if (mode == Mode.WRITE && demo == null && !busy && strokes.isNotEmpty() && strokes !== lastAnalyzed) {
            delay(2200)
            runAnalysis()
        }
    }

    BackHandler(enabled = mode != Mode.WRITE) {
        if (mode == Mode.EXPLAIN) mode = Mode.RESULTS else dismissResults()
    }

    val palette = if (dark) DarkPalette else LightPalette
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr,
        LocalPalette provides palette
    ) {
        ProvideTextStyle(TextStyle(fontFamily = Cairo, color = palette.ink)) {
            Box(Modifier.fillMaxSize().background(palette.bg)) {
                Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
                    BoardHeader(
                        dark = dark,
                        analysis = analysis.takeIf { mode != Mode.WRITE },
                        menu = menu,
                        onMenu = { menu = it },
                        onTheme = { dark = !dark },
                        onDismiss = { dismissResults() },
                        onClear = { clearBoard() },
                        onExample = {
                            clearBoard()
                            val d = buildDemo(handRegular, density, rowGap)
                            demo = d
                            analysis = d.analysis
                            selected = 0
                            mode = Mode.RESULTS
                        }
                    )
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        WritingBoard(
                            strokes = strokes,
                            tool = tool,
                            demo = demo,
                            analysis = analysis.takeIf { mode != Mode.WRITE },
                            onStrokes = { next ->
                                strokes = next
                                if (mode != Mode.WRITE || demo != null) {
                                    demo = null
                                    analysis = null
                                    mode = Mode.WRITE
                                }
                                if (!busy) status = ""
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        if (status.isNotBlank()) {
                            StatusBanner(status, statusIsError, Modifier.align(Alignment.TopCenter))
                        }
                        val blank = strokes.isEmpty() && demo == null
                        AnimatedVisibility(
                            visible = blank && mode == Mode.WRITE && status.isBlank(),
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
                            enter = fadeIn(), exit = fadeOut()
                        ) {
                            ExampleButton {
                                clearBoard()
                                val d = buildDemo(handRegular, density, rowGap)
                                demo = d
                                analysis = d.analysis
                                selected = 0
                                mode = Mode.RESULTS
                            }
                        }
                        AnimatedVisibility(
                            visible = mode == Mode.RESULTS && analysis != null,
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
                            enter = slideInVertically { it / 2 } + fadeIn(),
                            exit = fadeOut()
                        ) {
                            analysis?.let { a ->
                                ResultCard(a) { index -> selected = index; mode = Mode.EXPLAIN }
                            }
                        }
                    }
                    Toolbar(
                        tool = tool,
                        busy = busy,
                        onTool = { tool = it },
                        onUndo = {
                            if (demo != null) clearBoard()
                            else if (strokes.isNotEmpty()) {
                                strokes = strokes.dropLast(1)
                                if (mode != Mode.WRITE) dismissResults()
                            }
                        },
                        onWatch = {
                            if (strokes.isEmpty()) {
                                statusIsError = true
                                status = "اكتب كلمة أو جملة الأول، وأنا هراجعها."
                            } else {
                                lastAnalyzed = null
                                runAnalysis()
                            }
                        }
                    )
                }

                val fix = analysis?.findings?.getOrNull(selected)?.fix
                if (mode == Mode.EXPLAIN && fix != null) {
                    ExplainSheet(
                        fix = fix,
                        index = selected,
                        total = analysis?.findings?.size ?: 1,
                        onDismiss = { mode = Mode.RESULTS },
                        onWriteAgain = {
                            val word = fix.corrected
                            clearBoard()
                            statusIsError = false
                            status = "اكتب كلمة $word على اللوحة"
                        },
                        onSpeak = { ttsHolder[0]?.speak(fix.corrected, TextToSpeech.QUEUE_FLUSH, null, "word") }
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ header

@Composable
private fun BoardHeader(
    dark: Boolean,
    analysis: Analysis?,
    menu: Boolean,
    onMenu: (Boolean) -> Unit,
    onTheme: () -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onExample: () -> Unit
) {
    Box(Modifier.fillMaxWidth().height(92.dp).padding(horizontal = 18.dp)) {
        Box(Modifier.align(Alignment.CenterStart)) {
            if (analysis == null) {
                SquareButton(onTheme, if (dark) "الوضع الفاتح" else "الوضع الداكن") {
                    Icon(if (dark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, null, Modifier.size(26.dp), tint = LocalPalette.current.ink)
                }
            } else {
                CountChip(analysis.findings.size, onDismiss)
            }
        }
        BrandPill(Modifier.align(Alignment.Center))
        Box(Modifier.align(Alignment.CenterEnd)) {
            SquareButton({ onMenu(true) }, "القائمة") {
                Icon(Icons.Rounded.Menu, null, Modifier.size(26.dp), tint = LocalPalette.current.ink)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { onMenu(false) }, properties = PopupProperties(focusable = true)) {
                DropdownMenuItem(
                    text = { Text("مسح اللوحة") },
                    leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null) },
                    onClick = { onClear(); onMenu(false) }
                )
                DropdownMenuItem(
                    text = { Text("شوف مثال تصحيح") },
                    onClick = { onExample(); onMenu(false) }
                )
            }
        }
    }
}

@Composable
private fun BrandPill(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(50)
    Box(modifier.brutal(shape, Mint, p.edge, p.shadow, 3.dp, 5.dp).padding(horizontal = 20.dp, vertical = 9.dp)) {
        Text("Write Right AI", color = Dark, fontSize = 21.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
private fun SquareButton(onClick: () -> Unit, description: String, content: @Composable () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(18.dp)
    Box(
        Modifier.size(56.dp)
            .brutal(shape, p.surface, p.edge, p.shadow, 2.5.dp, 4.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun CountChip(count: Int, onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(18.dp)
    val ok = count == 0
    Box(
        Modifier.height(56.dp)
            .brutal(shape, if (ok) Mint else PaleRed, p.edge, p.shadow, 2.5.dp, 4.dp)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(if (ok) "ممتاز" else mistakesChip(count), color = Dark, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

// ------------------------------------------------------------------ board

private fun DrawScope.drawInk(points: List<PointData>, color: Color, width: Float) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(color, width * 0.6f, Offset(points[0].x, points[0].y))
        return
    }
    val path = Path()
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 2) {
        path.lineTo(points[1].x, points[1].y)
    } else {
        for (i in 1 until points.size - 1) {
            val mx = (points[i].x + points[i + 1].x) / 2f
            val my = (points[i].y + points[i + 1].y) / 2f
            path.quadraticBezierTo(points[i].x, points[i].y, mx, my)
        }
        path.lineTo(points.last().x, points.last().y)
    }
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

@Composable
private fun WritingBoard(
    strokes: List<StrokeData>,
    tool: Tool,
    demo: Demo?,
    analysis: Analysis?,
    onStrokes: (List<StrokeData>) -> Unit,
    modifier: Modifier
) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val handBold = remember { context.resources.getFont(R.font.kalam_bold) }
    val handRegular = remember { context.resources.getFont(R.font.kalam_regular) }

    val strokesNow by rememberUpdatedState(strokes)
    val toolNow by rememberUpdatedState(tool)
    val onStrokesNow by rememberUpdatedState(onStrokes)
    var active by remember { mutableStateOf(emptyList<PointData>()) }
    var eraser by remember { mutableStateOf<Offset?>(null) }

    // Corrections and circles appear together once the AI has read the board.
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(analysis) {
        reveal.snapTo(0f)
        if (analysis != null) reveal.animateTo(1f, tween(500))
    }

    val demoPaint = remember(p.ink, demo?.sizePx) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            typeface = handRegular
            color = p.ink.toArgb()
            textSize = demo?.sizePx ?: 40f
        }
    }
    val corrPaint = remember(p.good) {
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            typeface = handBold
            color = p.good.toArgb()
        }
    }

    Box(modifier) {
        Canvas(
            Modifier.fillMaxSize().pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val erasing = toolNow == Tool.ERASER
                    val radius = 18.dp.toPx()
                    var working = strokesNow
                    if (erasing) {
                        eraser = down.position
                        val next = eraseAt(working, down.position, radius)
                        if (next !== working) {
                            working = next
                            onStrokesNow(next)
                        }
                    } else {
                        active = listOf(PointData(down.position.x, down.position.y, System.currentTimeMillis()))
                    }
                    down.consume()
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (change.positionChanged()) {
                            val pos = change.position
                            if (erasing) {
                                eraser = pos
                                val next = eraseAt(working, pos, radius)
                                if (next !== working) {
                                    working = next
                                    onStrokesNow(next)
                                }
                            } else {
                                active = active + PointData(pos.x, pos.y, System.currentTimeMillis())
                            }
                            change.consume()
                        }
                    }
                    if (!erasing && active.isNotEmpty()) onStrokesNow(strokesNow + StrokeData(active))
                    active = emptyList()
                    eraser = null
                }
            }
        ) {
            val gap = ROW_GAP_DP.dp.toPx()
            var y = 0f
            while (y < size.height) {
                drawLine(p.rule, Offset(0f, y), Offset(size.width, y), 2.dp.toPx())
                y += gap
            }

            demo?.let { d ->
                drawIntoCanvas { c ->
                    d.lines.forEach { line -> c.nativeCanvas.drawText(line.text, line.x, line.baseline, demoPaint) }
                }
            }

            val inkWidth = 4.2.dp.toPx()
            strokes.forEach { drawInk(it.points, p.ink, inkWidth) }
            drawInk(active, p.ink, inkWidth)

            analysis?.findings?.forEach { f ->
                val a = reveal.value
                drawRoundRect(
                    color = Red.copy(alpha = a),
                    topLeft = f.oval.topLeft,
                    size = f.oval.size,
                    cornerRadius = CornerRadius(minOf(f.oval.width, f.oval.height) / 2f),
                    style = Stroke(2.6.dp.toPx())
                )
                corrPaint.textSize = f.corrSize
                corrPaint.color = p.good.toArgb()
                corrPaint.alpha = (a * 255f).toInt()
                drawIntoCanvas { c -> c.nativeCanvas.drawText(f.fix.corrected, f.corrLeft, f.corrBaseline, corrPaint) }
            }

            eraser?.let { e ->
                drawCircle(p.muted.copy(alpha = 0.18f), 18.dp.toPx(), e)
                drawCircle(p.muted, 18.dp.toPx(), e, style = Stroke(1.5.dp.toPx()))
            }
        }

        if (strokes.isEmpty() && demo == null && active.isEmpty() && analysis == null) {
            Column(
                Modifier.align(Alignment.Center).padding(horizontal = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("write anything...", fontSize = 42.sp, fontFamily = Hand, fontWeight = FontWeight.Bold, color = p.muted, textAlign = TextAlign.Center)
                Text("اكتب أي كلمة أو جملة بالإنجليزي بإيدك، وأنا هصححها قدامك على اللوحة", fontSize = 17.sp, color = p.muted, textAlign = TextAlign.Center, lineHeight = 27.sp)
            }
        }
    }
}

@Composable
private fun StatusBanner(text: String, isError: Boolean, modifier: Modifier) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth()
            .brutal(shape, if (isError) PaleRed else Color(0xFFD9FBEF), p.edge, p.shadow, 2.dp, 3.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text, color = Dark, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ExampleButton(onClick: () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(50)
    Row(
        Modifier.clip(shape).background(Dark, shape).border(2.dp, p.edge, shape).clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Rounded.ChevronLeft, null, Modifier.size(22.dp), tint = Mint)
        Text("شوف مثال تصحيح", color = Mint, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

// ------------------------------------------------------------------ results card

@Composable
private fun ResultCard(analysis: Analysis, onOpen: (Int) -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(30.dp)
    val count = analysis.findings.size
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .brutal(shape, Mint, p.edge, p.shadow, 2.5.dp, 5.dp)
            .clip(shape)
            .clickable(enabled = count > 0) { onOpen(0) }
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (count == 0) {
            Text("ممتاز! مفيش أي غلطة", Modifier.fillMaxWidth(), color = Dark, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Right)
            Text("اكتب جملة تانية وأنا هراجعها", Modifier.fillMaxWidth(), color = Dark, fontSize = 15.sp, textAlign = TextAlign.Right)
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ChevronLeft, null, Modifier.size(30.dp), tint = Dark)
                Text(mistakesTitle(count), Modifier.weight(1f), color = Dark, fontSize = 22.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Right)
            }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                analysis.findings.forEachIndexed { index, f ->
                    val chip = RoundedCornerShape(50)
                    Row(
                        Modifier.clip(chip).background(Color.White, chip).border(2.5.dp, Dark, chip)
                            .clickable { onOpen(index) }.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(f.fix.original, color = Dark, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Icon(Icons.Rounded.ArrowForward, null, Modifier.size(16.dp), tint = Dark)
                        Text(f.fix.corrected, color = Dark, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
            Text("اضغط هنا عشان أشرح لك كل غلطة", Modifier.fillMaxWidth(), color = Dark, fontSize = 15.sp, textAlign = TextAlign.Right)
        }
    }
}

// ------------------------------------------------------------------ toolbar

@Composable
private fun EraserIcon(tint: Color) {
    Canvas(Modifier.size(26.dp)) {
        val w = 2.2.dp.toPx()
        rotate(-40f, center) {
            drawRoundRect(
                tint, Offset(size.width * 0.18f, size.height * 0.3f), Size(size.width * 0.64f, size.height * 0.4f),
                CornerRadius(3.dp.toPx()), style = Stroke(w, cap = StrokeCap.Round)
            )
            drawLine(tint, Offset(size.width * 0.46f, size.height * 0.3f), Offset(size.width * 0.46f, size.height * 0.7f), w)
        }
        drawLine(tint, Offset(size.width * 0.2f, size.height * 0.88f), Offset(size.width * 0.8f, size.height * 0.88f), w, StrokeCap.Round)
    }
}

@Composable
private fun ToolButton(selected: Boolean, description: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier.size(52.dp)
            .background(if (selected) Mint else p.surface, shape)
            .border(2.5.dp, p.edge, shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun Toolbar(tool: Tool, busy: Boolean, onTool: (Tool) -> Unit, onUndo: () -> Unit, onWatch: () -> Unit) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 18.dp, top = 12.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val group = RoundedCornerShape(26.dp)
        Row(
            Modifier.brutal(group, p.surface, p.edge, p.shadow, 2.5.dp, 4.dp).padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolButton(tool == Tool.PEN, "قلم", { onTool(Tool.PEN) }) {
                Icon(Icons.Rounded.Edit, null, Modifier.size(24.dp), tint = if (tool == Tool.PEN) Dark else p.ink)
            }
            ToolButton(tool == Tool.ERASER, "ممحاة", { onTool(Tool.ERASER) }) {
                EraserIcon(if (tool == Tool.ERASER) Dark else p.ink)
            }
            ToolButton(false, "تراجع", onUndo) {
                Icon(Icons.Rounded.Undo, null, Modifier.size(24.dp), tint = p.ink)
            }
        }

        val pill = RoundedCornerShape(50)
        val pulse = rememberInfiniteTransition(label = "watch")
        val dotAlpha by pulse.animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
            label = "dot"
        )
        Row(
            Modifier.height(56.dp)
                .brutal(pill, Yellow, p.edge, p.shadow, 2.5.dp, 4.dp)
                .clip(pill)
                .clickable(enabled = !busy, onClick = onWatch)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Dark, strokeWidth = 2.5.dp)
                Text("بيقرا خطك...", color = Dark, fontSize = 16.sp, fontWeight = FontWeight.Black)
            } else {
                Text("الـ AI بيراقب", color = Dark, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Box(Modifier.size(12.dp).alpha(dotAlpha).background(Dark, CircleShape))
            }
        }
    }
}

// ------------------------------------------------------------------ explanation sheet

@Composable
private fun WordPill(text: String, fill: Color, crossed: Boolean, modifier: Modifier) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier.height(56.dp).brutal(shape, fill, p.edge, p.shadow, 2.5.dp, 0.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, color = Dark, fontSize = if (text.length > 9) 22.sp else 28.sp, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.padding(horizontal = 10.dp)
        )
        if (crossed) {
            Canvas(Modifier.matchParentSize()) {
                val inset = 14.dp.toPx()
                drawLine(Red, Offset(inset, size.height / 2f), Offset(size.width - inset, size.height / 2f), 3.5.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun ExplainSheet(
    fix: WordFix,
    index: Int,
    total: Int,
    onDismiss: () -> Unit,
    onWriteAgain: () -> Unit,
    onSpeak: () -> Unit
) {
    val p = LocalPalette.current
    val none = remember { MutableInteractionSource() }
    val top = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)

    Box(Modifier.fillMaxSize().background(Scrim).clickable(interactionSource = none, indication = null, onClick = onDismiss)) {
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.64f)
                .clip(top).background(p.sheet).border(3.dp, p.edge, top)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(start = 22.dp, end = 22.dp, top = 14.dp)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(Modifier.width(48.dp).height(6.dp).background(p.edge, CircleShape).align(Alignment.CenterHorizontally))

            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    // RTL progress: mistake 1 sits on the right.
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (i in total - 1 downTo 0) {
                            Box(
                                Modifier.width(24.dp).height(7.dp)
                                    .background(if (i == index) p.edge else p.rule, CircleShape)
                            )
                        }
                    }
                    Text(
                        "الغلطة ${toArabicDigits(index + 1)} من ${toArabicDigits(total)}",
                        fontSize = 22.sp, fontWeight = FontWeight.Black, color = p.ink
                    )
                }

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    WordPill(fix.original, PaleRed, true, Modifier.weight(1f))
                    Icon(Icons.Rounded.ArrowForward, null, Modifier.size(30.dp), tint = p.ink)
                    WordPill(fix.corrected, Mint, false, Modifier.weight(1f))
                }

                val box = RoundedCornerShape(26.dp)
                Column(
                    Modifier.fillMaxWidth().brutal(box, p.surface, p.edge, p.shadow, 2.5.dp, 0.dp).padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(fix.label, color = p.muted, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        val tileSize = if (fix.corrected.length > 8) 18.sp else 24.sp
                        val tile = RoundedCornerShape(12.dp)
                        fix.corrected.forEachIndexed { i, ch ->
                            val hot = i >= fix.fixStart && i < fix.fixEnd
                            Box(
                                Modifier.weight(1f).height(52.dp)
                                    .background(if (hot) Mint else PaleTile, tile)
                                    .border(2.5.dp, Dark, tile),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(ch.toString(), color = Dark, fontSize = tileSize, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }

                val tipShape = RoundedCornerShape(26.dp)
                Row(
                    Modifier.fillMaxWidth().brutal(tipShape, TipYellow, p.edge, p.shadow, 2.5.dp, 0.dp).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(fix.tip, Modifier.weight(1f), color = Dark, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Right, lineHeight = 25.sp)
                    Icon(Icons.Rounded.Lightbulb, null, Modifier.size(28.dp), tint = Dark)
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                val primary = RoundedCornerShape(24.dp)
                Row(
                    Modifier.weight(1f).height(64.dp)
                        .brutal(primary, Dark, Dark, Mint, 2.5.dp, 4.dp)
                        .clip(primary)
                        .clickable(onClick = onWriteAgain),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("اكتبها تاني على اللوحة", color = Mint, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Spacer(Modifier.width(10.dp))
                    Icon(Icons.Rounded.Edit, null, Modifier.size(20.dp), tint = Mint)
                }
                val side = RoundedCornerShape(22.dp)
                Box(
                    Modifier.size(64.dp)
                        .brutal(side, p.surface, p.edge, p.shadow, 2.5.dp, 4.dp)
                        .clip(side)
                        .clickable(onClick = onSpeak)
                        .semantics { contentDescription = "سمّع الكلمة" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.VolumeUp, null, Modifier.size(30.dp), tint = p.ink)
                }
            }
        }
    }
}
