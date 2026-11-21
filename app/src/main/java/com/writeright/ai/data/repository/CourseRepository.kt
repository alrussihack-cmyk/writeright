package com.writeright.ai.data.repository

import com.writeright.ai.data.local.WriteRightDao
import com.writeright.ai.data.local.entity.*
import com.writeright.ai.data.seed.B1CurriculumSeeder
import kotlinx.coroutines.flow.Flow

class CourseRepository(private val dao: WriteRightDao) {
    suspend fun initialize() = B1CurriculumSeeder.seedIfEmpty(dao)

    fun observeCourse(): Flow<CourseEntity?> = dao.observeActiveCourse()
    fun observeLessons(courseId: String): Flow<List<LessonEntity>> = dao.observeLessons(courseId)
    fun observeVocabulary(lessonId: String): Flow<List<VocabularyEntity>> = dao.observeVocabulary(lessonId)
    fun observeExercises(lessonId: String): Flow<List<ExerciseEntity>> = dao.observeExercises(lessonId)
    suspend fun firstLesson(courseId: String) = dao.getFirstLesson(courseId)
    suspend fun lesson(lessonId: String) = dao.getLesson(lessonId)

    suspend fun recordAttempt(exercise: ExerciseEntity, recognizedText: String, isCorrect: Boolean) {
        dao.insertAttempt(
            AttemptEntity(
                exerciseId = exercise.id,
                recognizedText = recognizedText,
                expectedAnswer = exercise.expectedAnswer,
                isCorrect = isCorrect
            )
        )
        val previous = dao.getProgress(exercise.id)
        val attempts = (previous?.attemptCount ?: 0) + 1
        val correct = (previous?.correctCount ?: 0) + if (isCorrect) 1 else 0
        dao.saveProgress(
            LearningProgressEntity(
                contentId = exercise.id,
                contentType = "EXERCISE",
                masteryLevel = ((correct.toFloat() / attempts) * 100).toInt(),
                correctCount = correct,
                attemptCount = attempts,
                lastPracticedAt = System.currentTimeMillis(),
                nextReviewAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
            )
        )
    }
}
