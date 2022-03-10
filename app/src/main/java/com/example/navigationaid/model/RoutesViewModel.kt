package com.example.navigationaid.model

import android.app.Activity
import android.app.Application
import android.os.AsyncTask
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.lifecycle.*
import com.example.navigationaid.R
import com.example.navigationaid.data.ItemDao
import com.example.navigationaid.data.PlaceItem
import com.example.navigationaid.data.RouteItem
import com.example.navigationaid.data.toGeoPoint
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import okhttp3.internal.userAgent
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

enum class RouteApiStatus { LOADING, ERROR, DONE }

class RoutesViewModel(application: Application, private val itemDao: ItemDao) : AndroidViewModel(application) {
    private val _allRoutes: MutableLiveData<List<RouteItem>> = MutableLiveData(mutableListOf())
    val allRoutes: LiveData<List<RouteItem>> get() = _allRoutes

    private val _allRoads: MutableLiveData<Array<Road>> = MutableLiveData(arrayOf())

    private val _status: MutableLiveData<RouteApiStatus> = MutableLiveData(RouteApiStatus.LOADING)
    val status: LiveData<RouteApiStatus> get() = _status

    private var _startPoint: MutableLiveData<GeoPoint?> = MutableLiveData()
    val startPoint: LiveData<GeoPoint?> get() = _startPoint

    private var _destination: PlaceItem? = null

    private var _endPoint: GeoPoint? = null

    private var _selectedRoute: RouteItem? = null
    val selectedRoute: RouteItem? get() = _selectedRoute

    // show MaterialAlertDialog with Help message
    fun showHelpDialog(activity: Activity, textId: Int) {
        val context = getApplication<Application>().applicationContext
        val textView = TextView(context)
        val scrollView = ScrollView(context)
        textView.setText(textId)
        textView.setPadding(context.resources.getDimensionPixelSize(R.dimen.padding))
        scrollView.addView(textView)
        scrollView.isScrollbarFadingEnabled = false

        MaterialAlertDialogBuilder(activity, R.style.MyAlertDialogStyle)
            .setTitle(R.string.help_dialog_title)
            .setIcon(R.drawable.ic_baseline_help_24)
            .setPositiveButton(R.string.help_dialog_okay, null)
            .setView(scrollView)
            .show()
    }

    // retrieves PlaceItem from the Database
    fun retrievePlaceItem(id: Int): LiveData<PlaceItem> {
        return itemDao.getPlaceItem(id).asLiveData()
    }

    // launch asyncTask to calculate all routes with OSRMRoadManager
    fun getRoads() {
        val context = getApplication<Application>().applicationContext
        val roadManager: RoadManager = OSRMRoadManager(context, userAgent)

        _endPoint = _destination!!.point.toGeoPoint()
        clearRoutingData()

        val waypoints: ArrayList<GeoPoint> = arrayListOf(_startPoint.value!!, _endPoint!!)

        (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_FOOT)
        val task = RoadGetter(this)

        viewModelScope.launch {
            @Suppress("DEPRECATION")
            task.execute(waypoints, roadManager)
        }
    }

    // wait for asyncTask to launch this method and save the roads, update API status
    fun onRoadTaskCompleted(roadList: Array<Road>) {
        if (roadList.size == 1 && roadList[0].mNodes.isEmpty()) {
            // default return values of RoadManager when error has occurred
            _allRoads.value = arrayOf()
            _status.value = RouteApiStatus.ERROR
        } else {
            _allRoads.value = roadList
            _status.value = RouteApiStatus.DONE
        }
    }

    // pack calculated roads with corresponding points, duration and difficulty into RouteItems
    fun calculateRoutes() {
        if (_allRoads.value != null) {
            val routePlaceholder: MutableList<RouteItem> = mutableListOf()
            for (road in _allRoads.value!!) {
                val routeItem = RouteItem(
                    road = road,
                    startPoint = _startPoint.value!!,
                    endPoint = _endPoint!!,
                    duration = road.mDuration,
                    distance = road.mLength
                )
                routePlaceholder.add(routeItem)
            }
            _allRoutes.value = routePlaceholder
        }
    }

    fun clearRoutingData() {
        _allRoads.value = arrayOf()
        _allRoutes.value = mutableListOf()
        _status.value = RouteApiStatus.LOADING
    }

    fun clearLocationData() {
        _startPoint = MutableLiveData()
        _endPoint = null
        _destination = null
    }

    fun setStartPoint(point: GeoPoint?) {
        _startPoint.value = point
    }

    fun setSelectRoad(route: RouteItem) {
        _selectedRoute = route
    }

    fun setDestination(placeItem: PlaceItem) {
        _destination = placeItem
    }

    fun getFormattedDistance(distance: Double): String {
        val context = getApplication<Application>().applicationContext
        val formatter = DecimalFormat("#.##")

        val formattedDistance = if (distance < 1.0) {
            val meters = formatter.format(distance * 1000)
            context.resources.getString(R.string.distance_meters, meters)
        } else {
            val kilometers = formatter.format(distance)
            context.resources.getString(R.string.distance_kilometers, kilometers)
        }

        return formattedDistance
    }

    // convert duration to minutes and format to "X Minuten" string
    fun getFormattedDuration(duration: Double): String {
        val context = getApplication<Application>().applicationContext

        val minutes = (duration / 60.0).toInt()
        return context.resources.getString(R.string.duration_minutes, minutes.toString())
    }

    // convert duration and format into date with calendar instance
    fun getFormattedEta(duration: Double): String {
        val timeMinutes = (duration / 60).toInt()
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MINUTE, timeMinutes)
        return formatter.format(calendar.time)
    }

    // format destination name into "Ziel: X" string
    fun getFormattedDestinationName(): String {
        val context = getApplication<Application>().applicationContext

        val name = _destination?.name
        return if(name != null) {
            context.resources.getString(R.string.destination, name)
        } else {
            ""
        }
    }

    // get ID of selected destination for study task check
    fun getDestinationId(): Int {
        return _destination?.id ?: -1
    }
}

class RoutesViewModelFactory(private val application: Application, private val itemDao: ItemDao) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoutesViewModel(application, itemDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Suppress("DEPRECATION")
class RoadGetter(private val caller: RoutesViewModel) : AsyncTask<Any?, Void?, Array<Road>>() {
    // calculate roads in background
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

    // notify viewModel function on calculation completion
    override fun onPostExecute(result: Array<Road>?) {
        caller.onRoadTaskCompleted(result!!)
    }
}