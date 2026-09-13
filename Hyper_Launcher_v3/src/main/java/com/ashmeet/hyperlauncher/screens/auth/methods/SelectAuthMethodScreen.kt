package com.ashmeet.hyperlauncher.screens.auth.methods

import androidx.compose.foundation.background
import com.ashmeet.hyperlauncher.utils.translation.translatedText
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ashmeet.hyperlauncher.components.button.MineButton

import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
import com.ashmeet.hyperlauncher.theme.PojavTheme
import net.ashmeet.hyperlauncher.R

@Composable
fun SelectAuthMethodScreen(
    onMicrosoftClick: () -> Unit,
    onElyByClick: () -> Unit,
    onLocalClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MineButton(
                text = translatedText(stringResource(R.string.auth_select_microsoft)),
                onClick = onMicrosoftClick,
                icon = painterResource(R.drawable.ic_auth_ms),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            MineButton(
                text = translatedText(stringResource(R.string.auth_select_elyby)),
                onClick = onElyByClick,
                icon = painterResource(R.drawable.ic_auth_elyby),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            MineButton(
                text = translatedText(stringResource(R.string.auth_select_local)),
                onClick = onLocalClick,
                icon = rememberVectorPainter(Icons.Default.SportsEsports),
                modifier = Modifier.fillMaxWidth(),
                tintIcon = true
            )
        }
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=800dp,height=400dp,dpi=420",
)
@Composable
fun SelectAuthMethodScreenPreview() {
    PojavTheme {
        SelectAuthMethodScreen(
            onMicrosoftClick = {},
            onElyByClick = {},
            onLocalClick = {}
        )
    }
}
