@file:OptIn(UnstableSaltApi::class)

package com.kingm.happylighting.ui

import android.graphics.Color.HSVToColor
import android.graphics.Color.colorToHSV
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.kingm.happylighting.model.MainUiState
import com.kingm.happylighting.model.PickerMode
import com.kingm.happylighting.model.SavedSwatch
import com.moriafly.salt.ui.Item
import com.moriafly.salt.ui.ItemArrowType
import com.moriafly.salt.ui.ItemContainer
import com.moriafly.salt.ui.ItemOuterTip
import com.moriafly.salt.ui.ItemOuterTitle
import com.moriafly.salt.ui.ItemSlider
import com.moriafly.salt.ui.ItemSwitcher
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import com.moriafly.salt.ui.TextButton
import com.moriafly.salt.ui.TitleBar
import com.moriafly.salt.ui.UnstableSaltApi
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

private val StatusGreen = Color(0xFF4DE3A0)
private val StatusYellow = Color(0xFFFFC766)
private val StatusOrange = Color(0xFFFF8E5E)
private val PowerOffRed = Color(0xFFE5484D)

@Composable
fun LumoraScreen(
    uiState: MainUiState,
    onRequestPermissions: () -> Unit,
    onScan: () -> Unit,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onTurnOn: () -> Unit,
    onTurnOff: () -> Unit,
    onRefreshStatus: () -> Unit,
    onColorChange: (Triple<Int, Int, Int>) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onPickerModeChange: (PickerMode) -> Unit,
    onSaveSwatch: () -> Unit,
    onApplySwatch: (Long) -> Unit,
    onRemoveSwatch: (Long) -> Unit,
    onAppAccentColorChange: (Triple<Int, Int, Int>) -> Unit,
    onAppSaturationChange: (Int) -> Unit,
    onAppContrastChange: (Int) -> Unit,
    onMatchLightColorChange: (Boolean) -> Unit,
    onAmoledModeChange: (Boolean) -> Unit,
    onAutoConnectOnLaunchChange: (Boolean) -> Unit,
) {
    var selectedAddress by rememberSaveable { mutableStateOf(uiState.savedDeviceAddress ?: uiState.deviceAddress) }
    var tab by rememberSaveable { mutableStateOf(0) }
    val preferredAddress = remember(uiState.devices) {
        (uiState.devices.firstOrNull { it.isLikelyMatch } ?: uiState.devices.firstOrNull())?.address
    }

    LaunchedEffect(uiState.savedDeviceAddress, uiState.deviceAddress, uiState.devices) {
        val selectedStillExists = selectedAddress != null && uiState.devices.any { it.address == selectedAddress }
        if (!selectedStillExists) {
            selectedAddress = uiState.savedDeviceAddress
                ?.takeIf { saved -> uiState.devices.any { it.address == saved } }
                ?: uiState.deviceAddress
                ?: preferredAddress
        }
    }

    AppleScene(amoled = uiState.appStyle.amoledMode) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            if (!uiState.permissionGranted) {
                PermissionGate(onRequestPermissions)
                return@Box
            }

            BackHandler(enabled = tab != 0) { tab = 0 }

            if (tab == 0) {
                MainScreen(
                    uiState = uiState,
                    onRefreshStatus = onRefreshStatus,
                    onColorChange = onColorChange,
                    onBrightnessChange = onBrightnessChange,
                    onTurnOn = onTurnOn,
                    onTurnOff = onTurnOff,
                    onPickerModeChange = onPickerModeChange,
                    onSaveSwatch = onSaveSwatch,
                    onApplySwatch = onApplySwatch,
                    onRemoveSwatch = onRemoveSwatch,
                )
            } else {
                SettingsScreen(
                    uiState = uiState,
                    selectedAddress = selectedAddress,
                    fallbackAddress = preferredAddress,
                    onSelectDevice = { selectedAddress = it },
                    onScan = onScan,
                    onConnect = { (selectedAddress ?: preferredAddress)?.let(onConnect) },
                    onDisconnect = onDisconnect,
                    onRefreshStatus = onRefreshStatus,
                    onAppAccentColorChange = onAppAccentColorChange,
                    onAppSaturationChange = onAppSaturationChange,
                    onAppContrastChange = onAppContrastChange,
                    onMatchLightColorChange = onMatchLightColorChange,
                    onAmoledModeChange = onAmoledModeChange,
                    onAutoConnectOnLaunchChange = onAutoConnectOnLaunchChange,
                )
            }

            AppleTabBar(
                options = listOf("Light", "Settings"),
                selectedIndex = tab,
                onSelect = { tab = it },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun PermissionGate(onRequestPermissions: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        AppleColumn {
            ItemContainer {
                Text(
                    text = "Lumora needs Bluetooth and location access to find your controller.",
                    style = SaltTheme.textStyles.paragraph,
                )
            }
            AppleItemButton(onClick = onRequestPermissions, text = "Grant Access")
        }
    }
}

// ---------------------------------------------------------------- main screen

@Composable
private fun MainScreen(
    uiState: MainUiState,
    onRefreshStatus: () -> Unit,
    onColorChange: (Triple<Int, Int, Int>) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onTurnOn: () -> Unit,
    onTurnOff: () -> Unit,
    onPickerModeChange: (PickerMode) -> Unit,
    onSaveSwatch: () -> Unit,
    onApplySwatch: (Long) -> Unit,
    onRemoveSwatch: (Long) -> Unit,
) {
    var wheelInteracting by remember { mutableStateOf(false) }
    val wheelScrollBlocker = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset =
                if (wheelInteracting) available else Offset.Zero
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(wheelScrollBlocker),
        userScrollEnabled = !wheelInteracting,
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppleLargeTitle("Lumora", Modifier.weight(1f))
                StatusPill(uiState, onRefreshStatus)
            }
        }
        item { HeroCard(uiState) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppleButton(
                    text = "Power On",
                    onClick = onTurnOn,
                    modifier = Modifier.weight(1f),
                    enabled = uiState.connected,
                    tint = SaltTheme.colors.highlight,
                )
                AppleButton(
                    text = "Power Off",
                    onClick = onTurnOff,
                    modifier = Modifier.weight(1f),
                    enabled = uiState.connected,
                    tint = PowerOffRed,
                )
            }
        }
        item {
            AppleColumn {
                AppleItemSlider(
                    value = uiState.brightness.toFloat(),
                    onValueChange = { onBrightnessChange(it.toInt()) },
                    text = "Brightness",
                    sub = "${uiState.brightness}%",
                    valueRange = 1f..100f,
                )
            }
        }
        item {
            ItemOuterTitle("SAVED COLORS")
            SwatchLibrary(
                swatches = uiState.savedSwatches,
                current = uiState.baseColor,
                onAdd = onSaveSwatch,
                onApply = onApplySwatch,
                onRemove = onRemoveSwatch,
            )
        }
        item {
            PickerSection(
                uiState = uiState,
                onColorChange = onColorChange,
                onPickerModeChange = onPickerModeChange,
                onWheelInteractionChange = { wheelInteracting = it },
            )
        }
        item { ItemOuterTip(uiState.statusText) }
        item { Spacer(modifier = Modifier.height(96.dp)) }
    }
}

