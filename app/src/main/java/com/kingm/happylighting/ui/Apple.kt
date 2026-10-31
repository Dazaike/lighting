package com.kingm.happylighting.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import com.moriafly.salt.ui.ItemContainer
import com.moriafly.salt.ui.SaltTheme
import com.moriafly.salt.ui.Text
import kotlin.math.max
import kotlin.math.roundToInt

/** System sans (Roboto on Android) in the weights iOS uses for body/headline text. */
val AppleFont: FontFamily = FontFamily.SansSerif

val AppleChipShape = RoundedCornerShape(14.dp)
val AppleSheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
val AppleDotShape = CircleShape
private val CardShape = RoundedCornerShape(22.dp)
private val ControlShape = RoundedCornerShape(10.dp)

private val CardFill = Color(0xFF1C1C1E).copy(alpha = 0.78f)
private val CardEdge = Color.White.copy(alpha = 0.08f)
private val Well = Color(0xFF3A3A3C).copy(alpha = 0.6f)
private val SegmentThumb = Color(0xFF636366)
private val SwitchGreen = Color(0xFF34C759)

/** Near-black page with a soft accent glow top-centre, like Apple Music's artwork wash. */
@Composable
fun AppleScene(amoled: Boolean, content: @Composable () -> Unit) {
    val base = if (amoled) Color.Black else Color(0xFF0A0A0C)
    val accent = SaltTheme.colors.highlight
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(base)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.width * 1.1f,
                    ),
                )
            },
    ) { content() }
}

/** Frosted grouped card. */
@Composable
fun AppleColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SaltTheme.dimens.padding, vertical = SaltTheme.dimens.padding * 0.5f)
            .clip(CardShape)
            .background(CardFill)
            .border(0.5.dp, CardEdge, CardShape),
        content = content,
    )
}

/** Text action in the accent colour, iOS list-button style. */
@Composable
fun AppleItemButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = true,
) {
    ItemContainer {
        Text(
            text = text,
            color = if (primary) SaltTheme.colors.highlight else SaltTheme.colors.subText,
            fontWeight = FontWeight.Medium,
            modifier = modifier
                .alpha(if (enabled) 1f else 0.4f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ),
        )
    }
}

/** Thin rounded track, white circular thumb with a soft shadow. Fraction in 0..1. */
@Composable
private fun AppleSlider(
    fraction: Float,
    onFractionChange: (Float) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val thumbPx = with(density) { 28.dp.toPx() }
    var widthPx by remember { mutableFloatStateOf(0f) }
    var pressed by remember { mutableStateOf(false) }
    val thumbScale by animateFloatAsState(
        targetValue = if (pressed) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "sliderThumbScale",
    )
    val accent = SaltTheme.colors.highlight
    val currentOnChange by rememberUpdatedState(onFractionChange)
    val currentEnabled by rememberUpdatedState(enabled)

    fun travel() = max(widthPx - thumbPx, 1f)
    fun fractionAt(x: Float) = ((x - thumbPx / 2f) / travel()).coerceIn(0f, 1f)

    Box(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        if (!currentEnabled) return@detectTapGestures
                        pressed = true
                        currentOnChange(fractionAt(offset.x))
                        tryAwaitRelease()
                        pressed = false
                    },
                )
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { if (currentEnabled) pressed = true },
                    onDragEnd = { pressed = false },
                    onDragCancel = { pressed = false },
                ) { change, _ ->
                    if (!currentEnabled) return@detectHorizontalDragGestures
                    change.consume()
                    currentOnChange(fractionAt(change.position.x))
                }
            },
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f)),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(with(density) { (fraction * travel() + thumbPx / 2f).toDp() })
                    .background(accent),
            )
        }
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((fraction * travel()).roundToInt(), 0) }
                .size(28.dp)
                .scale(thumbScale)
                .shadow(6.dp, CircleShape, spotColor = Color.Black, ambientColor = Color.Black)
                .background(Color.White, CircleShape),
        )
    }
}

