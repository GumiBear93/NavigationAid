package com.example.navigationaid

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.navigationaid.databinding.FragmentPlacesBinding
import com.example.navigationaid.model.PlacesViewModel
import com.example.navigationaid.model.PlacesViewModelFactory

class PlacesFragment : Fragment() {
    private var _binding: FragmentPlacesBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: PlacesViewModel by activityViewModels {
        PlacesViewModelFactory(
            (activity?.application as NavigationAidApplication).database.itemDao()
        )
    }

    private fun setGridLayout(orientation: Int) {
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            binding.recyclerView.layoutManager = GridLayoutManager(this.requireContext(), LANDSCAPE_SPAN)
        } else {
            binding.recyclerView.layoutManager = GridLayoutManager(this.requireContext(), PORTRAIT_SPAN)
        }
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

        val adapter = PlacesAdapter(requireContext().filesDir, getString(R.string.add_new_place), getString(R.string.edit_place))
        binding.recyclerView.adapter = adapter
        sharedViewModel.allPlaceItems.observe(this.viewLifecycleOwner) { placeItems ->
            placeItems.let {
                adapter.submitList(it)
            }

            (binding.recyclerView.layoutManager as GridLayoutManager).scrollToPosition(0)
        }

        val orientation = this.resources.configuration.orientation
        setGridLayout(orientation)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setGridLayout(newConfig.orientation)
    }

    companion object {
        private const val LANDSCAPE_SPAN = 3
        private const val PORTRAIT_SPAN = 2
    }
}