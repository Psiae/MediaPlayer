package com.example.mediaplayer.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.FragmentHomeBinding
import com.example.mediaplayer.model.data.entities.Album
import com.example.mediaplayer.model.data.entities.Artist
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.util.Constants.NOTIFY_CHILDREN
import com.example.mediaplayer.util.Constants.UPDATE_SONG
import com.example.mediaplayer.util.VersionHelper
import com.example.mediaplayer.view.adapter.AlbumAdapter
import com.example.mediaplayer.view.adapter.ArtistAdapter
import com.example.mediaplayer.view.adapter.HomeAdapter
import com.example.mediaplayer.viewmodel.SongViewModel
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class HomeFragment : Fragment() {

    companion object {
        val TAG: String? = HomeFragment::class.java.simpleName // Not nullable but Complains
    }

    @Inject
    lateinit var player: ExoPlayer

    @Inject
    @Named("homeAdapterNS")
    lateinit var suggestAdapter: HomeAdapter

    @Inject
    @Named("albumAdapterNS")
    lateinit var albumAdapter: AlbumAdapter

    @Inject
    @Named("artistAdapterNS")
    lateinit var artistAdapter: ArtistAdapter

    private lateinit var suggestListener: AsyncListDiffer.ListListener<Song>
    private lateinit var artistListener: AsyncListDiffer.ListListener<Artist>
    private lateinit var albumListener: AsyncListDiffer.ListListener<Album>

    private lateinit var navController: NavController
    private val songViewModel: SongViewModel by activityViewModels()

    private var _binding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding
        get() = _binding!!

    var observedSongList = listOf<Song>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        Timber.d("HomeFragment Inflated")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObserver()
        setupView()
        navController = requireActivity().findNavController(R.id.navHostContainer)
        enterTransition = MaterialFadeThrough().addTarget(view as ViewGroup)
        exitTransition = MaterialFadeThrough().addTarget(view).also {
            it.duration = 200L
        }
        reenterTransition = MaterialFadeThrough().addTarget(view).also {
            it.duration = 600L
        }
        lifecycleScope.launch {
            setupRecyclerView()
        }
    }

    private fun setupView() {
        binding.apply {
            tvWelcome.text = getTimeMsg()
        }
    }

    private fun setupRecyclerView() {
        binding.apply {

            suggestListener = AsyncListDiffer.ListListener { cur , prev ->
                if (cur == prev) Unit else rvSuggestion.scrollToPosition(0)
            }
            albumListener = AsyncListDiffer.ListListener { cur, prev ->
                if (cur == prev) Unit else rvAlbum.scrollToPosition(0)
            }
            artistListener = AsyncListDiffer.ListListener { cur, prev ->
                if (cur == prev) Unit else rvArtist.scrollToPosition(0)
            }

            rvSuggestion.apply {
                adapter = suggestAdapter.also {
                    it.setItemClickListener { song ->
                        val mediaItems = songViewModel.currentlyPlayingSongListObservedByMainActivity
                        if (!mediaItems.contains(song)) {
                            songViewModel.sendCommand(NOTIFY_CHILDREN, null, null, "", observedSongList).also {
                                lifecycleScope.launch {
                                    delay(1000)
                                    songViewModel.playOrToggle(song)
                                }
                            }
                        } else songViewModel.playOrToggle(song)
                    }
                    it.differ.addListListener(suggestListener)
                }
                layoutManager = LinearLayoutManager(requireContext()).also {
                    it.orientation = LinearLayoutManager.HORIZONTAL
                }
            }
            rvAlbum.apply {
                adapter = albumAdapter.also {
                    it.differ.addListListener(albumListener)
                    it.setItemClickListener { album ->
                        if (observedSongList.isNullOrEmpty()) {
                            Timber.d("observedSongList isNullOrEmpty")
                            songViewModel.updateSongList()
                        } else {
                            songViewModel.sendCommand(NOTIFY_CHILDREN, null, null, album.name, observedSongList)
                            Timber.d("${album.name}")
                        }
                    }
                }
                layoutManager = LinearLayoutManager(requireContext()).also {
                    it.orientation = LinearLayoutManager.HORIZONTAL
                }
            }
            rvArtist.apply {
                adapter = artistAdapter.also {
                    it.differ.addListListener(artistListener)
                    it.setItemClickListener { artist ->
                        if (observedSongList.isNullOrEmpty()) {
                            Timber.d("songList is NullOrEmpty")
                            songViewModel.updateSongList()
                        } else {
                            lifecycleScope.launch {
                                delay(150)
                                songViewModel.sendCommand(NOTIFY_CHILDREN, null, null, artist.name, observedSongList)
                            }
                            Timber.d("${artist.name}")
                        }
                    }
                }
                layoutManager = LinearLayoutManager(requireContext()).also {
                    it.orientation = LinearLayoutManager.HORIZONTAL

                }
            }
        }
    }

    private fun setupObserver() {
        songViewModel.apply {
            songList.observe(viewLifecycleOwner) {
                Timber.d("songListObserve ${it.size}")
                observedSongList = it
                checkShuffle(it, "HomeFragment Observer")
            }
            shuffles.observe(viewLifecycleOwner) {
                suggestAdapter.itemList = it
            }
            artistList.observe(viewLifecycleOwner) {
                artistAdapter.itemList = it
            }
            albumList.observe(viewLifecycleOwner) {
                albumAdapter.itemList = it
            }
            navHeight.observe(viewLifecycleOwner) {
                binding.nsvHome.setPadding(0,0,0, it + 30)
                binding.nsvHome.clipToPadding = false
                Timber.d("${binding.nsvHome} $it")
            }
        }
    }

    private fun getTimeMsg(): String {
        val c = if (VersionHelper.isNougat()) {
            Calendar.getInstance(Locale.getDefault())
        } else Calendar.getInstance()
        return when (c.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning"
            in 12..15 -> "Good Afternoon"
            in 16..23 -> "Good Evening"
            else -> "Hello"
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch() {
            songViewModel.updateMusicDB()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        with (binding) {
            this.apply {
                rvArtist.adapter = null
                rvAlbum.adapter = null
                rvSuggestion.adapter = null
                artistAdapter.differ.removeListListener(artistListener)
                albumAdapter.differ.removeListListener(albumListener)
                suggestAdapter.differ.removeListListener(suggestListener)
            }
        }
        _binding = null
        if (_binding == null) Timber.d("HomeFragment Destroyed")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (_binding == null) Timber.d("HomeFragment Destroyed")
    }

}