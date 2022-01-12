package com.example.mediaplayer.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.FragmentSongBinding
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.model.data.remote.testImageUrl
import com.example.mediaplayer.view.adapter.SongAdapter
import com.example.mediaplayer.viewmodel.SongViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class SongFragment : Fragment() {

    @Inject
    lateinit var songAdapter: SongAdapter
    lateinit var songViewModel: SongViewModel

    private var _binding: FragmentSongBinding? = null
    private val binding: FragmentSongBinding
    get() = _binding!!



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (_binding == null) {
            _binding = FragmentSongBinding.inflate(inflater, container, false)
            Timber.d("SongFragment Inflated")
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        songViewModel = ViewModelProvider(requireActivity())[SongViewModel::class.java]

        setupView()
        setupSongAdapter()
        setupRecyclerView()
        observeSongList()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        if (_binding == null) Timber.d("SongFragment Destroyed")
    }

    private fun observeSongList() {
        songViewModel.songList.observe(viewLifecycleOwner) {
            songAdapter.songList = it
        }
    }

    private fun setupView() {
        binding.apply {
            songToolbar.inflateMenu(R.menu.menu_song_toolbar)
        }
        binding.run {}
    }
    private fun setupSongAdapter() {
        val songList = listOf(
            Song(),
            Song(title = "Summit"),
            Song(title = "Summit", artist = "Rei"),
            Song(title = "Summit", artist = "Rei", album = "Romancer"),
            Song(
                title = "Summit",
                artist = "Rei",
                album = "Romancer",
                imageUri = testImageUrl
            ),
        )
        songViewModel.postSongList(songList)
    }
    private fun setupRecyclerView () {
        binding.rvSongList.apply {
            adapter = songAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
}