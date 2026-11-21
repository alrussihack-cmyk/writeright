package com.writeright.ai.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "attempts", indices = [Index("exerciseId"), Index("createdAt")])
data class AttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: String,
    val recognizedText: String,
    val expectedAnswer: String,
    val isCorrect: Boolean,
    val createdAt: Long = System.currentTimeMillis()
)
