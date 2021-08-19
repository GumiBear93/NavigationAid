package com.example.navigationaid.model

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.*
import com.example.navigationaid.data.ItemDao
import com.example.navigationaid.data.PlaceItem
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

const val LOG_TAG = "NavViewModel"

class NavigationViewModel(private val itemDao: ItemDao) : ViewModel() {
    private var _placeName: MutableLiveData<String?> = MutableLiveData(null)
    val placeName: LiveData<String?> get() = _placeName

    private var _placePoint: MutableLiveData<GeoPoint?> = MutableLiveData(null)
    val placePoint: LiveData<GeoPoint?> get() = _placePoint

    private val _placeImage: MutableLiveData<Bitmap?> = MutableLiveData(null)
    val placeImage: LiveData<Bitmap?> get() = _placeImage

    private var _placeImageName: String? = null

    // inserts PlaceItem in the Database
    private fun insertPlaceItem(placeItem: PlaceItem) {
        viewModelScope.launch {
            itemDao.insert(placeItem)
        }
    }

    // creates PlaceItem
    private fun getNewPlaceItemEntry(
        placeItemName: String,
        placePoint: GeoPoint,
        placeImageName: String
    ): PlaceItem {
        return PlaceItem(
            name = placeItemName,
            point = placePoint.toString(),
            imageName = placeImageName
        )
    }

    // only call if isEntryValid has returned true!
    // calls saveImage, if successful (_placeImageName has been set) call getNewPlaceItemEntry
    // saves the created PlaceItem in the database with insertPlaceItem
    fun addNewPlaceItem(context: Context, placeItemName: String) {
        saveImage(context)
        if (_placeImageName != null) {
            val newPlaceItem = getNewPlaceItemEntry(placeItemName, _placePoint.value!!, _placeImageName!!)
            insertPlaceItem(newPlaceItem)
        }
    }

    // checks for filled-in name of place, image and geoPoint
    fun isEntryValid(placeItemName: String): Boolean {
        if (placeItemName.isBlank() || _placeImage.value == null || _placePoint.value == null) {
            return false
        }
        return true
    }

    // resets ViewModel properties if Input is canceled
    fun resetUserInput() {
        _placePoint.value = null
        _placeImage.value = null
        _placeName.value = null
        _placeImageName = null
    }

    fun setPlaceImage(image: Bitmap) {
        _placeImage.value = image
    }


    fun setPlacePoint(point: GeoPoint) {
        _placePoint.value = point
    }

    fun setPlaceName(name: String) {
        _placeName.value = name
    }

    //attempts to save image to private app location
    @SuppressLint("SimpleDateFormat")
    private fun saveImage(context: Context) {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val fileName = "JPEG_${timeStamp}"
        try {
            val fos: FileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            val image = _placeImage.value!!
            image.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            _placeImageName = fileName
        } catch (e: Exception) {
            Log.d(LOG_TAG, e.toString())
        }
    }
}

class NavigationViewModelFactory(private val itemDao: ItemDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NavigationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NavigationViewModel(itemDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}