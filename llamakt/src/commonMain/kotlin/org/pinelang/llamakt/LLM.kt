package org.pinelang.llamakt

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.concurrent.thread

private external fun nativeSystemInfo(): String
private external fun initLLMBackend()

private external fun completionInit(
    context: Long,
    batch: Long,
    text: String,
    nLen: Int
): Int

private external fun completionLoop(
    context: Long,
    batch: Long,
    nLen: Int,
    ncur: IntVar
): String?

private class IntVar(value: Int) {
    @Volatile
    var value: Int = value
        private set

    fun inc() {
        synchronized(this) {
            value += 1
        }
    }
}

private external fun kvCacheClear(context: Long)
private external fun newContext(model: Long): Long
private external fun newBatch(nTokens: Int, embd: Int, nSeqMax: Int): Long

private external fun nativeLoadModel(path: String): Long
internal expect fun createLLM(): LLM

sealed interface LLMState {
    data object Idle: LLMState
    data class Loaded(val model: Long, val context: Long, val batch: Long): LLMState
}
abstract class LLM(
    protected val model: Long = 0L,
    protected val context: Long = 0L,
    protected val batch: Long = 0L,
    protected val threadLocalState: ThreadLocal<LLMState> = ThreadLocal.withInitial { LLMState.Idle }

) {

    private val nlen: Int = 64
    private val runLoop: CoroutineDispatcher = Executors.newSingleThreadExecutor {
        thread(start = false, name = "LLM-RunLoop") {
            //llm.loadModel("/home/paulo/Downloads/Phi-3-mini-4k-instruct-q4.gguf")

            it.run()
        }.apply {
            uncaughtExceptionHandler = Thread.UncaughtExceptionHandler { _, exception: Throwable ->
                Logger.e(exception) { "Unhandled exception" }
            }
        }
    }.asCoroutineDispatcher()
    /**
     * A native method that is implemented by the 'llamakt' native library,
     * which is packaged with this application.
     */
    fun systemInfo(): String = nativeSystemInfo()

    suspend fun loadModel(path: String) {
        withContext(runLoop) {
            when (threadLocalState.get()) {
                is LLMState.Idle -> {
                    val model = nativeLoadModel(path)
                    if (model == 0L)  throw IllegalStateException("load_model() failed")

                    val context = newContext(model)
                    if (context == 0L) throw IllegalStateException("new_context() failed")

                    val batch = newBatch(128, 0, 1)
                    if (batch == 0L) throw IllegalStateException("new_batch() failed")

                    Logger.i { "Loaded model $path" }
                    threadLocalState.set(LLMState.Loaded(model, context, batch))
                }
                else -> throw IllegalStateException("Model already loaded")
            }
        }
    }

    fun send(message: String): Flow<String> = flow {
        Logger.i { "send called"}
        when (val state = threadLocalState.get()) {
            is LLMState.Loaded -> {
                Logger.i { "Model loaded, sending..."}
                val ncur = IntVar(completionInit(state.context, state.batch, message, nlen))
                while (ncur.value <= nlen) {
                    val str = completionLoop(state.context, state.batch, nlen, ncur)
                    if (str == null) {
                        Logger.i { "receiving null"}
                        break
                    }
                    Logger.i { "emitting $str"}
                    emit(str)
                }
                kvCacheClear(state.context)
            }
            else -> {
                Logger.i { "Model not loaded, noop..."}
            }
        }
    }.flowOn(runLoop)

    companion object {
        fun create(): LLM {
            System.loadLibrary("llamakt")
            initLLMBackend()
            val llm = createLLM()
            runBlocking {
                //llm.loadModel("/data/data/org.pinelang.pineai/files/Phi-3-mini-4k-instruct-q4.gguf")
                llm.loadModel("/home/paulo/Downloads/Phi-3-mini-4k-instruct-q4.gguf")
            }
            return llm
        }
    }

}