package com.example.navigationaid

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.navigationaid.databinding.FragmentStudyManagerBinding
import com.example.navigationaid.model.Gender
import com.example.navigationaid.model.SendingState
import com.example.navigationaid.model.StudyDataViewModel
import com.example.navigationaid.model.StudyDataViewModelFactory
import kotlinx.coroutines.launch


class StudyManagerFragment : Fragment(), StudyManagerAdapter.OnTaskClickListener {
    private var _binding: FragmentStudyManagerBinding? = null
    private val binding get() = _binding!!

    private val dataViewModel: StudyDataViewModel by activityViewModels {
        StudyDataViewModelFactory(
            activity?.application as NavigationAidApplication,
            (activity?.application as NavigationAidApplication).database.itemDao()
        )
    }

    private fun toggleButtonState(active: Boolean) {
        binding.buttonUpload.apply {
            if (active) {
                alpha = 1.0f
                isClickable = true
            } else {
                alpha = 0.5f
                isClickable = false
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStudyManagerBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setMenuVisibility(false)

        val adapter = StudyManagerAdapter(dataViewModel, this)

        dataViewModel.studyInProgress.observe(this.viewLifecycleOwner) {
            if (it) {
                binding.apply {
                    layoutTaskManager.visibility = View.VISIBLE
                    layoutEntryForm.visibility = View.GONE

                    if(dataViewModel.studySubject != null) {
                        textId.text = dataViewModel.studySubject!!.subjectId.toString()
                        textAge.text = dataViewModel.studySubject!!.subjectAge.toString()
                        textGender.text = when (dataViewModel.studySubject!!.subjectGender) {
                            Gender.MALE -> getString(R.string.study_label_male_gender)
                            Gender.FEMALE -> getString(R.string.study_label_female_gender)
                            else -> getString(R.string.study_label_diverse_gender)
                        }
                        textFrequency.text = dataViewModel.studySubject!!.frequency.toString()
                        textVariety.text = dataViewModel.studySubject!!.variety.toString()
                    }
                }
            } else {
                binding.layoutTaskManager.visibility = View.GONE
                binding.layoutEntryForm.visibility = View.VISIBLE
            }
        }

        dataViewModel.userDataState.observe(this.viewLifecycleOwner) {
            when (it) {
                SendingState.WAITING -> {
                    binding.imageUpload.visibility = View.INVISIBLE
                    toggleButtonState(true)
                }
                SendingState.SENDING -> {
                    binding.imageUpload.visibility = View.INVISIBLE
                    toggleButtonState(false)
                }
                else -> { //DONE
                    binding.imageUpload.visibility = View.VISIBLE
                    toggleButtonState(false)
                }
            }
        }

        binding.apply {
            buttonConfirm.setOnClickListener {
                val id = binding.idInput.text.toString()
                val age = binding.ageInput.text.toString()
                val gender = when (binding.genderOptions.checkedRadioButtonId) {
                    binding.genderMale.id -> Gender.MALE
                    binding.genderFemale.id -> Gender.FEMALE
                    else -> Gender.FEMALE
                }
                val frequency = binding.sliderFrequency.value.toInt()
                val variety = binding.sliderVariety.value.toInt()

                dataViewModel.setStudySubject(id, age, gender, frequency, variety)
            }

            buttonCancel.setOnClickListener {
                val action = StudyManagerFragmentDirections.actionStudyManagerFragmentToHomeFragment()
                findNavController().navigate(action)
            }

            buttonEndStudy.setOnClickListener {
                dataViewModel.stopStudy()
                val action = StudyManagerFragmentDirections.actionStudyManagerFragmentToHomeFragment()
                findNavController().navigate(action)
            }

            buttonUpload.setOnClickListener {
                dataViewModel.sendUserData()
            }

            buttonResetDatabase.setOnClickListener {
                dataViewModel.prepareDataBase()
            }

            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
        }

        adapter.submitList(dataViewModel.getTaskItems())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onTaskClicked(taskId: Int) {
        dataViewModel.prepareTask(taskId)
        val  action = StudyManagerFragmentDirections.actionStudyManagerFragmentToStudyIntermissionFragment()
        findNavController().navigate(action)
    }
}