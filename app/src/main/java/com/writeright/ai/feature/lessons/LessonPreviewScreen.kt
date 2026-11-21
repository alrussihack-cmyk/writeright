package com.writeright.ai.feature.lessons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.writeright.ai.core.design.PageBackground
import com.writeright.ai.core.design.TextSecondary

@Composable
fun LessonPreviewScreen(onBack: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = PageBackground) {
        Column(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Spacer(Modifier.weight(1f))
                Text("LESSON 01  •  B1", modifier = Modifier.padding(top = 14.dp),
                    color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Text("Workplace vocabulary", fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("RESPONSIBLE", fontSize = 27.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Row {
                        Icon(Icons.Rounded.VolumeUp, contentDescription = "Pronunciation")
                        Text("  /rɪˈspɒnsəbəl/", color = TextSecondary)
                    }
                    Text("المعنى", fontWeight = FontWeight.Bold)
                    Text("مسؤول عن / مسؤول تجاه")
                    Text("Example", fontWeight = FontWeight.Bold)
                    Text("I am responsible for supervising the team.")
                    Text("Grammar tip", fontWeight = FontWeight.Bold)
                    Text("After “responsible for”, use a noun or a verb ending in -ing.",
                        color = TextSecondary)
                }
            }
            Text("NEXT STEP", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("In Phase 2, this lesson will load from the local course database. The stylus writing exercise will be added in Phase 3.")
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Back to dashboard")
            }
        }
    }
}
