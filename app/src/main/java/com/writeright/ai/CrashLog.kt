package com.writeright.ai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

/** Saves the last crash to a file so the next launch can show it instead of closing silently. */
internal object CrashLog {
    private fun file(context: Context) = File(context.filesDir, "last_crash.txt")

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                file(app).writeText(android.util.Log.getStackTraceString(error).take(6000))
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, error)
        }
    }

    fun read(context: Context): String? = try {
        file(context).takeIf { it.exists() }?.readText()?.takeIf { it.isNotBlank() }
    } catch (_: Throwable) {
        null
    }

    fun clear(context: Context) {
        try { file(context).delete() } catch (_: Throwable) {}
    }
}

@Composable
internal fun CrashScreen(report: String, onContinue: () -> Unit) {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().background(Color.White).systemBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("التطبيق وقف في المرة اللي فاتت. انسخ النص ده وابعته.", color = Color.Black, fontSize = 16.sp)
        SelectionContainer(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(report, color = Color(0xFF8A1020), fontSize = 11.sp, lineHeight = 14.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("crash", report))
            }) { Text("نسخ") }
            Button(onClick = onContinue) { Text("فتح التطبيق") }
        }
    }
}
