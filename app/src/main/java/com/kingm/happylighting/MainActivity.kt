package com.kingm.happylighting

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kingm.happylighting.bluetooth.BleGattClient
import com.kingm.happylighting.bluetooth.BleScanner
import com.kingm.happylighting.data.SettingsRepository
import com.kingm.happylighting.ui.LumoraScreen
import com.kingm.happylighting.ui.MainViewModel
import com.kingm.happylighting.ui.theme.HappyLightingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val viewModel: MainViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(
                        scanner = BleScanner(context),
                        gattClient = BleGattClient(context),
                        settingsRepository = SettingsRepository(context),
                    ) as T
                },
            )
            val uiState by viewModel.uiState.collectAsState()
            HappyLightingTheme(appStyle = uiState.appStyle) {
                val permissions = requiredPermissions()
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                ) { granted ->
                    viewModel.setPermissionGranted(granted.values.all { it })
                }

                LaunchedEffect(Unit) {
                    val granted = permissions.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    viewModel.setPermissionGranted(granted)
                    if (!granted) {
                        launcher.launch(permissions.toTypedArray())
                    }
                }

                LumoraScreen(
                    uiState = uiState,
                    onRequestPermissions = { launcher.launch(permissions.toTypedArray()) },
                    onScan = { viewModel.scanDevices() },
                    onConnect = viewModel::connectDevice,
                    onDisconnect = viewModel::disconnectDevice,
                    onTurnOn = viewModel::turnOn,
                    onTurnOff = viewModel::turnOff,
                    onRefreshStatus = viewModel::resyncConnection,
                    onColorChange = viewModel::setBaseColor,
                    onBrightnessChange = viewModel::setBrightness,
                    onPickerModeChange = viewModel::setPickerMode,
                    onSaveSwatch = viewModel::saveCurrentSwatch,
                    onApplySwatch = viewModel::applySwatch,
                    onRemoveSwatch = viewModel::removeSwatch,
                    onAppAccentColorChange = viewModel::setAppAccentColor,
                    onAppSaturationChange = viewModel::setAppSaturation,
                    onAppContrastChange = viewModel::setAppContrast,
                    onMatchLightColorChange = viewModel::setMatchLightColor,
                    onAmoledModeChange = viewModel::setAmoledMode,
                    onAutoConnectOnLaunchChange = viewModel::setAutoConnectOnLaunch,
                )
            }
        }
    }
}

internal fun requiredPermissions(): List<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    } else {
        listOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
