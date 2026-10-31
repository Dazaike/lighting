package com.kingm.happylighting

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.kingm.happylighting.bluetooth.BleGattClient
import com.kingm.happylighting.bluetooth.BleScanner
import com.kingm.happylighting.data.HappyLightingController
import com.kingm.happylighting.data.SettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * UI-less launch target: connects to the saved light, sends power on/off, disconnects, finishes.
 * The light accepts one BLE client at a time, so this fails if the main app is connected.
 */
abstract class QuickPowerActivity(private val turnOn: Boolean) : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val appContext = applicationContext

        if (!requiredPermissions().all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            toast("Open Lumora once to grant Bluetooth permissions.")
            openMainApp()
            return
        }
        val address = SettingsRepository(appContext).loadDefaults().savedDeviceAddress
        if (address == null) {
            toast("No saved light. Connect one in Lumora first.")
            openMainApp()
            return
        }

        val controller = HappyLightingController(BleGattClient(appContext))
        lifecycleScope.launch {
            val message = try {
                withTimeout(OPERATION_TIMEOUT_MS) {
                    val device = BleScanner(appContext).findSavedDevice(address, SCAN_TIMEOUT_MS)
                    if (device != null) controller.connect(device) else controller.connect(address)
                    if (turnOn) controller.turnOn() else controller.turnOff()
                }
                if (turnOn) "Light on." else "Light off."
            } catch (e: CancellationException) {
                if (e is kotlinx.coroutines.TimeoutCancellationException) "Timed out reaching the light." else throw e
            } catch (e: Exception) {
                Log.e(TAG, "Quick power action failed", e)
                e.message ?: "Could not reach the light."
            } finally {
                withContext(NonCancellable) { runCatching { controller.disconnect() } }
            }
            toast(message)
            finish()
        }
    }

    private fun openMainApp() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    private fun toast(text: String) = Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()

    private companion object {
        const val TAG = "HappyLightingQuick"
        const val SCAN_TIMEOUT_MS = 4_000L
        const val OPERATION_TIMEOUT_MS = 20_000L
    }
}

class LightsOnActivity : QuickPowerActivity(turnOn = true)

class LightsOffActivity : QuickPowerActivity(turnOn = false)
