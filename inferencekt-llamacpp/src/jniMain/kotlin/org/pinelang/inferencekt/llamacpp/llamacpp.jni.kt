package org.pinelang.inferencekt.llamacpp


private external fun nativeSystemInfo(): String
private external fun initLlamaBackend()

private external fun nativeCompletionInit(
    context: Long,
    batch: Long,
    text: String,
    nLen: Int
): Int

private external fun nativeCompletionLoop(
    context: Long,
    batch: Long,
    nLen: Int,
    ncur: Int
): String?

private external fun nativeKvCacheClear(context: Long)
private external fun nativeNewContext(model: Long): Long
private external fun nativeDeleteContext(context: Long): Unit
private external fun nativeNewBatch(nTokens: Int, embd: Int, nSeqMax: Int): Long
private external fun nativeDeleteBatch(batch: Long): Unit
private external fun nativeLoadModel(path: String): Long
private external fun nativeUnloadModel(model: Long): Unit
private external fun nativeTokenPerSecond(context: Long): Double


actual fun platformLoadModel(modelPath: String): Long = nativeLoadModel(modelPath)
actual fun platformUnloadModel(model: Long): Unit = nativeUnloadModel(model)

actual fun platformInitBackend() {
    System.loadLibrary("llamacpp")
    initLlamaBackend()
}

actual fun platformNewContext(model: Long): Long = nativeNewContext(model)
actual fun platformDeleteContext(context: Long): Unit = nativeDeleteContext(context)

actual fun platformNewBatch(nTokens: Int, embd: Int, nSeqMax: Int): Long =
    nativeNewBatch(nTokens, embd, nSeqMax)

actual fun platformDeleteBatch(batch: Long): Unit = nativeDeleteBatch(batch)

actual fun platformCompletionLoop(
    context: Long,
    batch: Long,
    nLen: Int,
    ncur: Int
): String? = nativeCompletionLoop(context, batch, nLen, ncur)

actual fun platformCompletionInit(
    context: Long,
    batch: Long,
    prompt: String,
    nLen: Int
): Int = nativeCompletionInit(context, batch, prompt, nLen)

actual fun platformKvCacheClear(context: Long) = nativeKvCacheClear(context)
actual fun platformTokensPerSecond(context: Long): Double = nativeTokenPerSecond(context)