package com.ashmeet.hyperlauncher.components.menu

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DropdownMenuGroup
import androidx.compose.material3.DropdownMenuPopup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorPosition
import androidx.compose.material3.MenuDefaults
import com.ashmeet.hyperlauncher.components.HyperOutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.util.fastForEachIndexed

@Composable
fun <T> HyperDropdownTextField(
    label: String,
    items: List<T>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    itemLabel: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    properties: PopupProperties = PopupProperties(focusable = true, clippingEnabled = false),
    containerColor: Color = Color.Unspecified,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 8.dp
) {
    HyperGroupedDropdownTextField(
        label = label,
        groupedItems = remember(items) { mapOf("" to items) },
        selectedItem = selectedItem,
        onItemSelected = onItemSelected,
        itemLabel = itemLabel,
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        offset = offset,
        properties = properties,
        containerColor = containerColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation
    )
}

@Composable
fun <T> HyperSpinner(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    labelProvider: (T) -> String = { it.toString() },
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = enabled) { expanded = true },
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            shape = RoundedCornerShape(8.dp),
            border = if (enabled) null else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = labelProvider(selectedOption),
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    style = MaterialTheme.typography.bodyLarge
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenuPopup(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                MenuAnchorPosition.Below,
                DpOffset(0.dp, 0.dp)
            ),
            properties = PopupProperties(focusable = true, clippingEnabled = false),
            modifier = Modifier.width(IntrinsicSize.Max)
        ) {
            val scrollState = rememberScrollState()
            
            Surface(
                modifier = Modifier
                    .heightIn(max = 300.dp)
                    .wrapContentHeight()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .verticalScroll(scrollState)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val groupInteractionSource = remember { MutableInteractionSource() }
                        val shapes = MenuDefaults.groupShape(0, 1)
                        val stableShapes = remember(shapes) {
                            shapes.copy(inactiveShape = shapes.shape)
                        }

                        DropdownMenuGroup(
                            shapes = stableShapes,
                            interactionSource = groupInteractionSource,
                            containerColor = Color.Transparent,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                        ) {
                            options.forEachIndexed { itemIndex, option ->
                                val isSelected = option == selectedOption
                                SelectableDropdownMenuItem(
                                    selected = isSelected,
                                    text = { Text(labelProvider(option)) },
                                    onClick = {
                                        onOptionSelected(option)
                                        expanded = false
                                    },
                                    shapes = MenuDefaults.itemShape(itemIndex, options.size),
                                    colors = MenuDefaults.selectableItemColors(
                                        containerColor = Color.Transparent,
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A reusable TextField component that opens a DropdownMenu with grouped items.
 * Uses [DropdownMenuGroup] for organizational clarity.
 */
@Composable
fun <T> HyperGroupedDropdownTextField(
    label: String,
    groupedItems: Map<String, List<T>>,
    selectedItem: T?,
    onItemSelected: (T) -> Unit,
    itemLabel: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    expanded: Boolean? = null,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    properties: PopupProperties = PopupProperties(focusable = true, clippingEnabled = false),
    containerColor: Color = Color.Unspecified,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 8.dp
) {
    var internalExpanded by remember { mutableStateOf(false) }
    val isExpanded = expanded ?: internalExpanded

    Box(modifier = modifier) {
        HyperOutlinedTextField(
            value = selectedItem?.let { itemLabel(it) } ?: "",
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledBorderColor = MaterialTheme.colorScheme.outline
            )
        )

        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable {
                    val nextState = !isExpanded
                    if (onExpandedChange == null) internalExpanded = nextState
                    else onExpandedChange(nextState)
                }
        )

        DropdownMenuPopup(
            expanded = isExpanded,
            onDismissRequest = {
                if (onExpandedChange == null) internalExpanded = false
                else onExpandedChange(false)
            },
            popupPositionProvider = MenuDefaults.rememberDropdownMenuPopupPositionProvider(
                MenuAnchorPosition.Below,
                offset
            ),
            properties = properties,
            modifier = Modifier.width(IntrinsicSize.Max)
        ) {
            val scrollState = rememberScrollState()

            Surface(
                modifier = Modifier
                    .heightIn(max = 300.dp)
                    .wrapContentHeight()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = if (containerColor == Color.Unspecified) MaterialTheme.colorScheme.surface else containerColor,
                tonalElevation = tonalElevation,
                shadowElevation = shadowElevation
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .verticalScroll(scrollState)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        val groupList = groupedItems.toList()
                        val groupInteractionSource = remember { MutableInteractionSource() }
                        
                        groupList.fastForEachIndexed { index, (groupLabel, items) ->
                            val shapes = MenuDefaults.groupShape(index, groupList.size)
                            val stableShapes = remember(shapes) {
                                shapes.copy(inactiveShape = shapes.shape)
                            }

                            DropdownMenuGroup(
                                shapes = stableShapes,
                                interactionSource = groupInteractionSource,
                                containerColor = Color.Transparent,
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                            ) {
                                if (groupLabel.isNotEmpty()) {
                                    MenuDefaults.DropdownMenuGroupLabel {
                                        Text(
                                            text = groupLabel,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    HorizontalDivider(
                                        modifier = Modifier.padding(MenuDefaults.HorizontalDividerPadding)
                                    )
                                }
                                items.forEachIndexed { itemIndex, item ->
                                    val isSelected = item == selectedItem
                                    SelectableDropdownMenuItem(
                                        selected = isSelected,
                                        text = { Text(itemLabel(item)) },
                                        onClick = {
                                            onItemSelected(item)
                                            if (onExpandedChange == null) internalExpanded = false
                                            else onExpandedChange(false)
                                        },
                                        shapes = MenuDefaults.itemShape(itemIndex, items.size),
                                        colors = MenuDefaults.selectableItemColors(
                                            containerColor = Color.Transparent,
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                            if (index < groupList.size - 1) {
                                Spacer(modifier = Modifier.height(MenuDefaults.GroupSpacing))
                            }
                        }
                    }
                }
            }
        }
    }
}
