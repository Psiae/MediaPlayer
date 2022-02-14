package com.example.mediaplayer.view.fragments

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Property
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ListView
import android.widget.SeekBar
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import androidx.transition.Slide
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.FragmentPlayingBinding
import com.example.mediaplayer.exoplayer.MusicService
import com.example.mediaplayer.exoplayer.isPlaying
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.util.Constants.FILTER_MODE_BACKGROUND
import com.example.mediaplayer.util.Constants.FILTER_MODE_BLUR
import com.example.mediaplayer.util.Constants.FILTER_MODE_NONE
import com.example.mediaplayer.util.Constants.FILTER_MODE_PALETTE
import com.example.mediaplayer.util.Constants.REPEAT_MODE_ALL_INT
import com.example.mediaplayer.util.Constants.REPEAT_MODE_OFF_INT
import com.example.mediaplayer.util.Constants.REPEAT_MODE_ONE_INT
import com.example.mediaplayer.util.ext.toast
import com.example.mediaplayer.view.adapter.PlayingAdapter
import com.example.mediaplayer.viewmodel.SongViewModel
import com.google.android.exoplayer2.Player
import dagger.hilt.android.AndroidEntryPoint
import jp.wasabeef.glide.transformations.BlurTransformation
import jp.wasabeef.glide.transformations.gpu.VignetteFilterTransformation
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

    lateinit var sbAnim: ObjectAnimator
    val sbAnimListener = object : Animator.AnimatorListener {
        override fun onAnimationStart(animation: Animator?) {
            Timber.d("animationStart")
            updateSeekbar = false
        }
        override fun onAnimationEnd(animation: Animator?) {
            updateSeekbar = true
        }
        override fun onAnimationCancel(animation: Animator?) {

        }
        override fun onAnimationRepeat(animation: Animator?) = Unit
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
    var lastIndex = -1
    var selectedSong: Song = Song()

    private val pagerCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {

            Timber.d("changeCallback pos: $position")

            if (lastIndex == position) return
            lastIndex = position

            if (init) {
                lifecycleScope.launch {
                    delay(250)
                    init = false
                }
                Timber.d("init Block")
                return
            }

            sbAnim = ObjectAnimator.ofInt(binding.sbPlaying, Property.of(SeekBar::class.java, Int::class.java, "progress"), 0).also {
                it.duration = 500
                it.interpolator = DecelerateInterpolator()
                it.addListener(sbAnimListener)
            }

            if (this@PlayingFragment::sbAnim.isInitialized) {
                sbAnim.setAutoCancel(true)
                sbAnim.start()
            }

            super.onPageSelected(position)
            Timber.d("passed pos: $position")

            with(songViewModel) {
                try {
                    playOrToggle(playingAdapter.songList[position], false, "PlayingFragment PagerCallback")
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
        }
    }

    var updateSeekbar = true
    var playingPos = -1

    private fun setupRecyclerView() {

        binding.apply {

            with(vpPlaying) {
                this.adapter = playingAdapter.also { it ->
                    unregisterOnPageChangeCallback(pagerCallback)
                    registerOnPageChangeCallback(pagerCallback)
                    it.setClickListener { _ ->
                        with(songViewModel) {
                            when (modeFilter) {
                                FILTER_MODE_NONE -> filterMode.value = FILTER_MODE_BACKGROUND
                                FILTER_MODE_BACKGROUND -> filterMode.value = FILTER_MODE_PALETTE
                                FILTER_MODE_PALETTE -> filterMode.value = FILTER_MODE_BLUR
                                FILTER_MODE_BLUR -> filterMode.value = FILTER_MODE_NONE
                            }
                        }
                    }
                }
            }
        }
    }

    var enableSeekbar = true
    var modeFilter = FILTER_MODE_NONE

    private fun setupObserver() {

        Timber.d("PlayingFragment setupObserver")

        lifecycleScope.launch {
            delay(300)
            if (playingPos > -1) {
                binding.sbPlaying.progress = playingPos
            } else {
                Timber.d("MtvSet 0, $playingPos")
                setMtvCurrent(0)
                try {
                    setMtvDuration(playingAdapter.songList[binding.vpPlaying.currentItem].length)
                } catch (e: Exception) {
                    if (e is IndexOutOfBoundsException) setMtvDuration(playingAdapter.songList[1].length)
                }
            }
        }

        with(songViewModel) {
            mediaItemSong.observe(viewLifecycleOwner) {
                playingAdapter.songList = it
            }

            curPlayerPosition.observe(viewLifecycleOwner) { pos ->
                if (updateSeekbar
                    && MusicService.curSongMediaId
                    == curPlaying.value?.mediaId) {
                    binding.sbPlaying.progress = pos.toInt()
                    playingPos = pos.toInt()
                    setMtvCurrent(pos)
                }
            }

            curSongDuration.observe(viewLifecycleOwner) { dur ->
                Timber.d("curSongDuration $dur")
                binding.sbPlaying.max = dur.toInt()
                setMtvDuration(dur)
            }

            var lastSong = Song()

            lifecycleScope.launch {
                delay(50)
                curPlaying.observe(viewLifecycleOwner) { song ->
                    selectedSong = song
                    if (lastSong == song) return@observe
                    lastSong = song

                    val filtered = when (val itemIndex = playingAdapter.songList.indexOf(song)) {
                        0 -> playingAdapter.songList.size - 2
                        playingAdapter.songList.lastIndex -> { 1 }
                        else -> itemIndex
                    }

                    Timber.d("curPlaying $filtered ${song.queue}")

                    if (filtered != -1) {
                        with(binding) {
                            mtvTitle.text = song.title
                            mtvSubtitle.text = song.artist
                            val curIndex = vpPlaying.currentItem
                            vpPlaying.setCurrentItem(filtered,
                                (curIndex < filtered + 2 && curIndex > filtered - 2 &&
                                        !(curIndex == 0 || curIndex == playingAdapter.songList.lastIndex))
                            )
                        }

                        glideBackground(song.imageUri)
                        Timber.d("itemIndex = $filtered")
                    }
                }
            }

            playbackState.observe(viewLifecycleOwner) {
                binding.ibPlayPause.setImageResource(
                    if (it?.isPlaying == true) R.drawable.ic_pause_30_widget
                    else R.drawable.ic_play_30_widget
                )
            }

            filterMode.observe(viewLifecycleOwner) {
                modeFilter = it
                /*binding.tbLib.menu.getItem(0).setIcon(when (it) {
                    FILTER_MODE_BACKGROUND -> R.drawable.ic_baseline_filter_image_24
                    FILTER_MODE_PALETTE -> R.drawable.ic_baseline_filter_1_24
                    FILTER_MODE_BLUR -> R.drawable.ic_baseline_filter_2_24
                    else -> R.drawable.ic_baseline_filter_none_24
                })*/
                glideBackground(selectedSong.imageUri)
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
        when (songViewModel.filterMode.value) {
            FILTER_MODE_NONE -> defaultBackground()
            FILTER_MODE_BACKGROUND -> glideVignette(uri)
            FILTER_MODE_BLUR -> glideBlur(uri)
            FILTER_MODE_PALETTE -> glidePalette(uri)
        }
    }

    private fun defaultBackground() {
        val default = requireContext().getColor(R.color.widgetBackgroundD)
        with(binding) {
            crlHome.background = null
            nsvHome.background = null
            imageView2.setImageResource(0)
            binding.ibPlayPause.backgroundTintList =
                ColorStateList.valueOf(requireContext().getColor(R.color.white))
            activity?.window?.navigationBarColor = default
            activity?.window?.statusBarColor = default
            binding.tbLib.setBackgroundColor(default)
        }
    }

    private fun glideVignette(uri: String) {
        Timber.d("uri: $uri")
        val default = requireContext().getColor(R.color.widgetBackgroundD)
        val white = requireContext().getColor(R.color.white)

        lifecycleScope.launch {
            val multi = if (default == white) MultiTransformation(
                CenterCrop()
            ) else MultiTransformation(
                VignetteFilterTransformation(),
                CenterCrop()
            )
            if (default == white) {
                binding.imageView2.setColorFilter(requireContext().getColor(R.color.transparent))
                binding.imageView2.alpha = 0.1f
            } else {
                binding.imageView2.setColorFilter(requireContext().getColor(R.color.bitDarker))
                binding.imageView2.alpha = 1f
            }


            glide.asBitmap()
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .apply(RequestOptions.bitmapTransform(multi))
                .transition(BitmapTransitionOptions.withCrossFade())
                .into(binding.imageView2)
        }
        binding.nsvHome.background = null
        binding.tbLib.background = null
        binding.ibPlayPause.backgroundTintList =
            ColorStateList.valueOf(requireContext().getColor(R.color.white))
        binding.ibPlayPause.rippleColor = requireContext().getColor(R.color.ibRipple)
        requireActivity().window.statusBarColor = requireContext().getColor(R.color.widgetBackgroundMax)
        requireActivity().window.navigationBarColor = requireContext().getColor(R.color.widgetBackgroundMax)
    }

    private fun glideBlur(uri: String) {
        Timber.d("uri: $uri")
        lifecycleScope.launch {
            val default = requireContext().getColor(R.color.widgetBackgroundD)
            val white = requireContext().getColor(R.color.white)
            val multi = if (default == white) MultiTransformation(
                BlurTransformation(25, 5),
                CenterCrop()
            ) else MultiTransformation(
                BlurTransformation(25, 5),
                CenterCrop()
            )
            if (default == white) {
                binding.imageView2.setColorFilter(requireContext().getColor(R.color.transparent))
                binding.imageView2.alpha = 0.25f
            } else {
                binding.imageView2.setColorFilter(requireContext().getColor(R.color.bitDarker))
                binding.imageView2.alpha = 1f
            }
            glide.asBitmap()
                .load(uri)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .apply(RequestOptions.bitmapTransform(multi))
                .transition(BitmapTransitionOptions.withCrossFade())
                .into(binding.imageView2)
        }
        binding.crlHome.background = null
        binding.nsvHome.background = null
        binding.ibPlayPause.backgroundTintList =
            ColorStateList.valueOf(requireContext().getColor(R.color.white))
        binding.ibPlayPause.rippleColor = requireContext().getColor(R.color.ibRipple)
        requireActivity().window.navigationBarColor = requireContext().getColor(R.color.widgetBackgroundMax)


    }

    private fun glidePalette(uri: String) {
        Timber.d("uri: $uri")
        lifecycleScope.launch  {
            glide.asBitmap()
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .load(uri)
                .into(object : CustomTarget<Bitmap>() {
                    @RequiresApi(Build.VERSION_CODES.N)
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {
                        Palette.from(resource).maximumColorCount(4).generate {
                            it?.let {
                                val default = requireContext().getColor(R.color.widgetBackgroundD)
                                val white = requireContext().getColor(R.color.white)
                                var dark = requireContext().getColor(R.color.darkGrey)

                                val grad = if (default == white) {

                                    val dominant = it.getDominantColor(default)
                                    val vibrant = it.getVibrantColor(dominant)
                                    val muted = it.getMutedColor(vibrant)
                                    val lightVibrant = it.getLightVibrantColor(muted)
                                    val lightMuted = it.getLightMutedColor(lightVibrant)

                                    val palettes = lightMuted
                                    val lightArray = intArrayOf(
                                        palettes, palettes, palettes, palettes, palettes,
                                        palettes, palettes, palettes, palettes, palettes
                                    )
                                    binding.ibPlayPause.setRippleColor(ColorStateList.valueOf(palettes))

                                    GradientDrawable(
                                        GradientDrawable.Orientation.TOP_BOTTOM, lightArray
                                    )

                                } else {

                                    val vibrant = it.getVibrantColor(default)
                                    val dominant = it.getDominantColor(vibrant)
                                    val muted = it.getMutedColor(dominant)
                                    val darkMuted = it.getDarkMutedColor(muted)

                                    val palettes = dominant
                                    val darkArray = intArrayOf(
                                        darkMuted, darkMuted, darkMuted, palettes, palettes,
                                        palettes, palettes, palettes, palettes, palettes
                                    )
                                    binding.ibPlayPause.setRippleColor(ColorStateList.valueOf(palettes))

                                    GradientDrawable(
                                        GradientDrawable.Orientation.TOP_BOTTOM, darkArray
                                    )
                                }

                                binding.nsvHome.background = grad
                                binding.crlHome.background = grad

                                binding.ibPlayPause.backgroundTintList =
                                    ColorStateList.valueOf(requireContext().getColor(R.color.white))
                                activity?.window?.navigationBarColor = grad.colors!!.last()
                                activity?.window?.statusBarColor = default
                                binding.tbLib.setBackgroundColor(default)
                            }
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) = Unit
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        if (selectedSong.imageUri != uri) return
                        val default = requireContext().getColor(R.color.widgetBackground)
                        binding.crlHome.background = null
                        binding.nsvHome.background = null
                        binding.ibPlayPause.backgroundTintList =
                            ColorStateList.valueOf(requireContext().getColor(R.color.widgetColor))
                        activity?.window?.navigationBarColor = default
                        activity?.window?.statusBarColor = default
                        binding.tbLib.setBackgroundColor(default)
                        return
                    }
                })
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

            binding.ibPlayPause.backgroundTintList = ColorStateList.valueOf(requireContext().getColor(R.color.widgetBackground))

            ibPlayPause.setOnClickListener {
                with(songViewModel) {
                    curPlaying.value?.let {
                        if (curPlaying.value?.mediaId ?: 0 != currentlyPlaying.value?.mediaId ?: 1) {
                            playOrToggle(it, true, "PlayingFragment curPlaying Observer").also { seekTo(sbPlaying.progress.toLong()) }
                            return@setOnClickListener
                        }
                        playOrToggle(it, true, "PlayingFragment playpause")
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
                    if (this@PlayingFragment::sbAnim.isInitialized) sbAnim.cancel()
                    updateSeekbar = false
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    seekBar?.let {
                        songViewModel.seekTo(it.progress.toDouble() / it.max.toDouble())
                        updateSeekbar = true
                    }
                }
            })

            ibPrev.setOnClickListener {
                if (songViewModel.checkQueue().isEmpty()) {
                    songViewModel.playOrToggle(playingAdapter.songList[vpPlaying.currentItem - 1], false, "ibPrev no queue")
                    return@setOnClickListener
                }
                songViewModel.skipPrev()
            }
            ibNext.setOnClickListener {
                if (songViewModel.checkQueue().isEmpty()) {
                    songViewModel.playOrToggle(playingAdapter.songList[vpPlaying.currentItem + 1], false, "ibPrev no queue")
                    return@setOnClickListener
                }
                songViewModel.skipNext()
            }
            ibFavorite.setOnClickListener { toast(requireContext(), "Coming Soon") }

            tbLib.apply {
                setNavigationOnClickListener {
                    findNavController().popBackStack()
                }
                setOnMenuItemClickListener { menu ->
                    when (menu.itemId) {
                        R.id.menu_filter -> {
                            with(songViewModel) {
                                when (modeFilter) {
                                    FILTER_MODE_NONE -> filterMode.value = FILTER_MODE_BACKGROUND
                                    FILTER_MODE_BACKGROUND -> filterMode.value = FILTER_MODE_PALETTE
                                    FILTER_MODE_PALETTE -> filterMode.value = FILTER_MODE_BLUR
                                    FILTER_MODE_BLUR -> filterMode.value = FILTER_MODE_NONE
                                }
                                true
                            }
                        }
                        else -> {
                            toast(requireContext(), "Coming Soon!")
                            false
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        Timber.d("PlayingFragment Destroyed")
        super.onDestroy()
    }
}