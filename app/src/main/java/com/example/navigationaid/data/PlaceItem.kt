package com.example.navigationaid.data

import android.util.Log
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.osmdroid.util.GeoPoint

const val LOG_TAG = "PlaceItem"

@Entity(tableName = "placeItem")
data class PlaceItem (
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "point")
    val point: String,
    @ColumnInfo(name = "imageName")
    val imageName: String
)

fun String.toGeoPoint(): GeoPoint {
    val pointList: List<String> = this.split(",")
    val point = GeoPoint(0.0, 0.0)
    if(pointList.size == 3) {
        try {
            point.latitude = pointList[0].toDouble()
            point.longitude = pointList[1].toDouble()
        } catch (e: Exception) {
            Log.d(LOG_TAG, e.toString())
        }
    }
    return point
}