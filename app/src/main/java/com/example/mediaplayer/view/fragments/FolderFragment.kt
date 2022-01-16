package com.example.mediaplayer.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediaplayer.databinding.FragmentFolderBinding
import com.example.mediaplayer.view.adapter.FolderAdapter
import com.example.mediaplayer.view.adapter.SongAdapter
import com.example.mediaplayer.viewmodel.SongViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class FolderFragment: Fragment() {

    @Inject
    lateinit var folderAdapter: FolderAdapter
    @Inject
    lateinit var songAdapter: SongAdapter

    private lateinit var songViewModel: SongViewModel

    private var _binding: FragmentFolderBinding? = null
    private val binding: FragmentFolderBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        if (_binding == null) {
            _binding = FragmentFolderBinding.inflate(inflater, container, false)
            Timber.d("${FolderFragment::class.java.simpleName} Inflated")
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        songViewModel = ViewModelProvider(requireActivity())[SongViewModel::class.java]

        setupView()
        subToObserver()

    }

    private fun setupView() {
        binding.apply {
            tbLib.setNavigationOnClickListener {
                findNavController().popBackStack()
            }
            rvLib.apply {
                adapter = songAdapter
                layoutManager = LinearLayoutManager(requireContext())
            }
        }
    }

    private fun subToObserver() {
        songViewModel.curFolder.observe(viewLifecycleOwner) {
            binding.tbLib.title = it.title
        }
        songViewModel.songList.observe(viewLifecycleOwner) {
            songAdapter.songList = it.filter {
                it.mediaPath == songViewModel.curFolder.value!!.title
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        if (_binding == null) Timber.d(FolderFragment::class.java.simpleName + "Destroyed")
    }
}