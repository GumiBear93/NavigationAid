package com.example.navigationaid

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.navigationaid.databinding.FragmentMapBinding
import com.example.navigationaid.model.PlacesViewModel
import com.example.navigationaid.model.PlacesViewModelFactory
import org.osmdroid.api.IMapController
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

class MapFragment : Fragment() {
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val navigationArgs: MapFragmentArgs by navArgs()

    private val sharedViewModel: PlacesViewModel by activityViewModels {
        PlacesViewModelFactory(
            (activity?.application as NavigationAidApplication).database.itemDao()
        )
    }

    private lateinit var map: MapView
    private lateinit var mapController: IMapController
    private lateinit var locationManager: LocationManager

    // ask user for location permission
    private fun getLocationPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                FINE_LOCATION_PERMISSION_CODE
            )
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION),
                COARSE_LOCATION_PERMISSION_CODE
            )
            false
        }
    }

    // prepare location provider, test for permission in getLocationPermission
    // try to fetch location and pan map-center to chosen point
    @SuppressLint("MissingPermission")
    private fun setLocationAsUserLocation() {
        var location: Location? = null
        val gpsEnabled: Boolean = try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            Log.d(LOG_TAG, "setLocationAsUserLocation: $e")
            false
        }
        if (gpsEnabled && getLocationPermission()) {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        }
        if (location != null) {
            mapController.animateTo(GeoPoint(location))
            mapController.setZoom(DETAIL_ZOOM)
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.location_disabled),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // navigate back and pass id of edited place plus new location
    private fun confirmLocation() {
        val id = navigationArgs.itemId
        val title = navigationArgs.title
        val finalLocation = GeoPoint(map.mapCenter).toString()
        val action = MapFragmentDirections.actionMapFragmentToPlaceEditorFragment(title, id, finalLocation)
        findNavController().navigate(action)
    }

    // navigate back and pass id of edited place without passing location
    // let PlaceEditor know if Map input has been canceled as to not reset user input
    private fun cancelUserInput() {
        val id = if (navigationArgs.itemId > 0) {
            navigationArgs.itemId
        } else {
            CANCEL_MAP_NAVIGATION_CODE
        }
        val title = navigationArgs.title
        val location = if (sharedViewModel.placePoint.value != null) {
            sharedViewModel.placePoint.value.toString()
        } else {
            null
        }
        val action = MapFragmentDirections.actionMapFragmentToPlaceEditorFragment(title, id, location)
        findNavController().navigate(action)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    // prepare map and location manager, bind buttons
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        map = binding.map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        mapController = map.controller

        if (sharedViewModel.placePoint.value == null) {
            mapController.setCenter(GeoPoint(MAP_DEFAULT_LATITUDE, MAP_DEFAULT_LONGITUDE))
            mapController.setZoom(OVERVIEW_ZOOM)
        } else {
            mapController.setCenter(sharedViewModel.placePoint.value)
            mapController.setZoom(DETAIL_ZOOM)
        }

        locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        binding.apply {
            fabMyLocation.setOnClickListener {
                setLocationAsUserLocation()
            }
            buttonConfirm.setOnClickListener {
                confirmLocation()
            }
            buttonCancel.setOnClickListener {
                cancelUserInput()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        const val CANCEL_MAP_NAVIGATION_CODE = -2
        private const val LOG_TAG = "MapFragment"
        private const val FINE_LOCATION_PERMISSION_CODE = 1
        private const val COARSE_LOCATION_PERMISSION_CODE = 2
        private const val DETAIL_ZOOM = 20.0
        private const val OVERVIEW_ZOOM = 5.0
        private const val MAP_DEFAULT_LATITUDE = 50.0
        private const val MAP_DEFAULT_LONGITUDE = 10.0
    }
}
