package com.example.navigationaid.model

import android.app.Activity
import android.app.Application
import android.os.AsyncTask
import android.util.Log
import androidx.lifecycle.*
import com.example.navigationaid.R
import com.example.navigationaid.data.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.*

enum class RouteApiStatus { LOADING, ERROR, DONE }

class RoutesViewModel(application: Application, private val itemDao: ItemDao) : AndroidViewModel(application) {
    private val _allRoutes: MutableLiveData<List<RouteItem>> = MutableLiveData(mutableListOf())
    val allRoutes: LiveData<List<RouteItem>> get() = _allRoutes
    private val _allRoads: MutableLiveData<Array<Road>> = MutableLiveData(arrayOf())
    private val _status: MutableLiveData<RouteApiStatus> = MutableLiveData(RouteApiStatus.LOADING)
    val status: LiveData<RouteApiStatus> get() = _status
    private var _destination: PlaceItem? = null
    val destination get() = _destination
    private var _selectedRoute: RouteItem? = null
    val selectedRoute: RouteItem? get() = _selectedRoute
    private var _startPoint: GeoPoint? = null
    private var _endPoint: GeoPoint? = null

    // show MaterialAlertDialog with Help message
    fun showHelpDialog(activity: Activity, message: String) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Hilfe:")
            .setIcon(R.drawable.ic_baseline_help_24)
            .setMessage(message)
            .setPositiveButton("Okay") { _, _ -> }
            .show()
    }

    // retrieves PlaceItem from the Database
    fun retrievePlaceItem(id: Int): LiveData<PlaceItem> {
        return itemDao.getPlaceItem(id).asLiveData()
    }

    fun getRoads(startPoint: GeoPoint?, destinationItem: PlaceItem, roadManager: RoadManager) {
        this._startPoint = startPoint
        this._endPoint = destinationItem.point.toGeoPoint()
        clearData()

        val waypoints: ArrayList<GeoPoint> = arrayListOf(this._startPoint!!, this._endPoint!!)

        (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_FOOT)
        val task = RoadGetter(this)

        viewModelScope.launch {
            task.execute(waypoints, roadManager)
        }
    }

    fun onRoadTaskCompleted(roadList: Array<Road>) {
        if (roadList.size == 1 && roadList[0].mNodes.isEmpty()) {
            _allRoads.value = null
            _status.value = RouteApiStatus.ERROR
        } else {
            _allRoads.value = roadList
            _status.value = RouteApiStatus.DONE
        }
    }

    fun calculateRoutes() {
        if (_allRoads.value != null) {
            val routePlaceholder: MutableList<RouteItem> = mutableListOf()
            for (road in _allRoads.value!!) {
                val routeItem = RouteItem(
                    road = road,
                    startPoint = _startPoint!!,
                    endPoint = _endPoint!!,
                    duration = road.mDuration,
                    roadDifficulty = RoadDifficulty.DIFFICULTY_5
                )
                routePlaceholder.add(routeItem)
            }
            _allRoutes.value = routePlaceholder
        }
    }

    fun clearData() {
        _allRoads.value = arrayOf()
        _allRoutes.value = mutableListOf()
        _status.value = RouteApiStatus.LOADING
    }

    fun setSelectRoad(route: RouteItem) {
        _selectedRoute = route
    }

    fun setDestination(placeItem: PlaceItem) {
        _destination = placeItem
    }

    fun getDifficultyImageResourceId(difficulty: RoadDifficulty): Int {
        return when (difficulty) {
            RoadDifficulty.DIFFICULTY_1 -> R.drawable.ic_difficulty_1
            RoadDifficulty.DIFFICULTY_2 -> R.drawable.ic_difficulty_2
            RoadDifficulty.DIFFICULTY_3 -> R.drawable.ic_difficulty_3
            RoadDifficulty.DIFFICULTY_4 -> R.drawable.ic_difficulty_4
            else -> R.drawable.ic_difficulty_5
        }
    }

    fun getDifficultyImageDescription(difficulty: RoadDifficulty): String {
        val context = getApplication<Application>().applicationContext

        val difficultyDescriptions = arrayOf(
            context.resources.getString(R.string.description_difficulty_1),
            context.resources.getString(R.string.description_difficulty_2),
            context.resources.getString(R.string.description_difficulty_3),
            context.resources.getString(R.string.description_difficulty_4),
            context.resources.getString(R.string.description_difficulty_5)
        )
        return when (difficulty) {
            RoadDifficulty.DIFFICULTY_1 -> difficultyDescriptions[0]
            RoadDifficulty.DIFFICULTY_2 -> difficultyDescriptions[1]
            RoadDifficulty.DIFFICULTY_3 -> difficultyDescriptions[2]
            RoadDifficulty.DIFFICULTY_4 -> difficultyDescriptions[3]
            else -> difficultyDescriptions[4]
        }
    }

    fun getFormattedDuration(duration: Double): String {
        val context = getApplication<Application>().applicationContext

        val minutes = (duration / 60.0).toInt()
        return context.resources.getString(R.string.duration_minutes, minutes.toString())
    }

    fun getFormattedEta(duration: Double): String {
        val timeMinutes = (duration / 60).toInt()
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, timeMinutes)
        return formatter.format(calendar.time)
    }

    fun getFormattedDestinationName(): String {
        val context = getApplication<Application>().applicationContext

        val name = _destination?.name
        return if(name != null) {
            context.resources.getString(R.string.destination, name)
        } else {
            ""
        }
    }
}

class RoutesViewModelFactory(private val application: Application, private val itemDao: ItemDao) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoutesViewModel(application, itemDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class RoadGetter(private val caller: RoutesViewModel) : AsyncTask<Any?, Void?, Array<Road>>() {
    @Suppress("UNCHECKED_CAST")
    override fun doInBackground(vararg params: Any?): Array<Road> {
        return try {
            val waypoints = params[0] as ArrayList<GeoPoint>
            val roadManager = params[1] as OSRMRoadManager
            roadManager.getRoads(waypoints)
        } catch (e: Exception) {
            Log.d("RoadGetter", "doInBackground: $e")
            arrayOf()
        }
    }

    override fun onPostExecute(result: Array<Road>?) {
        caller.onRoadTaskCompleted(result!!)
    }
}