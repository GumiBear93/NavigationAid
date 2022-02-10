package com.example.navigationaid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.navigationaid.databinding.FragmentRouteOverviewBinding
import com.example.navigationaid.model.RoutesViewModel
import com.example.navigationaid.model.RoutesViewModelFactory
import com.example.navigationaid.model.StudyDataViewModel
import com.example.navigationaid.model.StudyDataViewModelFactory


class RouteOverviewFragment : Fragment() {
    private var _binding: FragmentRouteOverviewBinding? = null
    private val binding get() = _binding!!

    private val navigationArgs: RouteOverviewFragmentArgs by navArgs()

    private val sharedViewModel: RoutesViewModel by activityViewModels {
        RoutesViewModelFactory(
            activity?.application as NavigationAidApplication,
            (activity?.application as NavigationAidApplication).database.itemDao()
        )
    }

    private val dataViewModel: StudyDataViewModel by activityViewModels {
        StudyDataViewModelFactory(
            activity?.application as NavigationAidApplication
        )
    }

    // bind all the known info of the selected route to UI elements
    private fun bindInfo() {
        val route = sharedViewModel.selectedRoute!!

        val destinationText = sharedViewModel.getFormattedDestinationName()
        val durationText = sharedViewModel.getFormattedDuration(route.duration)
        val eta = sharedViewModel.getFormattedEta(route.duration)
        val distance = sharedViewModel.getFormattedDistance(route.distance)

        binding.apply {
            textViewDestination.text = destinationText
            textViewDuration.text = durationText
            textViewEta.text = eta
            textViewDistance.text = distance
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val routeId = navigationArgs.routeId
        val routeItem = sharedViewModel.allRoutes.value!![routeId]
        sharedViewModel.setSelectRoad(routeItem)

        bindInfo()

        // open RouteViewer with selected route
        binding.apply {
            buttonOpenMap.setOnClickListener {
                dataViewModel.actionTrigger("$N_FRAGMENT.$N_BUT_OPEN_MAP")
                val action =
                    RouteOverviewFragmentDirections.actionRouteOverviewFragmentToRouteViewerFragment()
                findNavController().navigate(action)
            }
            buttonStartNavigation.setOnClickListener {
                dataViewModel.actionTrigger("$N_FRAGMENT.$N_BUT_START_NAVIGATION")
                if (dataViewModel.checkCompletion(
                        1,
                        sharedViewModel.getDestinationId().toString()
                    )
                ) {
                    val action =
                        RouteOverviewFragmentDirections.actionRouteOverviewFragmentToStudyIntermissionFragment()
                    findNavController().navigate(action)
                }
            }
        }

        setHasOptionsMenu(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.help_menu) {
            dataViewModel.actionTrigger("${N_FRAGMENT}.${N_MEN_HELP}")
            sharedViewModel.showHelpDialog(
                requireActivity(),
                R.string.help_route_overview
            )
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val N_MEN_HELP = "HelpMenu"
        private const val N_FRAGMENT = "RouteOverviewFragment"
        private const val N_BUT_OPEN_MAP = "ButtonOpenMap"
        private const val N_BUT_START_NAVIGATION = "ButtonStartNavigation"
    }
}