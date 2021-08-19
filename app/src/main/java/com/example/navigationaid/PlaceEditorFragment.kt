package com.example.navigationaid

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.navigationaid.databinding.FragmentPlaceEditorBinding
import com.example.navigationaid.model.NavigationViewModel
import com.example.navigationaid.model.NavigationViewModelFactory

class PlaceEditorFragment : Fragment() {
    private var _binding: FragmentPlaceEditorBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: NavigationViewModel by activityViewModels {
        NavigationViewModelFactory(
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
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonCamera.setOnClickListener {
            openCamera()
        }
        binding.buttonGallery.setOnClickListener {
            openGallery()
        }
        binding.buttonConfirm.setOnClickListener {
            addNewPlaceItem()
        }
        binding.buttonCancel.setOnClickListener {
            cancelUserInput()
        }
        binding.buttonMap.setOnClickListener {
            val action = PlaceEditorFragmentDirections.actionPlaceEditorFragmentToMapFragment()
            findNavController().navigate(action)
        }
        sharedViewModel.placeImage.observe(viewLifecycleOwner,
            {
                displayImage()
            })
        sharedViewModel.placePoint.observe(viewLifecycleOwner,
            {
                updateLocationHint()
            })
    }

    // receives chosen image file after camera/ gallery intent
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE) {
                val thumbnail: Bitmap? = data?.extras?.get("data") as Bitmap?
                if (thumbnail != null) {
                    sharedViewModel.prepareImage(thumbnail)
                }
            } else if (requestCode == GALLERY_REQUEST_CODE) {
                val imageUri: Uri? = data?.data
                if (imageUri != null) {
                    val thumbnail: Bitmap = MediaStore.Images.Media.getBitmap(
                        requireContext().contentResolver,
                        imageUri
                    )
                    sharedViewModel.prepareImage(thumbnail)
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
                    getString(R.string.permission_required),
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