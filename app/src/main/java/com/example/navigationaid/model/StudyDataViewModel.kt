package com.example.navigationaid.model

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.JsonWriter
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.*
import com.example.navigationaid.R
import com.example.navigationaid.data.ItemDao
import com.example.navigationaid.data.PlaceItem
import com.example.navigationaid.data.PlaceItemRoomDatabase
import com.example.navigationaid.data.TaskItem
import kotlinx.coroutines.*
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.io.StringWriter
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class StudyDataViewModel(application: Application, private val itemDao: ItemDao) :
    AndroidViewModel(application) {
    private var _loggingEnabled = false
    val loggingEnabled: Boolean get() = _loggingEnabled

    private val _studyInProgress: MutableLiveData<Boolean> = MutableLiveData(false)
    val studyInProgress: LiveData<Boolean> get() = _studyInProgress

    private var _taskStartTime: Long = -1

    private val _actions = mutableListOf<ActionType>()

    private var _currentTask = StudyTask()
    val currentTask get() = _currentTask

    private val _tasks: MutableList<StudyTask> = parseTasks()

    private var _studySubject: StudySubject? = null
    val studySubject: StudySubject? get() = _studySubject

    private var _abortCounter: Int = 0

    private val _userDataState = MutableLiveData(SendingState.WAITING)
    val userDataState: LiveData<SendingState> get() = _userDataState

    private val _actionDataState = MutableLiveData(SendingState.WAITING)
    val actionDataState: LiveData<SendingState> get() = _actionDataState

    private var _allowBackPress = true
    val allowBackPress get() = _allowBackPress

    private val client = OkHttpClient.Builder()
        .addInterceptor(AddHeaderInterceptor())
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(40, TimeUnit.SECONDS)
        .connectTimeout(40, TimeUnit.SECONDS)
        .build()

    fun prepareTask(taskId: Int) {
        if (_loggingEnabled) return
        Log.d(TAG, "prepareTask: preparing Task")
        for (task in _tasks) {
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
        age: String,
        gender: Gender,
        frequency: Int,
        variety: Int
    ): Boolean {
        return if (isEntryValid(id, age)) {
            prepareStudy(id.toInt(), age.toInt(), gender, frequency, variety)
            true
        } else {
            false
        }
    }

    private fun isEntryValid(id: String, age: String): Boolean {
        return (id.isNotBlank() && age.isNotBlank())
    }

    private fun prepareStudy(
        id: Int,
        age: Int,
        gender: Gender,
        frequency: Int,
        variety: Int
    ) {
        Log.d(TAG, "prepareStudy: preparing study subject")
        val subject = StudySubject(id, age, gender, frequency, variety)
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
        newAction.actionTime = (timestamp.toFloat() / 1000)

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
                    if (eventType == XmlPullParser.TEXT) {
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
        _userDataState.value = SendingState.WAITING
        _actionDataState.value = SendingState.WAITING
        resetTaskData()
    }

    fun resetTaskData() {
        Log.d(TAG, "resetTaskData: Task data reset")
        _taskStartTime = -1
        _actions.clear()
        _currentTask = StudyTask()
        _loggingEnabled = false
        _abortCounter = 0
        _actionDataState.value = SendingState.WAITING
    }

    fun abortTask(): Boolean {
        if (!_loggingEnabled) return false

        val context = getApplication<Application>().applicationContext
        if (_abortCounter >= 5) {
            Toast.makeText(context, "task aborted", Toast.LENGTH_SHORT).show()
            resetTaskData()
            return true
        } else {
            _abortCounter++
            return false
        }
    }

    fun getTaskButtonLabel(taskId: Int): String {
        val context = getApplication<Application>().applicationContext
        return context.resources.getString(
            R.string.button_start_task_number,
            (taskId + 1).toString()
        )
    }

    fun getTaskItems(): List<TaskItem> {
        val taskItems = mutableListOf<TaskItem>()
        for (t in _tasks) {
            val taskItem = TaskItem(t.id)
            taskItems.add(taskItem)
        }
        return taskItems
    }

    fun sendUserData() {
        _userDataState.value = SendingState.SENDING

        val userDataOutput = StringWriter()
        JsonWriter(userDataOutput).use { jsonWriter ->
            jsonWriter.beginObject()
            jsonWriter.name("id").value(_studySubject!!.subjectId)
            jsonWriter.name("age").value(_studySubject!!.subjectAge)
            jsonWriter.name("gender").value(_studySubject!!.subjectGender.name)
            jsonWriter.name("frequency").value(_studySubject!!.frequency)
            jsonWriter.name("variety").value(_studySubject!!.variety)
            jsonWriter.endObject()
        }

        val call = client.newCall(
            Request.Builder()
                .url("$DB_URL/createuser.php")
                .method("POST", userDataOutput.toString().toRequestBody(JSON))
                .build()
        )

        val response: Response
        runBlocking {
            response = withContext(Dispatchers.Default) {
                call.execute()
            }
        }
        _userDataState.value = if (response.isSuccessful) {
            SendingState.DONE
        } else {
            SendingState.WAITING
        }
    }

    fun sendActionData() {
        _actionDataState.value = SendingState.SENDING
        val actionDataOutput = StringWriter()

        for (act in _actions) {
            JsonWriter(actionDataOutput).use { jsonWriter ->
                jsonWriter.beginObject()
                jsonWriter.name("user_id").value(_studySubject!!.subjectId)
                jsonWriter.name("task_id").value(_currentTask.id)
                jsonWriter.name("actionname").value(act.actionName)
                jsonWriter.name("actiontime").value(act.actionTime)
                jsonWriter.endObject()
            }
            if (act != _actions.last()) {
                actionDataOutput.append("*")
            }
        }

        val call = client.newCall(
            Request.Builder()
                .url("$DB_URL/createdata.php")
                .method("POST", actionDataOutput.toString().toRequestBody(JSON))
                .build()
        )

        val response: Response
        runBlocking {
            response = withContext(Dispatchers.Default) {
                call.execute()
            }
        }
        _actionDataState.value = if (response.isSuccessful) {
            SendingState.DONE
        } else {
            SendingState.WAITING
        }
    }

    fun allowBackPress(allow: Boolean) {
        _allowBackPress = allow
    }

    fun prepareDataBase() {
        val context = getApplication<Application>().applicationContext
        runBlocking(Dispatchers.IO) {
            val allPlaceItems = itemDao.getStaticPlaceItems()
            for (pItem in allPlaceItems) {
                val file = File(context.filesDir, pItem.imageName)
                if (file.exists()) {
                    file.delete()
                }
            }
            itemDao.nukeTable()
            itemDao.resetTableCounter()
            Log.d(TAG, "prepareDataBase: Database reset")
        }

        for (place in 0..2) {
            val img = BitmapFactory.decodeResource(context.resources, imagesPlaces[place])
            val fileName = "Studie$place"
            try {
                val fos: FileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
                img.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            } catch (e: Exception) {
                Log.d(TAG, e.toString())
            }

            val item = PlaceItem(
                name = namesPlaces[place],
                point = pointsPlaces[place],
                imageName = fileName
            )

            runBlocking(Dispatchers.IO) {
                itemDao.insert(item)
            }
            Log.d(TAG, "prepareDataBase: prepared place ${place + 1}")
        }

        Toast.makeText(context, "Datenbank vorbereitet", Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "StudyDataViewModel"
        private const val DB_URL = "https://www-user.tu-chemnitz.de/~flthu/navigationaid/api"
        private val JSON = "application/json".toMediaTypeOrNull()
        private val namesPlaces = listOf("Bääckeri", "Lidl", "Rossmann")
        private val pointsPlaces = listOf(
            "50.8350215,12.886006,0.0",
            "50.8333567,12.8753004,0.0",
            "50.8311811,12.8937353,0.0"
        )
        private val imagesPlaces =
            listOf(R.drawable.baeckerei, R.drawable.lidl, R.drawable.rossmann)
    }
}

class StudyDataViewModelFactory(
    private val application: Application,
    private val itemDao: ItemDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StudyDataViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StudyDataViewModel(application, itemDao) as T
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
    var actionTime: Float = 0.0f
}

enum class Gender { MALE, FEMALE, DIVERSE }

class StudySubject(
    val subjectId: Int,
    val subjectAge: Int,
    val subjectGender: Gender,
    val frequency: Int,
    val variety: Int
)

enum class SendingState { WAITING, SENDING, DONE }

class AddHeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return chain.proceed(
            chain.request().newBuilder()
                .header("X-Requested-With", "XMLHttpRequest")
                .build()
        )
    }
}