/** Compact capsule: status dot + label. Tap to resync when possible. */
@Composable
private fun StatusPill(uiState: MainUiState, onRefreshStatus: () -> Unit) {
    val statusLabel = when {
        uiState.autoConnectInProgress -> "Connecting"
        uiState.connected -> "Connected"
        uiState.scanning -> "Scanning"
        else -> "Not connected"
    }
    val statusColor = when {
        uiState.autoConnectInProgress -> StatusYellow
        uiState.connected -> StatusGreen
        uiState.scanning -> StatusOrange
        else -> SaltTheme.colors.subText
    }
    val canResync = uiState.connected || (uiState.savedDeviceAddress != null && !uiState.autoConnectInProgress)

    Row(
        modifier = Modifier
            .clip(AppleDotShape)
            .background(Color.White.copy(alpha = 0.10f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = canResync,
                onClick = onRefreshStatus,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(8.dp).clip(AppleDotShape).background(statusColor))
        Text(text = statusLabel, style = SaltTheme.textStyles.sub)
    }
}

/** Artwork-style card filled with the colour the light is showing. */
@Composable
private fun HeroCard(uiState: MainUiState) {
    val preview = uiState.appliedColor.toColor()
    val textColor = if (preview.luminance() > 0.55f) Color.Black else Color.White
    val shape = RoundedCornerShape(32.dp)
    val title = when {
        uiState.connected -> uiState.deviceName
        uiState.scanning -> "Searching for your controller"
        else -> uiState.savedDeviceName ?: "Ready to connect"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(230.dp)
            .shadow(28.dp, shape, ambientColor = preview, spotColor = preview)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(androidx.compose.ui.graphics.lerp(preview, Color.White, 0.18f), preview),
                ),
            )
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), shape),
    ) {
        Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
            Text(
                text = "#%02X%02X%02X".format(
                    (preview.red * 255).toInt(),
                    (preview.green * 255).toInt(),
                    (preview.blue * 255).toInt(),
                ),
                color = textColor,
                fontWeight = FontWeight.Bold,
            )
            Text(text = title, color = textColor.copy(alpha = 0.7f), style = SaltTheme.textStyles.sub)
        }
    }
}

