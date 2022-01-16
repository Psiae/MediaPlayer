package com.example.mediaplayer.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.FragmentLibraryBinding
import com.example.mediaplayer.model.data.entities.Folder
import com.example.mediaplayer.util.ext.libraryConstructing
import com.example.mediaplayer.view.adapter.FolderAdapter
import com.example.mediaplayer.view.adapter.SongAdapter
import com.example.mediaplayer.viewmodel.SongViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LibraryFragment: Fragment() {

    @Inject
    lateinit var folderAdapter: FolderAdapter
    @Inject
    lateinit var songAdapter: SongAdapter

    private lateinit var songViewModel: SongViewModel

    private var _binding: FragmentLibraryBinding? = null
    private val binding: FragmentLibraryBinding
        get() = _binding!!

    private var allowRefresh = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (_binding == null) {
            _binding = FragmentLibraryBinding.inflate(inflater, container, false)
            Timber.d("LibraryBinding Inflated")
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (!libraryConstructing) binding.imageView.visibility = View.INVISIBLE

        songViewModel = ViewModelProvider(requireActivity())[SongViewModel::class.java]

        folderAdapter.setOnFolderClicked {
            songViewModel.curFolder.value = it
            binding.run {
                findNavController().navigate(R.id.folderFragment)
            }
        }

        binding.run {
            setupRecyclerView()
            subToObserver()
            setupFolderAdapter()
        }
    }

    private fun setupFolderAdapter() {}

    private fun setupRecyclerView() {
        binding.rvLib.apply {
            adapter = folderAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onResume() {
        songViewModel.getDeviceSong()
        super.onResume()
    }

    private fun subToObserver() {
        songViewModel.songList.observe(viewLifecycleOwner) {
            songAdapter.songList = it
        }
        songViewModel.folderList.observe(viewLifecycleOwner) { folderList ->
            Timber.d(folderList.toString())
            val sizedList = mutableListOf<Folder>()
            folderList.forEach { folder ->
                val filtered = songAdapter.songList.filter {
                    it.mediaPath == folder.title
                }
                folder.size = filtered.size
                sizedList.add(folder)
            }
            Timber.d(sizedList.toString())
            folderAdapter.folderList = sizedList
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        if (_binding == null) Timber.d("LibraryFragment Destroyed")
    }
}