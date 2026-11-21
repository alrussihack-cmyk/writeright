package com.writeright.ai

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import kotlin.math.max
import kotlin.math.min

/** Distance between two ruled lines on the board. */
internal const val ROW_GAP_DP = 72f

internal data class PointData(val x: Float, val y: Float, val time: Long)
internal data class StrokeData(val points: List<PointData>)

/**
 * A mistake placed on the board: [oval] circles the wrong letters, and the corrected word is
 * drawn at ([corrLeft], [corrBaseline]) in a handwriting font, one row above the mistake.
 */
internal data class Finding(
    val fix: WordFix,
    val oval: Rect,
    val corrLeft: Float,
    val corrBaseline: Float,
    val corrSize: Float
)

internal data class Analysis(val text: String, val findings: List<Finding>)

internal data class DemoLine(val text: String, val x: Float, val baseline: Float)
internal data class Demo(val lines: List<DemoLine>, val sizePx: Float, val analysis: Analysis)

// ---------------------------------------------------------------- geometry helpers

private fun StrokeData.bounds(): Rect {
    var l = Float.MAX_VALUE
    var t = Float.MAX_VALUE
    var r = -Float.MAX_VALUE
    var b = -Float.MAX_VALUE
    for (p in points) {
        l = min(l, p.x); t = min(t, p.y)
        r = max(r, p.x); b = max(b, p.y)
    }
    return Rect(l, t, r, b)
}

private fun union(rects: List<Rect>): Rect = Rect(
    rects.minOf { it.left }, rects.minOf { it.top },
    rects.maxOf { it.right }, rects.maxOf { it.bottom }
)

/** Stroke eraser: drops every stroke that passes within [radius] of [at]. */
internal fun eraseAt(strokes: List<StrokeData>, at: Offset, radius: Float): List<StrokeData> {
    val r2 = radius * radius
    fun near(ax: Float, ay: Float, bx: Float, by: Float): Boolean {
        val dx = bx - ax
        val dy = by - ay
        val len2 = dx * dx + dy * dy
        val t = if (len2 == 0f) 0f else (((at.x - ax) * dx + (at.y - ay) * dy) / len2).coerceIn(0f, 1f)
        val px = ax + t * dx - at.x
        val py = ay + t * dy - at.y
        return px * px + py * py <= r2
    }
    val kept = strokes.filterNot { s ->
        val pts = s.points
        when {
            pts.isEmpty() -> false
            pts.size == 1 -> near(pts[0].x, pts[0].y, pts[0].x, pts[0].y)
            else -> (1 until pts.size).any { i -> near(pts[i - 1].x, pts[i - 1].y, pts[i].x, pts[i].y) }
        }
    }
    return if (kept.size == strokes.size) strokes else kept
}

/** Groups strokes into text lines by clustering their vertical centres. */
private fun groupLines(strokes: List<StrokeData>, gap: Float): List<List<StrokeData>> {
    val sorted = strokes.filter { it.points.isNotEmpty() }
        .map { it to it.bounds() }
        .sortedBy { (_, b) -> (b.top + b.bottom) / 2f }
    val lines = mutableListOf<MutableList<StrokeData>>()
    var lastCenter = 0f
    for ((stroke, b) in sorted) {
        val c = (b.top + b.bottom) / 2f
        if (lines.isEmpty() || c - lastCenter > gap) lines.add(mutableListOf())
        lines.last().add(stroke)
        lastCenter = c
    }
    return lines
}

/** Left/right extent of each word in a line, found from horizontal gaps between strokes. */
private fun wordSpans(strokes: List<StrokeData>, gap: Float): List<Pair<Float, Float>> {
    val boxes = strokes.map { it.bounds() }.sortedBy { it.left }
    val spans = mutableListOf<Pair<Float, Float>>()
    var l = boxes[0].left
    var r = boxes[0].right
    for (i in 1 until boxes.size) {
        val b = boxes[i]
        if (b.left - r > gap) {
            spans.add(l to r)
            l = b.left
            r = b.right
        } else {
            r = max(r, b.right)
        }
    }
    spans.add(l to r)
    return spans
}

/** Fallback when the gap count doesn't match the word count: share the width by letter count. */
private fun proportionalSpans(left: Float, right: Float, words: List<String>): List<Pair<Float, Float>> {
    val total = words.sumOf { it.length } + (words.size - 1)
    val unit = (right - left) / max(total, 1)
    var cursor = 0
    return words.map { w ->
        val s = left + cursor * unit
        val e = left + (cursor + w.length) * unit
        cursor += w.length + 1
        s to e
    }
}

private fun buildFindings(
    words: List<String>,
    spans: List<Pair<Float, Float>>,
    top: Float,
    bottom: Float,
    baseline: Float,
    density: Float,
    rowGap: Float
): List<Finding> {
    val h = max(bottom - top, 1f)
    val out = mutableListOf<Finding>()
    words.forEachIndexed { i, word ->
        val fix = lookupFix(word) ?: return@forEachIndexed
        val (l, r) = spans[i]
        val len = max(fix.original.length, 1)
        var left = l + (r - l) * fix.badStart / len - 7f * density
        var right = l + (r - l) * fix.badEnd / len + 7f * density
        val minWidth = 34f * density
        if (right - left < minWidth) {
            val c = (left + right) / 2f
            left = c - minWidth / 2f
            right = c + minWidth / 2f
        }
        left = max(left, 2f * density)
        val oval = Rect(left, top - 12f * density, right, bottom + 12f * density)

        val size = (h * 0.8f).coerceIn(30f * density, 46f * density)
        val shift = max(rowGap, h * 1.15f)
        var cb = baseline - shift
        if (cb - size * 0.9f < 0f) cb = baseline + shift // no room above: write it below
        out.add(Finding(fix, oval, l, cb, size))
    }
    return out
}

