package com.example.mediaplayer.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.FragmentSongBinding
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.util.Constants.FADETHROUGH_IN_DURATION
import com.example.mediaplayer.util.Constants.FADETHROUGH_OUT_DURATION
import com.example.mediaplayer.util.VersionHelper
import com.example.mediaplayer.util.ext.toast
import com.example.mediaplayer.view.adapter.SongAdapter
import com.example.mediaplayer.viewmodel.SongViewModel
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named


@AndroidEntryPoint
class SongFragment : Fragment(), SearchView.OnQueryTextListener {

    companion object {
        val TAG: String = SongFragment::class.java.simpleName
    }

    @Inject
    @Named("songAdapterNS")
    lateinit var songAdapter: SongAdapter

    private val songViewModel: SongViewModel by activityViewModels()
    private lateinit var navController: NavController
    private lateinit var songListener: AsyncListDiffer.ListListener<Song>

    private var _binding: FragmentSongBinding? = null
    private val binding: FragmentSongBinding
        get() = _binding!!

    var observedSongList = listOf<Song>()

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
        super.onViewCreated(view, savedInstanceState)
        observeSongList()
        setupView()
        setupRecyclerView()
        navController = requireActivity().findNavController(R.id.navHostContainer)
        enterTransition = MaterialFadeThrough().addTarget(view as ViewGroup).also {
            it.duration = FADETHROUGH_IN_DURATION
        }
        exitTransition = MaterialFadeThrough().addTarget(view).also {
            it.duration = FADETHROUGH_OUT_DURATION
        }

    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch() {
            songViewModel.updateMusicDB()
        }
    }

    private fun observeSongList() {

        songViewModel.apply {

            navHeight.observe(viewLifecycleOwner) {
                binding.rvSongList.setPadding(0, 0, 0, it)
                Timber.d("${binding.rvSongList.paddingBottom} $it")
            }
            songList.observe(viewLifecycleOwner) {
                songAdapter.songList = it
                if (it.isNullOrEmpty() && VersionHelper.isQ()) {
                    binding.tvNoSong.visibility = View.VISIBLE
                } else binding.tvNoSong.visibility = View.GONE
            }
        }
    }

    private fun setupView() {
        binding.apply {
            songToolbar.apply {
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
                                /*navController.navigate(R.id.navBottomSettings)*/
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
        songListener = AsyncListDiffer.ListListener { prev, cur ->
            when (prev) {
                cur -> Unit
                else -> {
                    binding.rvSongList.scrollToPosition(0)
                }
            }
        }
        songAdapter.differ.addListListener(songListener)

    }

    private fun setupRecyclerView () {
        binding.rvSongList.apply {
            songAdapter.setItemClickListener { song ->
                songViewModel.requestPlay(song, songAdapter.songList, " SongFragment")
            }
            adapter = songAdapter
            layoutManager = GridLayoutManager(requireContext(),
                1, GridLayoutManager.VERTICAL,
                false
            )
        }
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        return false
    }

    override fun onQueryTextChange(query: String?): Boolean {
        if (!query.isNullOrEmpty()) {
            val list = songViewModel.songList.value!!
                .filter {
                    it.title.lowercase().contains(query.lowercase().trim())
                        || it.album.lowercase() == query.lowercase().trim()
                        || it.artist.lowercase() == query.lowercase().trim()
                }
            songAdapter.songList = list
        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                if (query.isNullOrEmpty()) {
                    songAdapter.songList = songViewModel.songList.value!!
                }
            }
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        with (binding) {
            this.apply {
                songToolbar.menu.clear()
                rvSongList.adapter = null
            }
        }
        _binding = null
    }
}

