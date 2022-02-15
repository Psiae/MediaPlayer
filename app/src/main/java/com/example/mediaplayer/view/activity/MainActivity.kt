package com.example.mediaplayer.view.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.palette.graphics.Palette
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.ActivityMainBinding
import com.example.mediaplayer.exoplayer.service.MusicService
import com.example.mediaplayer.exoplayer.isPlaying
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.util.Constants.DEFAULT_SCREEN
import com.example.mediaplayer.util.Constants.FOREGROUND_SERVICE
import com.example.mediaplayer.util.Constants.FULL_SCREEN
import com.example.mediaplayer.util.Constants.PAGER_SCREEN
import com.example.mediaplayer.util.Constants.PERMISSION_FOREGROUND_SERVICE_REQUEST_CODE
import com.example.mediaplayer.util.Constants.PERMISSION_WRITE_EXT_REQUEST_CODE
import com.example.mediaplayer.util.Constants.WRITE_STORAGE
import com.example.mediaplayer.util.Perms
import com.example.mediaplayer.util.PermsHelper
import com.example.mediaplayer.util.Version
import com.example.mediaplayer.util.ext.curToast
import com.example.mediaplayer.util.ext.toast
import com.example.mediaplayer.view.adapter.SwipeAdapter
import com.example.mediaplayer.viewmodel.SongViewModel
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("LogNotTimber")
class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    companion object {
        val TAG = MainActivity::class.java.simpleName
    }
    
    @Inject
    lateinit var swipeAdapter: SwipeAdapter

    @Inject
    lateinit var glide: RequestManager

    private lateinit var binding: ActivityMainBinding
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController
    private lateinit var containerFragment: NavHostFragment
    private lateinit var containerController: NavController

    private val songViewModel: SongViewModel by viewModels()

    private var alreadySetup: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MediaPlayer)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        // navController & Destination setup
        setupNavController()

        // Permission check
        checkPermission()

        // View setup
        setupView()

        // toaster Setup
        setToaster()

        // setup ViewModel & Observer
        setupSongVM()

        lifecycleScope.launch {
            delay(1500)
            getControlHeight()
        }
    }

    override fun onPause() {
        super.onPause()
        binding.viewPager2.unregisterOnPageChangeCallback(pagerCallback)
    }

    var suspendedpager = -1

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            delay(300)
            binding.viewPager2.registerOnPageChangeCallback(pagerCallback)
            if (suspendedpager != -1) binding.viewPager2.currentItem = suspendedpager
        }
    }

    /**
     * Navigation & View Setup
     */
    private fun setupNavController() {

        navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostContainer) as NavHostFragment
        navController = navHostFragment.navController

        setDestinationListener(navController)


        /*containerFragment =
            supportFragmentManager.findFragmentById(R.id.globalContainer) as NavHostFragment
        containerController = containerFragment.navController

        supportFragmentManager.beginTransaction().hide(containerFragment).commit()*/
    }

    private fun setDestinationListener(controller: NavController) {
        controller.addOnDestinationChangedListener { _, destination, _ ->
            getControlHeight()
            when (val id = destination.id) {
                R.id.navBottomHome -> setControl(DEFAULT_SCREEN)
                R.id.navBottomSong -> setControl(DEFAULT_SCREEN)
                R.id.navBottomPlaylist -> setControl(DEFAULT_SCREEN)
                R.id.navBottomLibrary -> setControl(DEFAULT_SCREEN)
                R.id.folderFragment -> setControl(PAGER_SCREEN)
                R.id.playingFragment -> setControl(FULL_SCREEN)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupView() {

        with(binding) {

            bottomNavigationView.apply {
                setupWithNavController(navController)
                setOnItemReselectedListener { }
            }

            // curPlaying.value refer to selected swipeAdapter
            ibPlayPause.setOnClickListener {
                with(songViewModel) {
                    curPlaying.value?.let {
                        playOrToggle(it, true, "MainActivity playpause")
                    }
                }
            }

            sbSeekbar.apply { setOnTouchListener { _, _ -> true } }

            sivCurImage.setOnClickListener { toast(this@MainActivity, "Image Clicked") }

            viewPager2.apply {
                adapter = swipeAdapter.also {
                    it.setItemListener { song ->
                        navController.navigate(R.id.playingFragment)
                        if (song.queue != MusicService.lastSongQueue) {
                            songViewModel.playOrToggle(song, false,"MainActivity ViewPager2").also {
                                lifecycleScope.launch {
                                    while (song.queue != MusicService.lastSongQueue
                                        && songViewModel.playbackState.value?.isPlaying == false){
                                        delay(200)
                                        songViewModel.pause()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
/*
    fun showPlayingFragment() {
        setControl(FULL_SCREEN)
        supportFragmentManager.beginTransaction().show(containerFragment).commit()
    }

    fun hidePlayingFragment() {
        setControl(DEFAULT_SCREEN)
        supportFragmentManager.beginTransaction().hide(containerFragment).commit()
    }*/

    fun hideNavHost() {
        supportFragmentManager.beginTransaction().hide(navHostFragment).commit()
    }

    private fun setToaster() = apply { curToast = "" }

    private fun getControlHeight() {
        lifecycleScope.launch {
            delay(100)
            if (getDummyHeight() != 0) songViewModel.navHeight.value = getDummyHeight()
            else songViewModel.navHeight.value = 300
        }
    }

    private fun getDummyHeight(): Int {
        return binding.dummy.measuredHeight
    }

    private fun getHidBnvHeight(): Int {
        return binding.bottomNavigationView.measuredHeight
    }

    private fun getClpHeight(): Int {
        return binding.clPager.measuredHeight
    }

    fun setControl(controlMode: String) {

        when (controlMode) {

            DEFAULT_SCREEN -> binding.apply {
                with(View.VISIBLE) {
                    clPager.visibility = this
                    bottomNavigationView.visibility = this
                }
                with(navHostContainer) { layoutParams.height = 0 }
                window.statusBarColor = resources.getColor(R.color.statusBarColor, this@MainActivity.theme)
                window.navigationBarColor = resources.getColor(R.color.bnvColor, this@MainActivity.theme)
            }

            FULL_SCREEN -> binding.apply {
                with(View.GONE) {
                    bottomNavigationView.visibility = this
                }
                with(View.INVISIBLE) {
                    clPager.visibility = this
                }
                navHostContainer.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }

            PAGER_SCREEN -> binding.apply {
                with(View.GONE) { bottomNavigationView.visibility = this}
                with(View.VISIBLE) { clPager.visibility = this }
                with(getClpHeight()) { if (this != 0) songViewModel.navHeight.value = this }
                with(navHostContainer) { layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT }
                window.statusBarColor = resources.getColor(R.color.statusBarColor, this@MainActivity.theme)
                window.navigationBarColor = resources.getColor(R.color.bnvColor, this@MainActivity.theme)
            }
        }
    }

    private val pagerCallback = object : ViewPager2.OnPageChangeCallback() {


        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)

            Timber.d("changeCallback pos: $position")
        }
    }

    /**
     * ViewModel & Data Provider
     */


    private fun  setupSongVM() {

        songViewModel.apply {
            mediaItemSong.observe(this@MainActivity) { mediaItems ->
                swipeAdapter.songList = mediaItems
                songQueue = mediaItems

                Timber.d("mediaItems updated")

                if (!alreadySetup && !songViewModel.currentlyPlaying.hasActiveObservers()) {
                    alreadySetup = true
                    observePlayer()
                    binding.viewPager2.unregisterOnPageChangeCallback(pagerCallback)
                    binding.viewPager2.registerOnPageChangeCallback(pagerCallback)
                }
            }

            curPlayerPosition.observe(this@MainActivity) {
                binding.sbSeekbar.progress = it.toInt()
            }

            curSongDuration.observe(this@MainActivity) {
                binding.sbSeekbar.max = it.toInt()
            }

            lifecycleScope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    updateMusicDB()
                }
            }
        }
    }

    var lastItemIndex = Pair(Song(), -1)
    var observePlaying = true

    private fun observePlayer() {

        Timber.d("observePlayer")

        with(songViewModel) {
            lifecycleScope.launch {

                currentlyPlaying.observe(this@MainActivity) { song ->
                    if (!observePlaying) return@observe
                    song?.let {
                        if (swipeAdapter.songList.isEmpty()) {
                            return@let
                        } else {
                            Timber.d("currentlyPlaying observer ${it.queue} ${it.title}")
                            val itemIndex = swipeAdapter.songList.indexOf(song)
                            if (lastItemIndex == Pair(song, itemIndex)) return@let
                            lastItemIndex = Pair(song, itemIndex)

                            curPlaying.value = song
                            observedPlaying = song
                            glideCurSong(song)

                            binding.viewPager2.setCurrentItem(itemIndex)
                        }
                    }
                }
            }

            playbackState.observe(this@MainActivity) {
                binding.ibPlayPause.setImageResource(
                    if (it?.isPlaying == true) R.drawable.ic_pause_24_widget
                    else R.drawable.ic_play_24_widget
                )
            }
        }
    }

    private fun glideCurSong(it: Song) {
        Timber.d("glide $it")
        binding.apply {
            glide.load(it.imageUri)
                .error(R.drawable.ic_music_library_transparent)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(sivCurImage)
        }
        if (false) glidePalette(it.imageUri)
    }

    private fun glidePalette(uri: String) {
        glide.asBitmap().load(uri).into(object : CustomTarget<Bitmap>() {
            @RequiresApi(Build.VERSION_CODES.N)
            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                Palette.from(resource).maximumColorCount(1).generate {
                    it?.let {
                        var default = getColor(R.color.widgetBackground)
                        var dominant = it.getDominantColor(default)
                        var vibrant = it.getVibrantColor(dominant)
                        var muted = it.getMutedColor(vibrant)
                        var lightVibrant = it.getLightVibrantColor(muted)
                        var lightMuted = it.getLightMutedColor(lightVibrant)
                        var darkVibrant =  it.getDarkVibrantColor(lightMuted)
                        var darkMuted = it.getDarkMutedColor(darkVibrant)
                        var grad = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                            intArrayOf(darkMuted, darkMuted)
                        )

                        if (grad.colors!!.first() == default) {
                            Timber.d("grad == default")
                            Palette.from(resource).maximumColorCount(10).generate {
                                it?.let {
                                    default = getColor(R.color.widgetBackground)
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
                                    binding.clPager.background = grad
                                }
                            }
                        } else binding.clPager.background = grad
                    }
                }
            }
            override fun onLoadCleared(placeholder: Drawable?) = Unit
            override fun onLoadFailed(errorDrawable: Drawable?) {
                binding.clPager.setBackgroundColor(this@MainActivity.getColor(R.color.widgetBackground))
                return
            }
        })
    }

    /**
     * Permission Setup
     */

    private fun permissionScreen() {
        binding.apply {

            with(View.GONE) {
                clPager.visibility = this
                navHostContainer.visibility = this
                bottomNavigationView.visibility = this
            }
            with(View.VISIBLE) {
                btnGrantPermission.visibility = this
            }
            btnGrantPermission.setOnClickListener {
                if (checkPermission()) {
                    restartApp(false)
                }
            }
        }
    }

    @SuppressLint("InlinedApi")
    private fun checkPermission(): Boolean {
        if (!PermsHelper.checkStoragePermission(this))
            requestPermission(Perms(
                WRITE_STORAGE,
                PERMISSION_WRITE_EXT_REQUEST_CODE,
                "Write External Storage"
            ))

        if (!PermsHelper.checkForegroundServicePermission(this))
            requestPermission(Perms(
                FOREGROUND_SERVICE,
                PERMISSION_FOREGROUND_SERVICE_REQUEST_CODE,
                "Foreground Service"
            ))

        return PermsHelper.checkStoragePermission(this)
                && PermsHelper.checkForegroundServicePermission(this)
    }

    private fun requestPermission(perms: Perms) {
        Timber.d("requestPermission: $perms")
        EasyPermissions.requestPermissions(
            this,
            "This App Need ${perms.msg} Permission",
            perms.requestId,
            perms.permission
        )
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            toast(this,"Permission Denied!")
            SettingsDialog.Builder(this).build().show()
            Timber.d("$perms")
        }
        if (EasyPermissions.somePermissionDenied(this, perms.first())) {
            toast(this,"Permission Needed!")
        }
        permissionScreen()
    }
    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        if (checkPermission()) {
            toast(this,"Permission Granted!, Restarting...", blockable = false)
            restartApp(true)
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode,
            permissions, grantResults, this
        )
    }


    /**
     * Behavior
     */
    private fun restartApp(blockable: Boolean)
    = CoroutineScope(Dispatchers.Main.immediate).launch {
            toast(this@MainActivity,
                "App will be restarted shortly...",
                blockable = blockable
            )
            delay(1000)
            val resIntent = Intent(this@MainActivity, MainActivity::class.java)
            finishAffinity()
            startActivity(resIntent)
    }

    override fun onBackPressed() {
        Timber.d("onBackPressed")
        if (false) {
            /*hidePlayingFragment()*/
            return
        } else if (Version.isQ() && isTaskRoot && supportFragmentManager.primaryNavigationFragment?.
            childFragmentManager?.backStackEntryCount == 0
            && supportFragmentManager.backStackEntryCount == 0
        ) {
            finishAfterTransition()
            return
        } else {
            super.onBackPressed()
        }
    }



    override fun onDestroy() {
        this.cacheDir.deleteRecursively()
        this.externalCacheDir?.deleteRecursively()
        Timber.d("MainActivity Destroyed")
        super.onDestroy()
    }

    /**
     * Ignore This
     */
    @Suppress("UNREACHABLE_CODE")
    @SuppressLint("LogNotTimber")
    fun brainException(msg: String) {
        Log.wtf("WTF", msg, throw RuntimeException())
    }

}




