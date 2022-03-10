package com.example.navigationaid

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.navigationaid.model.StudyDataViewModel
import com.example.navigationaid.model.StudyDataViewModelFactory
import org.osmdroid.config.Configuration

class MainActivity : AppCompatActivity(R.layout.activity_main) {
    private lateinit var navController: NavController

    private val dataViewModel: StudyDataViewModel by viewModels {
        StudyDataViewModelFactory(
            this.application as NavigationAidApplication,
            (this.application as NavigationAidApplication).database.itemDao()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        setupActionBarWithNavController(navController)

        // useragent for OSMbonusPack, required for loading map and building routes
        Configuration.getInstance().userAgentValue = BuildConfig.APPLICATION_ID
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        if (dataViewModel.allowBackPress) super.onBackPressed()
    }

    // shows Help-Symbol on each screen for user support
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.layout_menu, menu)
        return true
    }
}