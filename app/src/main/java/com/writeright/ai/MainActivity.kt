package com.writeright.ai

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Eraser
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Pen
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import java.util.Locale

private val Mint = Color(0xFF22D7B2)
private val Dark = Color(0xFF10231D)
private val Rule = Color(0xFFCDE9E0)
private val Muted = Color(0xFF49665E)
private val Yellow = Color(0xFFFFD94E)
private val PaleMint = Color(0xFFF0FFF9)
private val Red = Color(0xFFE52E43)
private val PaleRed = Color(0xFFFFE7E5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WriteRightApp() }
    }
}

private enum class Page { WRITE, RESULTS, EXPLANATION }
private enum class Tool { PEN, ERASER }
private data class PointData(val x: Float, val y: Float, val time: Long)
private data class StrokeData(val points: List<PointData>, val eraser: Boolean = false)
private data class WordFix(val original: String, val corrected: String, val explanation: String, val tip: String)

@Composable
private fun WriteRightApp() {
    val context = LocalContext.current
    var page by remember { mutableStateOf(Page.WRITE) }
    var dark by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var tool by remember { mutableStateOf(Tool.PEN) }
    var strokes by remember { mutableStateOf(emptyList<StrokeData>()) }
    var recognized by remember { mutableStateOf("") }
    var fixes by remember { mutableStateOf(emptyList<WordFix>()) }
    var selected by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val tts = remember { TextToSpeech(context) {} }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Mint, background = Color(0xFF15221E), surface = Color(0xFF1B3029)) else lightColorScheme(primary = Dark)) {
        when (page) {
            Page.WRITE -> WritingPage(
                dark = dark, strokes = strokes, tool = tool, status = status, busy = busy, menu = menu,
                onMenu = { menu = it }, onTheme = { dark = !dark },
                onTool = { tool = it }, onStrokes = { strokes = it }, onStatus = { status = it },
                onExample = {
                    recognized = "I recieve an emale every day"
                    fixes = correctionList(recognized)
                    page = Page.RESULTS
                    status = ""
                },
                onMonitor = {
                    if (strokes.isEmpty()) status = "اكتب كلمة أو جملة أولاً، ثم اضغط على زر المراقبة."
                    else {
                        busy = true
                        status = "جاري تجهيز نموذج التعرف على الكتابة..."
                        recognize(context, strokes, { status = it }, { text ->
                            busy = false
                            recognized = text
                            fixes = correctionList(text)
                            status = ""
                            page = Page.RESULTS
                        }, { error ->
                            busy = false
                            status = error
                        })
                    }
                }
            )
            Page.RESULTS -> ResultsPage(
                recognized = recognized, fixes = fixes,
                onBack = { page = Page.WRITE },
                onSelect = { selected = it; page = Page.EXPLANATION },
                onRetry = { strokes = emptyList(); status = ""; page = Page.WRITE }
            )
            Page.EXPLANATION -> ExplanationPage(
                fix = fixes.getOrNull(selected), index = selected, total = fixes.size,
                onBack = { page = Page.RESULTS },
                onWriteAgain = { strokes = emptyList(); status = ""; page = Page.WRITE },
                onSpeak = { tts.speak(it, TextToSpeech.QUEUE_FLUSH, null, "word") }
            )
        }
    }
}

