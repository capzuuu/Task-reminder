package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", -1)
        if (taskId == -1) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val task = db.taskDao().getTaskById(taskId)
                if (task != null && !task.isCompleted && !task.isNotified) {
                    val currentTime = System.currentTimeMillis()
                    val timeLeftMillis = task.dueTimeMillis - currentTime

                    // Verified that task is due in less than an hour (allowing late triggers up to 24 hours)
                    if (timeLeftMillis <= 3660_000 && timeLeftMillis > -86400_000) { // Up to 24 hours past due or soon
                        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val formattedTime = sdf.format(task.dueTimeMillis)
                        
                        // Show system notification
                        NotificationHelper.showNotification(
                            context = context,
                            taskId = task.id,
                            title = task.title,
                            content = "Due at $formattedTime"
                        )

                        // Update notified state
                        db.taskDao().updateTask(task.copy(isNotified = true))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
