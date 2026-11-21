package com.writeright.ai.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val language: String = "English",
    val level: String = "B1",
    val isActive: Boolean = true
)
