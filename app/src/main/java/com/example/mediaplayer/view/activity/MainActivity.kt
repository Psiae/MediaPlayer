package com.example.mediaplayer.view.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.*
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.mediaplayer.R
import com.example.mediaplayer.databinding.ActivityMainBinding
import com.example.mediaplayer.util.Constants.FOREGROUND_SERVICE
import com.example.mediaplayer.util.Constants.INTERNET
import com.example.mediaplayer.util.Constants.PERMISSION_FOREGROUND_SERVICE_REQUEST_CODE
import com.example.mediaplayer.util.Constants.PERMISSION_INTERNET_REQUEST_CODE
import com.example.mediaplayer.util.Constants.PERMISSION_WRITE_EXT_REQUEST_CODE
import com.example.mediaplayer.util.Constants.READ_STORAGE
import com.example.mediaplayer.util.Constants.WRITE_STORAGE
import com.example.mediaplayer.util.ext.*
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import timber.log.Timber
import java.util.*

@AndroidEntryPoint
@SuppressLint("LogNotTimber")
class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_MediaPlayer)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        curToast = "default"

        // navController setup
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.navHostContainer) as NavHostFragment
        navController = navHostFragment.navController
        setDestinationListener(navController)

        // View setup
        binding.apply {
            bottomNavigationView.apply {
                setupWithNavController(navController)
                setOnItemReselectedListener { }
            }
        }

        // Permission check
        // shows permissionScreen if permission is not granted
        if (!checkPermission()) {
            permissionScreen()
        } else curToast = ""
    }

    /**
     * Navigation Setup
     */
    private fun setDestinationListener(controller: NavController) {
        controller.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.navBottomHome -> if (homeConstructing) toast("Coming Soon!")
                R.id.navBottomSong -> if (songConstructing) toast("Coming Soon!")
                R.id.navBottomPlaylist -> if (playlistConstructing) toast("Coming Soon!")
                R.id.navBottomLibrary -> if (libraryConstructing) toast("Coming Soon!")
                R.id.navBottomSettings -> if (settingsConstructing) toast("Coming Soon!")
            }
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

        if (!hasPermission(Perms(FOREGROUND_SERVICE))
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                requestPermission(Perms(
                    FOREGROUND_SERVICE,
                    PERMISSION_FOREGROUND_SERVICE_REQUEST_CODE,
                "Foreground Service"
                ))

        if (!hasPermission(Perms(WRITE_STORAGE))
            || !hasPermission(Perms(READ_STORAGE)))
                requestPermission(Perms(
                    WRITE_STORAGE,
                    PERMISSION_WRITE_EXT_REQUEST_CODE,
                    "Write External Storage"
                ))

        return (hasPermission(Perms(INTERNET))
                && hasPermission(Perms(FOREGROUND_SERVICE))
                && (hasPermission(Perms(WRITE_STORAGE))
                    || hasPermission(Perms(READ_STORAGE))
                ))
    }
    private fun hasPermission(perms: Perms): Boolean {
        return try {
            EasyPermissions.hasPermissions(this, perms.permission)
        } catch (e: Exception) {
            Timber.wtf(RuntimeException("$e $perms But Why?"))
            false
        }

        /*return when (perms) {
            Manifest.permission.INTERNET ->
                EasyPermissions.hasPermissions(this, perms)
            Manifest.permission.FOREGROUND_SERVICE ->
                EasyPermissions.hasPermissions(this, perms)
            Manifest.permission.WRITE_EXTERNAL_STORAGE ->
                EasyPermissions.hasPermissions(this, perms)
            Manifest.permission.READ_EXTERNAL_STORAGE ->
                EasyPermissions.hasPermissions(this, perms)
            else -> false
        }*/
    }
    private fun requestPermission(perms: Perms) {
        Timber.d("requestPermission: $perms")
        EasyPermissions.requestPermissions(
            this,
            "This App Need ${perms.msg} Permission",
            perms.requestId,
            perms.permission
        )
        /*when (perms.permission) {
            Manifest.permission.INTERNET ->
                EasyPermissions.requestPermissions(
                    this,
                    "This App Need Internet Permission",
                    PERMISSION_INTERNET_REQUEST_CODE,
                    perms.permission
                )
            Manifest.permission.FOREGROUND_SERVICE ->
                EasyPermissions.requestPermissions(
                    this,
                    "This App Need Foreground Permission",
                    PERMISSION_FOREGROUND_SERVICE_REQUEST_CODE,
                    perms.permission
                )
            Manifest.permission.WRITE_EXTERNAL_STORAGE ->
                EasyPermissions.requestPermissions(
                    this,
                    "This App Need Write Storage Permission",
                    PERMISSION_WRITE_EXT_REQUEST_CODE,
                    perms.permission
                )
            Manifest.permission.READ_EXTERNAL_STORAGE ->
                EasyPermissions.requestPermissions(
                    this,
                    "This App Need Read Storage Permission",
                    PERMISSION_READ_EXT_REQUEST_CODE,
                    perms.permission
                )
            else -> Unit
        }*/
    }
    private fun permissionScreen() {
        binding.apply {
            bottomNavigationView.visibility = View.GONE
            navHostContainer.visibility = View.GONE
            seekBar.visibility = View.GONE
            btnGrantPermission.visibility = View.VISIBLE
        }
        binding.run {
            btnGrantPermission.setOnClickListener {
                if (checkPermission()) {
                    restartApp()
                }
            }
        }
    }

    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            toast("Permission Denied!")
            SettingsDialog.Builder(this).build().show()
        }
        if (EasyPermissions.somePermissionDenied(this, perms.first())) {
            toast("Permission Needed!")
        }
    }
    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        if (checkPermission()) {
            toast("Permission Granted!, Restarting...")
            restartApp()
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    private fun toast(
        msg: String = "",
        short: Boolean = true,
        blockable: Boolean = true
    ) = CoroutineScope(Dispatchers.Main.immediate).launch {
        if (blockable) {
            if (msg == curToast) return@launch
            if (curToast.isNotEmpty()) {
                curToast = ""
                return@launch
            }
        }
        if (short) Toast.makeText(this@MainActivity, msg,
            Toast.LENGTH_SHORT).show()
        else Toast.makeText(this@MainActivity, msg,
            Toast.LENGTH_LONG).show()

        curToast = msg
        if (short) delay(2000) else delay(3500)
        curToast = ""
    }

    private fun restartApp() {
        CoroutineScope(Dispatchers.Main.immediate).launch {
            toast("App will be restarted shortly...")
            delay(1000)
            val resIntent = Intent(this@MainActivity, MainActivity::class.java)
            finishAffinity()
            startActivity(resIntent)
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
