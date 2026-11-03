package edu.iau.cshj.csc402.lab8

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.iau.cshj.csc402.lab8.ui.theme.Lab8Theme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// File 1: StudyTask
data class StudyTask(
    val id: Int,
    val title: String,
    val course: String,
    val isDone: Boolean = false
)

// File 2: TodayUiState
data class TodayUiState(
    val isLoading: Boolean = true,
    val tasks: List<StudyTask> = emptyList(),
    val message: String? = null
)

// File 3: FakeTaskRepository
class FakeTaskRepository {

    suspend fun loadTodayTasks(): List<StudyTask> {
        delay(1500L)
        return listOf(
            StudyTask(1, "Read Chapter 11", "Mobile Apps"),
            StudyTask(2, "Lab 8 report", "Mobile Apps"),
            StudyTask(3, "Revise SQL joins", "Databases")
        )
    }

    suspend fun setDone(id: Int, done: Boolean) {
        delay(300L)
    }
}

// File 4: TodayViewModel
class TodayViewModel : ViewModel() {

    private val repository = FakeTaskRepository()

    private val _uiState = MutableStateFlow(TodayUiState())
    val uiState: StateFlow<TodayUiState> =
        _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val tasks = repository.loadTodayTasks()
            _uiState.update {
                it.copy(isLoading = false, tasks = tasks)
            }
        }
    }

    fun onTaskCheckedChange(task: StudyTask, done: Boolean) {
        viewModelScope.launch {
            repository.setDone(task.id, done)
            _uiState.update { state ->
                state.copy(
                    tasks = state.tasks.map {
                        if (it.id == task.id) it.copy(isDone = done)
                        else it
                    },
                    message = if (done) "Marked as done" else null
                )
            }
        }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(message = null) }
    }
}

// File 5: TodayScreen
@Composable
fun TodayRoute(viewModel: TodayViewModel = viewModel()) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TodayScreen(
        uiState = uiState,
        onTaskCheckedChange = { task, done ->
            viewModel.onTaskCheckedChange(task, done)
        },
        onMessageShown = {
            viewModel.onMessageShown()
        }
    )
}

@Composable
fun TaskRow(
    task: StudyTask,
    onCheckedChange: (StudyTask, Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = task.isDone,
            onCheckedChange = { done ->
                onCheckedChange(task, done)
            }
        )

        Column {
            Text(task.title)
            Text(
                task.course,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun TodayScreen(
    uiState: TodayUiState,
    onTaskCheckedChange: (StudyTask, Boolean) -> Unit,
    onMessageShown: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.message) {
        val message = uiState.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onMessageShown()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { innerPadding ->

        Column(
            Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                Text(
                    "Today",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium
                )

                TextButton(onClick = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Tick a task when you finish it"
                        )
                    }
                }) {
                    Text("Help")
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                uiState.tasks.forEach { task ->
                    TaskRow(task, onTaskCheckedChange)
                }
            }
        }
    }
}

// MainActivity
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Lab8Theme {
                TodayRoute()
            }
        }
    }
}