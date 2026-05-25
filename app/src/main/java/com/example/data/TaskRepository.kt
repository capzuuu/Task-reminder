package com.example.data

import android.content.Context
import com.example.notification.AlarmScheduler
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val context: Context
) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun getTaskById(id: Int): Task? {
        return taskDao.getTaskById(id)
    }

    suspend fun insertTask(task: Task): Long {
        val id = taskDao.insertTask(task)
        val createdTask = task.copy(id = id.toInt())
        // Run alarm scheduling
        AlarmScheduler.scheduleAlarmForTask(context, createdTask)
        return id
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
        // Adjust/schedule alarm based on new details
        AlarmScheduler.scheduleAlarmForTask(context, task)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
        // Clean up system alarm
        AlarmScheduler.cancelAlarmForTask(context, task.id)
    }

    suspend fun setTaskCompleted(task: Task, isCompleted: Boolean) {
        val updatedTask = task.copy(isCompleted = isCompleted)
        taskDao.updateTask(updatedTask)
        if (isCompleted) {
            AlarmScheduler.cancelAlarmForTask(context, task.id)
        } else {
            // Re-schedule if uncompleted
            AlarmScheduler.scheduleAlarmForTask(context, updatedTask.copy(isNotified = false))
        }
    }
}
