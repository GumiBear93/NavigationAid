package com.example.navigationaid

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.provider.MediaStore
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
import androidx.lifecycle.lifecycleScope
import com.example.navigationaid.databinding.FragmentRouteViewerBinding
import com.example.navigationaid.model.RoutesViewModel
import com.example.navigationaid.model.RoutesViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class RouteViewerFragment : Fragment() {
    private var _binding: FragmentRouteViewerBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: RoutesViewModel by activityViewModels {
        RoutesViewModelFactory(
            activity?.application as NavigationAidApplication,
            (activity?.application as NavigationAidApplication).database.itemDao()
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpMap(road: Road) {
        val map = binding.map
        val roadOverlay: Polyline = RoadManager.buildRoadOverlay(road)
        map.overlays.add(roadOverlay)

        val startMarker = Marker(map)
        startMarker.position = road.mNodes.first().mLocation
        startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        startMarker.icon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_baseline_location_on_24)

        val endMarker = Marker(map)
        endMarker.position = road.mNodes.last().mLocation
        endMarker.setAnchor(Marker.ANCHOR_LEFT, Marker.ANCHOR_BOTTOM)
        endMarker.icon = ContextCompat.getDrawable(requireActivity(), R.drawable.ic_goal_flag)

        map.overlays.add(startMarker)
        map.overlays.add(endMarker)

        map.invalidate()

        // disable all user interaction on the map
        map.setOnTouchListener { _, _ ->
            true
        }

        lifecycleScope.launch {
            waitForRouteZoom()
        }
    }

    // wait for map to initialize to zoom to bounding box
    private suspend fun waitForRouteZoom() {
        val map = binding.map
        val road = sharedViewModel.selectedRoute!!.road

        delay(200)
        map.zoomToBoundingBox(road.mBoundingBox, false, 128)
    }

    private fun getExtStoragePermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                URI_PERMISSION_CODE
            )
            false
        }
    }

    // create file to share screenshot of the
    private fun shareMap() {
        if (!getExtStoragePermission()) {
            Toast.makeText(requireContext(), getString(R.string.storage_permission_required), Toast.LENGTH_SHORT).show()
            return
        }

        val rootView = binding.constraintLayout.rootView
        val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        rootView.draw(canvas)

        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "JPEG_${timeStamp}_MapView"

        val shareIntent = Intent(Intent.ACTION_SEND).setType("image/jpeg")
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, fileName)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")

        val contentResolver = requireContext().contentResolver
        val uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )
        val outStream: OutputStream
        try {
            outStream = contentResolver.openOutputStream(uri!!)!!
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
            outStream.close()

            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            startActivity(Intent.createChooser(shareIntent, "Karte teilen"))
        } catch (e: Exception) {
            Log.d("RouteViewerFragment", "shareMap: $e")
        }
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

        binding.apply {
            textViewEta.text = etaText
            textViewDestination.text = destinationName
            fabShareMap.setOnClickListener {
                fabShareMap.visibility = View.GONE
                shareMap()
                fabShareMap.visibility = View.VISIBLE
            }
        }

        setUpMap(road)

        setHasOptionsMenu(true)
    }

    // re-fit the map after device rotation or app-scale change
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        lifecycleScope.launch {
            waitForRouteZoom()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.help_menu) {
            sharedViewModel.showHelpDialog(requireActivity(), getString(R.string.help_route_viewer))
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val URI_PERMISSION_CODE = 1
    }
}