@Composable
private fun WritingPage(
    dark: Boolean, strokes: List<StrokeData>, tool: Tool, status: String, busy: Boolean, menu: Boolean,
    onMenu: (Boolean) -> Unit, onTheme: () -> Unit, onTool: (Tool) -> Unit,
    onStrokes: (List<StrokeData>) -> Unit, onStatus: (String) -> Unit,
    onExample: () -> Unit, onMonitor: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(if (dark) Color(0xFF15221E) else Color.White)) {
        Header(dark, menu, onMenu, onTheme, { onStrokes(emptyList()); onStatus("") }, onExample)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            WritingCanvas(strokes, tool, onStrokes, Modifier.fillMaxSize())
            if (strokes.isEmpty()) {
                Column(Modifier.align(Alignment.Center).padding(horizontal = 25.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("write anything...", fontSize = 42.sp, fontFamily = FontFamily.Cursive, fontWeight = FontWeight.Bold, color = Muted, textAlign = TextAlign.Center)
                    Text("اكتب أي كلمة أو جملة بالإنجليزي بإيدك، وأنا هصححها قدامك على اللوحة", fontSize = 19.sp, color = Muted, textAlign = TextAlign.Center, lineHeight = 29.sp)
                }
            }
            if (status.isNotBlank()) {
                Surface(Modifier.align(Alignment.TopCenter).padding(12.dp).fillMaxWidth(), color = if (busy) PaleMint else PaleRed, shape = RoundedCornerShape(14.dp), shadowElevation = 3.dp) {
                    Text(status, Modifier.padding(12.dp), color = Dark, textAlign = TextAlign.Center, fontSize = 13.sp)
                }
            }
        }
        Button(onClick = onExample, Modifier.align(Alignment.CenterHorizontally).padding(vertical = 10.dp), colors = ButtonDefaults.buttonColors(containerColor = Dark, contentColor = Mint), shape = RoundedCornerShape(50)) {
            Text("‹   شوف مثال تصحيح", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 5.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.border(5.dp, Dark, RoundedCornerShape(26.dp)).padding(6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ToolButton(Icons.Rounded.Pen, tool == Tool.PEN) { onTool(Tool.PEN) }
                ToolButton(Icons.Rounded.Eraser, tool == Tool.ERASER) { onTool(Tool.ERASER) }
                ToolButton(Icons.Rounded.Undo, false) { if (strokes.isNotEmpty()) onStrokes(strokes.dropLast(1)) }
            }
            Button(onClick = onMonitor, enabled = !busy, Modifier.height(62.dp), colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Dark), shape = RoundedCornerShape(50)) {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Dark, strokeWidth = 2.dp)
                else Text("●  الـ AI بيراقب", fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun Header(dark: Boolean, menu: Boolean, onMenu: (Boolean) -> Unit, onTheme: () -> Unit, onClear: () -> Unit, onExample: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(92.dp).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        HeaderIcon(Icons.Rounded.LightMode, onTheme)
        Box(Modifier.clip(RoundedCornerShape(50)).background(Mint).border(6.dp, Dark, RoundedCornerShape(50)).padding(horizontal = 20.dp, vertical = 11.dp)) {
            Text("Write Right AI", color = Dark, fontSize = 23.sp, fontWeight = FontWeight.Black)
        }
        Box {
            HeaderIcon(Icons.Rounded.Menu) { onMenu(true) }
            DropdownMenu(expanded = menu, onDismissRequest = { onMenu(false) }, properties = PopupProperties(focusable = true)) {
                DropdownMenuItem(text = { Text("مسح اللوحة") }, leadingIcon = { Icon(Icons.Rounded.DeleteOutline, null) }, onClick = { onClear(); onMenu(false) })
                DropdownMenuItem(text = { Text("عرض مثال التصحيح") }, onClick = { onExample(); onMenu(false) })
            }
        }
    }
    Box(Modifier.fillMaxWidth().height(3.dp).background(Rule))
}

@Composable
private fun HeaderIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Box(Modifier.size(58.dp).clip(RoundedCornerShape(20.dp)).background(Color.White).border(5.dp, Dark, RoundedCornerShape(20.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Dark, modifier = Modifier.size(27.dp))
    }
}

@Composable
private fun WritingCanvas(strokes: List<StrokeData>, tool: Tool, onStrokes: (List<StrokeData>) -> Unit, modifier: Modifier) {
    var active by remember { mutableStateOf(emptyList<PointData>()) }
    Canvas(modifier.background(Color.White).pointerInput(tool, strokes) {
        detectDragGestures(
            onDragStart = { p -> active = listOf(PointData(p.x, p.y, System.currentTimeMillis())) },
            onDrag = { change, _ ->
                change.consume()
                active = active + PointData(change.position.x, change.position.y, System.currentTimeMillis())
            },
            onDragEnd = {
                if (active.isNotEmpty()) onStrokes(strokes + StrokeData(active, tool == Tool.ERASER))
                active = emptyList()
            },
            onDragCancel = { active = emptyList() }
        )
    }) {
        var y = 0f
        val gap = 72.dp.toPx()
        while (y < size.height) {
            drawLine(Rule, Offset(0f, y), Offset(size.width, y), 2.dp.toPx())
            y += gap
        }
        fun paint(points: List<PointData>, erase: Boolean) {
            if (points.isEmpty()) return
            val path = Path().apply {
                moveTo(points.first().x, points.first().y)
                points.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, if (erase) Color.White else Dark, style = Stroke(if (erase) 25.dp.toPx() else 4.2.dp.toPx(), cap = StrokeCap.Round))
            if (points.size == 1) drawCircle(if (erase) Color.White else Dark, 2.5.dp.toPx(), Offset(points[0].x, points[0].y))
        }
        strokes.forEach { paint(it.points, it.eraser) }
        paint(active, tool == Tool.ERASER)
    }
}

@Composable
private fun ToolButton(icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(54.dp).clip(RoundedCornerShape(17.dp)).background(if (selected) Mint else Color.White).border(4.dp, Dark, RoundedCornerShape(17.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = Dark, modifier = Modifier.size(25.dp))
    }
}

@Composable
private fun ResultsPage(recognized: String, fixes: List<WordFix>, onBack: () -> Unit, onSelect: (Int) -> Unit, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.White)) {
        Row(Modifier.fillMaxWidth().height(92.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Surface(color = if (fixes.isEmpty()) PaleMint else PaleRed, shape = RoundedCornerShape(18.dp)) {
                Text(if (fixes.isEmpty()) "ممتاز!" else fixes.size.toString() + " غلطات", Modifier.clickable { onBack() }.padding(horizontal = 17.dp, vertical = 12.dp), color = Dark, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            BrandPill()
            HeaderIcon(Icons.Rounded.Close, onBack)
        }
        Box(Modifier.fillMaxWidth().height(3.dp).background(Rule))
        Column(Modifier.weight(1f).fillMaxWidth()) {
            Spacer(Modifier.height(28.dp))
            Text(recognized, Modifier.fillMaxWidth().padding(horizontal = 20.dp), color = Dark, fontSize = 35.sp, fontFamily = FontFamily.Cursive, textAlign = TextAlign.Center, lineHeight = 62.sp)
            Spacer(Modifier.height(16.dp))
            fixes.forEachIndexed { index, fix ->
                Column(Modifier.fillMaxWidth().clickable { onSelect(index) }.padding(horizontal = 22.dp, vertical = 8.dp)) {
                    Text(fix.corrected, Modifier.fillMaxWidth(), color = Color(0xFF008866), fontSize = 35.sp, fontFamily = FontFamily.Cursive, textAlign = TextAlign.Center)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text(fix.original, color = Red, fontSize = 29.sp, fontFamily = FontFamily.Cursive)
                        Text("   →   ", color = Dark, fontSize = 23.sp)
                        Text(fix.corrected, color = Color(0xFF008866), fontSize = 29.sp, fontFamily = FontFamily.Cursive)
                    }
                    Box(Modifier.fillMaxWidth().height(2.dp).background(Rule))
                }
            }
            if (fixes.isEmpty()) Text("ممتاز! لم نكتشف أخطاء إملائية في النص.", Modifier.fillMaxWidth().padding(28.dp), color = Color(0xFF008866), textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        if (fixes.isNotEmpty()) {
            Surface(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp).clickable { onSelect(0) }, color = Mint, shape = RoundedCornerShape(26.dp), shadowElevation = 5.dp) {
                Column(Modifier.padding(17.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("استنى! فيه " + fixes.size + " غلطات", color = Dark, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(fixes) { fix -> Surface(color = Color.White, shape = RoundedCornerShape(50)) { Text(fix.original + " → " + fix.corrected, Modifier.padding(horizontal = 13.dp, vertical = 8.dp), color = Dark, fontWeight = FontWeight.Bold) } }
                    }
                    Text("اضغط هنا عشان أشرح لك كل غلطة", color = Dark, fontSize = 15.sp)
                }
            }
        }
        TextButton(onClick = onRetry, Modifier.align(Alignment.CenterHorizontally)) { Text("اكتب جملة جديدة", color = Dark) }
    }
}

@Composable
private fun BrandPill() {
    Box(Modifier.clip(RoundedCornerShape(50)).background(Mint).border(5.dp, Dark, RoundedCornerShape(50)).padding(horizontal = 18.dp, vertical = 10.dp)) {
        Text("Write Right AI", color = Dark, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ExplanationPage(fix: WordFix?, index: Int, total: Int, onBack: () -> Unit, onWriteAgain: () -> Unit, onSpeak: (String) -> Unit) {
    val current = fix ?: WordFix("recieve", "receive", "الخطأ في ترتيب حرفي i و e.", "بعد حرف c ييجي e الأول وبعدها i، زي receive و ceiling.")
    Box(Modifier.fillMaxSize().background(Color(0x66091A15))) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.72f).align(Alignment.BottomCenter).clip(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)).background(PaleMint).padding(horizontal = 22.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(Modifier.width(70.dp).height(7.dp).clip(CircleShape).background(Dark).align(Alignment.CenterHorizontally))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("━  ━", color = Rule, fontSize = 20.sp)
                Text("الغلطة " + (index + 1) + " من " + total, color = Dark, fontSize = 23.sp, fontWeight = FontWeight.Black)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                WordPill(current.original, PaleRed, Red, true)
                Text("→", fontSize = 32.sp, color = Dark, fontWeight = FontWeight.Bold)
                WordPill(current.corrected, Mint, Dark)
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White).border(3.dp, Dark, RoundedCornerShape(22.dp)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(current.explanation, Modifier.fillMaxWidth(), color = Muted, textAlign = TextAlign.Center, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    items(current.corrected.toList()) { letter ->
                        Box(Modifier.size(42.dp).clip(RoundedCornerShape(10.dp)).background(if (current.corrected == "receive" && letter in "ei") Mint else PaleMint).border(3.dp, Dark, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                            Text(letter.toString(), color = Dark, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Surface(Modifier.fillMaxWidth(), color = Color(0xFFFFF4C2), shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(3.dp, Dark)) {
                Text("💡  " + current.tip, Modifier.padding(16.dp), color = Dark, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onWriteAgain, Modifier.weight(1f).height(70.dp), shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Dark, contentColor = Mint)) {
                    Text("اكتبها تاني على اللوحة  ✎", fontSize = 17.sp, fontWeight = FontWeight.Black)
                }
                Box(Modifier.size(70.dp).clip(RoundedCornerShape(20.dp)).background(Color.White).border(4.dp, Dark, RoundedCornerShape(20.dp)).clickable { onSpeak(current.corrected) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.VolumeUp, "Listen", tint = Dark, modifier = Modifier.size(32.dp))
                }
            }
        }
        HeaderIcon(Icons.Rounded.Close, onBack)
    }
}

@Composable
private fun WordPill(text: String, bg: Color, fg: Color, crossed: Boolean = false) {
    Box(Modifier.clip(RoundedCornerShape(18.dp)).background(bg).border(3.dp, Dark, RoundedCornerShape(18.dp)).padding(horizontal = 13.dp, vertical = 12.dp)) {
        Text(text, color = fg, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        if (crossed) Canvas(Modifier.matchParentSize()) {
            drawLine(Red, Offset(8.dp.toPx(), size.height / 2), Offset(size.width - 8.dp.toPx(), size.height / 2), 4.dp.toPx())
        }
    }
}

private fun correctionList(text: String): List<WordFix> {
    val rules = mapOf(
        "recieve" to WordFix("recieve", "receive", "الخطأ في ترتيب حرفي i و e. بعد حرف c يأتي e أولاً ثم i.", "بعد حرف c ييجي e الأول وبعدين i، زي receive و ceiling."),
        "emale" to WordFix("emale", "email", "الكلمة الصحيحة تُكتب email: e ثم m ثم a ثم i ثم l.", "افتكر شكل كلمة email كلما كتبت رسالة إلكترونية."),
        "teh" to WordFix("teh", "the", "ترتيب الحروف في the هو t ثم h ثم e.", "كلمة the من أكثر الكلمات استخداماً في الإنجليزية."),
        "becuase" to WordFix("becuase", "because", "لاحظ ترتيب الحروف في because: b-e-c-a-u-s-e.", "اربط because بمعنى «لأن»."),
        "wierd" to WordFix("wierd", "weird", "في كلمة weird يأتي e قبل i.", "احفظ شكل الكلمة weird كما هي.")
    )
    return text.split(Regex("\\\\s+"))
        .map { it.trim().trim(',', '.', '!', '?', ';', ':', '"', '\\'').lowercase(Locale.US) }
        .filter { rules.containsKey(it) }.distinct().map { rules.getValue(it) }
}

private fun recognize(
    context: android.content.Context, strokes: List<StrokeData>,
    onStatus: (String) -> Unit, onSuccess: (String) -> Unit, onError: (String) -> Unit
) {
    val identifier = try { DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US") } catch (_: Exception) { null }
    if (identifier == null) { onError("تعذر تجهيز نموذج اللغة الإنجليزية على هذا الجهاز."); return }
    val model = DigitalInkRecognitionModel.builder(identifier).build()
    val manager = RemoteModelManager.getInstance()

    fun runRecognition() {
        try {
            val inkBuilder = Ink.builder()
            strokes.forEach { saved ->
                if (saved.points.isNotEmpty()) {
                    val strokeBuilder = Ink.Stroke.builder()
                    saved.points.forEach { p -> strokeBuilder.addPoint(Ink.Point.create(p.x, p.y, p.time)) }
                    inkBuilder.addStroke(strokeBuilder.build())
                }
            }
            val recognizer: DigitalInkRecognizer = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
            recognizer.recognize(inkBuilder.build())
                .addOnSuccessListener { result ->
                    recognizer.close()
                    val candidate = result.candidates.firstOrNull()?.text.orEmpty()
                    if (candidate.isBlank()) onError("لم أستطع قراءة الكتابة بوضوح. جرّب تكتب بشكل أكبر.") else onSuccess(candidate)
                }
                .addOnFailureListener { error ->
                    recognizer.close()
                    onError("حصلت مشكلة أثناء قراءة الخط: " + (error.localizedMessage ?: "حاول مرة أخرى"))
                }
        } catch (error: Exception) {
            onError("تعذر بدء التعرف على الكتابة: " + (error.localizedMessage ?: "خطأ غير معروف"))
        }
    }

    manager.isModelDownloaded(model).addOnSuccessListener { downloaded ->
        if (downloaded) {
            onStatus("جاري قراءة خطك...")
            runRecognition()
        } else {
            onStatus("تحميل نموذج الكتابة الإنجليزية لأول مرة (حوالي 20 ميجابايت)...")
            manager.download(model, DownloadConditions.Builder().build())
                .addOnSuccessListener { onStatus("تم تجهيز النموذج. جاري قراءة خطك..."); runRecognition() }
                .addOnFailureListener { error -> onError("لم يكتمل تحميل نموذج التعرف. اتصل بالإنترنت وحاول مرة أخرى. " + (error.localizedMessage ?: "")) }
        }
    }.addOnFailureListener { error ->
        onError("تعذر التحقق من نموذج الكتابة: " + (error.localizedMessage ?: ""))
    }
}
