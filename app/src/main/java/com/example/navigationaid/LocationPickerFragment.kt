package com.example.navigationaid

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.navigationaid.databinding.FragmentLocationPickerBinding
import com.example.navigationaid.model.LocationFetchStatus
import com.example.navigationaid.model.PlacesViewModel
import com.example.navigationaid.model.PlacesViewModelFactory
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import org.osmdroid.api.IMapController
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

class LocationPickerFragment : Fragment() {
    private var _binding: FragmentLocationPickerBinding? = null
    private val binding get() = _binding!!

    private val navigationArgs: LocationPickerFragmentArgs by navArgs()

    private val sharedViewModel: PlacesViewModel by activityViewModels {
        PlacesViewModelFactory(
            activity?.application as NavigationAidApplication,
            (activity?.application as NavigationAidApplication).database.itemDao()
        )
    }

    private lateinit var map: MapView
    private lateinit var mapController: IMapController // control zoom and pan of map
    private lateinit var cancelTokenSrc: CancellationTokenSource // source for token to send cancellation signal to abort fetching the location

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
    private fun getPlayServiceLocation() {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        var location: Location?
        val gpsEnabled: Boolean = try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            Log.d(LOG_TAG, "getPlayServiceLocation: $e")
            false
        }
        if (gpsEnabled && getLocationPermission()) {
            sharedViewModel.setStatusFetching(true)
            val cancelToken = cancelTokenSrc.token
            fusedLocationClient.getCurrentLocation(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                cancelToken
            )
                .addOnSuccessListener { freshLocation: Location? ->
                    location = freshLocation
                    sharedViewModel.setStatusFetching(false)
                    if (location != null) {
                        mapController.animateTo(GeoPoint(location))
                        mapController.setZoom(DETAIL_ZOOM)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.location_unavailable),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnCanceledListener {
                    sharedViewModel.setStatusFetching(false)
                }
        } else {
            Toast.makeText(
                requireContext(),
                getString(R.string.location_unavailable),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // navigate back and pass id of edited place plus new location
    private fun confirmLocation() {
        val id = navigationArgs.itemId
        val title = navigationArgs.title
        val finalLocation = GeoPoint(map.mapCenter).toString()
        val action = LocationPickerFragmentDirections.actionMapFragmentToPlaceEditorFragment(
            title,
            id,
            finalLocation
        )
        findNavController().navigate(action)
    }

    // navigate back and pass id of edited place without passing location
    // let PlaceEditor know if Map input has been canceled as to not reset user input
    private fun cancelUserInput() {
        val id = if (navigationArgs.itemId > 0) {
            navigationArgs.itemId
        } else {
            CANCEL_MAP_NAVIGATION_CODE // instead of using default ID value, pass special code to give place editor the context of user action
        }
        val title = navigationArgs.title // preserve title of place editor, depending on adding or editing a place
        val location = if (sharedViewModel.placePoint.value != null) {
            sharedViewModel.placePoint.value.toString()
        } else {
            null
        }
        val action = LocationPickerFragmentDirections.actionMapFragmentToPlaceEditorFragment(
            title,
            id,
            location
        )
        findNavController().navigate(action)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationPickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    // prepare map, cancel token and location button, bind button click-listeners
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        map = binding.map
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        mapController = map.controller

        cancelTokenSrc = CancellationTokenSource()

        // when not editing a previous place, zoom to default map view over central europe
        if (sharedViewModel.placePoint.value == null) {
            mapController.setCenter(GeoPoint(MAP_DEFAULT_LATITUDE, MAP_DEFAULT_LONGITUDE))
            mapController.setZoom(OVERVIEW_ZOOM)
        } else {
            mapController.setCenter(sharedViewModel.placePoint.value)
            mapController.setZoom(DETAIL_ZOOM)
        }

        // visualize that location needs time to fetch
        sharedViewModel.locationStatus.observe(this.viewLifecycleOwner) { state ->
            if (state == LocationFetchStatus.WAITING) {
                binding.fabMyLocation.setImageResource(R.drawable.ic_baseline_my_location_24)
            } else {
                binding.fabMyLocation.setImageResource(R.drawable.ic_baseline_watch_later_24)
            }
        }

        binding.apply {
            fabMyLocation.setOnClickListener {
                // user can fetch location or abort fetching, depending of the state of the operation
                when (sharedViewModel.locationStatus.value) {
                    LocationFetchStatus.WAITING -> {
                        getPlayServiceLocation()
                    }
                    else -> {
                        cancelTokenSrc.cancel()
                        cancelTokenSrc = CancellationTokenSource()
                    }
                }
            }
            buttonConfirm.setOnClickListener {
                confirmLocation()
            }
            buttonCancel.setOnClickListener {
                cancelUserInput()
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelTokenSrc.cancel()
        _binding = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.help_menu) {
            sharedViewModel.showHelpDialog(
                requireActivity(),
                getString(R.string.help_location_picker)
            )
            return true
        }

        return super.onOptionsItemSelected(item)
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
