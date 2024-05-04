package com.example.mystreamapplication

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mystreamapplication.databinding.FragmentChooserBinding

class ChooserFragment: Fragment(R.layout.fragment_chooser) {

    val sharedViewModel: MainViewModel by activityViewModels()

    private var _binding: FragmentChooserBinding? = null
    private val binding: FragmentChooserBinding
        get() = _binding!!

    companion object {
        fun newInstance(): ChooserFragment = ChooserFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentChooserBinding.bind(view)

        binding.rvPlayVideo.layoutManager = LinearLayoutManager(requireContext())
        val adapter = ChooserAdapter { position ->

            sharedViewModel.onPlayVideoSelected(position)
        }
        binding.rvPlayVideo.adapter = adapter

        sharedViewModel.setVideos.observe(viewLifecycleOwner) { items ->
            adapter.setData(items)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}