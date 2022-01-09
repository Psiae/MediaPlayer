package com.example.mediaplayer.view.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.*
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.ActivityMainBinding
import com.example.mediaplayer.util.ext.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.lang.Exception
import java.util.*

@AndroidEntryPoint
@SuppressLint("LogNotTimber")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MediaPlayer)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostContainer) as NavHostFragment
        navController = navHostFragment.navController
        navController.popBackStack(R.id.navBottomHome, true)
        navController.navigate(R.id.navBottomSong)


        binding.apply {
            bottomNavigationView.apply {
                setupWithNavController(navController)
                setNavItemSelected()
            }
        }
    }

    private fun setNavItemSelected() {
        binding.bottomNavigationView.apply {
            setOnItemSelectedListener {
                navController.popBackStack(R.id.navBottomSong, false)
                try {
                    navController.navigate(it.itemId)
                } catch (e: Exception) {
                    navController.popBackStack(R.id.navBottomSong, true)
                    navController.navigate(R.id.navBottomSong)
                }
                when (it.itemId) {
                    R.id.navBottomHome -> {
                        if (homeConstructing) toast()
                        true
                    }
                    R.id.navBottomSong -> {
                        if (songConstructing) toast()
                        true
                    }
                    R.id.navBottomPlaylist -> {
                        if (playlistConstructing) toast()
                        true
                    }
                    R.id.navBottomLibrary -> {
                        if (libraryConstructing) toast()
                        true
                    }
                    R.id.navBottomSettings -> {
                        if (settingsConstructing) toast()
                        false
                    }
                    else -> false
                }
            }
            setOnItemReselectedListener {}
        }
    }


    private var curToast = ""
    private fun toast(
        msg: String = "Coming Soon!",
        short: Boolean = true
    ) = CoroutineScope(Dispatchers.Main.immediate).launch {
        if (msg == curToast) return@launch
        if (short) Toast.makeText(this@MainActivity,
            msg,
            Toast.LENGTH_SHORT).show()
        else Toast.makeText(this@MainActivity,
            msg,
            Toast.LENGTH_LONG).show()

        curToast = msg
        if (short) delay(2000) else delay(3500)
        curToast = ""
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
