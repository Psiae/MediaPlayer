package com.example.mediaplayer.util.ext

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

var curToast = "default"
fun toast(context: Context,
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
    curToast = msg
    if (short) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        delay(2000)
    } else {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        delay(3500)
    }
    curToast = ""
}
