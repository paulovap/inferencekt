package org.pinelang.pineai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.pinelang.inferencekt.Model
import androidx.compose.material.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import co.touchlab.kermit.Logger
import org.pinelang.inferencekt.InferenceEngine
import org.pinelang.inferencekt.llamacpp.platformTokensPerSecond

@Preview
@Composable
fun ModelDebugText(inference: InferenceEngine) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        var text = "Model: ${inference.model?.modelName ?: "Not loaded"}"
        val ts = (inference.tokensPerSecond * 10).toInt()
        text += if (ts != 0) "\nTokens per second: ${ts/10}.${ts.mod(10)} t/s" else ""
        Text(text=text, color = Color.Red)
    }
}