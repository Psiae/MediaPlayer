package com.example.mediaplayer.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.FragmentLibraryBinding
import com.example.mediaplayer.model.data.entities.Folder
import com.example.mediaplayer.util.Constants
import com.example.mediaplayer.view.adapter.FolderAdapter
import com.example.mediaplayer.view.adapter.SongAdapter
import com.example.mediaplayer.viewmodel.SongViewModel
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class LibraryFragment: Fragment() {

    companion object {
        val TAG = LibraryFragment::class.java.simpleName
    }

    @Inject
    @Named("folderAdapterNS")
    lateinit var folderAdapter: FolderAdapter
    @Inject
    @Named("songAdapterNS")
    lateinit var songAdapter: SongAdapter

    private val songViewModel: SongViewModel by activityViewModels()

    private var _binding: FragmentLibraryBinding? = null
    private val binding: FragmentLibraryBinding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

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
        subToObserver()
        setupFolderAdapter()
        setupRecyclerView()

        folderAdapter.setOnFolderClicked {
            songViewModel.setCurFolder(it)
            findNavController().navigate(R.id.folderFragment)
        }
        enterTransition = MaterialFadeThrough().addTarget(view as ViewGroup).also {
            it.duration = Constants.FADETHROUGH_IN_DURATION
        }
        exitTransition = MaterialFadeThrough().addTarget(view).also {
            it.duration = Constants.FADETHROUGH_OUT_DURATION
        }
    }

    private fun setupFolderAdapter() {}

    private fun setupRecyclerView() {
        binding.rvLib.apply {
            adapter = folderAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun subToObserver() {
        songViewModel.apply {
            songList.observe(viewLifecycleOwner) { songList ->
                Timber.d("song ${songList.size}")
                songAdapter.songList = songList
            }
            folderList.observe(viewLifecycleOwner) { folderList ->
                Timber.d("folder ${folderList.size}")
                val songList = songAdapter.songList.ifEmpty { songViewModel.songList.value }
                val sizedList = mutableListOf<Folder>()
                folderList.forEach { folder ->
                    val filtered = songList!!.filter {
                        it.mediaPath == folder.title
                    }
                    folder.size = filtered.size
                    sizedList.add(folder)
                }
                Timber.d(sizedList.toString())
                folderAdapter.folderList = sizedList
            }
            navHeight.observe(viewLifecycleOwner) {
                binding.rvLib.clipToPadding = false
                binding.rvLib.setPadding(0, 0, 0, it)
                Timber.d("${binding.rvLib.paddingBottom} $it")
            }
        }
    }
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch() {
            delay(200)
            songViewModel.updateMusicDB()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        with (binding) {
            rvLib.adapter = null
        }
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (_binding == null) Timber.d("LibraryFragment Destroyed") else _binding = null
    }
}