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
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.FragmentHomeBinding
import com.example.mediaplayer.util.VersionHelper
import com.example.mediaplayer.view.adapter.AlbumAdapter
import com.example.mediaplayer.view.adapter.ArtistAdapter
import com.example.mediaplayer.view.adapter.HomeAdapter
import com.example.mediaplayer.viewmodel.SongViewModel
import com.google.android.exoplayer2.ExoPlayer
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
        val TAG = HomeFragment::class.java.simpleName
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

    lateinit var navController: NavController
    val songViewModel: SongViewModel by activityViewModels()

    private var _binding: FragmentHomeBinding? = null
    private val binding: FragmentHomeBinding
        get() = _binding!!

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
        navController = requireActivity().findNavController(R.id.navHostContainer)
        setupView()
        lifecycleScope.launch {
            delay(100)
            setupRecyclerView()
        }
        setupObserver()
    }

    private fun setupView() {
        binding.apply {
            tvWelcome.text = getTimeMsg()
        }
    }

    private fun setupRecyclerView() {
        binding.apply {
            rvSuggestion.apply {
                adapter = suggestAdapter.also {
                    it.differ.addListListener { _, _ -> rvSuggestion.scrollToPosition(0) }
                }
                layoutManager = LinearLayoutManager(requireContext()).also {
                    it.orientation = LinearLayoutManager.HORIZONTAL
                }
            }
            rvAlbum.apply {
                adapter = albumAdapter.also {
                    it.differ.addListListener { _, _ -> rvSuggestion.scrollToPosition(0) }
                }
                layoutManager = LinearLayoutManager(requireContext()).also {
                    it.orientation = LinearLayoutManager.HORIZONTAL
                }
            }
            rvArtist.apply {
                adapter = artistAdapter.also {
                    it.differ.addListListener { _, _ -> rvSuggestion.scrollToPosition(0) }
                }
                layoutManager = LinearLayoutManager(requireContext()).also {
                    it.orientation = LinearLayoutManager.HORIZONTAL
                }
            }
        }
    }

    private fun setupObserver() {
        songViewModel.apply {
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
        lifecycleScope.launch(Dispatchers.IO) {
            delay(100)
            songViewModel.getDeviceSong("HomeFragment")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
        if (_binding == null) Timber.d("HomeFragment Destroyed")
    }

}