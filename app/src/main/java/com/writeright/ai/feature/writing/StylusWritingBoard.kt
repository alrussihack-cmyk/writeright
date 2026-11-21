package com.writeright.ai.feature.writing

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun StylusWritingBoard(
    modifier: Modifier = Modifier,
    onStrokeCountChanged: (Int) -> Unit = {}
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    if (strokes.isNotEmpty()) {
                        strokes.removeAt(strokes.lastIndex)
                        onStrokeCountChanged(strokes.size)
                    }
                },
                enabled = strokes.isNotEmpty()
            ) { Text("Undo") }

            OutlinedButton(
                onClick = {
                    strokes.clear()
                    onStrokeCountChanged(0)
                },
                enabled = strokes.isNotEmpty()
            ) { Text("Clear") }

            Text(
                text = "${strokes.size} strokes",
                modifier = Modifier.padding(top = 14.dp),
                fontSize = 12.sp
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(androidx.compose.ui.graphics.Color.White)
                .border(1.dp, androidx.compose.ui.graphics.Color(0xFFD8DEEA), RoundedCornerShape(16.dp))
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (down.type != PointerType.Stylus) {
                            return@awaitEachGesture
                        }

                        val points = mutableListOf(down.position)
                        down.consume()
                        var drawing = true
                        while (drawing) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id }
                            if (change == null) {
                                drawing = false
                            } else if (change.pressed) {
                                points.add(change.position)
                                change.consume()
                            } else {
                                drawing = false
                            }
                        }
                        if (points.isNotEmpty()) {
                            strokes.add(points.toList())
                            onStrokeCountChanged(strokes.size)
                        }
                    }
                }
        ) {
            strokes.forEach { points ->
                if (points.size == 1) {
                    drawCircle(
                        color = androidx.compose.ui.graphics.Color(0xFF172B4D),
                        radius = 3.5.dp.toPx(),
                        center = points.first()
                    )
                } else {
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path = path,
                        color = androidx.compose.ui.graphics.Color(0xFF172B4D),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        }

        Text(
            "Write with your stylus. Finger touch is ignored.",
            fontSize = 12.sp,
            color = androidx.compose.ui.graphics.Color(0xFF65718A)
        )
    }
}
