package com.example.quizapplication.ui.quiz

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.androidexam.QuizApplication
import com.example.quizapplication.data.database.quiz.QuizRepository
import com.example.quizapplication.data.database.quiz.CachedDbQuiz
import com.example.quizapplication.data.database.result.Result
import com.example.quizapplication.data.database.result.ResultRepository
import com.example.quizapplication.ui.QuizState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuizViewModel(private val quizRepository: QuizRepository, private val resultRepository: ResultRepository) : ViewModel() {

    private val _quizState = MutableStateFlow<QuizState>(QuizState.Loading)
    val quizState: StateFlow<QuizState> = _quizState

    var currentQuestionIndex = mutableIntStateOf(0)
        private set
    var quizList by mutableStateOf<List<CachedDbQuiz>>(listOf())
        private set

    init {
        loadQuiz()
        getQuizIndex()
    }

    private fun getQuizIndex() {
        viewModelScope.launch {
            currentQuestionIndex.intValue = quizRepository.getQuizIndex()
            Log.i("QuizViewModel", "getQuizIndex: ${currentQuestionIndex.intValue}")
        }
    }

    /// This function is called when the user clicks on the "Delete quiz" button.
    /// The quiz is deleted from the database.
    private fun loadQuiz() {
        viewModelScope.launch {
            try {
                quizRepository.fetchQuizFromDatabase().collect { quizzes ->
                    if (quizzes.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            quizList = quizzes
                            _quizState.value =
                                QuizState.QuizLoaded(quizzes[currentQuestionIndex.intValue])
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            _quizState.value = QuizState.Error("No quizzes found.")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _quizState.value = QuizState.Error("Error loading quizzes: ${e.message}")
                }
            }
        }
    }

    /// This function is called when the user clicks on an answer.
    /// The answer is checked and the next question is displayed.
    fun onAnswerSelected(selectedAnswer: String) {
        viewModelScope.launch {
            checkAnswer(selectedAnswer)
        }
    }

    /// This function is called when the user clicks on an answer.
    /// The answer is checked and the next question is displayed.
    /// The user's progress is saved in the database.
    private suspend fun checkAnswer(selectedAnswer: String) {
        val correctAnswers = quizRepository.getCorrectAnswers()
        val currentQuiz = quizList[currentQuestionIndex.intValue]
        val isCorrect = selectedAnswer == currentQuiz.correctAnswer

        _quizState.value =
            QuizState.AnswerSelected(isCorrect, selectedAnswer, currentQuiz.correctAnswer)
        if (isCorrect) {
            quizRepository.saveProgress(currentQuestionIndex.intValue + 1, correctAnswers + 1)
            currentQuestionIndex.intValue += 1
        } else {
            quizRepository.saveProgress(currentQuestionIndex.intValue + 1, correctAnswers)
            currentQuestionIndex.intValue += 1
        }
    }

    fun moveToNextQuestion() {
        viewModelScope.launch {
            if (currentQuestionIndex.intValue <= quizList.size) {
                _quizState.value = QuizState.QuizLoaded(quizList[currentQuestionIndex.intValue])
            } else {
                _quizState.value = QuizState.Completed
            }
        }
    }

    fun completeQuiz() {
        viewModelScope.launch {
            val quizResult = Result(
                category = quizList[0].category,
                totalQuestions = quizList.size,
                correctAnswers = quizRepository.getCorrectAnswers()
            )
            resultRepository.saveQuizResult(quizResult)

            quizRepository.saveProgress(0, 0)
            currentQuestionIndex.intValue = 0
            _quizState.value = QuizState.Completed
        }
    }


companion object {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer {
            val application = (this[APPLICATION_KEY] as QuizApplication)
            val quizRepository = application.container.quizRepository
            val resultRepository = application.container.resultRepository
            QuizViewModel(quizRepository = quizRepository, resultRepository = resultRepository)
        }
    }
}


}