package com.writeright.ai.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vocabulary",
    foreignKeys = [ForeignKey(
        entity = LessonEntity::class,
        parentColumns = ["id"],
        childColumns = ["lessonId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("lessonId"), Index("word")]
)
data class VocabularyEntity(
    @PrimaryKey val id: String,
    val lessonId: String,
    val word: String,
    val meaningArabic: String,
    val pronunciation: String,
    val example: String,
    val exampleMeaningArabic: String
)
