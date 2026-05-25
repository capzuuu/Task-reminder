package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Task
import com.example.data.TaskRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TaskRepository

    // Real-time ticker to auto-refresh relative and due-soon times
    private val _currentTime = MutableStateFlow(System.currentTimeMillis())
    val currentTime: StateFlow<Long> = _currentTime.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TaskRepository(database.taskDao(), application)

        // Launch a coroutine to tick every 10 seconds
        viewModelScope.launch {
            while (true) {
                delay(10000)
                _currentTime.value = System.currentTimeMillis()
            }
        }
    }

    val tasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTask(title: String, description: String, dueTimeMillis: Long) {
        viewModelScope.launch {
            val task = Task(
                title = title.ifBlank { "Untitled Task" },
                description = description,
                dueTimeMillis = dueTimeMillis,
                isCompleted = false,
                isNotified = false
            )
            repository.insertTask(task)
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            repository.setTaskCompleted(task, !task.isCompleted)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun addDemoTask(minutesOption: Int) {
        viewModelScope.launch {
            val title = when (minutesOption) {
                5 -> "Demo Task (Due in 5 mins)"
                45 -> "Demo Task (Due in 45 mins)"
                120 -> "Demo Task (Due in 2 hrs)"
                else -> "Demo Task"
            }
            val dueTime = System.currentTimeMillis() + (minutesOption * 60 * 1000L)
            val task = Task(
                title = title,
                description = "This is an automated demo task to test the < 1 hour due notification logic.",
                dueTimeMillis = dueTime,
                isCompleted = false,
                isNotified = false
            )
            repository.insertTask(task)
        }
    }
}
