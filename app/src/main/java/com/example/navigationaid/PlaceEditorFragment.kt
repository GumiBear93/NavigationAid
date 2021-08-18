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

    private val viewModel: NavigationViewModel by activityViewModels {
        NavigationViewModelFactory(
            (activity?.application as NavigationAidApplication).database.itemDao()
        )
    }

    private fun isEntryValid(): Boolean {
        return viewModel.isEntryValid(
            binding.placeName.text.toString()
        )
    }

    private fun addNewPlaceItem() {
        if (isEntryValid()) {
            viewModel.addNewPlaceItem(
                requireContext(),
                binding.placeName.text.toString()
            )
            viewModel.resetUserInput()
            val action = PlaceEditorFragmentDirections.actionPlaceEditorFragmentToPlacesFragment()
            findNavController().navigate(action)
        }
    }

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

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(
            Intent.createChooser(intent, getString(R.string.choose_image)),
            GALLERY_REQUEST_CODE
        )
    }

    private fun displayImage() {
        val imageFile = viewModel.placeImage.value
        if (imageFile !== null) {
            binding.imagePreview.setImageBitmap(imageFile)
        } else {
            binding.imagePreview.setImageResource(R.drawable.home)
        }
    }

    private fun cancelUserInput() {
        viewModel.resetUserInput()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayImage()

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
        viewModel.placeImage.observe(viewLifecycleOwner,
            {
                displayImage()
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.resetUserInput()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE) {
                val thumbnail: Bitmap? = data?.extras?.get("data") as Bitmap?
                if (thumbnail != null) {
                    viewModel.prepareImage(thumbnail)
                }
            } else if (requestCode == GALLERY_REQUEST_CODE) {
                val imageUri: Uri? = data?.data
                if (imageUri != null) {
                    val thumbnail: Bitmap = MediaStore.Images.Media.getBitmap(
                        requireContext().contentResolver,
                        imageUri
                    )
                    viewModel.prepareImage(thumbnail)
                }
            }
        }
    }

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