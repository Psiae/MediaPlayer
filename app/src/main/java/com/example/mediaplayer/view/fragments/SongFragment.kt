package com.example.mediaplayer.view.fragments

import android.content.ContentUris
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.SearchView
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.FragmentSongBinding
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.util.ext.toast
import com.example.mediaplayer.view.adapter.SongAdapter
import com.example.mediaplayer.viewmodel.SongViewModel
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named


@AndroidEntryPoint
class SongFragment : Fragment(), SearchView.OnQueryTextListener {

    @Inject
    @Named("songAdapterNS")
    lateinit var songAdapter: SongAdapter

    private val songViewModel: SongViewModel by activityViewModels()
    private lateinit var navController: NavController

    @Inject
    lateinit var player: ExoPlayer

    private var _binding: FragmentSongBinding? = null
    private val binding: FragmentSongBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        _binding = FragmentSongBinding.inflate(inflater, container, false)
        Timber.d("SongFragment Inflated")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        navController = requireActivity().findNavController(R.id.navHostContainer)
        songAdapter.setOnSongClickListener { song ->
            play(song)
        }
        CoroutineScope(Dispatchers.Main).launch {
            setupView()
            setupRecyclerView()
            delay(100)
            binding.songProgressBar.visibility = View.GONE
            observeSongList()
        }
    }

    override fun onResume() {
        super.onResume()
        songViewModel.getDeviceSong("songFragment onResume")
    }

    private fun play(song: Song) {
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.mediaId)
        val sourceFactory = DefaultDataSource.Factory(requireContext())
        val mediaSource = ProgressiveMediaSource.Factory(sourceFactory)
            .createMediaSource(MediaItem.fromUri(uri))
        Timber.d("$mediaSource $uri $song $player")
        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true
        songViewModel.isPlaying.value = true
    }

    private fun observeSongList() {
        songViewModel.navHeight.observe(viewLifecycleOwner) {
            binding.rvSongList.setPadding(0,0,0, it)
            Timber.d("${binding.rvSongList.paddingBottom} $it")
        }
        songViewModel.songList.observe(viewLifecycleOwner) {
            songAdapter.songList = it
        }
    }

    private fun setupView() {
        binding.apply {
            songToolbar.run {
                menu.clear()
                inflateMenu(R.menu.menu_song_toolbar)

                val search = menu.findItem(R.id.menuSearch).actionView as SearchView
                search.apply {
                    isSubmitButtonEnabled = true
                    setOnQueryTextListener(this@SongFragment)
                }

                Timber.d("songToolbar Inflated")
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
        binding.run {
            Timber.d("BindingRun")
            songAdapter.differ.addListListener { prev, cur ->
                when (prev) {
                    cur -> Unit
                    else -> rvSongList.scrollToPosition(0)
                }
            }
        }
    }

    private fun setupRecyclerView () {
        binding.rvSongList.apply {
            adapter = songAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        if (_binding == null) Timber.d("SongFragment Destroyed")
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return true
    }

    override fun onQueryTextChange(query: String?): Boolean {
        if (!query.isNullOrEmpty()) {
            val list = songViewModel.songList.value!!
                .filter { it.title.lowercase().contains(query.lowercase().trim())
                        || it.album.lowercase() == query.lowercase().trim()
                        || it.artist.lowercase() == query.lowercase().trim()
            }
            songAdapter.songList = list
        } else {
            songAdapter.songList = songViewModel.songList.value!!
        }
        return true
    }
}

