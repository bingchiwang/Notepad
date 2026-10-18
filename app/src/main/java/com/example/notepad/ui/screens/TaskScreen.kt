package com.example.notepad.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.notepad.data.local.entity.ToDo
import com.example.notepad.data.repository.TaskRepository

@Composable
fun TaskScreen(
    taskRepository: TaskRepository,
    onTaskClick: (Int) -> Unit
) {
    val toDos by taskRepository.allToDos.collectAsState(initial = emptyList())

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(toDos) { toDo ->
            TaskItem(toDo = toDo, onClick = { onTaskClick(toDo.id) })
        }
    }
}

@Composable
fun TaskItem(toDo: ToDo, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = toDo.title, style = MaterialTheme.typography.titleMedium)
            if (toDo.description.isNotEmpty()) {
                Text(text = toDo.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun TaskDetailScreen(
    taskId: Int,
    taskRepository: TaskRepository
) {
    val toDos by taskRepository.allToDos.collectAsState(initial = emptyList())
    val task = toDos.find { it.id == taskId }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        if (task != null) {
            Text(text = task.title, style = MaterialTheme.typography.headlineMedium)
            Text(text = task.description, style = MaterialTheme.typography.bodyLarge)
        } else {
            Text(text = "Select a task", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
