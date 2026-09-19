package com.ashmeet.hyperlauncher.components.dialog

//noinspection SuspiciousImport
import android.R
import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ashmeet.hyperlauncher.components.HyperAlertDialog
import com.ashmeet.hyperlauncher.components.HyperOutlinedTextField
import com.ashmeet.hyperlauncher.components.slider.SimpleTextSlider
import com.ashmeet.hyperlauncher.components.switch.DefaultSwitch
import com.ashmeet.hyperlauncher.screens.settings.preferences.LauncherPreferences
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kdt.pojavlaunch.utils.KeycodeUtils
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun MarqueeText(text: String, modifier: Modifier = Modifier) {
    Text(text = text, modifier = modifier)
}

@Composable
fun cardColor() = MaterialTheme.colorScheme.surfaceVariant

@Composable
fun onCardColor() = MaterialTheme.colorScheme.onSurfaceVariant

fun Modifier.fadeEdge() = this
fun Modifier.verticalScrollWithBar(state: ScrollState) = this.verticalScroll(state)

@Composable
fun ImePanContainer(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier, contentAlignment = contentAlignment, content = content)
}

@Composable
fun SingleLineTextCheck(
) {}

@Composable
fun rememberDialogMaxHeight(): Dp {
    return LocalWindowInfo.current.containerSize.height.dp
}

@Composable
fun SimpleAlertDialog(
    title: String,
    text: String,
    confirmText: String = stringResource(R.string.ok),
    dismissText: String = stringResource(R.string.cancel),
    dismissByDialog: Boolean = true,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    HyperAlertDialog(
        onDismissRequest = {
            if (dismissByDialog) onDismiss()
        },
        title = { Text(translatedText(title)) },
        text = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fadeEdge()
                    .verticalScrollWithBar(state = scrollState)
            ) {
                Text(text = text)
            }
        },
        confirmText = confirmText,
        onConfirm = onConfirm,
        dismissText = dismissText,
        onDismiss = onDismiss,
        isDestructive = isDestructive
    )
}

@Composable
fun SideDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    fromRight: Boolean = false,
    width: Dp = 340.dp,
    verticalPadding: Dp = 32.dp,
    startText: String? = null,
    onStartClick: (() -> Unit)? = null,
    endText: String? = null,
    onEndClick: (() -> Unit)? = null,
    header: @Composable (ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismissRequest
                    )
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(
                initialOffsetX = { if (fromRight) it else -it },
                animationSpec = tween(600)
            ),
            exit = slideOutHorizontally(
                targetOffsetX = { if (fromRight) it else -it },
                animationSpec = tween(600)
            ),
            modifier = Modifier
                .align(if (fromRight) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(horizontal = 20.dp, vertical = verticalPadding)
                .fillMaxHeight()
                .width(IntrinsicSize.Min)
        ) {
            Surface(
                modifier = Modifier
                    .width(width)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(
                    when {
                        verticalPadding <= 0.dp -> 0.dp
                        verticalPadding <= 4.dp -> 16.dp
                        else -> 32.dp
                    }
                ),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 0.dp,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    header?.invoke(this)

                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        content()
                    }

                    if (startText != null || endText != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (startText != null) {
                                FilledTonalButton(
                                    modifier = Modifier.weight(1f),
                                    onClick = onStartClick ?: onDismissRequest
                                ) {
                                    Text(text = translatedText(startText))
                                }
                            }

                            if (endText != null) {
                                Button(
                                    modifier = Modifier.weight(1f),
                                    onClick = onEndClick ?: onDismissRequest
                                ) {
                                    Text(text = translatedText(endText))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialogCard(
    modifier: Modifier = Modifier,
    position: CardPosition = CardPosition.SINGLE,
    outerShape: Dp = 20.dp,
    innerShape: Dp = 6.dp,
    useSurface: Boolean = false,
    containerColor: Color? = null,
    animate: Boolean = true,
    delayIndex: Int = 0,
    content: @Composable ColumnScope.() -> Unit
) {
    val topRadius = if (position == CardPosition.TOP || position == CardPosition.SINGLE) outerShape else innerShape
    val bottomRadius = if (position == CardPosition.BOTTOM || position == CardPosition.SINGLE) outerShape else innerShape

    val isMatte = LauncherPreferences.PREF_BLURRED_ELEMENTS_ENABLED

    val cardColor = containerColor ?: if (useSurface) {
        if (isMatte) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    } else {
        if (isMatte) MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.background
    }

    val scale = remember { Animatable(if (animate) 0.95f else 1f) }
    val alpha = remember { Animatable(if (animate) 0f else 1f) }

    if (animate) {
        LaunchedEffect(Unit) {
            delay((delayIndex * 40L).milliseconds)
            launch {
                scale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                alpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 300)
                )
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.scaleX = scale.value
                this.scaleY = scale.value
                this.alpha = alpha.value
            },
        shape = RoundedCornerShape(
            topStart = topRadius,
            topEnd = topRadius,
            bottomStart = bottomRadius,
            bottomEnd = bottomRadius
        ),
        color = cardColor,
        content = {
            Box(modifier = Modifier.fillMaxWidth()) {
                if (isMatte) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .blur(12.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.1f))
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    content = content
                )
            }
        }
    )
}

enum class CardPosition {
    TOP, MIDDLE, BOTTOM, SINGLE
}

@Composable
fun DialogTitleAndSummary(
    title: String,
    summary: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.titleMedium,
    summaryStyle: TextStyle = MaterialTheme.typography.bodySmall
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = titleStyle,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (summary != null) {
            Text(
                text = summary,
                style = summaryStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogActionItem(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    enabled: Boolean = true,
    warningTooltip: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null || iconPainter != null) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Icon(
                    painter = iconPainter!!,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            DialogTitleAndSummary(title = title, summary = summary)
        }

        if (warningTooltip != null) {
            val tooltipState = rememberTooltipState()
            val scope = rememberCoroutineScope()
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text(warningTooltip) } },
                state = tooltipState
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { scope.launch { tooltipState.show() } }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogSwitchItem(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    enabled: Boolean = true,
    checked: Boolean,
    warningTooltip: String? = null,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .alpha(if (enabled) 1f else 0.5f)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null || iconPainter != null) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            } else {
                Icon(
                    painter = iconPainter!!,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            DialogTitleAndSummary(title = title, summary = summary)
        }

        if (warningTooltip != null) {
            val tooltipState = rememberTooltipState()
            val scope = rememberCoroutineScope()
            TooltipBox(
                positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                tooltip = { PlainTooltip { Text(warningTooltip) } },
                state = tooltipState
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { scope.launch { tooltipState.show() } }
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        DefaultSwitch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogSliderItem(
    title: String,
    summary: String? = null,
    icon: ImageVector? = null,
    iconPainter: Painter? = null,
    enabled: Boolean = true,
    warningTooltip: String? = null,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    onValueChange: (Float) -> Unit,
    valueSuffix: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.5f)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null || iconPainter != null) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Icon(
                        painter = iconPainter!!,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                DialogTitleAndSummary(title = title, summary = summary)
            }

            if (warningTooltip != null) {
                val tooltipState = rememberTooltipState()
                val scope = rememberCoroutineScope()
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                    tooltip = { PlainTooltip { Text(warningTooltip) } },
                    state = tooltipState
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { scope.launch { tooltipState.show() } }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        SimpleTextSlider(
            value = value,
            enabled = enabled,
            onValueChange = onValueChange,
            valueRange = valueRange,
            toInt = true,
            modifier = Modifier.fillMaxWidth(),
            suffix = valueSuffix
        )
    }
}

@Composable
fun DialogTextInput(
    title: String,
    initialValue: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialValue) }

    HyperAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = translatedText(title)) },
        text = {
            HyperOutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(translatedText(title)) },
                singleLine = true
            )
        },
        confirmText = stringResource(R.string.ok),
        onConfirm = { onConfirm(text) },
        dismissText = stringResource(R.string.cancel),
        onDismiss = onDismiss
    )
}

