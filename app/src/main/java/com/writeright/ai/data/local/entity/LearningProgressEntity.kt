package com.writeright.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_progress")
data class LearningProgressEntity(
    @PrimaryKey val contentId: String,
    val contentType: String,
    val masteryLevel: Int = 0,
    val correctCount: Int = 0,
    val attemptCount: Int = 0,
    val lastPracticedAt: Long? = null,
    val nextReviewAt: Long? = null
)
