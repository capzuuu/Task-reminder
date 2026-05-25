package com.example.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Task
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskScreen(
    viewModel: TaskViewModel,
    hasNotificationPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Due Soon (<1h)", "Active", "Completed"

    // Filter tasks based on selected filter and current time
    val filteredTasks = remember(tasks, selectedFilter, currentTime) {
        when (selectedFilter) {
            "Due Soon (<1h)" -> tasks.filter { 
                !it.isCompleted && (it.dueTimeMillis - currentTime in 0..3600_000) 
            }
            "Active" -> tasks.filter { !it.isCompleted }
            "Completed" -> tasks.filter { it.isCompleted }
            else -> tasks
        }
    }

    val dueSoonCount = remember(tasks, currentTime) {
        tasks.count { !it.isCompleted && (it.dueTimeMillis - currentTime in 0..3600_000) }
    }

    // Capture the single most urgent task due in < 1h
    val urgentTask = remember(tasks, currentTime) {
        tasks.filter { !it.isCompleted && (it.dueTimeMillis - currentTime in 0..3600_000) }
            .minByOrNull { it.dueTimeMillis }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding()
            ) {
                // Header Row
                HeaderSection()
                
                PermissionBanner(
                    hasPermission = hasNotificationPermission,
                    onRequestPermission = onRequestPermission
                )
            }
        },
        bottomBar = {
            // Elegant modern High Density Navigation Bar
            HighDensityNavigationBar(
                selectedFilter = selectedFilter,
                onFilterSelected = { selectedFilter = it },
                dueSoonCount = dueSoonCount
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary, // #D0BCFF
                contentColor = MaterialTheme.colorScheme.onPrimary, // #381E72
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .testTag("add_task_fab"),
                shape = RoundedCornerShape(16.dp),
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Urgent Reminder Box (Hero widget if there are tasks due in < 1 hour)
            if (urgentTask != null) {
                UrgentReminderBox(
                    task = urgentTask,
                    currentTime = currentTime,
                    onToggleComplete = { viewModel.toggleTaskCompletion(urgentTask) },
                    onEditClick = { taskToEdit = urgentTask }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Section Info Row
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (selectedFilter) {
                        "Due Soon (<1h)" -> "Due In Less Than An Hour"
                        "Active" -> "Active Reminders"
                        "Completed" -> "Completed Today"
                        else -> "All Scheduled Tasks"
                    }.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary, // #D0BCFF
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 4.dp)
                )
                
                Text(
                    text = "${filteredTasks.size} ${if (filteredTasks.size == 1) "TASK" else "TASKS"}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            // Task List or Empty State
            if (filteredTasks.isEmpty()) {
                EmptyStateView(filter = selectedFilter, onAddClicked = { showAddDialog = true })
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        val index = filteredTasks.indexOf(task)
                        val isFirst = index == 0
                        val isLast = index == filteredTasks.size - 1
                        
                        val isDueSoon = !task.isCompleted && (task.dueTimeMillis - currentTime in 0..3600_000)
                        val isOverdue = !task.isCompleted && (task.dueTimeMillis < currentTime)

                        val itemShape = when {
                            isFirst && isLast -> RoundedCornerShape(24.dp)
                            isFirst -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                            isLast -> RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                            else -> RoundedCornerShape(0.dp)
                        }

                        Column {
                            if (!isFirst) {
                                HorizontalDivider(
                                    color = Color(0xFF4A4458),
                                    thickness = 1.dp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            TaskItemRow(
                                task = task,
                                currentTime = currentTime,
                                isDueSoon = isDueSoon,
                                isOverdue = isOverdue,
                                shape = itemShape,
                                onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                                onDelete = { viewModel.deleteTask(task) },
                                onEditClick = { taskToEdit = task }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, desc, dueTime ->
                viewModel.addTask(title, desc, dueTime)
                showAddDialog = false
            },
            onDemoPreset = { presetMinutes ->
                viewModel.addDemoTask(presetMinutes)
                showAddDialog = false
            }
        )
    }

    if (taskToEdit != null) {
        EditTaskDialog(
            task = taskToEdit!!,
            onDismiss = { taskToEdit = null },
            onConfirm = { title, desc, dueTime ->
                viewModel.updateTask(taskToEdit!!, title, desc, dueTime)
                taskToEdit = null
            }
        )
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Task Reminder Logo",
                tint = MaterialTheme.colorScheme.primary, // #D0BCFF
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = "TaskReminder",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 0.5.sp
            )
        }
        
        // Custom Initials Avatar to Match theme jd look
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(Color(0xFF4A4458), shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "TR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun PermissionBanner(
    hasPermission: Boolean,
    onRequestPermission: () -> Unit
) {
    if (!hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Alert",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Notifications are disabled. We cannot alert you of due tasks!",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("enable_notifications_button")
                ) {
                    Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TaskItemRow(
    task: Task,
    currentTime: Long,
    isDueSoon: Boolean,
    isOverdue: Boolean,
    shape: androidx.compose.ui.graphics.Shape,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onEditClick: () -> Unit
) {
    val containerColor = if (task.isCompleted) {
        Color(0xFF25232A).copy(alpha = 0.5f)
    } else {
        Color(0xFF25232A)
    }

    Surface(
        shape = shape,
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Checkbox Styled with custom High Density border
            val checkboxColor = if (task.isCompleted) Color(0xFFD0BCFF) else Color(0xFFD0BCFF).copy(alpha = 0.7f)
            
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(width = 2.dp, color = checkboxColor, shape = RoundedCornerShape(6.dp))
                    .background(
                        color = if (task.isCompleted) Color(0xFFD0BCFF) else Color.Transparent,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onToggleComplete() },
                contentAlignment = Alignment.Center
            ) {
                if (task.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color(0xFF381E72),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Task Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onEditClick() }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.isCompleted) Color(0xFFE6E1E5).copy(alpha = 0.5f) else Color(0xFFE6E1E5),
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = task.description,
                        fontSize = 13.sp,
                        color = if (task.isCompleted) Color(0xFFE6E1E5).copy(alpha = 0.4f) else Color(0xFFE6E1E5).copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Date and Alerts row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sdfDate = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
                    val dateFormatted = sdfDate.format(Date(task.dueTimeMillis))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Due time icon",
                            tint = Color(0xFFD0BCFF).copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = dateFormatted,
                            fontSize = 11.sp,
                            color = Color(0xFFE6E1E5).copy(alpha = 0.6f)
                        )
                    }

                    if (!task.isCompleted) {
                        when {
                            isDueSoon -> {
                                val timeLeftMins = (task.dueTimeMillis - currentTime) / 60_000
                                val timeText = if (timeLeftMins <= 0) "Now!" else "${timeLeftMins}m left"
                                Text(
                                    text = "• due in $timeText",
                                    fontSize = 11.sp,
                                    color = Color(0xFFEF9A9A),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            isOverdue -> {
                                Text(
                                    text = "• overdue",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFF8A80),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Delete action button
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("delete_task_${task.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    tint = Color(0xFFE6E1E5).copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun UrgentReminderBox(
    task: Task,
    currentTime: Long,
    onToggleComplete: () -> Unit,
    onEditClick: () -> Unit
) {
    val timeLeftSecs = (task.dueTimeMillis - currentTime) / 1000
    val timeLeftMins = timeLeftSecs / 60

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF2B8B5) // Urgent pink/red gradient equivalent in design
        ),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEditClick() }
            .testTag("urgent_banner"),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Urgent reminder logo",
                        tint = Color(0xFF601410),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "URGENT REMINDER",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF601410),
                        letterSpacing = 1.2.sp
                    )
                }
                
                // Status Pill
                Box(
                    modifier = Modifier
                        .background(Color(0xFF601410), shape = RoundedCornerShape(100.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "DUE SOON",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF2B8B5)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body content
            Text(
                text = task.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF601410),
                lineHeight = 22.sp
            )
            
            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = task.description,
                    fontSize = 13.sp,
                    color = Color(0xFF601410).copy(alpha = 0.85f),
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Alert Row + Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Timer alarm icon",
                        tint = Color(0xFF601410),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Due soon (${if (timeLeftMins <= 0) "Immediate" else "$timeLeftMins mins left"})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF601410)
                    )
                }
                
                // Active trigger button
                Button(
                    onClick = onToggleComplete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF601410),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = "Done",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun HighDensityNavigationBar(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    dueSoonCount: Int
) {
    val items = listOf(
        Triple("Active", Icons.Default.DateRange, "Active"),
        Triple("Due Soon (<1h)", Icons.Default.Notifications, "Due Soon"),
        Triple("All", Icons.Default.List, "All"),
        Triple("Completed", Icons.Default.CheckCircle, "Completed")
    )

    Surface(
        color = Color(0xFF211F26),
        border = BorderStroke(width = 0.5.dp, color = Color(0xFF4A4458)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .navigationBarsPadding()
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (filterValue, icon, label) ->
                val isSelected = selectedFilter == filterValue

                Column(
                    modifier = Modifier
                        .clickable(
                            onClick = { onFilterSelected(filterValue) },
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        )
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Active Pill indicator
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF4A4458), shape = RoundedCornerShape(100.dp))
                                .padding(horizontal = 20.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BadgeBox(count = if (filterValue == "Due Soon (<1h)") dueSoonCount else 0) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = Color(0xFFD0BCFF),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BadgeBox(count = if (filterValue == "Due Soon (<1h)") dueSoonCount else 0) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = Color(0xFFE6E1E5).copy(alpha = 0.6f),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFFE6E1E5) else Color(0xFFE6E1E5).copy(alpha = 0.6f),
                        letterSpacing = (-0.1).sp
                    )
                }
            }
        }
    }
}

