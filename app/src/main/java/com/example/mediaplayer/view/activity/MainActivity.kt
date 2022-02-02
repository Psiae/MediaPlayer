package com.example.mediaplayer.view.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.ActivityMainBinding
import com.example.mediaplayer.exoplayer.isPlaying
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.util.Constants.FOREGROUND_SERVICE
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
import com.google.android.exoplayer2.ExoPlayer
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
    lateinit var player: ExoPlayer

    @Inject
    lateinit var glide: RequestManager

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val songViewModel: SongViewModel by viewModels()

    private var alreadySetup: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MediaPlayer)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        /* navController & Destination setup */
        setupNavController()

        // Permission check
        checkPermission()

        /* View setup */
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

    /**
     * Navigation & View Setup
     */
    private fun setupNavController() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostContainer) as NavHostFragment
        navController = navHostFragment.navController
        setDestinationListener(navController)
    }

    private fun setDestinationListener(controller: NavController) {
        controller.addOnDestinationChangedListener { _, destination, _ ->
            getControlHeight()
            when (val id = destination.id) {
                R.id.navBottomHome -> setControl(id = id)
                R.id.navBottomSong -> setControl(id = id)
                R.id.navBottomPlaylist -> setControl(id = id)
                R.id.navBottomLibrary -> setControl(id = id)
                R.id.folderFragment -> setControl(true, id)
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
            ibPlayPause.setOnClickListener { with(songViewModel) {
                    curPlaying.value?.let {
                        playOrToggle(it, true)
                    }
                }
            }

            sbSeekbar.apply { setOnTouchListener { _, _ -> true } }
            sivCurImage.setOnClickListener { toast(this@MainActivity, "Image Clicked") }

            viewPager2.apply {
                adapter = swipeAdapter.also {
                    it.setItemListener { song ->
                        toast(this@MainActivity, song.title)
                    }
                }
            }
            lifecycleScope.launch {
                while (true) {
                    delay(500)
                    with (binding.sbSeekbar) {
                        progress = ((player.currentPosition * 100) / player.duration).toInt()
                    }
                }
            }
        }
    }

    private fun setToaster() = apply { curToast = "" }
    private fun getControlHeight() {
        if (getDummyHeight() != 0) songViewModel.navHeight.value = getDummyHeight()
    }

    private fun setControl(fullscreen: Boolean = false, id: Int) {
        if (fullscreen) {
            binding.apply {
                with(View.GONE) {
                    bottomNavigationView.visibility = this
                }
                with(View.VISIBLE) {
                    clPager.visibility = this
                }
                if (getClpHeight() != 0) songViewModel.navHeight.value = getClpHeight()
                navHostContainer.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
            return
        }
        binding.apply {
            with(View.GONE) {}
            with(View.VISIBLE) {
                clPager.visibility = this
                bottomNavigationView.visibility = this
            }
            binding.navHostContainer.layoutParams.height = 0
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

    val pagerCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            Timber.d("changeCallback pos: $position")
            with(songViewModel) {
                if (playbackState.value?.isPlaying == true) {
                    Timber.d("chaged to $position")
                    playOrToggle(swipeAdapter.songList[position])
                } else {
                    Timber.d("pos $position")
                    try {
                        curPlaying.value = swipeAdapter.songList[position]
                    } catch (e: Exception) {
                        Timber.e(e)
                    }
                }
            }
        }
    }

    /**
     * ViewModel & Data Provider
     */
    private fun  setupSongVM() {

        songViewModel.apply {
            mediaItemSong.observe(this@MainActivity) { mediaItems ->
                Timber.d("mediaItems: $mediaItems")
                swipeAdapter.songList = mediaItems
                currentlyPlayingSongListObservedByMainActivity = mediaItems
                if (!alreadySetup || !songViewModel.currentlyPlaying.hasActiveObservers()) {
                    lifecycleScope.launch {
                        alreadySetup = true
                        observePlayer()
                        binding.viewPager2.unregisterOnPageChangeCallback(pagerCallback)
                        binding.viewPager2.registerOnPageChangeCallback(pagerCallback)
                    }
                }
            }
            lifecycleScope.launch() {
                lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    updateMusicDB()
                }
            }
        }
    }

    private fun observePlayer() {
        alreadySetup = true
        with(songViewModel) {
            currentlyPlaying.observe(this@MainActivity) { song ->
                Timber.d("isCurrentSame${song.mediaId.toString() == player.currentMediaItem!!.mediaId}")
                song?.let {
                    if (swipeAdapter.songList.isEmpty()) {
                        lifecycleScope.launch {
                            updateSongList()
                            delay(100)
                            curPlaying.value = song
                            glideCurSong(song)
                            val itemIndex = swipeAdapter.songList.indexOf(song)
                            if (itemIndex != -1) binding.viewPager2.currentItem = itemIndex
                            Timber.d("currentlyPlaying song: ${song} $itemIndex")
                        }
                    } else {
                        lifecycleScope.launch {
                            curPlaying.value = song
                            glideCurSong(song)
                            val itemIndex = swipeAdapter.songList.indexOf(song)
                            if (itemIndex != -1) binding.viewPager2.currentItem = itemIndex
                            Timber.d("currentlyPlaying song: ${song.title} $itemIndex")
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
            Timber.d("glideCurrentSong")
            glide.load(it.imageUri)
                .error(R.drawable.ic_music_library_transparent)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(sivCurImage)
        }
    }

    /**
     * Permission Setup
     */

    private fun permissionScreen() {
        binding.apply {
            clPager.visibility = View.GONE
            navHostContainer.visibility = View.GONE
            bottomNavigationView.visibility = View.GONE
            btnGrantPermission.visibility = View.VISIBLE
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
                && PermsHelper.checkStoragePermission(this)
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
        if (Version.isQ() && isTaskRoot && supportFragmentManager.primaryNavigationFragment
                ?.childFragmentManager?.backStackEntryCount == 0
            && supportFragmentManager.backStackEntryCount == 0
        ) finishAfterTransition()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("MainActivity Destroyed")
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




