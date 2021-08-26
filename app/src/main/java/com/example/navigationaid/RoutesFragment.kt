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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.navigationaid.databinding.FragmentRoutesBinding
import com.example.navigationaid.model.RouteApiStatus
import com.example.navigationaid.model.RoutesViewModel
import com.example.navigationaid.model.RoutesViewModelFactory
import okhttp3.internal.userAgent
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.util.GeoPoint

class RoutesFragment : Fragment() {
    private var _binding: FragmentRoutesBinding? = null
    private val binding get() = _binding!!

    private val navigationArgs: RoutesFragmentArgs by navArgs()

    private val sharedViewModel: RoutesViewModel by activityViewModels {
        RoutesViewModelFactory(
            (activity?.application as NavigationAidApplication).database.itemDao()
        )
    }

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

    // prepare location provider
    // try to fetch location
    @SuppressLint("MissingPermission")
    private fun getUserLocation(): GeoPoint? {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
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
            return GeoPoint(location)
        }
        return null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRoutesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.clearData()

        if (!getLocationPermission()) {
            Toast.makeText(requireContext(), getString(R.string.location_permission_required), Toast.LENGTH_SHORT).show()
            val action = RoutesFragmentDirections.actionRoutesFragmentToPlacesFragment()
            findNavController().navigate(action)
        }

        val roadManager: RoadManager = OSRMRoadManager(requireContext(), userAgent)
        val userLocation = getUserLocation()
        val id = navigationArgs.itemId

        if (userLocation == null) {
            Toast.makeText(requireContext(), getString(R.string.location_disabled), Toast.LENGTH_SHORT).show()
            val action = RoutesFragmentDirections.actionRoutesFragmentToPlacesFragment()
            findNavController().navigate(action)
        }

        val adapter = RoutesAdapter(requireContext(), sharedViewModel)
        binding.apply {
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
        }

        sharedViewModel.retrievePlaceItem(id).observe(this.viewLifecycleOwner) { selectedDestination ->
            sharedViewModel.setDestination(selectedDestination)
            sharedViewModel.getRoads(userLocation, selectedDestination, roadManager)

            val destinationName = sharedViewModel.getFormattedDestinationName(requireContext())
            binding.textViewDestination.text = destinationName
        }

        sharedViewModel.status.observe(this.viewLifecycleOwner) {
            Log.d(LOG_TAG, "onViewCreated: API Status changed")
            when(it) {
                RouteApiStatus.ERROR -> {
                    Log.d(LOG_TAG, "onViewCreated: API Status: Error")
                    Toast.makeText(requireContext(), "Fehler bei der Routenberechnung", Toast.LENGTH_SHORT).show()
                    val action = RoutesFragmentDirections.actionRoutesFragmentToPlacesFragment()
                    findNavController().navigate(action)
                }
                RouteApiStatus.DONE -> {
                    Log.d(LOG_TAG, "onViewCreated: API Status: Done")
                    binding.imageViewLoading.visibility = View.GONE
                    sharedViewModel.calculateRoutes()
                }
                else -> {
                    Log.d(LOG_TAG, "onViewCreated: API Status: Loading")
                    binding.imageViewLoading.visibility = View.VISIBLE
                }
            }
        }

        sharedViewModel.allRoutes.observe(this.viewLifecycleOwner) { routeItems ->
            routeItems.let {
                if(it.isNotEmpty()) {
                    Log.d(LOG_TAG, "onViewCreated: List submitted")
                    adapter.submitList(it)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {
        private const val LOG_TAG = "RoutesFragment"
        private const val FINE_LOCATION_PERMISSION_CODE = 1
        private const val COARSE_LOCATION_PERMISSION_CODE = 2
    }
}