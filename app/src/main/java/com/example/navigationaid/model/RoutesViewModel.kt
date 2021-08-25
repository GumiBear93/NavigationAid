package com.example.navigationaid.model

import android.os.AsyncTask
import android.util.Log
import androidx.lifecycle.*
import com.example.navigationaid.data.ItemDao
import com.example.navigationaid.data.PlaceItem
import com.example.navigationaid.data.toGeoPoint
import com.example.navigationaid.routing.RoadDifficulty
import com.example.navigationaid.routing.RouteItem
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint

enum class RouteApiStatus { LOADING, ERROR, DONE }

class RoutesViewModel(private val itemDao: ItemDao) : ViewModel() {
    private val _allRoutes: MutableLiveData<List<RouteItem>> = MutableLiveData(mutableListOf())
    val allRoutes: LiveData<List<RouteItem>> get() = _allRoutes
    private val _allRoads: MutableLiveData<Array<Road>> = MutableLiveData(arrayOf())
    val allRoads: LiveData<Array<Road>> get() = _allRoads
    private val _status: MutableLiveData<RouteApiStatus> = MutableLiveData(null)
    val status: LiveData<RouteApiStatus> get() = _status


    private lateinit var startPoint: GeoPoint
    private lateinit var endPoint: GeoPoint


    // retrieves PlaceItem from the Database
    fun retrievePlaceItem(id: Int): LiveData<PlaceItem> {
        return itemDao.getPlaceItem(id).asLiveData()
    }

    fun getRoads(startPoint: GeoPoint, destinationItem: PlaceItem, roadManager: RoadManager) {
        this.startPoint = startPoint
        this.endPoint = destinationItem.point.toGeoPoint()
        _status.value = RouteApiStatus.LOADING
        val waypoints: ArrayList<GeoPoint> = arrayListOf(this.startPoint, this.endPoint)

        (roadManager as OSRMRoadManager).setMean(OSRMRoadManager.MEAN_BY_FOOT)
        val task = RoadGetter()

        viewModelScope.launch {
            task.execute(waypoints, roadManager)
        }
        viewModelScope.launch {
            val roadPlaceholder: Array<Road> = task.get()

            if (roadPlaceholder.size == 1 && roadPlaceholder[0].mNodes.isEmpty()) {
                _allRoads.value = null
                _status.value = RouteApiStatus.ERROR
            } else {
                _allRoads.value = roadPlaceholder
                _status.value = RouteApiStatus.DONE
            }
        }
    }

    fun calculateRoutes() {
        if(_allRoads.value != null) {
            val routePlaceholder: MutableList<RouteItem> = mutableListOf()
            for (road in _allRoads.value!!) {
                val routeItem = RouteItem(
                    road = road,
                    startPoint = startPoint,
                    endPoint = endPoint,
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
        _status.value = null
    }
}

class RoutesViewModelFactory(private val itemDao: ItemDao) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RoutesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RoutesViewModel(itemDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class RoadGetter : AsyncTask<Any?, Void?, Array<Road>>() {
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
}