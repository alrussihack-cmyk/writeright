package com.writeright.ai.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "lessons",
    foreignKeys = [ForeignKey(
        entity = CourseEntity::class,
        parentColumns = ["id"],
        childColumns = ["courseId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("courseId")]
)
data class LessonEntity(
    @PrimaryKey val id: String,
    val courseId: String,
    val orderIndex: Int,
    val title: String,
    val objective: String,
    val explanationArabic: String,
    val grammarTip: String,
    val estimatedMinutes: Int = 10
)
