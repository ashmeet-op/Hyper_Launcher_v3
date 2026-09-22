package com.ashmeet.hyperlauncher.components.text

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.ashmeet.hyperlauncher.components.HyperAlertDialog
import com.ashmeet.hyperlauncher.theme.PojavTheme
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import net.ashmeet.hyperlauncher.R


object LegacyMigratedComponentsBridge {
    @JvmStatic
    fun setProgressLayoutContent(
        view: ComposeView,
        progressText: MutableState<String>,
        isExpanded: MutableState<Boolean>,
        tasks: List<ProgressTaskState>
    ) {
        view.setContent {
            PojavTheme {
                ProgressLayoutContent(
                    progressText = progressText.value,
                    isExpanded = isExpanded.value,
                    onExpandClick = { isExpanded.value = !isExpanded.value },
                    tasks = tasks
                )
            }
        }
    }

    @JvmStatic
    fun setVersionSelectorContent(
        view: ComposeView,
        groups: List<String>,
        groupData: List<List<String>>,
        onDismiss: () -> Unit,
        onItemClick: (Int, Int) -> Unit
    ) {
        view.setContent {
            PojavTheme {
                HyperAlertDialog(
                    onDismissRequest = onDismiss,
                    title = { Text(translatedText("Select Version")) },
                    confirmText = "Done",
                    onConfirm = onDismiss,
                    dismissText = stringResource(android.R.string.cancel),
                    onDismiss = onDismiss,
                    text = {
                        ExpandableVersionList(
                            groups = groups,
                            getItems = { group -> groupData[groups.indexOf(group)] },
                            groupContent = { group, isExpanded, onToggle ->
                                val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "rotation")
                                SimpleListItem1(
                                    text = group,
                                    onClick = onToggle,
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowDropDown,
                                            contentDescription = null,
                                            modifier = Modifier.rotate(rotation),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                )
                            },
                            itemContent = { item ->

                                var groupIdx = -1
                                var itemIdx = -1
                                for (i in groupData.indices) {
                                    if (groupData[i].contains(item)) {
                                        groupIdx = i
                                        itemIdx = groupData[i].indexOf(item)
                                        break
                                    }
                                }
                                SimpleListItem1(text = item, onClick = {
                                    onItemClick(groupIdx, itemIdx)
                                })
                            }
                        )
                    }
                )
            }
        }
    }
}


class ProgressTaskState(
    val progress: MutableState<Int>,
    val message: MutableState<String>,
)


@Composable
fun ProgressLayoutContent(
    progressText: String,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    tasks: List<ProgressTaskState>
) {
    ViewProgress(
        progressText = progressText,
        isExpanded = isExpanded,
        onExpandClick = onExpandClick
    ) {
        tasks.forEach { task ->
            TextProgressBar(
                progress = task.progress.value,
                text = task.message.value,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ViewProgress(
    progressText: String,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.padding_heavy))
                    .padding(top = dimensionResource(R.dimen.padding_heavy)),
                content = content
            )
        }


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(R.dimen._40sdp)),
            verticalAlignment = Alignment.CenterVertically
        ) {

            LoadingIndicator(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(
                        width = dimensionResource(R.dimen._40sdp),
                        height = dimensionResource(R.dimen._30sdp)
                    )
                    .padding(dimensionResource(R.dimen.padding_small)),
                color = MaterialTheme.colorScheme.primary
            )


            Text(
                text = progressText,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = dimensionResource(R.dimen._40sdp)),
                textAlign = TextAlign.Center,
                fontSize = with(LocalDensity.current) { dimensionResource(R.dimen._12ssp).toSp() },
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )


            IconButton(
                onClick = onExpandClick,
                modifier = Modifier
                    .padding(end = dimensionResource(R.dimen._8sdp))
                    .size(dimensionResource(R.dimen.padding_extra_large))
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.rotate(if (isExpanded) 180f else 0f),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}


@Composable
fun SimpleListItem1(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = dimensionResource(R.dimen.padding_input_top),
                    bottom = dimensionResource(R.dimen.padding_input_bottom)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontSize = with(LocalDensity.current) { dimensionResource(R.dimen._13ssp).toSp() },
                textAlign = TextAlign.Start,
                color = MaterialTheme.colorScheme.onSurface
            )
            trailingIcon?.invoke()
        }
    }
}


@Composable
fun CenteredTextView(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.bodyMedium
    )
}


@Composable
fun CenteredTextViewLarge(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}


@Composable
fun VersionProfileItem(
    text: String,
    icon: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val painter = when (icon) {
        is Int -> painterResource(id = icon)
        is Painter -> icon
        is Drawable -> BitmapPainter(icon.toBitmap().asImageBitmap())
        else -> painterResource(id = R.drawable.ic_hyper_full)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen._52sdp))
            .clickable(onClick = onClick)
            .padding(start = dimensionResource(R.dimen._17sdp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(R.dimen._36sdp)),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.width(dimensionResource(R.dimen._19sdp)))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
fun TextProgressBar(
    progress: Int,
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen._20sdp)),
        contentAlignment = Alignment.CenterStart
    ) {
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = dimensionResource(R.dimen._6sdp)),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@Composable
fun <G, I> ExpandableVersionList(
    groups: List<G>,
    getItems: (G) -> List<I>,
    groupContent: @Composable (G, Boolean, () -> Unit) -> Unit,
    itemContent: @Composable (I) -> Unit,
    modifier: Modifier = Modifier
) {
    val expandedStates = remember { mutableStateMapOf<Int, Boolean>() }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = dimensionResource(R.dimen._400sdp))
    ) {
        groups.forEachIndexed { index, group ->
            item(key = "group_$index") {
                val isExpanded = expandedStates[index] ?: false
                groupContent(group, isExpanded) {
                    expandedStates[index] = !isExpanded
                }
            }

            if (expandedStates[index] == true) {
                items(getItems(group)) { item ->
                    itemContent(item)
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
