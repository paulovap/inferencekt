package org.pinelang.pineai

import androidx.compose.foundation.horizontalScroll
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.pinelang.llamakt.LLM

@OptIn(ExperimentalResourceApi::class)
@Composable
@Preview
fun App() {
    val llm by remember { mutableStateOf(LLM.create()) }
    val stringState = remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("<|user|>\n" +
            "How to explain Internet for a medieval knight in two sentences?<|end|>\n" +
            "<|assistant|>") }
    val scope = rememberCoroutineScope()
    var flow: Flow<String> = emptyFlow()
    MaterialTheme {
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            TextField(value = prompt,
                onValueChange = { prompt = it })
            Button(onClick = {
                Logger.d { "load model clicked" }
                scope.launch {
                    llm.send(prompt).catch {
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
