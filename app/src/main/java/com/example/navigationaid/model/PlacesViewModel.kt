package com.example.navigationaid.model

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.*
import com.example.navigationaid.data.ItemDao
import com.example.navigationaid.data.PlaceItem
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PlacesViewModel(application: Application, private val itemDao: ItemDao) : AndroidViewModel(application) {

    val allPlaceItems: LiveData<List<PlaceItem>> = itemDao.getPlaceItems().asLiveData()

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

    // retrieves PlaceItem from the Database
    fun retrievePlaceItem(id: Int): LiveData<PlaceItem> {
        return itemDao.getPlaceItem(id).asLiveData()
    }

    // update PlaceItem in the Database
    private fun updatePlaceItem(
        placeItem: PlaceItem
    ) {
        viewModelScope.launch {
            itemDao.update(placeItem)
        }
    }

    // swaps preview images, create new and updated placeItem, call function to update in Database
    fun updatePlaceItem(
        oldPlaceItem: PlaceItem,
        placeItemName: String
    ) {
        deleteImage(oldPlaceItem.imageName)
        saveImage()
        val updatedPlaceItem = getUpdatedPlaceItemEntry(
            oldPlaceItem.id,
            placeItemName,
            _placePoint.value!!,
            _placeImageName!!
        )
        updatePlaceItem(updatedPlaceItem)
    }

    // creates and returns PlaceItem
    private fun getNewPlaceItemEntry(
        placeItemName: String,
        placeItemPoint: GeoPoint,
        placeImageName: String
    ): PlaceItem {
        return PlaceItem(
            name = placeItemName,
            point = placeItemPoint.toString(),
            imageName = placeImageName
        )
    }

    // updates and returns PlaceItem
    private fun getUpdatedPlaceItemEntry(
        placeItemId: Int,
        placeItemName: String,
        placeItemPoint: GeoPoint,
        placeItemImageName: String
    ): PlaceItem {
        return PlaceItem(
            id = placeItemId,
            name = placeItemName,
            point = placeItemPoint.toString(),
            imageName = placeItemImageName
        )
    }

    // only call if isEntryValid has returned true!
    // calls saveImage, if successful (_placeImageName has been set) call getNewPlaceItemEntry
    // saves the created PlaceItem in the database with insertPlaceItem
    fun addNewPlaceItem(placeItemName: String) {
        saveImage()
        if (_placeImageName != null) {
            val newPlaceItem =
                getNewPlaceItemEntry(placeItemName, _placePoint.value!!, _placeImageName!!)
            insertPlaceItem(newPlaceItem)
        }
    }

    fun deletePlaceItem(placeItem: PlaceItem) {
        deleteImage(placeItem.imageName)
        viewModelScope.launch {
            itemDao.delete(placeItem)
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

    fun setPlaceImageName(imageName: String) {
        _placeImageName = imageName
    }

    fun setPlacePoint(point: GeoPoint) {
        _placePoint.value = point
    }

    fun setPlaceName(name: String) {
        _placeName.value = name
    }

    // attempts to save image to private app location
    private fun saveImage() {
        val context = getApplication<Application>().applicationContext
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
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

    // attempts to delete old image to make room for updated image
    private fun deleteImage(fileName: String) {
        val context = getApplication<Application>().applicationContext

        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            file.delete()
        }
    }

    companion object {
        private const val LOG_TAG = "PlacesViewModel"
    }
}

class PlacesViewModelFactory(val application: Application, private val itemDao: ItemDao) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlacesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlacesViewModel(application, itemDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}