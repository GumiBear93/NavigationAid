package com.example.navigationaid

import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.navigationaid.databinding.FragmentStudyIntermissionBinding
import com.example.navigationaid.model.SendingState
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

    private fun toggleButtonStates(sendingState: SendingState) {
        binding.apply {
            buttonUpload.apply {
                if (sendingState == SendingState.WAITING) {
                    isClickable = true
                    alpha = 1.0f
                } else {
                    isClickable = false
                    alpha = 0.5f
                }
            }
            buttonBack.apply {
                if (sendingState == SendingState.DONE) {
                    isClickable = true
                    alpha = 1.0f
                } else {
                    isClickable = false
                    alpha = 0.5f
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()
        dataViewModel.allowBackPress(false)

        _binding = FragmentStudyIntermissionBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dataViewModel.actionDataState.observe(this.viewLifecycleOwner) {
            if (it == SendingState.DONE) {
                binding.imageUpload.visibility = View.VISIBLE
            } else {
                binding.imageUpload.visibility = View.INVISIBLE
            }
            toggleButtonStates(it)
        }

        if (dataViewModel.loggingEnabled) {
            binding.layoutSuccess.visibility = View.VISIBLE
            binding.layoutTaskBeginning.visibility = View.GONE
        } else {
            binding.layoutSuccess.visibility = View.GONE
            binding.layoutTaskBeginning.visibility = View.VISIBLE
            binding.textTaskDesc.text = dataViewModel.currentTask.desc
        }

        binding.apply {
            buttonBack.setOnClickListener {
                dataViewModel.resetTaskData()
                val action =
                    StudyIntermissionFragmentDirections.actionStudyIntermissionFragmentToStudyManagerFragment()
                findNavController().navigate(action)
            }
            buttonStartTask.setOnClickListener {
                dataViewModel.startTask()
                val action =
                    StudyIntermissionFragmentDirections.actionStudyIntermissionFragmentToHomeFragment()
                findNavController().navigate(action)
            }
            buttonUpload.setOnClickListener {
                dataViewModel.sendActionData()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        dataViewModel.allowBackPress(true)
    }
}