package com.example.navigationaid.model

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import com.example.navigationaid.R
import com.example.navigationaid.data.TaskItem
import org.xmlpull.v1.XmlPullParser

class StudyDataViewModel(application: Application) : AndroidViewModel(application) {
    private var _loggingEnabled = false
    val loggingEnabled: Boolean get() = _loggingEnabled

    private val _studyInProgress: MutableLiveData<Boolean> = MutableLiveData(false)
    val studyInProgress: LiveData<Boolean> get() = _studyInProgress

    private var _taskStartTime: Long = -1

    private val _actions = mutableListOf<ActionType>()
    val actions: List<ActionType> get() = _actions

    private var _currentTask = StudyTask()
    val currentTask get() = _currentTask

    private val _tasks: MutableList<StudyTask> = parseTasks()
    val tasks: List<StudyTask> get() = _tasks

    private var _studySubject: StudySubject? = null
    val studySubject: StudySubject? get() = _studySubject

    private var abortCounter: Int = 0

    fun prepareTask(taskId: Int) {
        if (_loggingEnabled) return
        Log.d(TAG, "prepareTask: preparing Task")
        for (task in tasks) {
            if (task.id == taskId) {
                _currentTask = task
            }
        }
    }

    fun startTask() {
        _loggingEnabled = true
        _taskStartTime = System.currentTimeMillis()
        Log.d(TAG, "startTask: started task ${_currentTask.id}")
    }

    fun setStudySubject(
        id: String,
        name: String,
        age: String,
        gender: Gender,
        frequency: Int,
        variety: Int
    ): Boolean {
        return if (isEntryValid(id, name, age)) {
            prepareStudy(id.trim(), name.trim(), age.toInt(), gender, frequency, variety)
            true
        } else {
            false
        }
    }

    private fun isEntryValid(id: String, name: String, age: String): Boolean {
        return (id.isNotBlank() && name.isNotBlank() && age.isNotBlank())
    }

    private fun prepareStudy(
        id: String,
        name: String,
        age: Int,
        gender: Gender,
        frequency: Int,
        variety: Int
    ) {
        Log.d(TAG, "prepareStudy: preparing study subject")
        val subject = StudySubject(id, name, age, gender, frequency, variety)
        _studySubject = subject
        _studyInProgress.value = true
    }

    fun actionTrigger(identifier: String) {
        if (!_loggingEnabled) return

        val filteredIdentifier = identifier.filterNot { it.isWhitespace() }
        val timestamp = System.currentTimeMillis() - _taskStartTime

        Log.d(TAG, "actionTrigger: $identifier at $timestamp")

        val newAction = ActionType()
        newAction.actionName = filteredIdentifier
        newAction.actionTime = timestamp

        _actions.add(newAction)
    }

    private fun parseTasks(): MutableList<StudyTask> {
        val taskList = mutableListOf<StudyTask>()

        val parser: XmlPullParser = getApplication<Application>().resources.getXml(R.xml.tasks)
        var eventType = -1

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.name.equals("task")) {
                    val newTask = StudyTask()
                    newTask.id = parser.getAttributeValue(null, "id").toInt()
                    newTask.name = parser.getAttributeValue(null, "name")

                    while (eventType != XmlPullParser.START_TAG || !parser.name.equals("params")) {
                        eventType = parser.next()
                    }
                    eventType = parser.next()

                    while (eventType != XmlPullParser.END_TAG || !parser.name.equals("params")) {
                        if (eventType == XmlPullParser.START_TAG) {
                            eventType = parser.next()
                            if (eventType == XmlPullParser.TEXT) {
                                val newParameter = parser.text
                                newTask.params.add(newParameter)
                            }
                        }
                        eventType = parser.next()
                    }

                    while (eventType != XmlPullParser.START_TAG || !parser.name.equals("desc")) {
                        eventType = parser.next()
                    }
                    eventType = parser.next()
                    if(eventType == XmlPullParser.TEXT) {
                        val newDescription = parser.text
                        newTask.desc = newDescription
                    }

                    taskList.add(newTask)
                }
            }

            eventType = parser.next()
        }

        return taskList
    }

    fun checkCompletion(taskId: Int, vararg params: String): Boolean {
        if (!_loggingEnabled) return false

        if (taskId == _currentTask.id) {
            var iterator = 0
            for (p in params) {
                if (p.lowercase() != _currentTask.params[iterator].lowercase()) return false
                iterator++
            }
            return true
        }
        return false
    }

    fun stopStudy() {
        Log.d(TAG, "stopStudy: Study stopped")
        _studySubject = null
        _studyInProgress.value = false
        resetTaskData()
    }

    fun resetTaskData() {
        Log.d(TAG, "resetTaskData: Task data reset")
        _taskStartTime = -1
        _actions.clear()
        _currentTask = StudyTask()
        _loggingEnabled = false
        abortCounter = 0
    }

    fun abortTask(): Boolean {
        if (!_loggingEnabled) return false

        val context = getApplication<Application>().applicationContext
        if (abortCounter >= 5) {
            Toast.makeText(context, "task aborted", Toast.LENGTH_SHORT).show()
            resetTaskData()
            return true
        } else {
            abortCounter++
            return false
        }
    }

    fun getTaskButtonLabel(taskId: Int): String {
        val context = getApplication<Application>().applicationContext
        return context.resources.getString(R.string.button_start_task_number, (taskId + 1).toString())
    }

    fun getTaskItems(): List<TaskItem> {
        val taskItems = mutableListOf<TaskItem>()
        for (t in _tasks) {
            val taskItem = TaskItem(t.id)
            taskItems.add(taskItem)
        }
        return taskItems
    }

    companion object {
        const val TAG = "StudyDataViewModel"
    }
}

class StudyDataViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyDataViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudyDataViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class StudyTask {
    var id = -1
    var name = ""
    val params = mutableListOf<String>()
    var desc = ""
}

class ActionType {
    var actionName = ""
    var actionTime: Long = 0
}

enum class Gender { MALE, FEMALE, DIVERSE }

class StudySubject(
    val subjectId: String,
    val subjectName: String,
    val subjectAge: Int,
    val subjectGender: Gender,
    val frequency: Int,
    val variety: Int
)