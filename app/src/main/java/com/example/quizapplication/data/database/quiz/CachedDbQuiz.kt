package com.example.quizapplication.data.database.quiz

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.quizapplication.model.Quiz

/**
 * Data class for the cached quiz
 * This is the data class that is used to store the quiz in the database
 */
@Entity(tableName="cached_quiz_table")
data class CachedDbQuiz (
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val question: String,
    val difficulty: String,
    val category: String,
    val correctAnswer: String,
    val incorrectAnswers: List<String>,
)

fun List<CachedDbQuiz>.asDomainQuizzes(): List<Quiz>{
    var quizList = this.map {
        Quiz(it.id, it.question, it.difficulty, it.category, it.correctAnswer, it.incorrectAnswers)
    }
    return quizList
}