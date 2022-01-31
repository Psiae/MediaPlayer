package com.example.mediaplayer.view.fragments

import android.content.ContentUris
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediaplayer.databinding.FragmentFolderBinding
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.view.adapter.FolderAdapter
import com.example.mediaplayer.view.adapter.SongAdapter
import com.example.mediaplayer.viewmodel.SongViewModel
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.material.transition.MaterialFade
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class FolderFragment: Fragment() {

    companion object {
        val TAG = FolderFragment::class.java.simpleName
    }

    @Inject
    @Named("folderAdapterNS")
    lateinit var folderAdapter: FolderAdapter
    @Inject
    @Named("songAdapterNS")
    lateinit var songAdapter: SongAdapter

    @Inject
    lateinit var player: ExoPlayer

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
        enterTransition = MaterialFadeThrough().addTarget(view as ViewGroup).also {
            it.duration = 600L
        }
        exitTransition = MaterialFadeThrough().addTarget(view).also {
            it.duration = 200L
        }
        setupView()
        subToObserver()
    }

    private fun setupView() {
        binding.apply {

            tbLib.setNavigationOnClickListener {
                findNavController().popBackStack()
            }

            rvLib.apply {
                adapter = songAdapter.also {
                    it.setItemClickListener { song ->
                        songViewModel.playOrToggle(song)
                    }
                }
                layoutManager = LinearLayoutManager(requireContext())
            }
        }
    }

    private fun subToObserver() {
        songViewModel.apply {
            curFolder.observe(viewLifecycleOwner) {
                binding.tbLib.title = it.title
            }
            songList.observe(viewLifecycleOwner) {
                val title = songViewModel.curFolder.value!!.title
                songAdapter.songList = it.filter { song ->
                    song.mediaPath == title
                }
            }
            navHeight.observe(viewLifecycleOwner) {
                binding.rvLib.clipToPadding = false
                binding.rvLib.setPadding(0, 0, 0, it)
                Timber.d("${binding.rvLib.paddingBottom} $it")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        with (binding) {
            rvLib.adapter = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        if (_binding == null) Timber.d(FolderFragment::class.java.simpleName + "Destroyed")
    }
}