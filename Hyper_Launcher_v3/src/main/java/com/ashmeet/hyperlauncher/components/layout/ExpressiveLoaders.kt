package com.ashmeet.hyperlauncher.components.layout

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingIndicatorDemo() {
    var progressValue by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progressValue,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow,
            visibilityThreshold = 1 / 1000f,
        ),
        label = "LoadingIndicatorAnimation"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LoadingIndicator()

        Spacer(Modifier.requiredHeight(20.dp))

        LoadingIndicator(
            progress = { animatedProgress }
        )

        Spacer(Modifier.requiredHeight(30.dp))
        Text("Set loading progress:")
        Slider(
            value = progressValue,
            onValueChange = { progressValue = it },
            modifier = Modifier.width(300.dp)
        )
    }
}