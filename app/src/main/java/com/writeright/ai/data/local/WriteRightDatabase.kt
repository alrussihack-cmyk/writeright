package com.writeright.ai.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.writeright.ai.data.local.entity.*

@Database(
    entities = [
        CourseEntity::class,
        LessonEntity::class,
        VocabularyEntity::class,
        ExerciseEntity::class,
        AttemptEntity::class,
        LearningProgressEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WriteRightDatabase : RoomDatabase() {
    abstract fun writeRightDao(): WriteRightDao

    companion object {
        @Volatile private var INSTANCE: WriteRightDatabase? = null

        fun getInstance(context: Context): WriteRightDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    WriteRightDatabase::class.java,
                    "writeright.db"
                ).build().also { INSTANCE = it }
            }
    }
}
