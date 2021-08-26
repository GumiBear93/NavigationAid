package com.example.navigationaid

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.example.navigationaid.databinding.FragmentRouteOverviewBinding
import com.example.navigationaid.model.RoutesViewModel
import com.example.navigationaid.model.RoutesViewModelFactory


class RouteOverviewFragment : Fragment() {
    private var _binding: FragmentRouteOverviewBinding? = null
    private val binding get() = _binding!!

    private val navigationArgs: RouteOverviewFragmentArgs by navArgs()

    private val sharedViewModel: RoutesViewModel by activityViewModels {
        RoutesViewModelFactory(
            (activity?.application as NavigationAidApplication).database.itemDao()
        )
    }

    private fun bind() {
        val route = sharedViewModel.selectedRoute!!
        val duration = ((route.duration)/60).toInt()

        val destinationText = sharedViewModel.getFormattedDestinationName(requireContext())
        val durationText = sharedViewModel.getFormattedDuration(route.duration, requireContext())
        val eta = sharedViewModel.getFormattedEta(duration)
        val difficultyImageResourceId = sharedViewModel.getDifficultyImageResourceId(route.roadDifficulty)
        val difficultyDescription = sharedViewModel.getDifficultyImageDescription(route.roadDifficulty, requireContext())

        binding.apply {
            textViewDestination.text = destinationText
            textViewDuration.text = durationText
            textViewEta.text = eta
            imageViewDifficulty.setImageResource(difficultyImageResourceId)
            imageViewDifficulty.contentDescription = difficultyDescription
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRouteOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val routeId = navigationArgs.routeId
        val routeItem = sharedViewModel.allRoutes.value!![routeId]
        sharedViewModel.setSelectRoad(routeItem)

        bind()
    }
}