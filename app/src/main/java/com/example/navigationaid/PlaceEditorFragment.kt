package com.example.navigationaid

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
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
import com.example.navigationaid.data.PlaceItem
import com.example.navigationaid.data.toGeoPoint
import com.example.navigationaid.databinding.FragmentPlaceEditorBinding
import com.example.navigationaid.model.PlacesViewModel
import com.example.navigationaid.model.PlacesViewModelFactory
import java.io.File

class PlaceEditorFragment : Fragment() {
    private var _binding: FragmentPlaceEditorBinding? = null
    private val binding get() = _binding!!

    lateinit var placeItem: PlaceItem
    private val navigationArgs: PlaceEditorFragmentArgs by navArgs()

    private val sharedViewModel: PlacesViewModel by activityViewModels {
        PlacesViewModelFactory(
            (activity?.application as NavigationAidApplication).database.itemDao()
        )
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
                requireContext(),
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
                requireContext(),
                this.placeItem,
                this.binding.placeName.text.toString()
            )
            sharedViewModel.resetUserInput()
            val action = PlaceEditorFragmentDirections.actionPlaceEditorFragmentToPlacesFragment()
            findNavController().navigate(action)
        }
    }

    // ask for camera permission and create intent to start camera
    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, CAMERA_REQUEST_CODE)
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    // create intent to open gallery for user to choose image
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(
            Intent.createChooser(intent, getString(R.string.choose_image)),
            GALLERY_REQUEST_CODE
        )
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
                if (id != MapFragment.CANCEL_MAP_NAVIGATION_CODE) {
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
                    PlaceEditorFragmentDirections.actionPlaceEditorFragmentToMapFragment(id, title)
                findNavController().navigate(action)
            }
            buttonCancel.setOnClickListener {
                cancelUserInput()
            }
        }
    }

    // repopulate text input if returning from camera, gallery or map
    override fun onResume() {
        super.onResume()
        if (sharedViewModel.placeName.value.toString().isNotEmpty()) {
            binding.placeName.setText(sharedViewModel.placeName.value)
        }
    }

    // receives chosen image file after camera/ gallery intent
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE) {
                val thumbnail: Bitmap? = data?.extras?.get("data") as Bitmap?
                if (thumbnail != null) {
                    sharedViewModel.setPlaceImage(thumbnail)
                }
            } else if (requestCode == GALLERY_REQUEST_CODE) {
                val imageUri: Uri? = data?.data
                if (imageUri != null) {
                    val thumbnail: Bitmap = MediaStore.Images.Media.getBitmap(
                        requireContext().contentResolver,
                        imageUri
                    )
                    sharedViewModel.setPlaceImage(thumbnail)
                }
            }
        }
    }

    // handles action after user has been asked for Camera Permission
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(intent, CAMERA_REQUEST_CODE)
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.image_permission_required),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // store codes for camera/ gallery intents in companion object
    companion object {
        private const val CAMERA_PERMISSION_CODE = 1
        private const val CAMERA_REQUEST_CODE = 2
        private const val GALLERY_REQUEST_CODE = 3
    }
}