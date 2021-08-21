package com.example.navigationaid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.navigationaid.databinding.FragmentPlacesBinding
import com.example.navigationaid.model.NavigationViewModel
import com.example.navigationaid.model.NavigationViewModelFactory

class PlacesFragment : Fragment() {
    private var _binding: FragmentPlacesBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: NavigationViewModel by activityViewModels {
        NavigationViewModelFactory(
            (activity?.application as NavigationAidApplication).database.itemDao()
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlacesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = PlacesAdapter(requireContext().filesDir)
        binding.recyclerView.adapter = adapter
        sharedViewModel.allPlaceItems.observe(this.viewLifecycleOwner) { placeItems ->
            placeItems.let {
                adapter.submitList(it)
            }
        }
        adapter.submitList(sharedViewModel.allPlaceItems.value)
        binding.recyclerView.layoutManager = GridLayoutManager(this.requireContext(), 3)
    }
}