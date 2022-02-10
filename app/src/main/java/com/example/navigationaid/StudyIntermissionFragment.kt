package com.example.navigationaid

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.navigationaid.databinding.FragmentStudyIntermissionBinding
import com.example.navigationaid.model.StudyDataViewModel
import com.example.navigationaid.model.StudyDataViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder


class StudyIntermissionFragment : Fragment() {
    private var _binding: FragmentStudyIntermissionBinding? = null
    private val binding get() = _binding!!

    private val dataViewModel: StudyDataViewModel by activityViewModels {
        StudyDataViewModelFactory(
            activity?.application as NavigationAidApplication
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudyIntermissionBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(dataViewModel.loggingEnabled) {
            binding.layoutSuccess.visibility = View.VISIBLE
            binding.layoutTaskBeginning.visibility = View.GONE

            var msg = ""
            for (act in dataViewModel.actions) {
                msg = "$msg\n${(act.actionTime / 1000).toInt()} - ${act.actionName}"
            }
            MaterialAlertDialogBuilder(requireContext(), R.style.MyAlertDialogStyle)
                .setPositiveButton(R.string.help_dialog_okay, null)
                .setMessage(msg)
                .show()
        } else {
            binding.layoutSuccess.visibility = View.GONE
            binding.layoutTaskBeginning.visibility = View.VISIBLE
            binding.textTaskDesc.text = dataViewModel.currentTask.desc
        }

        binding.apply {
            buttonBack.setOnClickListener {
                dataViewModel.resetTaskData()
                val action = StudyIntermissionFragmentDirections.actionStudyIntermissionFragmentToStudyManagerFragment()
                findNavController().navigate(action)
            }
            buttonStartTask.setOnClickListener {
                dataViewModel.startTask()
                val action = StudyIntermissionFragmentDirections.actionStudyIntermissionFragmentToHomeFragment()
                findNavController().navigate(action)
            }
        }
    }

}