// ---------------------------------------------------------------- demo

/** Ready-made handwriting sample ("I recieve an emale every day") with its corrections. */
internal fun buildDemo(typeface: Typeface, density: Float, rowGap: Float): Demo {
    val size = 44f * density
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.typeface = typeface
        textSize = size
    }
    val lines = mutableListOf<DemoLine>()
    val findings = mutableListOf<Finding>()
    listOf(Triple("I recieve an", 3, 28f), Triple("emale every day", 5, 28f)).forEach { (text, row, xDp) ->
        val x = xDp * density
        val baseline = row * rowGap - 15f * density
        lines.add(DemoLine(text, x, baseline))
        val words = text.split(" ")
        var cursor = 0
        val spans = words.map { w ->
            val left = x + paint.measureText(text.substring(0, cursor))
            val right = left + paint.measureText(w)
            cursor += w.length + 1
            left to right
        }
        findings += buildFindings(words, spans, baseline - 0.72f * size, baseline + 0.2f * size, baseline, density, rowGap)
    }
    return Demo(lines, size, Analysis("I recieve an emale every day", findings))
}

// ---------------------------------------------------------------- recognition

private fun recognizeLines(
    recognizer: DigitalInkRecognizer,
    lines: List<List<StrokeData>>,
    index: Int,
    acc: MutableList<String>,
    onDone: (List<String>) -> Unit,
    onError: (String) -> Unit
) {
    if (index >= lines.size) {
        onDone(acc)
        return
    }
    val ink = Ink.builder().apply {
        lines[index].forEach { s ->
            addStroke(Ink.Stroke.builder().apply {
                s.points.forEach { p -> addPoint(Ink.Point.create(p.x, p.y, p.time)) }
            }.build())
        }
    }.build()
    recognizer.recognize(ink)
        .addOnSuccessListener { result ->
            acc.add(result.candidates.firstOrNull()?.text.orEmpty())
            recognizeLines(recognizer, lines, index + 1, acc, onDone, onError)
        }
        .addOnFailureListener { error ->
            onError("حصلت مشكلة أثناء قراءة الخط: " + (error.localizedMessage ?: "حاول مرة أخرى"))
        }
}

/** Reads the handwriting line by line and locates every known misspelling on the board. */
internal fun analyzeInk(
    context: Context,
    strokes: List<StrokeData>,
    density: Float,
    rowGap: Float,
    onStatus: (String) -> Unit,
    onSuccess: (Analysis) -> Unit,
    onError: (String) -> Unit
) {
    val identifier = try {
        DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
    } catch (_: Exception) {
        null
    }
    if (identifier == null) {
        onError("تعذر تجهيز نموذج اللغة الإنجليزية على هذا الجهاز.")
        return
    }
    val model = DigitalInkRecognitionModel.builder(identifier).build()
    val manager = RemoteModelManager.getInstance()
    val lines = groupLines(strokes, rowGap * 0.3f)

    fun finish(texts: List<String>) {
        val cleaned = texts.map { it.replace('\n', ' ').trim() }
        if (cleaned.all { it.isBlank() }) {
            onError("لم أستطع قراءة الكتابة بوضوح. جرّب تكتب بشكل أكبر وأوضح.")
            return
        }
        val findings = mutableListOf<Finding>()
        lines.forEachIndexed { i, line ->
            val words = cleaned[i].split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.isEmpty()) return@forEachIndexed
            val box = union(line.map { it.bounds() })
            val spans = wordSpans(line, max(0.3f * box.height, 10f * density))
            val use = if (spans.size == words.size) spans else proportionalSpans(box.left, box.right, words)
            val baseline = box.bottom - 0.1f * box.height
            findings += buildFindings(words, use, box.top, box.bottom, baseline, density, rowGap)
        }
        onSuccess(Analysis(cleaned.filter { it.isNotBlank() }.joinToString(" "), findings))
    }

    fun recognizeAll() {
        onStatus("جاري قراءة خطك...")
        val recognizer = try {
            DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
        } catch (error: Exception) {
            onError("تعذر بدء التعرف على الكتابة: " + (error.localizedMessage ?: "خطأ غير معروف"))
            return
        }
        recognizeLines(
            recognizer, lines, 0, mutableListOf(),
            { texts -> recognizer.close(); finish(texts) },
            { message -> recognizer.close(); onError(message) }
        )
    }

    manager.isModelDownloaded(model).addOnSuccessListener { downloaded ->
        if (downloaded) {
            recognizeAll()
        } else {
            onStatus("بحمّل نموذج الكتابة الإنجليزية لأول مرة (حوالي 20 ميجابايت)...")
            manager.download(model, DownloadConditions.Builder().build())
                .addOnSuccessListener { recognizeAll() }
                .addOnFailureListener { error ->
                    onError("لم يكتمل تحميل النموذج. اتصل بالإنترنت وحاول مرة أخرى. " + (error.localizedMessage ?: ""))
                }
        }
    }.addOnFailureListener { error ->
        onError("تعذر التحقق من نموذج الكتابة: " + (error.localizedMessage ?: ""))
    }
}
