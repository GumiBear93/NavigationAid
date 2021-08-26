package com.example.navigationaid.data

import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.util.GeoPoint

data class RouteItem(
    val road: Road,
    val startPoint: GeoPoint,
    val endPoint: GeoPoint,
    val duration: Double,
    val roadDifficulty: RoadDifficulty
)

enum class RoadDifficulty {
    DIFFICULTY_1,
    DIFFICULTY_2,
    DIFFICULTY_3,
    DIFFICULTY_4,
    DIFFICULTY_5
}