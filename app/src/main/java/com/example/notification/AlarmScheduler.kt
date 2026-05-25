package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.Task

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"

    fun scheduleAlarmForTask(context: Context, task: Task) {
        if (task.isCompleted || task.isNotified) {
            cancelAlarmForTask(context, task.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val currentTime = System.currentTimeMillis()
        
        // Notifications are triggered exactly 1 hour before the task's due time
        val triggerTime = task.dueTimeMillis - 3600_000

        Log.d(TAG, "Scheduling task ${task.id}: Current Time = $currentTime, Due Time = ${task.dueTimeMillis}, Trigger Time = $triggerTime")

        // If the task is due in less than an hour from now and is not yet notified, trigger IMMEDIATELY
        if (task.dueTimeMillis > currentTime && triggerTime <= currentTime) {
            Log.d(TAG, "Task ${task.id} is due in less than 1 hour! Triggering immediate notification.")
            triggerImmediateNotification(context, task)
            return
        }

        // If both due time and trigger time are already in the past, do not schedule
        if (triggerTime <= currentTime) {
            Log.d(TAG, "Trigger time in the past, skipping automatic alarm schedule.")
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            val canSetExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            if (canSetExact && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.d(TAG, "Using setExactAndAllowWhileIdle for millisecond precision")
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Log.d(TAG, "Exact alarm permission not granted, falling back to setAndAllowWhileIdle")
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Alarm schedule succeeded for Task ID: ${task.id}")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException scheduling exact alarm, attempting setAndAllowWhileIdle fallback", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Fallback alarm scheduling also failed", ex)
            }
        }
    }

    fun cancelAlarmForTask(context: Context, taskId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Canceled scheduled alarm for Task ID: $taskId")
        }
    }

    private fun triggerImmediateNotification(context: Context, task: Task) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
        }
        context.sendBroadcast(intent)
    }
}
