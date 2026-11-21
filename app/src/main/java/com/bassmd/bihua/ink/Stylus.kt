package com.bassmd.bihua.ink

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.input.InputManager
import android.view.InputDevice

/**
 * True when this device has a real stylus. Samsung tablets do not always declare
 * `android.hardware.stylus`, so this also checks the S Pen vendor feature and
 * the input devices themselves.
 */
fun hasStylus(context: Context): Boolean {
    val pm = context.packageManager
    if (pm.hasSystemFeature("android.hardware.stylus")) return true
    if (pm.hasSystemFeature("com.sec.feature.spen_usp")) return true
    val input = context.getSystemService(Context.INPUT_SERVICE) as? InputManager ?: return false
    return input.inputDeviceIds.any { id ->
        val device = input.getInputDevice(id) ?: return@any false
        val sources = device.sources
        sources and InputDevice.SOURCE_STYLUS == InputDevice.SOURCE_STYLUS ||
            sources and InputDevice.SOURCE_BLUETOOTH_STYLUS == InputDevice.SOURCE_BLUETOOTH_STYLUS
    }
}
