package com.example.navigationaid

import androidx.multidex.MultiDexApplication
import com.example.navigationaid.data.PlaceItemRoomDatabase

class NavigationAidApplication : MultiDexApplication() {
    // define database that is to be accessible everywhere in the app
    val database: PlaceItemRoomDatabase by lazy { PlaceItemRoomDatabase.getDatabase(this) }
}