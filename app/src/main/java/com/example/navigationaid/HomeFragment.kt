package com.example.navigationaid

import android.app.Application
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.navigationaid.databinding.FragmentHomeBinding
import com.example.navigationaid.model.PlacesViewModel
import com.example.navigationaid.model.PlacesViewModelFactory
import com.example.navigationaid.model.StudyDataViewModel
import com.example.navigationaid.model.StudyDataViewModelFactory


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val sharedViewModel: PlacesViewModel by activityViewModels {
        PlacesViewModelFactory(
            activity?.application as NavigationAidApplication,
            (activity?.application as NavigationAidApplication).database.itemDao()
        )
    }

    private val dataViewModel: StudyDataViewModel by activityViewModels {
        StudyDataViewModelFactory(
            activity?.application as NavigationAidApplication
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //take user to List of Places in PlacesFragment
        binding.buttonMyPlaces.setOnClickListener {
            dataViewModel.actionTrigger("$N_FRAGMENT.$N_BUT_MY_PLACES")
            findNavController().navigate(R.id.action_homeFragment_to_placesFragment)
        }
        binding.buttonStudyInfo.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_studyManagerFragment)
        }
        binding.buttonStudyInfo.visibility = if (dataViewModel.loggingEnabled) {
            View.GONE
        } else {
            View.VISIBLE
        }
        binding.imageLogo.setOnClickListener {
            if (dataViewModel.abortTask()) {
                val action = HomeFragmentDirections.actionHomeFragmentToStudyManagerFragment()
                findNavController().navigate(action)
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.help_menu) {
            dataViewModel.actionTrigger("$N_FRAGMENT.$N_MEN_HELP")
            sharedViewModel.showHelpDialog(requireActivity(), R.string.help_home)
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val N_MEN_HELP = "HelpMenu"
        private const val N_FRAGMENT = "HomeFragment"
        private const val N_BUT_MY_PLACES = "ButtonMyPlaces"
    }
}