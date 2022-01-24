package com.example.mediaplayer.view.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.*
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.ActivityMainBinding
import com.example.mediaplayer.model.data.entities.Song
import com.example.mediaplayer.model.data.remote.testImageUrl
import com.example.mediaplayer.util.*
import com.example.mediaplayer.util.Constants.FOREGROUND_SERVICE
import com.example.mediaplayer.util.Constants.INTERNET
import com.example.mediaplayer.util.Constants.PERMISSION_FOREGROUND_SERVICE_REQUEST_CODE
import com.example.mediaplayer.util.Constants.PERMISSION_INTERNET_REQUEST_CODE
import com.example.mediaplayer.util.Constants.PERMISSION_WRITE_EXT_REQUEST_CODE
import com.example.mediaplayer.util.Constants.READ_STORAGE
import com.example.mediaplayer.util.Constants.WRITE_STORAGE
import com.example.mediaplayer.util.ext.*
import com.example.mediaplayer.view.adapter.FolderAdapter
import com.example.mediaplayer.view.adapter.SongAdapter
import com.example.mediaplayer.view.adapter.SwipeAdapter
import com.example.mediaplayer.viewmodel.SongViewModel
import com.google.android.exoplayer2.ExoPlayer
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Named

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
     * Permission Setup
     */


    @SuppressLint("InlinedApi")
    private fun checkPermission(): Boolean {
        if (!hasPermission(Perms(INTERNET)))
            requestPermission(Perms(
                INTERNET,
                PERMISSION_INTERNET_REQUEST_CODE,
                "Internet"
            ))

        if (!hasPermission(Perms(WRITE_STORAGE))
            || !hasPermission(Perms(READ_STORAGE)))
            requestPermission(Perms(
                WRITE_STORAGE,
                PERMISSION_WRITE_EXT_REQUEST_CODE,
                "Write External Storage"
            ))

        if (!hasPermission(Perms(FOREGROUND_SERVICE))
            && VersionHelper.isPie())
            requestPermission(Perms(
                FOREGROUND_SERVICE,
                PERMISSION_FOREGROUND_SERVICE_REQUEST_CODE,
                "Foreground Service"
            ))

        return if (VersionHelper.isPie()) {
                (hasPermission(Perms(INTERNET))
                    && hasPermission(Perms(FOREGROUND_SERVICE))
                    && (hasPermission(Perms(WRITE_STORAGE))
                        || hasPermission(Perms(READ_STORAGE))))
        } else {
                (hasPermission(Perms(INTERNET))
                        && (hasPermission(Perms(WRITE_STORAGE))
                        || hasPermission(Perms(READ_STORAGE))))
        }
    }
    private fun hasPermission(perms: Perms): Boolean {
        return try {
            Timber.d("hasPermission ?: $perms")
            EasyPermissions.hasPermissions(this, perms.permission)
        } catch (e: Exception) {
            Timber.wtf(RuntimeException("$e $perms"))
            brainException("$e $perms")
            false
        }
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
            when (destination.id) {
                R.id.navBottomHome -> setControl()
                R.id.navBottomSong -> setControl()
                R.id.navBottomPlaylist -> setControl()
                R.id.navBottomLibrary -> setControl()
                /*R.id.navBottomSettings -> setControl()*/
                R.id.exoplayerFragment -> setControl(fullscreen = true)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupView() {
        binding.apply {
            bottomNavigationView.apply {
                setupWithNavController(navController)
                setOnItemReselectedListener { }
            }
            sbSeekbar.apply {
                setOnTouchListener { _, _ -> true }
                progress = 20
            }
            sivCurImage.setOnClickListener {
                toast(this@MainActivity, "Image Clicked")
            }
            viewPager2.apply {
                adapter = swipeAdapter
            }
            ibPlayPause.setOnClickListener {
                songViewModel.isPlaying.value =
                    if (songViewModel.isPlaying.value!!) {
                        player.pause()
                        false
                    } else {
                        player.play()
                        true
                    }
            }
        }
    }

    private fun setToaster() = apply { curToast = "" }
    private fun getControlHeight() {
        songViewModel.navHeight.value =
            if (binding.dummy.measuredHeight != 0) binding.dummy.measuredHeight
        else return
    }
    private fun setControl(fullscreen: Boolean = false) {
        if (fullscreen) {
            binding.apply {
                bottomNavigationView.visibility = View.GONE
                clPager.visibility = View.GONE
                navHostContainer.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
            return
        }
        binding.apply {
            binding.bottomNavigationView.visibility = View.VISIBLE
            binding.clPager.visibility = View.VISIBLE
            binding.navHostContainer.layoutParams.height = 0
        }
    }

    /**
     * ViewModel & Data Provider
     */
    private fun setupSongVM() {
        songViewModel.apply {
            isPlaying.observe(this@MainActivity) {
                if (it) binding.ibPlayPause.setImageResource(R.drawable.ic_pause_24_widget)
                else binding.ibPlayPause.setImageResource(R.drawable.ic_play_24_widget)
            }
            songList.observe(this@MainActivity) {
                swipeAdapter.songList = it
            }
            curPlayingSong.observe(this@MainActivity) {
                it?.let {
                    val itemIndex = swipeAdapter.songList.indexOf(it)
                    binding.viewPager2.currentItem = itemIndex
                    glideCurSong(it)

                }/* ?: glideCurSong(swipeAdapter.songList[0])*/
            }
            CoroutineScope(Dispatchers.Main).launch {
                getShuffledSong(20)
            }
        }
    }

    private fun glideCurSong(it: Song) {
        Timber.d("glide $it")
        binding.apply {
            if (it.artist.lowercase() == "rei"
                || it.album.lowercase() == "romancer"
                || it.album.lowercase() == "summit"
            ) {
                glide.load(testImageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .centerInside()
                    .placeholder(R.drawable.splash_image_24_transparent)
                    .into(sivCurImage)
            } else glide.load(R.drawable.splash_image_24_transparent)
                .transition(DrawableTransitionOptions.withCrossFade())
                .centerInside()
                .into(sivCurImage)
        }
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

    override fun onResume() {
        super.onResume()
        CoroutineScope(Dispatchers.IO).launch {
            songViewModel.getDeviceSong("MainActivity onResume")
        }
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
        Log.wtf("WTF", "BrainException occurred: $msg", throw RuntimeException())
    }

}




