package com.example.mediaplayer.view.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.transition.Slide
import androidx.viewpager2.widget.ViewPager2
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.FragmentPlayingBinding
import com.example.mediaplayer.exoplayer.isPlaying
import com.example.mediaplayer.util.Constants.REPEAT_MODE_ALL_INT
import com.example.mediaplayer.util.Constants.REPEAT_MODE_OFF_INT
import com.example.mediaplayer.util.Constants.REPEAT_MODE_ONE_INT
import com.example.mediaplayer.util.ext.toast
import com.example.mediaplayer.view.activity.MainActivity
import com.example.mediaplayer.view.adapter.PlayingAdapter
import com.example.mediaplayer.viewmodel.SongViewModel
import com.google.android.exoplayer2.Player
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
class PlayingFragment: Fragment() {

    @Inject
    @Named("playingAdapterNS")
    lateinit var playingAdapter: PlayingAdapter

    private val songViewModel: SongViewModel by activityViewModels()

    private var _binding: FragmentPlayingBinding? = null
    private val binding: FragmentPlayingBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPlayingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setupRecyclerView()
        setupObserver()

        enterTransition = Slide()
        returnTransition = Slide()
    }

    var init = true

    private val pagerCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)

            Timber.d("changeCallback pos: $position")
            if (init ) {
                lifecycleScope.launch {
                    delay(500)
                    init = false
                }
                return
            }

            with(songViewModel) {

                try {
                    playOrToggle(playingAdapter.songList[position])
                } catch (e: Exception) {
                    Timber.e(e)
                }


                /*if (playbackState.value?.isPlaying == true) {
                    Timber.d("changed to $position")
                    try {
                        playOrToggle(playingAdapter.songList[position])
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                } else {
                    Timber.d("pos $position")
                    try {
                        curPlaying.value = playingAdapter.songList[position]
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }*/
            }
        }
    }

    var updateSeekbar = true
    var playingPos = 0

    private fun setupRecyclerView() {

        binding.apply {

            with(vpPlaying) {
                this.adapter = playingAdapter.also { it ->
                    unregisterOnPageChangeCallback(pagerCallback)
                    registerOnPageChangeCallback(pagerCallback)
                    it.setClickListener { song ->
                        toast(requireContext(), song.title)
                    }
                }
            }
        }
    }

    private fun setupObserver() {

        Timber.d("PlayingFragment setupObserver")

        lifecycleScope.launch {
            delay(500)
            if (playingPos > 0 && songViewModel.curPlaying.value?.length == songViewModel.curSongDuration.value) {
                binding.sbPlaying.progress = playingPos
            } else {
                setMtvCurrent(0)
                try {
                    setMtvDuration(playingAdapter.songList[binding.vpPlaying.currentItem].length)
                } catch (e: Exception) {
                    if (e is IndexOutOfBoundsException) setMtvDuration(playingAdapter.songList[0].length)
                }
            }
        }

        with(songViewModel) {
            mediaItemSong.observe(viewLifecycleOwner) {
                playingAdapter.songList = it
            }

            curPlayerPosition.observe(viewLifecycleOwner) { pos ->
                if (updateSeekbar) {
                    binding.sbPlaying.progress = pos.toInt()
                    playingPos = pos.toInt()
                    setMtvCurrent(pos)
                }
            }

            curSongDuration.observe(viewLifecycleOwner) { dur ->
                binding.sbPlaying.max = dur.toInt()
                setMtvDuration(dur)
            }

            curPlaying.observe(viewLifecycleOwner) { song ->
                val itemIndex = playingAdapter.songList.indexOf(song)
                if (itemIndex != -1) {
                    binding.mtvTitle.text = song.title
                    binding.mtvSubtitle.text = song.artist
                    lifecycleScope.launch {
                        delay(50)
                        val curIndex = binding.vpPlaying.currentItem
                        binding.vpPlaying.setCurrentItem(itemIndex,
                            (true /*curIndex == itemIndex + 1 || curIndex == itemIndex - 1*/))
                    }
                    Timber.d("itemIndex = $itemIndex")
                }
            }

            playbackState.observe(viewLifecycleOwner) {
                binding.ibPlayPause.setImageResource(
                    if (it?.isPlaying == true) R.drawable.ic_pause_30
                    else R.drawable.ic_play_30
                )
            }

            repeatState.observe(viewLifecycleOwner) {
                with(binding.ibRepeatMode) {
                    when (it) {
                        Player.REPEAT_MODE_OFF -> {
                            setImageResource(R.drawable.ic_baseline_repeat_24_trans)
                            setOnClickListener { setRepeatMode(REPEAT_MODE_ONE_INT) }
                        }
                        Player.REPEAT_MODE_ONE -> {
                            setImageResource(R.drawable.ic_baseline_repeat_one_24_widget)
                            setOnClickListener { setRepeatMode(REPEAT_MODE_ALL_INT) }
                        }
                        Player.REPEAT_MODE_ALL -> {
                            setImageResource(R.drawable.ic_baseline_repeat_24)
                            setOnClickListener { setRepeatMode(REPEAT_MODE_OFF_INT) }
                        }
                    }
                }
            }



            lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    updateMusicDB()
                }
            }
        }
    }

    private fun setMtvDuration(ms: Long) {
        val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
        binding.mtvDuration.text = dateFormat.format(ms)
    }

    private fun setMtvCurrent(ms: Long) {
        val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
        binding.mtvCurTime.text = dateFormat.format(ms)
    }

    private fun setupView() {

        binding.apply {

            ibPlayPause.setOnClickListener {
                with(songViewModel) {
                    curPlaying.value?.let {
                        playOrToggle(it, true)
                    }
                }
            }

            sbPlaying.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    if (fromUser) {
                        setMtvCurrent(progress.toLong())
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    updateSeekbar = false
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.let {
                        songViewModel.seekTo(it.progress.toLong())
                        updateSeekbar = true
                    }
                }
            })

            ibPrev.setOnClickListener { songViewModel.skipPrev() }
            ibNext.setOnClickListener { songViewModel.skipNext() }
            ibFavorite.setOnClickListener { toast(requireContext(), "Coming Soon") }


            tbLib.apply {
                setNavigationOnClickListener {
                    findNavController().popBackStack()
                }
            }
        }
    }

    fun hideFragment() {
        val activity = activity as MainActivity
    }




    override fun onDestroy() {
        Timber.d("PlayingFragment Destroyed")
        super.onDestroy()
    }
}