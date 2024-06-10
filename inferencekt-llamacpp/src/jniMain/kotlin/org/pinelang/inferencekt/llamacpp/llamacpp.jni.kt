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
    ncur: IntVar
): String?

private external fun nativeKvCacheClear(context: Long)
private external fun nativeNewContext(model: Long): Long
private external fun nativeNewBatch(nTokens: Int, embd: Int, nSeqMax: Int): Long
private external fun nativeLoadModel(path: String): Long


actual fun platformLoadModel(modelPath: String): Long = nativeLoadModel(modelPath)

actual fun platformInitBackend() {
    System.loadLibrary("llamakt")
    initLlamaBackend()
}

actual fun platformNewContext(model: Long): Long = nativeNewContext(model)
actual fun platformNewBatch(nTokens: Int, embd: Int, nSeqMax: Int): Long =
    nativeNewBatch(nTokens, embd, nSeqMax)

actual fun platformCompletionLoop(
    context: Long,
    batch: Long,
    nLen: Int,
    ncur: IntVar
): String? = nativeCompletionLoop(context, batch, nLen, ncur)

actual fun platformCompletionInit(
    context: Long,
    batch: Long,
    prompt: String,
    nLen: Int
): Int = nativeCompletionInit(context, batch, prompt, nLen)

actual fun platformKvCacheClear(context: Long) = nativeKvCacheClear(context)