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
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.pinelang.llamakt.Chat
import org.pinelang.llamakt.Content
import org.pinelang.llamakt.LlammaCPPInferenceEngine
import org.pinelang.llamakt.LocalInferenceLoader
import org.pinelang.llamakt.Role
import org.pinelang.llamakt.createDefaultModel
import kotlin.system.measureTimeMillis

@OptIn(ExperimentalResourceApi::class)
@Composable
@Preview
fun App() {
    val inference by remember { mutableStateOf(LocalInferenceLoader(
        model = createDefaultModel(),
        inferenceEngine = LlammaCPPInferenceEngine()
    )) }
    val stringState = remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("How to explain Internet for a medieval knight in two sentences") }
    val scope = rememberCoroutineScope()
    MaterialTheme {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            TextField(value = prompt,
                onValueChange = { prompt = it })
            Button(onClick = {
                stringState.value = ""
                Logger.d { "load model clicked" }
                scope.launch {
                    Logger.i {
                        "Loaded model in ${measureTimeMillis { inference.loadModel()}}" //noop if already loaded }
                    }

                    inference.generateText(Content(listOf(Chat(Role.User, prompt)))).catch {
                            Logger.e(it) { "send() failed" }
                            stringState.value = it.message!!
                    }.collect {
                        stringState.value += it
                    }
                }
            }) {
                Text("load model")
            }
            Text(stringState.value)
        }
    }
}
