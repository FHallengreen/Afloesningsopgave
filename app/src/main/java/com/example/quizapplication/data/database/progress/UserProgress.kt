package com.example.quizapplication.data.database.progress

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Data class for the user progress
 */
@Entity(tableName="user_progress_table")
data class UserProgress (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 1,
    val progress: Int,
    val correctAnswers: Int = 0,
)
