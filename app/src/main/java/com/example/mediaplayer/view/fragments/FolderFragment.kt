package com.example.mediaplayer.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
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
import javax.inject.Named

@AndroidEntryPoint
class FolderFragment: Fragment() {

    @Inject
    @Named("folderAdapter")
    lateinit var folderAdapter: FolderAdapter
    @Inject
    @Named("songAdapterNS")
    lateinit var songAdapter: SongAdapter

    private val songViewModel: SongViewModel by activityViewModels()

    private var _binding: FragmentFolderBinding? = null
    private val binding: FragmentFolderBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentFolderBinding.inflate(inflater, container, false)
        Timber.d("FolderFragment Inflated")
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        subToObserver()
    }

    private fun setupView() {
        binding.apply {
            tbLib.setNavigationOnClickListener {
                findNavController().popBackStack()
            }
            rvLib.run {
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
            Timber.d("$it ${songAdapter.songList} ${songViewModel.curFolder}")
            songAdapter.songList = it.filter { song ->
                song.mediaPath == songViewModel.curFolder.value!!.title
            }
            Timber.d("$it ${songAdapter.songList} ${songViewModel.curFolder}")
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
        if (_binding == null) Timber.d(FolderFragment::class.java.simpleName + "Destroyed")
    }
}