package com.example.mediaplayer.view.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.*
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.ActivityMainBinding
import com.example.mediaplayer.util.ext.*
import com.example.mediaplayer.view.fragments.HomeFragment
import com.example.mediaplayer.view.fragments.LibraryFragment
import com.example.mediaplayer.view.fragments.PlaylistFragment
import com.example.mediaplayer.view.fragments.SongFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var currentFragment: Int = -1
    private var _myNavController: NavController? = null

    private val myNavController: NavController
        get() = _myNavController!!

    @SuppressLint("LogNotTimber")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MediaPlayer)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(
            R.id.nav_host_fragment_container
        ) as NavHostFragment

        _myNavController = navHostFragment.navController
        val navController = myNavController

        navController.popBackStack()
        navController.navigate(R.id.navBottomSong)

        binding.apply {
            bottomNavigationView.apply {
                setupWithNavController(navController)
                setBottomNavListener(navHostFragment.id)
            }
        }
    }

    private fun setBottomNavListener(containerId: Int) {
        binding.bottomNavigationView.setOnItemSelectedListener {
            Timber.d("$currentFragment, ${it.itemId}")
            if (currentFragment == it.itemId) return@setOnItemSelectedListener false
            Timber.d("$currentFragment, ${it.itemId}")
            val id = it.itemId

            when (it.itemId) {
                R.id.navBottomHome ->
                    navReplace(fragmentHomeConstructing, HomeFragment(), containerId, id)
                R.id.navBottomSong ->
                    navReplace(fragmentSongConstructing, SongFragment(), containerId, id)
                R.id.navBottomPlaylist ->
                    navReplace(fragmentPlaylistConstructing, PlaylistFragment(), containerId, id)
                R.id.navBottomLibrary ->
                    navReplace(fragmentLibraryConstructing, LibraryFragment(), containerId, id)
                R.id.navBottomSettings ->
                    navReplace(fragmentSettingsConstructing, null, containerId, id)
                else -> false
            }
        }
    }

    private fun navReplace(
        constructing: Boolean,
        fragment: Fragment?,
        containerId: Int,
        itemId: Int
        ): Boolean {
        return if (constructing) {
            shortToast("Coming Soon")
            false
        } else {
            if (currentFragment != fragment!!.id) {
                supportFragmentManager.beginTransaction()
                    .replace(containerId, fragment)
                    .commit()
            }
            currentFragment = itemId
            true
        }
    }

    var curToast = ""
    private fun shortToast(msg: String) {
        if (msg != curToast) CoroutineScope(Dispatchers.Default).launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
            }
            curToast = msg
            delay(2000)
            curToast = ""
        }
    }
    private fun longToast(msg: String) {
        if (msg != curToast) CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(this@MainActivity, msg, Toast.LENGTH_LONG).show()
            curToast = msg
            delay(2000)
            curToast = ""
        }
    }
}


/*Log.wtf(
        brainException("but why",
            throw Brain()))*/
/*@SuppressLint("LogNotTimber")
    fun brainException(msg: String? = null, cause: Throwable? = null): Throwable? {
        Log.wtf("brr", msg, cause)
        throw RuntimeException()
    }*/