@Composable
fun BadgeBox(
    count: Int,
    content: @Composable () -> Unit
) {
    if (count > 0) {
        Box {
            content()
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(Color(0xFFEF5350), shape = CircleShape)
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-4).dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
    } else {
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, dueTimeMillis: Long) -> Unit,
    onDemoPreset: (presetMinutes: Int) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    // Default due time is 1.5 hours from now
    val calendar = remember { Calendar.getInstance().apply { add(Calendar.MINUTE, 90) } }
    var selectedTime by remember { mutableStateOf(calendar.timeInMillis) }
    var customDateTimeSelected by remember { mutableStateOf(false) }

    fun showDatePicker() {
        val currentCalendar = Calendar.getInstance().apply { timeInMillis = selectedTime }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                currentCalendar.set(Calendar.YEAR, year)
                currentCalendar.set(Calendar.MONTH, month)
                currentCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                selectedTime = currentCalendar.timeInMillis
                customDateTimeSelected = true
            },
            currentCalendar.get(Calendar.YEAR),
            currentCalendar.get(Calendar.MONTH),
            currentCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showTimePicker() {
        val currentCalendar = Calendar.getInstance().apply { timeInMillis = selectedTime }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                currentCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                currentCalendar.set(Calendar.MINUTE, minute)
                selectedTime = currentCalendar.timeInMillis
                customDateTimeSelected = true
            },
            currentCalendar.get(Calendar.HOUR_OF_DAY),
            currentCalendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Add New Task",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Named") },
                    placeholder = { Text("E.g., Turn off stove") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_title_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details (Optional)") },
                    placeholder = { Text("E.g., It will be fully simmered...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_desc_input"),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 3
                )

                Text(
                    text = "Due Time Presets (Perfect for Quick Testing!):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { onDemoPreset(5) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFEDD5),
                            contentColor = Color(0xFFC2410C)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("preset_5m_button")
                    ) {
                        Text("⚡ Just 5 Mins", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onDemoPreset(45) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFEDD5),
                            contentColor = Color(0xFFC2410C)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("preset_45m_button")
                    ) {
                        Text("⏳ 45 Mins (<1h)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onDemoPreset(120) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("preset_2h_button")
                    ) {
                        Text("2 Hours", fontSize = 10.sp)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Or Set Custom Time & Date:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val sdfDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    val sdfTime = SimpleDateFormat("h:mm a", Locale.getDefault())

                    Button(
                        onClick = { showDatePicker() },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("picker_date_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (customDateTimeSelected) sdfDate.format(Date(selectedTime)) else "Choose Date",
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = { showTimePicker() },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("picker_time_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (customDateTimeSelected) sdfTime.format(Date(selectedTime)) else "Choose Time",
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (customDateTimeSelected) {
                    val sdfFull = SimpleDateFormat("EEEE, MMM d 'at' h:mm a", Locale.getDefault())
                    Text(
                        text = "Selected: ${sdfFull.format(Date(selectedTime))}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(title, description, selectedTime)
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("dialog_save_button")
            ) {
                Text("Save Task")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_cancel_button")
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun EmptyStateView(filter: String, onAddClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val (icon, title, tip) = when (filter) {
            "Due Soon (<1h)" -> Triple(
                Icons.Default.CheckCircle,
                "No Tasks Due Soon!",
                "Everything is well in hand. No tasks are due in less than an hour."
            )
            "Active" -> Triple(
                Icons.Default.DateRange,
                "All Tasks Complete",
                "Fantastic job! Go ahead and add a task to stay on top of your day."
            )
            "Completed" -> Triple(
                Icons.Default.Check,
                "No Completed Tasks Yet",
                "Finish a task from the active list to see your accomplishments here!"
            )
            else -> Triple(
                Icons.Default.DateRange,
                "Your Tasks List is Empty",
                "Build a list of tasks. We'll automatically remind you when they are due in less than an hour!"
            )
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            modifier = Modifier.size(72.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = tip,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 32.dp),
            lineHeight = 18.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (filter == "All" || filter == "Active") {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onAddClicked,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("empty_state_add_button")
            ) {
                Text("Add Task Now")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onConfirm: (title: String, desc: String, dueTimeMillis: Long) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    var selectedTime by remember { mutableStateOf(task.dueTimeMillis) }
    var customDateTimeSelected by remember { mutableStateOf(true) }

    fun showDatePicker() {
        val currentCalendar = Calendar.getInstance().apply { timeInMillis = selectedTime }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                currentCalendar.set(Calendar.YEAR, year)
                currentCalendar.set(Calendar.MONTH, month)
                currentCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                selectedTime = currentCalendar.timeInMillis
                customDateTimeSelected = true
            },
            currentCalendar.get(Calendar.YEAR),
            currentCalendar.get(Calendar.MONTH),
            currentCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun showTimePicker() {
        val currentCalendar = Calendar.getInstance().apply { timeInMillis = selectedTime }
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                currentCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                currentCalendar.set(Calendar.MINUTE, minute)
                selectedTime = currentCalendar.timeInMillis
                customDateTimeSelected = true
            },
            currentCalendar.get(Calendar.HOUR_OF_DAY),
            currentCalendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit Task",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Named") },
                    placeholder = { Text("E.g., Turn off stove") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_edit_title_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details (Optional)") },
                    placeholder = { Text("E.g., It will be fully simmered...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_edit_desc_input"),
                    shape = RoundedCornerShape(10.dp),
                    maxLines = 3
                )

                Text(
                    text = "Adjust Due Time Presets:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = { selectedTime = System.currentTimeMillis() + (5 * 60 * 1000L) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFEDD5),
                            contentColor = Color(0xFFC2410C)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("edit_preset_5m_button")
                    ) {
                        Text("⚡ Just 5 Mins", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { selectedTime = System.currentTimeMillis() + (45 * 60 * 1000L) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFEDD5),
                            contentColor = Color(0xFFC2410C)
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("edit_preset_45m_button")
                    ) {
                        Text("⏳ 45 Mins (<1h)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { selectedTime = System.currentTimeMillis() + (120 * 60 * 1000L) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("edit_preset_2h_button")
                    ) {
                        Text("2 Hours", fontSize = 10.sp)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Or Set Custom Time & Date:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val sdfDate = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                    val sdfTime = SimpleDateFormat("h:mm a", Locale.getDefault())

                    Button(
                        onClick = { showDatePicker() },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_picker_date_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = sdfDate.format(Date(selectedTime)),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = { showTimePicker() },
                        colors = ButtonDefaults.filledTonalButtonColors(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("edit_picker_time_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = sdfTime.format(Date(selectedTime)),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                val sdfFull = SimpleDateFormat("EEEE, MMM d 'at' h:mm a", Locale.getDefault())
                Text(
                    text = "Selected: ${sdfFull.format(Date(selectedTime))}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(title, description, selectedTime)
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("dialog_update_button")
            ) {
                Text("Update Task")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_edit_cancel_button")
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}
