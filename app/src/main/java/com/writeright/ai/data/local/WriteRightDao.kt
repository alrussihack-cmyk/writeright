package com.writeright.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.writeright.ai.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WriteRightDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCourse(course: CourseEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLessons(items: List<LessonEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVocabulary(items: List<VocabularyEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercises(items: List<ExerciseEntity>)

    @Insert
    suspend fun insertAttempt(attempt: AttemptEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: LearningProgressEntity)

    @Query("SELECT * FROM courses WHERE isActive = 1 LIMIT 1")
    fun observeActiveCourse(): Flow<CourseEntity?>

    @Query("SELECT * FROM lessons WHERE courseId = :courseId ORDER BY orderIndex")
    fun observeLessons(courseId: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lessons WHERE id = :lessonId LIMIT 1")
    suspend fun getLesson(lessonId: String): LessonEntity?

    @Query("SELECT * FROM lessons WHERE courseId = :courseId ORDER BY orderIndex LIMIT 1")
    suspend fun getFirstLesson(courseId: String): LessonEntity?

    @Query("SELECT * FROM vocabulary WHERE lessonId = :lessonId ORDER BY word")
    fun observeVocabulary(lessonId: String): Flow<List<VocabularyEntity>>

    @Query("SELECT * FROM exercises WHERE lessonId = :lessonId ORDER BY orderIndex")
    fun observeExercises(lessonId: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM learning_progress WHERE contentId = :contentId LIMIT 1")
    suspend fun getProgress(contentId: String): LearningProgressEntity?

    @Query("SELECT COUNT(*) FROM lessons")
    suspend fun lessonCount(): Int
}