@Composable
private fun PickerSection(
    uiState: MainUiState,
    onColorChange: (Triple<Int, Int, Int>) -> Unit,
    onPickerModeChange: (PickerMode) -> Unit,
    onWheelInteractionChange: (Boolean) -> Unit,
) {
    ItemOuterTitle("COLOR")
    AppleColumn {
        ItemContainer {
            AppleSegmented(
                options = listOf("Wheel", "RGB"),
                selectedIndex = if (uiState.pickerMode == PickerMode.WHEEL) 0 else 1,
                onSelect = { onPickerModeChange(if (it == 0) PickerMode.WHEEL else PickerMode.RGB) },
            )
        }
        when (uiState.pickerMode) {
            PickerMode.WHEEL -> ColorWheelPicker(
                color = uiState.baseColor,
                onColorChange = onColorChange,
                onInteractionChange = onWheelInteractionChange,
            )
            PickerMode.RGB -> {
                AppleItemSlider(
                    value = uiState.baseColor.first.toFloat(),
                    onValueChange = { onColorChange(Triple(it.toInt(), uiState.baseColor.second, uiState.baseColor.third)) },
                    text = "Red",
                    sub = uiState.baseColor.first.toString(),
                    valueRange = 0f..255f,
                )
                AppleItemSlider(
                    value = uiState.baseColor.second.toFloat(),
                    onValueChange = { onColorChange(Triple(uiState.baseColor.first, it.toInt(), uiState.baseColor.third)) },
                    text = "Green",
                    sub = uiState.baseColor.second.toString(),
                    valueRange = 0f..255f,
                )
                AppleItemSlider(
                    value = uiState.baseColor.third.toFloat(),
                    onValueChange = { onColorChange(Triple(uiState.baseColor.first, uiState.baseColor.second, it.toInt())) },
                    text = "Blue",
                    sub = uiState.baseColor.third.toString(),
                    valueRange = 0f..255f,
                )
            }
        }
    }
}

// -------------------------------------------------------------- settings screen

