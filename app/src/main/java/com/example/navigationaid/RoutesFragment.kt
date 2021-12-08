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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.navigationaid.databinding.FragmentRoutesBinding
import com.example.navigationaid.model.RouteApiStatus
import com.example.navigationaid.model.RoutesViewModel
import com.example.navigationaid.model.RoutesViewModelFactory
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import org.osmdroid.util.GeoPoint

class RoutesFragment : Fragment() {
    private var _binding: FragmentRoutesBinding? = null
    private val binding get() = _binding!!

    private val navigationArgs: RoutesFragmentArgs by navArgs()

    private lateinit var cancelTokenSrc: CancellationTokenSource

    private val sharedViewModel: RoutesViewModel by activityViewModels {
        RoutesViewModelFactory(
            activity?.application as NavigationAidApplication,
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
    private fun getPlayServiceLocation() {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        val gpsEnabled: Boolean = try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            Log.d(LOG_TAG, "getPlayServiceLocation: $e")
            false
        }
        if (gpsEnabled && getLocationPermission()) {
            val cancelToken = cancelTokenSrc.token
            fusedLocationClient.getCurrentLocation(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                cancelToken
            )
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        sharedViewModel.setStartPoint(GeoPoint(location))
                    } else {
                        sharedViewModel.setStartPoint(null)
                    }
                }
        } else {
            sharedViewModel.setStartPoint(null)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRoutesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cancelTokenSrc = CancellationTokenSource()

        sharedViewModel.clearRoutingData()

        if (!getLocationPermission()) {
            Toast.makeText(requireContext(), getString(R.string.location_permission_required), Toast.LENGTH_SHORT).show()
            val action = RoutesFragmentDirections.actionRoutesFragmentToPlacesFragment()
            findNavController().navigate(action)
        }

        val id = navigationArgs.itemId

        // initialize recyclerView adapter
        val adapter = RoutesAdapter(sharedViewModel)
        binding.apply {
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
        }

        // fetch destination from database, update UI and look for user location
        sharedViewModel.retrievePlaceItem(id).observe(this.viewLifecycleOwner) { selectedDestination ->
            sharedViewModel.setDestination(selectedDestination)
            getPlayServiceLocation()

            val destinationName = sharedViewModel.getFormattedDestinationName()
            binding.textViewDestination.text = destinationName
        }

        // wait for location to be fetched, if successful calculate road with known points
        sharedViewModel.startPoint.observe(this.viewLifecycleOwner) {
            if(it == null) {
                Toast.makeText(requireContext(), getString(R.string.location_unavailable), Toast.LENGTH_SHORT).show()
                val action = RoutesFragmentDirections.actionRoutesFragmentToPlacesFragment()
                findNavController().navigate(action)
            } else {
                sharedViewModel.getRoads()
            }
        }

        // observe API status of route calculation, update UI accordingly
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

        // submit final list of calculated routes, if any have been found
        sharedViewModel.allRoutes.observe(this.viewLifecycleOwner) { routeItems ->
            routeItems.let {
                if(it.isNotEmpty()) {
                    Log.d(LOG_TAG, "onViewCreated: List submitted")
                    adapter.submitList(it)
                }
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            cancelTokenSrc.cancel()
        } catch (e: Exception) {
            Log.d(LOG_TAG, "onDestroy: $e")
        }
        sharedViewModel.clearLocationData()
        _binding = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.help_menu) {
            sharedViewModel.showHelpDialog(requireActivity(), R.string.help_routes)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val LOG_TAG = "RoutesFragment"
        private const val FINE_LOCATION_PERMISSION_CODE = 1
        private const val COARSE_LOCATION_PERMISSION_CODE = 2
    }
}