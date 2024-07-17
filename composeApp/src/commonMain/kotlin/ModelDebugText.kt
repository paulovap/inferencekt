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

@Preview
@Composable
fun ModelDebugText(model: Model) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text=model.modelName, color = Color.Red)
    }
}