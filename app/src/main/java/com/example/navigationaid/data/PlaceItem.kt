package com.example.navigationaid.data

import org.osmdroid.util.GeoPoint

class PlaceItem (
    val id: Int = 0,
    val name: String,
    val point: GeoPoint,
    val fileName: String
)