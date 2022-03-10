package com.example.navigationaid

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.navigationaid.databinding.FragmentPlacesBinding
import com.example.navigationaid.model.PlacesViewModel
import com.example.navigationaid.model.PlacesViewModelFactory
import com.example.navigationaid.model.StudyDataViewModel
import com.example.navigationaid.model.StudyDataViewModelFactory

class PlacesFragment : Fragment(), PlacesAdapter.OnItemClickListener {
    private var _binding: FragmentPlacesBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: PlacesViewModel by activityViewModels {
        PlacesViewModelFactory(
            activity?.application as NavigationAidApplication,
            (activity?.application as NavigationAidApplication).database.itemDao()
        )
    }

    private val dataViewModel: StudyDataViewModel by activityViewModels {
        StudyDataViewModelFactory(
            activity?.application as NavigationAidApplication,
            (activity?.application as NavigationAidApplication).database.itemDao()
        )
    }

    // display more items in the row in landscape orientation
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
    ): View {
        _binding = FragmentPlacesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = PlacesAdapter(requireContext().filesDir, getString(R.string.add_new_place), getString(R.string.edit_place), this)
        binding.recyclerView.adapter = adapter
        sharedViewModel.allPlaceItems.observe(this.viewLifecycleOwner) { placeItems ->
            placeItems.let {
                adapter.submitList(it)
            }

            (binding.recyclerView.layoutManager as GridLayoutManager).scrollToPosition(0)
        }

        val orientation = this.resources.configuration.orientation
        setGridLayout(orientation)

        setHasOptionsMenu(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setGridLayout(newConfig.orientation)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.help_menu) {
            dataViewModel.actionTrigger("$N_FRAGMENT.$N_MEN_HELP")
            sharedViewModel.showHelpDialog(requireActivity(), R.string.help_places)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onItemClicked(name: String) {
        dataViewModel.actionTrigger("$N_FRAGMENT.$name")
    }

    companion object {
        private const val LANDSCAPE_SPAN = 3
        private const val PORTRAIT_SPAN = 2

        private const val N_MEN_HELP = "HelpMenu"
        private const val N_FRAGMENT = "PlacesFragment"
    }
}