package org.pinelang.inferencekt.llamacpp

import inferencekt_llamacpp.*
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.cstr
import kotlinx.cinterop.toCPointer
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

data class NativeModel(
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

actual fun platformUnloadModel(model: Long): Unit = platform_unload_model(model.toCPointer())

actual fun platformNewContext(model: Long): Long = platform_new_context(model.toCPointer()).toLong()

actual fun platformDeleteContext(context: Long): Unit = platform_delete_context(context.toCPointer())

actual fun platformNewBatch(
    nTokens: Int,
    embd: Int,
    nSeqMax: Int
): Long = platform_new_batch(nTokens, embd, nSeqMax).toLong()

actual fun platformDeleteBatch(batch: Long): Unit = platform_delete_batch(batch.toCPointer())

actual fun platformCompletionLoop(
    context: Long,
    batch: Long,
    nLen: Int,
    ncur: Int
): String? = platform_completion_loop(context.toCPointer(), batch.toCPointer(), nLen, ncur)?.toKString()

actual fun platformCompletionInit(
    context: Long,
    batch: Long,
    prompt: String,
    nLen: Int
): Int = platform_completion_init(context.toCPointer(), batch.toCPointer(), prompt, nLen)

actual fun platformKvCacheClear(context: Long){
    platform_kv_cache_clear(context.toCPointer())
}

actual fun platformTokensPerSecond(context: Long): Double {
    return platform_tokens_per_second(context.toCPointer())
}