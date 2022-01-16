package com.example.mediaplayer.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.FragmentSongBinding
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.model.data.remote.testImageUrl
import com.example.mediaplayer.util.ext.toast
import com.example.mediaplayer.view.adapter.SongAdapter
import com.example.mediaplayer.viewmodel.SongViewModel
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject


@AndroidEntryPoint
class SongFragment : Fragment() {

    @Inject
    lateinit var songAdapter: SongAdapter

    private lateinit var songViewModel: SongViewModel
    private lateinit var navController: NavController

    private var _binding: FragmentSongBinding? = null
    private val binding: FragmentSongBinding
    get() = _binding!!



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
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
        navController = requireActivity().findNavController(R.id.navHostContainer)


        songAdapter.setOnSongClickListener {
            /*val player = SimpleExoPlayer.Builder(requireContext()).build()
            val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it.mediaId)
            val sourceFactory = DefaultDataSource.Factory(requireContext())
            val mediaSource = ProgressiveMediaSource.Factory(sourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))
            Timber.d("$mediaSource $uri $it")
            player.setMediaSource(mediaSource)
            player.prepare()
            player.play()
            binding.pvSong.apply {
                setPlayer(player)
                visibility = View.VISIBLE
            }*/
        }

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
            Timber.d(it.toString())
            songAdapter.songList = it.toList()
        }
    }

    private fun setupView() {
        binding.apply {
            songToolbar.apply {
                inflateMenu(R.menu.menu_song_toolbar)
                setOnMenuItemClickListener { menu ->
                    when (menu.itemId) {
                        R.id.menuSort -> {
                            toast(requireContext(), "Sorted")
                            val list = songAdapter.songList
                            val asc = list.sortedBy { it.title.lowercase() }
                            val desc = list.sortedByDescending { it.title.lowercase() }
                            songAdapter.songList = if (list.toList() == asc) desc else asc
                            true
                        }
                        R.id.menuSettings -> {
                            toast(requireContext(), "Settings Menu")
                            try {
                                navController.navigate(R.id.navBottomSettings)
                            } catch (e: Exception) {
                                toast(requireContext(), "Coming Soon!", blockable = false)
                            }
                            true
                        }
                        else -> false
                    }
                }
            }
        }
        binding.run {}
    }

    private fun setupSongAdapter() {
        if (songViewModel.songList.value?.isEmpty() == true) {
            val songList = mutableListOf(
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
    }
    private fun setupRecyclerView () {
        binding.rvSongList.apply {
            adapter = songAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }
}
