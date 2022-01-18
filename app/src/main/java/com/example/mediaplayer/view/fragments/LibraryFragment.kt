package com.example.mediaplayer.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.FragmentLibraryBinding
import com.example.mediaplayer.model.data.entities.Folder
import com.example.mediaplayer.view.adapter.FolderAdapter
import com.example.mediaplayer.view.adapter.SongAdapter
import com.example.mediaplayer.viewmodel.SongViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class LibraryFragment: Fragment() {

    @Inject
    @Named("folderAdapter")
    lateinit var folderAdapter: FolderAdapter
    @Inject
    @Named("songAdapter")
    lateinit var songAdapter: SongAdapter

    private val songViewModel: SongViewModel by viewModels()

    private var _binding: FragmentLibraryBinding? = null
    private val binding: FragmentLibraryBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        Timber.d("LibraryBinding Inflated")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
        super.onResume()
    }

    private fun subToObserver() {
        songViewModel.songList.observe(viewLifecycleOwner) {}
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
        songViewModel.navHeight.observe(viewLifecycleOwner) {
            binding.rvLib.clipToPadding = false
            binding.rvLib.setPadding(0,0,0, it)
            Timber.d("${binding.rvLib.paddingBottom} $it")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        if (_binding == null) Timber.d("LibraryFragment Destroyed")
    }
}