@Composable
private fun SettingsScreen(
    uiState: MainUiState,
    selectedAddress: String?,
    fallbackAddress: String?,
    onSelectDevice: (String) -> Unit,
    onScan: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onRefreshStatus: () -> Unit,
    onAppAccentColorChange: (Triple<Int, Int, Int>) -> Unit,
    onAppSaturationChange: (Int) -> Unit,
    onAppContrastChange: (Int) -> Unit,
    onMatchLightColorChange: (Boolean) -> Unit,
    onAmoledModeChange: (Boolean) -> Unit,
    onAutoConnectOnLaunchChange: (Boolean) -> Unit,
) {
    val appStyle = uiState.appStyle
    val accentPreview = appStyle.accentColor.toColor()

    Column(modifier = Modifier.fillMaxSize()) {
        AppleLargeTitle("Settings", Modifier.padding(horizontal = 20.dp, vertical = 12.dp))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                ItemOuterTitle("APP STYLE")
                AppleColumn {
                    AppleItemSwitcher(state = appStyle.amoledMode,
                    onChange = onAmoledModeChange,
                    text = "AMOLED Mode",
                    sub = "Use deeper blacks for the app surfaces.",)
                    AppleItemSwitcher(state = appStyle.matchLightColor,
                    onChange = onMatchLightColorChange,
                    text = "Match Light Color",
                    sub = "Use the current light output as the app accent.",)
                    ItemContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clip(AppleChipShape)
                                .background(accentPreview),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Accent #%02X%02X%02X".format(
                                    appStyle.accentColor.first,
                                    appStyle.accentColor.second,
                                    appStyle.accentColor.third,
                                ),
                                color = if (accentPreview.luminance() > 0.55f) Color.Black else Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    AppleItemSlider(value = appStyle.accentColor.first.toFloat(),
                    onValueChange = { onAppAccentColorChange(Triple(it.toInt(), appStyle.accentColor.second, appStyle.accentColor.third)) },
                    text = "Accent Red",
                    sub = appStyle.accentColor.first.toString(),
                    enabled = !appStyle.matchLightColor,
                    valueRange = 0f..255f,)
                    AppleItemSlider(value = appStyle.accentColor.second.toFloat(),
                    onValueChange = { onAppAccentColorChange(Triple(appStyle.accentColor.first, it.toInt(), appStyle.accentColor.third)) },
                    text = "Accent Green",
                    sub = appStyle.accentColor.second.toString(),
                    enabled = !appStyle.matchLightColor,
                    valueRange = 0f..255f,)
                    AppleItemSlider(value = appStyle.accentColor.third.toFloat(),
                    onValueChange = { onAppAccentColorChange(Triple(appStyle.accentColor.first, appStyle.accentColor.second, it.toInt())) },
                    text = "Accent Blue",
                    sub = appStyle.accentColor.third.toString(),
                    enabled = !appStyle.matchLightColor,
                    valueRange = 0f..255f,)
                    AppleItemSlider(value = appStyle.saturation.toFloat(),
                    onValueChange = { onAppSaturationChange(it.toInt()) },
                    text = "Saturation",
                    sub = "${appStyle.saturation}%",
                    valueRange = 0f..200f,)
                    AppleItemSlider(value = appStyle.contrast.toFloat(),
                    onValueChange = { onAppContrastChange(it.toInt()) },
                    text = "Contrast",
                    sub = "${appStyle.contrast}%",
                    valueRange = 60f..160f,)
                }
            }
            item {
                ItemOuterTitle("ON LAUNCH")
                AppleColumn {
                    AppleItemSwitcher(state = uiState.autoConnectOnLaunch,
                    onChange = onAutoConnectOnLaunchChange,
                    text = "Connect & turn on",
                    sub = "Connect to your saved light and turn it on when the app opens.",)
                }
            }
            item {
                ItemOuterTitle("DEVICES")
                AppleColumn {
                    if (uiState.devices.isEmpty()) {
                        Item(
                            onClick = {},
                            text = "No scanned devices yet.",
                            enabled = false,
                            arrowType = ItemArrowType.None,
                        )
                    }
                    uiState.devices.forEach { device ->
                        val selected = device.address == selectedAddress
                        Item(
                            onClick = { onSelectDevice(device.address) },
                            text = device.name,
                            textColor = if (selected) SaltTheme.colors.highlight else SaltTheme.colors.text,
                            sub = buildString {
                                append(device.address)
                                if (device.isLikelyMatch) append(" • likely match")
                                if (selected) append(" • selected")
                            },
                            arrowType = ItemArrowType.None,
                        )
                    }
                    AppleItemButton(
                        onClick = onScan,
                        text = if (uiState.scanning) "Scanning..." else "Scan for devices",
                        enabled = !uiState.scanning,
                    )
                    AppleItemButton(
                        onClick = onConnect,
                        text = if (uiState.autoConnectInProgress) "Connecting..." else "Connect",
                        enabled = (selectedAddress != null || fallbackAddress != null) &&
                            !uiState.connected && !uiState.autoConnectInProgress,
                    )
                    AppleItemButton(onClick = onRefreshStatus, text = "Sync", enabled = uiState.connected)
                    AppleItemButton(onClick = onDisconnect, text = "Disconnect", enabled = uiState.connected, primary = false)
                }
            }
            item { Spacer(modifier = Modifier.height(96.dp)) }
        }
    }
}

// ------------------------------------------------------------------- widgets

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwatchLibrary(
    swatches: List<SavedSwatch>,
    current: Triple<Int, Int, Int>,
    onAdd: () -> Unit,
    onApply: (Long) -> Unit,
    onRemove: (Long) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(AppleDotShape)
                    .background(Color.White.copy(alpha = 0.10f))
                    .clickable(onClick = onAdd),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "+", fontWeight = FontWeight.Bold, color = SaltTheme.colors.highlight)
            }
        }
        items(swatches, key = { it.id }) { swatch ->
            val selected = swatch.color == current
            Box(
                modifier = Modifier
                    .animateItem()
                    .size(48.dp)
                    .clip(AppleDotShape)
                    .background(swatchPreviewColor(swatch))
                    .border(
                        width = if (selected) 2.dp else 0.5.dp,
                        color = if (selected) Color.White else Color.White.copy(alpha = 0.2f),
                        shape = AppleDotShape,
                    )
                    .combinedClickable(
                        onClick = { onApply(swatch.id) },
                        onLongClick = { onRemove(swatch.id) },
                    ),
            )
        }
    }
}