/** Label + value row over an Apple slider. */
@Composable
fun AppleItemSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    text: String,
    sub: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    val span = valueRange.endInclusive - valueRange.start
    ItemContainer {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = text, style = SaltTheme.textStyles.main)
                Text(text = sub, style = SaltTheme.textStyles.sub)
            }
            AppleSlider(
                fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f),
                onFractionChange = { onValueChange(valueRange.start + it * span) },
                enabled = enabled,
                modifier = modifier,
            )
        }
    }
}

/** iOS segmented control: a raised grey thumb springs between options. */
@Composable
fun AppleSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val noRipple = remember { MutableInteractionSource() }
    var widthPx by remember { mutableFloatStateOf(0f) }
    val position by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
        label = "segmentPosition",
    )
    val paddingPx = with(density) { 3.dp.toPx() }
    val segmentPx = (widthPx - 2f * paddingPx) / options.size

    Box(
        modifier
            .fillMaxWidth()
            .height(40.dp)
            .onSizeChanged { widthPx = it.width.toFloat() },
    ) {
        Box(Modifier.matchParentSize().clip(ControlShape).background(Well))
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((paddingPx + position * segmentPx).roundToInt(), 0) }
                .size(
                    width = with(density) { segmentPx.coerceAtLeast(0f).toDp() },
                    height = 34.dp,
                )
                .shadow(3.dp, RoundedCornerShape(8.dp), spotColor = Color.Black)
                .background(SegmentThumb, RoundedCornerShape(8.dp)),
        )
        Row(Modifier.matchParentSize().padding(3.dp)) {
            options.forEachIndexed { index, label ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = noRipple, indication = null) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = if (index == selectedIndex) Color.White else SaltTheme.colors.subText,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/** Filled rounded button that dims and shrinks slightly on press. [tint] is the face colour. */
@Composable
fun AppleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = Color.Unspecified,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val face = if (tint == Color.Unspecified) Well else tint
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )

    Box(
        modifier = modifier
            .alpha(if (!enabled) 0.45f else if (pressed) 0.85f else 1f)
            .height(50.dp)
            .scale(scale)
            .clip(AppleChipShape)
            .background(face)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

/** iOS switch: green pill, white thumb that springs across. */
@Composable
private fun AppleSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val density = LocalDensity.current
    val progress by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "switchProgress",
    )
    val track = lerp(Well, SwitchGreen, progress)

    Box(
        Modifier
            .size(width = 51.dp, height = 31.dp)
            .toggleable(
                value = checked,
                interactionSource = null,
                indication = null,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            ),
    ) {
        Box(Modifier.matchParentSize().clip(CircleShape).background(track))
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(with(density) { (2.dp.toPx() + progress * 20.dp.toPx()).roundToInt() }, 0) }
                .size(27.dp)
                .shadow(4.dp, CircleShape, spotColor = Color.Black)
                .background(Color.White, CircleShape),
        )
    }
}

@Composable
fun AppleItemSwitcher(
    state: Boolean,
    onChange: (Boolean) -> Unit,
    text: String,
    sub: String? = null,
) {
    ItemContainer {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = text, style = SaltTheme.textStyles.main)
                if (sub != null) Text(text = sub, style = SaltTheme.textStyles.sub)
            }
            AppleSwitch(checked = state, onCheckedChange = onChange)
        }
    }
}

/** iOS large title. */
@Composable
fun AppleLargeTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = Color.White,
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
    )
}

/** Floating frosted tab bar with a sliding highlight behind the selected label. */
@Composable
fun AppleTabBar(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val noRipple = remember { MutableInteractionSource() }
    val accent = SaltTheme.colors.highlight
    Row(
        modifier = modifier
            .shadow(16.dp, CircleShape, spotColor = Color.Black)
            .clip(CircleShape)
            .background(Color(0xFF1C1C1E).copy(alpha = 0.92f))
            .border(0.5.dp, CardEdge, CircleShape)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                Modifier
                    .width(116.dp)
                    .height(44.dp)
                    .clip(CircleShape)
                    .background(if (selected) accent.copy(alpha = 0.22f) else Color.Transparent)
                    .clickable(interactionSource = noRipple, indication = null) { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (selected) accent else SaltTheme.colors.subText,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}
