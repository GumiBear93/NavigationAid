package com.example.navigationaid

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.navigationaid.data.PlaceItem
import com.example.navigationaid.data.toGeoPoint
import com.example.navigationaid.databinding.FragmentPlaceEditorBinding
import com.example.navigationaid.model.PlacesViewModel
import com.example.navigationaid.model.PlacesViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class PlaceEditorFragment : Fragment() {
    private var _binding: FragmentPlaceEditorBinding? = null
    private val binding get() = _binding!!

    private lateinit var placeItem: PlaceItem
    private val navigationArgs: PlaceEditorFragmentArgs by navArgs()

    private val sharedViewModel: PlacesViewModel by activityViewModels {
        PlacesViewModelFactory(
            activity?.application as NavigationAidApplication,
            (activity?.application as NavigationAidApplication).database.itemDao()
        )
    }

    private val getCameraImage = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            sharedViewModel.setPlaceImage(bitmap)
        }
    }

    private val getGalleryImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }
            sharedViewModel.setPlaceImage(bitmap)
        }
    }

    // call ViewModel function to validate user input
    // name is only part of the UI that is not saved in ViewModel
    private fun isEntryValid(): Boolean {
        return sharedViewModel.isEntryValid(
            binding.placeName.text.toString()
        )
    }

    // confirm user input, call ViewModel functions to save input to database, navigate back
    private fun addNewPlaceItem() {
        if (isEntryValid()) {
            sharedViewModel.addNewPlaceItem(
                binding.placeName.text.toString()
            )
            sharedViewModel.resetUserInput()
            val action = PlaceEditorFragmentDirections.actionPlaceEditorFragmentToPlacesFragment()
            findNavController().navigate(action)
        }
    }

    // confirm user input, call ViewModel functions to update PlaceItem in database, navigate back
    private fun updatePlaceItem() {
        if (isEntryValid()) {
            sharedViewModel.updatePlaceItem(
                this.placeItem,
                this.binding.placeName.text.toString()
            )
            sharedViewModel.resetUserInput()
            val action = PlaceEditorFragmentDirections.actionPlaceEditorFragmentToPlacesFragment()
            findNavController().navigate(action)
        }
    }

    // ask for camera permission and launch activity contract to start camera
    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val void: Void? = null
            getCameraImage.launch(void)
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    // launch activity contract to open gallery for user to choose image
    private fun openGallery() {
        getGalleryImage.launch(GALLERY_IMAGE_MIMETYPE)
    }

    // update preview image (called by observer)
    private fun displayImage() {
        if (sharedViewModel.placeImage.value !== null) {
            binding.imagePreview.setImageBitmap(sharedViewModel.placeImage.value!!)
        } else {
            binding.imagePreview.setImageResource(R.drawable.home)
        }
    }

    // change the hint's text when the user selected a location
    private fun updateLocationHint() {
        if (sharedViewModel.placePoint.value == null) {
            binding.textViewMap.setText(R.string.choose_place)
        } else {
            binding.textViewMap.setText(R.string.place_chosen)
        }
    }

    // save name when navigating into Map, Camera or Gallery
    private fun storeTextInput() {
        if (binding.placeName.text.toString().isNotEmpty()) {
            sharedViewModel.setPlaceName(binding.placeName.text.toString())
        }
    }

    // binding all known PlaceItem properties to UI and prepare to edit item
    private fun bindItem(placeItem: PlaceItem) {
        val file = File(requireContext().filesDir, placeItem.imageName)
        val image: Bitmap? = try {
            BitmapFactory.decodeFile(file.path)
        } catch (e: Exception) {
            Log.d("PlaceAdapter", "bind: Error reading image")
            null
        }

        sharedViewModel.apply {
            setPlacePoint(placeItem.point.toGeoPoint())
            setPlaceImageName(placeItem.imageName)
            if (image != null) {
                setPlaceImage(image)
            }
        }

        binding.placeName.setText(placeItem.name)
    }

    // reset viewModel input after user presses "abort"
    private fun cancelUserInput() {
        sharedViewModel.resetUserInput()
        val action = PlaceEditorFragmentDirections.actionPlaceEditorFragmentToPlacesFragment()
        findNavController().navigate(action)
    }

    private fun showConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext()).setTitle(getString(R.string.delete_confirmation_title))
            .setMessage(getString(R.string.delete_confirmation_message))
            .setCancelable(true)
            .setNegativeButton(getString(R.string.cancel)) { _, _ -> }
            .setPositiveButton(getString(R.string.delete_place)) { _, _ ->
                deletePlace()
                findNavController().navigateUp()
            }
            .show()
    }

    private fun deletePlace() {
        sharedViewModel.deletePlaceItem(placeItem)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaceEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    // bind buttons and set observers to image/ location data to update UI
    // change functionality to editing if id was passed through navArgs
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedViewModel.apply {
            placeImage.observe(viewLifecycleOwner,
                {
                    displayImage()
                })
            placePoint.observe(viewLifecycleOwner,
                {
                    updateLocationHint()
                })
        }

        // I am extremely sorry for the following lines of code
        // this is the only way i could think of to cover all cases of user navigation
        // please don't hurt me, writing this has hurt enough

        val id = navigationArgs.itemId
        val placePointString = navigationArgs.selectedGeopoint

        // user is editing a PlaceItem
        if (id > 0) {
            binding.apply {
                buttonConfirm.setOnClickListener {
                    updatePlaceItem()
                }
                buttonDelete.setOnClickListener {
                    showConfirmationDialog()
                }
                buttonDelete.visibility = View.VISIBLE
            }


            // user just entered PlaceEditor
            if (placePointString == null) {
                sharedViewModel.retrievePlaceItem(id)
                    .observe(this.viewLifecycleOwner) { selectedItem ->
                        placeItem = selectedItem
                        bindItem(placeItem)
                    }
            }
            // user selected new GeoPoint while editing PlaceItem
            else {
                sharedViewModel.retrievePlaceItem(id)
                    .observe(this.viewLifecycleOwner) { selectedItem ->
                        placeItem = selectedItem
                    }
                sharedViewModel.setPlacePoint(placePointString.toGeoPoint())

            }
        }
        // user is adding a new PlaceItem
        else {
            binding.apply {
                buttonConfirm.setOnClickListener {
                    addNewPlaceItem()
                }
                buttonDelete.visibility = View.GONE
            }

            // user just entered PlaceEditor or comes back from Map without saving location
            if (placePointString == null) {
                // user entered PlaceEditor from Places List
                if (id != LocationPickerFragment.CANCEL_MAP_NAVIGATION_CODE) {
                    sharedViewModel.resetUserInput()
                }
            }
            // user selected a new GeoPoint for new PlaceItem
            else {
                sharedViewModel.setPlacePoint(placePointString.toGeoPoint())
            }
        }

        binding.apply {
            buttonCamera.setOnClickListener {
                storeTextInput()
                openCamera()
            }
            buttonGallery.setOnClickListener {
                storeTextInput()
                openGallery()
            }
            buttonMap.setOnClickListener {
                storeTextInput()
                val title = navigationArgs.title
                val action =
                    PlaceEditorFragmentDirections.actionPlaceEditorFragmentToLocationPickerFragment(id, title)
                findNavController().navigate(action)
            }
            buttonCancel.setOnClickListener {
                cancelUserInput()
            }
        }

        setHasOptionsMenu(true)
    }

    // repopulate text input if returning from camera, gallery or map
    override fun onResume() {
        super.onResume()
        if (sharedViewModel.placeName.value.toString().isNotEmpty()) {
            binding.placeName.setText(sharedViewModel.placeName.value)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.help_menu) {
            sharedViewModel.showHelpDialog(requireActivity(), getString(R.string.help_place_editor))
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    // store codes for camera/ gallery intents in companion object
    companion object {
        private const val CAMERA_PERMISSION_CODE = 1
        private const val GALLERY_IMAGE_MIMETYPE = "image/*"
    }
}