@Composable
private fun ColorWheelPicker(
    color: Triple<Int, Int, Int>,
    onColorChange: (Triple<Int, Int, Int>) -> Unit,
    onInteractionChange: (Boolean) -> Unit,
) {
    var interacting by remember { mutableStateOf(false) }
    val hsv = remember(color) { color.toHsv() }
    val selectorColor = Color.hsv(hsv[0], hsv[1], 1f)
    val ringOutlineColor = SaltTheme.colors.stroke

    val thumbScale by animateFloatAsState(
        targetValue = if (interacting) 1.25f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.76f),
        label = "thumb-scale",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(272.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(248.dp)
                .pointerInput(Unit) {
                    detectWheelGestures(
                        onInteractionChange = {
                            interacting = it
                            onInteractionChange(it)
                        },
                        onColorSelected = { selected -> onColorChange(selected) },
                    )
                },
        ) {
            val radius = size.minDimension / 2f - 12.dp.toPx()
            val center = center
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color.Red,
                        Color.Yellow,
                        Color.Green,
                        Color.Cyan,
                        Color.Blue,
                        Color.Magenta,
                        Color.Red,
                    ),
                    center = center,
                ),
                radius = radius,
                center = center,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
            drawCircle(
                color = ringOutlineColor,
                radius = radius + 2.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx()),
            )

            val angle = Math.toRadians(hsv[0].toDouble())
            val selectorRadius = hsv[1] * radius
            val selectorCenter = Offset(
                x = center.x + cos(angle).toFloat() * selectorRadius,
                y = center.y + sin(angle).toFloat() * selectorRadius,
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = 16.dp.toPx() * thumbScale,
                center = selectorCenter,
            )
            drawCircle(
                color = Color.White,
                radius = 13.dp.toPx() * thumbScale,
                center = selectorCenter,
            )
            drawCircle(
                color = selectorColor,
                radius = 10.dp.toPx() * thumbScale,
                center = selectorCenter,
            )
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectWheelGestures(
    onInteractionChange: (Boolean) -> Unit,
    onColorSelected: (Triple<Int, Int, Int>) -> Unit,
) {
    while (true) {
        awaitPointerEventScope {
            val pointerSize = this@detectWheelGestures.size.toSize()
            var change = awaitPointerEvent().changes.firstOrNull { it.pressed } ?: return@awaitPointerEventScope
            onInteractionChange(true)
            onColorSelected(offsetToColor(size = pointerSize, offset = change.position))
            while (change.pressed) {
                val event = awaitPointerEvent()
                change = event.changes.firstOrNull { it.id == change.id }
                    ?: event.changes.firstOrNull()
                    ?: break
                onColorSelected(offsetToColor(size = pointerSize, offset = change.position))
            }
            onInteractionChange(false)
        }
    }
}

private fun swatchPreviewColor(swatch: SavedSwatch): Color {
    val factor = swatch.brightness.coerceIn(1, 100) / 100f
    return Color(
        red = (swatch.color.first / 255f) * factor,
        green = (swatch.color.second / 255f) * factor,
        blue = (swatch.color.third / 255f) * factor,
    )
}

private fun offsetToColor(size: Size, offset: Offset): Triple<Int, Int, Int> {
    val radius = min(size.width, size.height) / 2f - 12f
    val center = Offset(size.width / 2f, size.height / 2f)
    val dx = offset.x - center.x
    val dy = offset.y - center.y
    val distance = hypot(dx, dy).coerceAtMost(radius)
    val hue = ((Math.toDegrees(atan2(dy, dx).toDouble()) + 360.0) % 360.0).toFloat()
    val saturation = (distance / radius).coerceIn(0f, 1f)
    val colorInt = HSVToColor(floatArrayOf(hue, saturation, 1f))
    return Triple(
        android.graphics.Color.red(colorInt),
        android.graphics.Color.green(colorInt),
        android.graphics.Color.blue(colorInt),
    )
}

private fun Triple<Int, Int, Int>.toColor(): Color = Color(first, second, third)

private fun Triple<Int, Int, Int>.toHsv(): FloatArray {
    val hsv = FloatArray(3)
    colorToHSV(android.graphics.Color.rgb(first, second, third), hsv)
    return hsv
}