@Composable
private fun simpleEditDialogBody(
    title: String,
    value: String,
    onValueChange: (newValue: String) -> Unit,
    label: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = 3,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    extraBody: @Composable (() -> Unit)? = null,
    extraContent: @Composable (() -> Unit)? = null,
    onConfirm: () -> Unit = {}
): @Composable ColumnScope.() -> Unit = {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium
    )

    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fadeEdge()
            .weight(1f, fill = false)
            .verticalScrollWithBar(state = scrollState)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        extraBody?.let {
            it.invoke()
            Spacer(modifier = Modifier.size(8.dp))
        }

        val focusManager = LocalFocusManager.current

        if (singleLine) {
            SingleLineTextCheck(
            )
        }

        HyperOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onValueChange,
            label = label,
            isError = isError,
            supportingText = supportingText,
            singleLine = singleLine,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions.copy(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus(true)
                    onConfirm()
                }
            ),
            shape = MaterialTheme.shapes.large
        )
        extraContent?.invoke()
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun <T> SimpleListDialog(
    items: List<T>,
    onItemSelected: (T) -> Unit,
    onDismissRequest: (selected: Boolean) -> Unit,
    current: T? = null,
    itemLayout: @Composable (item: T, isCurrent: Boolean, onClick: () -> Unit) -> Unit,
    showConfirm: Boolean = false,
    confirmText: @Composable RowScope.() -> Unit = {
        MarqueeText(text = stringResource(R.string.ok))
    }
) {
    var selectedItem: T? by remember { mutableStateOf(current) }

    Dialog(
        onDismissRequest = {
            onDismissRequest(false)
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .heightIn(max = rememberDialogMaxHeight())
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .padding(all = 3.dp)
                    .heightIn(max = (maxHeight - 6.dp).coerceAtMost(rememberDialogMaxHeight()))
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                shadowElevation = 3.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val state = rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier
                            .fadeEdge()
                            .weight(1f, fill = false),
                        state = state
                    ) {
                        items(items) { item ->
                            val isCurrent = selectedItem == item
                            itemLayout(item, isCurrent) {
                                selectedItem = item
                                if (!showConfirm && !isCurrent) {
                                    onItemSelected(item)
                                    onDismissRequest(true)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.size(4.dp))

                    if (showConfirm) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (selectedItem != null) {
                                    onItemSelected(selectedItem!!)
                                    onDismissRequest(true)
                                }
                            }
                        ) {
                            confirmText()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeycodePickerDialog(
    title: String,
    initialValue: Int,
    onKeycodePicked: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val keyNames = remember { KeycodeUtils.generateKeyName() }
    val initialIndex = remember {
        val idx = KeycodeUtils.getIndexByValue(initialValue)
        if (idx < 0) 0 else idx
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    HyperAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = translatedText(title)) },
        text = {
            Box(modifier = Modifier.height(300.dp)) {
                LazyColumn(state = listState) {
                    itemsIndexed(keyNames) { index, name ->
                        val value = KeycodeUtils.getValueByIndex(index)
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(name)
                                    Text(
                                        text = value.toString(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            },
                            onClick = {
                                onKeycodePicked(value)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        },
        confirmText = stringResource(R.string.cancel),
        onConfirm = onDismiss
    )
}
