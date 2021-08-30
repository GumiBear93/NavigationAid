package com.example.navigationaid

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.navigationaid.databinding.FragmentRouteViewerBinding
import com.example.navigationaid.model.RoutesViewModel
import com.example.navigationaid.model.RoutesViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.views.overlay.Polyline

class RouteViewerFragment : Fragment() {
    private var _binding: FragmentRouteViewerBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: RoutesViewModel by activityViewModels {
        RoutesViewModelFactory(
            activity?.application as NavigationAidApplication,
            (activity?.application as NavigationAidApplication).database.itemDao()
        )
    }

    // wait for map to initialize to zoom to bounding box
    private suspend fun waitForRouteZoom() {
        val map = binding.map
        val road = sharedViewModel.selectedRoute!!.road

        delay(200)
        map.zoomToBoundingBox(road.mBoundingBox, false, 64)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val road = sharedViewModel.selectedRoute!!.road

        val destinationName = sharedViewModel.getFormattedDestinationName()
        val etaText =
            getString(R.string.estimated_time_of_arrival) + " " + sharedViewModel.getFormattedEta(
                road.mDuration
            )
        binding.textViewEta.text = etaText
        binding.textViewDestination.text = destinationName


        val map = binding.map
        val roadOverlay: Polyline = RoadManager.buildRoadOverlay(road)
        map.overlays.add(roadOverlay)
        map.invalidate()

        // disable all user interaction on the map
        map.setOnTouchListener { _, _ ->
            true
        }

        lifecycleScope.launch {
            waitForRouteZoom()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        lifecycleScope.launch {
            waitForRouteZoom()
        }
    }
}