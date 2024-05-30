package org.pinelang.inferencekt

import org.pinelang.llamakt.Content
import org.pinelang.llamakt.InferenceParams
import org.pinelang.llamakt.Model
import org.pinelang.llamakt.Role

actual fun platformInitBackend() {
}

actual fun platformCreateDefaultModel(): Model {
    return NativeModel()
}

class NativeModel(
override val modelName: String = "Phi-3-mini-4k-instruct-q4.gguf",
override val modelPath: String = "/home/paulo/Downloads/$modelName",
override val isLocal: Boolean = true,
override val config: InferenceParams = InferenceParams()): Model {

    override fun formatContent(content: Content): String {
        val stringBuffer = StringBuilder()
        content.chat.forEach {
            when (it.role) {
                Role.User -> stringBuffer.append("<|user|>\n")
                Role.Assistant -> stringBuffer.append("<|assistant|>\n")
                Role.System -> stringBuffer.append("<|system|>\n")
            }
            stringBuffer.append(it.content)
            stringBuffer.append("<|end|>")
        }
        //TODO: move the leading part to make it optional
        stringBuffer.append("<|assistant|>")
        return stringBuffer.toString()
    }
}

actual fun platformLoadModel(modelPath: String): Long {
    TODO("Not yet implemented")
}

actual fun platformNewContext(model: Long): Long {
    TODO("Not yet implemented")
}

actual fun platformNewBatch(
    nTokens: Int,
    embd: Int,
    nSeqMax: Int
): Long {
    TODO("Not yet implemented")
}

actual fun platformCompletionLoop(
    context: Long,
    batch: Long,
    nLen: Int,
    ncur: IntVar
): String? {
    TODO("Not yet implemented")
}

actual fun platformCompletionInit(
    context: Long,
    batch: Long,
    prompt: String,
    nLen: Int
): Int {
    TODO("Not yet implemented")
}

actual fun platformKvCacheClear(context: Long) {
    TODO("Not yet implemented")
}