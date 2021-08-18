package com.example.navigationaid

import androidx.multidex.MultiDexApplication
import com.example.navigationaid.data.PlaceItemRoomDatabase

class NavigationAidApplication : MultiDexApplication() {
    val database: PlaceItemRoomDatabase by lazy { PlaceItemRoomDatabase.getDatabase(this) }
}