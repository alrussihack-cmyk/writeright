package com.writeright.ai.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exercises",
    foreignKeys = [ForeignKey(
        entity = LessonEntity::class,
        parentColumns = ["id"],
        childColumns = ["lessonId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("lessonId")]
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val lessonId: String,
    val orderIndex: Int,
    val instructionArabic: String,
    val promptEnglish: String,
    val expectedAnswer: String,
    val exerciseType: String = "WRITE_WORD"
)
