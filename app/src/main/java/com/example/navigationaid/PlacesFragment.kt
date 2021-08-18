package com.example.navigationaid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.navigationaid.databinding.FragmentPlacesBinding

class PlacesFragment : Fragment() {
    private var _binding: FragmentPlacesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlacesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerView.layoutManager = GridLayoutManager(this.requireContext(), 3)
        binding.recyclerView.adapter = PlacesAdapter()
    }

    fun createNewPlace() {
        findNavController().navigate(R.id.action_placesFragment_to_placeEditorFragment)
    }

    fun editPlace() {
        val action = PlacesFragmentDirections.actionPlacesFragmentToPlaceEditorFragment(

        )
    }

    fun choosePlace() {
        findNavController().navigate(R.id.action_placesFragment_to_routesFragment)
    }
}