package org.pinelang.llamakt

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.newSingleThreadContext

expect fun createDefaultModel(): Model
expect fun platformInitBackend()

private external fun nativeSystemInfo(): String
private external fun initLlamaBackend()

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
    var value: Int = value
        private set

    fun inc() {
        value += 1
    }
}

private external fun kvCacheClear(context: Long)
private external fun newContext(model: Long): Long
private external fun newBatch(nTokens: Int, embd: Int, nSeqMax: Int): Long
private external fun nativeLoadModel(path: String): Long



data class LlamainternalPointers(
    val model: Long = 0,
    val context: Long = 0,
    val batch: Long = 0)

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
class LlammaCPPInferenceEngine(): InferenceEngine {
    override val model: Model?
        get() = _model
    private var _model: Model? = null

    override val modelStatus: ModelStatus
        get() = _modelStatus
    private var _modelStatus: ModelStatus = ModelStatus.Idle
    private val nlen = 512
    private var pointers = LlamainternalPointers()
    private val runLoop: CoroutineDispatcher = newSingleThreadContext("lammathread")

    init {
        platformInitBackend()
    }

    override suspend fun loadModel(model: Model): ModelStatus {
//        val nativeModel = nativeLoadModel(model.modelPath)
//        if (nativeModel == 0L){
//            _modelStatus = ModelStatus.Error(IllegalStateException("load_model() failed"))
//            return modelStatus
//        }
//        val context = newContext(nativeModel)
//        if (context == 0L) {
//            _modelStatus =  ModelStatus.Error(IllegalStateException("new_context() failed"))
//            return modelStatus
//        }
//
//        val batch = newBatch(512, 0, 1)
//        if (batch == 0L) {
//            _modelStatus =  ModelStatus.Error(IllegalStateException("new_batch() failed"))
//        }
//
//        Logger.i { "Loaded model $model" }
//        pointers = LlamainternalPointers(nativeModel, context, batch)
//        _modelStatus = ModelStatus.Loaded
//        _model = model
        return modelStatus
    }

    /**
     * Each model has a different input formats, so we need to apply the template
     * based on the implemented model.
     */
    override suspend fun generateText(prompt: String): Flow<String> {
        Logger.i { "generateText for input:($prompt)" }
        return flow {
            when (modelStatus) {
                is ModelStatus.Loaded -> {
                    val ncur = IntVar(
                        completionInit(
                            pointers.context,
                            pointers.batch,
                            prompt,
                            nlen
                        )
                    )
                    while (ncur.value <= nlen) {
                        val str = completionLoop(pointers.context, pointers.batch, nlen, ncur)
                        if (str == null) {
                            Logger.i { "receiving null" }
                            break
                        }
                        emit(str)
                    }
                    kvCacheClear(pointers.context)
                }

                else -> {
                    Logger.i { "Model not loaded, noop..." }
                }
            }
        }.flowOn(runLoop)
    }
}