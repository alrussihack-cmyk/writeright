package com.writeright.ai.feature.lessons

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.writeright.ai.core.design.PageBackground
import com.writeright.ai.core.design.TextSecondary
import com.writeright.ai.data.local.entity.LessonEntity
import com.writeright.ai.data.repository.CourseRepository
import com.writeright.ai.feature.writing.StylusWritingBoard

@Composable
fun LessonPreviewScreen(
    lessonId: String,
    repository: CourseRepository,
    onBack: () -> Unit
) {
    val lesson by produceState<LessonEntity?>(initialValue = null, lessonId) {
        value = repository.lesson(lessonId)
    }
    val vocabulary by repository.observeVocabulary(lessonId).collectAsState(initial = emptyList())
    val exercises by repository.observeExercises(lessonId).collectAsState(initial = emptyList())
    var exerciseIndex by remember(lessonId) { mutableIntStateOf(0) }
    val exercise = exercises.getOrNull(exerciseIndex)

    Surface(modifier = Modifier.fillMaxSize(), color = PageBackground) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth()) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(Modifier.weight(1f))
                    Text("B1 • ENGLISH FOR WORK", modifier = Modifier.padding(top = 14.dp),
                        color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            item {
                Text(lesson?.title ?: "Loading lesson…", fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text(lesson?.objective.orEmpty(), color = TextSecondary)
            }
            item {
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("شرح الدرس", fontWeight = FontWeight.Bold)
                        Text(lesson?.explanationArabic.orEmpty())
                        Text("Grammar tip", fontWeight = FontWeight.Bold)
                        Text(lesson?.grammarTip.orEmpty(), color = TextSecondary)
                    }
                }
            }
            item { Text("KEY VOCABULARY", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 12.sp) }
            items(vocabulary, key = { it.id }) { word ->
                Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(word.word, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary)
                        Row {
                            Icon(Icons.Rounded.VolumeUp, contentDescription = "Pronunciation")
                            Text("  ${word.pronunciation}", color = TextSecondary)
                        }
                        Text(word.meaningArabic, fontWeight = FontWeight.SemiBold)
                        Text(word.example)
                        Text(word.exampleMeaningArabic, color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
            item {
                Text("WRITE WITH YOUR STYLUS", fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp, fontSize = 12.sp)
            }
            if (exercise != null) {
                item(key = exercise.id) {
                    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("EXERCISE ${exerciseIndex + 1} OF ${exercises.size}",
                                color = MaterialTheme.colorScheme.primary, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold)
                            Text(exercise.instructionArabic, fontWeight = FontWeight.SemiBold)
                            if (exercise.promptEnglish.isNotBlank()) {
                                Text(exercise.promptEnglish, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            StylusWritingBoard()
                            Text(
                                "Your handwriting is not checked yet. Automatic recognition will be added in the next step.",
                                color = TextSecondary, fontSize = 12.sp
                            )
                            Button(
                                onClick = { if (exerciseIndex < exercises.lastIndex) exerciseIndex++ },
                                enabled = exerciseIndex < exercises.lastIndex,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (exerciseIndex < exercises.lastIndex) "Next exercise" else "End of exercises")
                            }
                        }
                    }
                }
            } else {
                item {
                    Text("No writing exercises are available for this lesson yet.", color = TextSecondary)
                }
            }
        }
    }
}
