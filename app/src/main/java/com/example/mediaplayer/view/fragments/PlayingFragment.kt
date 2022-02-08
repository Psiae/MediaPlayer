package com.example.mediaplayer.view.fragments

import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.findNavController
import androidx.palette.graphics.Palette
import androidx.transition.Slide
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.ImageViewTarget
import com.bumptech.glide.request.transition.Transition
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

    @Inject
    lateinit var glide: RequestManager

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
            delay(200)
            if (playingPos > 0) {
                binding.sbPlaying.progress = playingPos
            } else {
                Timber.d("MtvSet 0, $playingPos")
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

            lifecycleScope.launch {
                delay(50)
                curPlaying.observe(viewLifecycleOwner) { song ->
                    val itemIndex = playingAdapter.songList.indexOf(song)
                    if (itemIndex != -1) {
                        binding.mtvTitle.text = song.title
                        binding.mtvSubtitle.text = song.artist
                        val curIndex = binding.vpPlaying.currentItem
                        binding.vpPlaying.setCurrentItem(itemIndex,
                            (curIndex < itemIndex + 2 && curIndex > itemIndex - 2))
                        if (false) glidePalette(song.imageUri)
                        Timber.d("itemIndex = $itemIndex")
                    }
                }
            }

            playbackState.observe(viewLifecycleOwner) {
                binding.ibPlayPause.setImageResource(
                    if (it?.isPlaying == true) R.drawable.ic_pause_30_widget
                    else R.drawable.ic_play_30_widget
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

    private fun glideBackground(uri: String) {
        glide.asDrawable().load(uri).into(object : CustomTarget<Drawable>() {
            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                binding.nsvHome.background = resource.also { it.alpha = 150 }
            }
            override fun onLoadCleared(placeholder: Drawable?) = Unit
        })
    }

    private fun glidePalette(uri: String) {
        Timber.d("uri: $uri")
        glide.asBitmap()
            .load(uri)
            .into(object : CustomTarget<Bitmap>() {
            @RequiresApi(Build.VERSION_CODES.N)
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                Palette.from(resource).maximumColorCount(1).generate {
                    it?.let {
                        var default = requireContext().getColor(R.color.widgetBackground)
                        var dominant = it.getDominantColor(default)
                        var vibrant = it.getVibrantColor(dominant)
                        var muted = it.getMutedColor(vibrant)
                        var lightVibrant = it.getLightVibrantColor(muted)
                        var lightMuted = it.getLightMutedColor(lightVibrant)
                        var darkVibrant =  it.getDarkVibrantColor(lightMuted)
                        var darkMuted = it.getDarkMutedColor(darkVibrant)
                        var grad = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(darkMuted, darkMuted, darkMuted)
                        )
                        if (grad.colors!!.first() == default) {
                            Timber.d("grad == default")
                            Palette.from(resource).maximumColorCount(10).generate {
                                it?.let {
                                    default = requireContext().getColor(R.color.widgetBackground)
                                    dominant = it.getDominantColor(default)
                                    vibrant = it.getVibrantColor(dominant)
                                    muted = it.getMutedColor(vibrant)
                                    lightVibrant = it.getLightVibrantColor(muted)
                                    lightMuted = it.getLightMutedColor(lightVibrant)
                                    darkVibrant =  it.getDarkVibrantColor(lightMuted)
                                    darkMuted = it.getDarkMutedColor(darkVibrant)
                                    grad = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                                        intArrayOf(darkMuted, darkMuted)
                                    )
                                    binding.crlHome.background = grad
                                    binding.nsvHome.background = grad
                                    binding.ibPlayPause.backgroundTintList = ColorStateList.valueOf(darkMuted)
                                    requireActivity().window.navigationBarColor = grad.colors!!.last()
                                    requireActivity().window.statusBarColor = grad.colors!!.first()
                                    binding.tbLib.setBackgroundColor(grad.colors!!.first())
                                }
                            }
                        } else {
                            binding.crlHome.background = grad
                            binding.nsvHome.background = grad
                            binding.ibPlayPause.backgroundTintList = ColorStateList.valueOf(darkMuted)
                            requireActivity().window.navigationBarColor = grad.colors!!.last()
                            requireActivity().window.statusBarColor = grad.colors!!.first()
                            binding.tbLib.setBackgroundColor(grad.colors!!.first())
                        }

                    }
                }

            }
                override fun onLoadCleared(placeholder: Drawable?) = Unit
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    val default = requireContext().getColor(R.color.widgetBackground)
                    binding.crlHome.background = null
                    binding.nsvHome.background = null
                    binding.ibPlayPause.backgroundTintList = ColorStateList.valueOf(requireContext().getColor(R.color.widgetColor))
                    requireActivity().window.navigationBarColor = default
                    requireActivity().window.statusBarColor = default
                    binding.tbLib.setBackgroundColor(default)
                    return
                }
            })
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

            binding.ibPlayPause.backgroundTintList = ColorStateList.valueOf(requireContext().getColor(R.color.widgetBackground))

            ibPlayPause.setOnClickListener {
                with(songViewModel) {
                    curPlaying.value?.let {
                        if (curPlaying.value?.mediaId ?: 0 != currentlyPlaying.value?.mediaId ?: 1) {
                            playOrToggle(it, true).also { seekTo(sbPlaying.progress.toLong()) }
                            return@setOnClickListener
                        }
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

            ibPrev.setOnClickListener {
                if (songViewModel.checkQueue().isEmpty()) {
                    songViewModel.playOrToggle(playingAdapter.songList[vpPlaying.currentItem - 1])
                    return@setOnClickListener
                }
                songViewModel.skipPrev()
            }
            ibNext.setOnClickListener {
                if (songViewModel.checkQueue().isEmpty()) {
                    songViewModel.playOrToggle(playingAdapter.songList[vpPlaying.currentItem + 1])
                    return@setOnClickListener
                }
                songViewModel.skipNext()
            }
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

    fun makePalette(bitmap: Bitmap) {
    }


    override fun onDestroyView() {
        super.onDestroyView()
    }

    override fun onDestroy() {
        Timber.d("PlayingFragment Destroyed")
        super.onDestroy()
    }
}