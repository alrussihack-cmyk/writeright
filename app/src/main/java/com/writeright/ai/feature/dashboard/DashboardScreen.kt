package com.writeright.ai.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.writeright.ai.core.design.PageBackground
import com.writeright.ai.core.design.SoftBlue
import com.writeright.ai.core.design.TextSecondary
import com.writeright.ai.data.repository.CourseRepository

@Composable
fun DashboardScreen(repository: CourseRepository, onStartLesson: (String) -> Unit) {
    val lessons by repository.observeLessons("english-for-work-b1").collectAsState(initial = emptyList())
    Surface(modifier = Modifier.fillMaxSize(), color = PageBackground) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("WRITERIGHT AI", color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Your English writing coach", fontSize = 27.sp, fontWeight = FontWeight.Bold)
                    Text("Learn it. Listen to it. Write it.", color = TextSecondary)
                }
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF172B4D)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("TODAY'S LEARNING PATH", color = Color(0xFFB9C9FF), fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("English for Work", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        Text("Build the vocabulary and writing skills you use at work.",
                            color = Color(0xFFE0E7F5))
                        Text("B1 • ${lessons.size} lessons", color = Color(0xFFE0E7F5))
                    }
                }
            }
            item {
                Text("YOUR LEARNING", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Current level", "B1", Icons.Rounded.Edit, Modifier.weight(1f))
                    MetricCard("Day streak", "0 days", Icons.Rounded.LocalFireDepartment, Modifier.weight(1f))
                }
            }
            item { Text("COURSE LESSONS", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 12.sp) }
            items(lessons, key = { it.id }) { lesson ->
                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("LESSON ${lesson.orderIndex.toString().padStart(2, '0')} • B1",
                            color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(lesson.title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text(lesson.objective, color = TextSecondary, fontSize = 13.sp)
                        Button(onClick = { onStartLesson(lesson.id) }) {
                            Icon(Icons.Rounded.AutoStories, contentDescription = null)
                            Spacer(Modifier.padding(3.dp))
                            Text("Open lesson")
                        }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = SoftBlue),
                    shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("A small step every day", fontWeight = FontWeight.SemiBold)
                            Text("Your course and lessons are saved on this device.",
                                color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 12.sp, color = TextSecondary)
        }
    }
}
