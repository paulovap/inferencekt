package org.pinelang.inferencekt.llamacpp

import inferencekt_llamacpp.*
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.cstr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.toLong
import org.pinelang.inferencekt.Content
import org.pinelang.inferencekt.InferenceParams
import org.pinelang.inferencekt.Model
import org.pinelang.inferencekt.Role

actual fun platformInitBackend() {
    init_llama_backend()
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

actual fun platformLoadModel(modelPath: String): Long = platform_load_model(modelPath).toLong()

actual fun platformNewContext(model: Long): Long = platform_new_context(model)

actual fun platformNewBatch(
    nTokens: Int,
    embd: Int,
    nSeqMax: Int
): Long = platform_new_batch(nTokens, embd, nSeqMax)

actual fun platformCompletionLoop(
    context: Long,
    batch: Long,
    nLen: Int,
    ncur: IntVar
): String? = platform_completion_loop(context, batch, nLen, ncur.value)?.toKString()

actual fun platformCompletionInit(
    context: Long,
    batch: Long,
    prompt: String,
    nLen: Int
): Int = platform_completion_init(context, batch, prompt, nLen)

actual fun platformKvCacheClear(context: Long){
    platform_kv_cache_clear(context)
}