package org.pinelang.pineai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.pinelang.inferencekt.Chat
import org.pinelang.inferencekt.Content
import org.pinelang.inferencekt.LocalInferenceLoader
import org.pinelang.inferencekt.ModelStatus
import org.pinelang.inferencekt.Role
import org.pinelang.inferencekt.llamacpp.LlammaCPPInferenceEngine
import org.pinelang.inferencekt.llamacpp.platformCreateDefaultModel
import kotlin.time.measureTime

@OptIn(ExperimentalResourceApi::class)
@Composable
@Preview
fun App() {
    val inference by remember { mutableStateOf(
        LocalInferenceLoader(
        model = platformCreateDefaultModel(),
        inferenceEngine = LlammaCPPInferenceEngine()
    )
    ) }
    val stringState = remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("What is the top 5 most used programming languages?") }
    val scope = rememberCoroutineScope()
    MaterialTheme {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            ModelDebugText(inference.inferenceEngine)
            TextField(value = prompt,
                onValueChange = { prompt = it })
            Button(onClick = {
                stringState.value = ""
                Logger.d { "load model clicked" }
                scope.launch {
                    Logger.i {
                        "Loaded model in ${measureTime { inference.loadModel()}}" //noop if already loaded }
                    }

                    inference.generateText(Content(listOf(Chat(Role.User, prompt)))).catch {
                            Logger.e(it) { "send() failed" }
                            stringState.value = it.message!!
                    }.collect {
                        stringState.value += it
                    }
                }
            },
                enabled = inference.modelStatus != ModelStatus.Generating) {
                Text("Generate")
            }
            Text(stringState.value)
        }
    }
}
