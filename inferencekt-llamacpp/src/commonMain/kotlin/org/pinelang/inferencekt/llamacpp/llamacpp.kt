package org.pinelang.inferencekt.llamacpp

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.newSingleThreadContext
import org.pinelang.inferencekt.InferenceEngine
import org.pinelang.inferencekt.Model
import org.pinelang.inferencekt.ModelStatus

expect fun platformCreateDefaultModel(): Model
expect fun platformInitBackend()
expect fun platformLoadModel(modelPath: String): Long
expect fun platformNewContext(model: Long): Long
expect fun platformNewBatch(nTokens: Int, embd: Int, nSeqMax: Int): Long
expect fun platformCompletionLoop(context: Long, batch: Long, nLen: Int, ncur: IntVar): String?
expect fun platformCompletionInit(context: Long, batch: Long, prompt: String, nLen: Int): Int
expect fun platformKvCacheClear(context: Long)

class IntVar(value: Int) {
    var value: Int = value
        private set

    fun inc() {
        value += 1
    }
}

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
        val nativeModel = platformLoadModel(model.modelPath)
        if (nativeModel == 0L){
            _modelStatus = ModelStatus.Error(IllegalStateException("load_model() failed"))
            return modelStatus
        }
        val context = platformNewContext(nativeModel)
        if (context == 0L) {
            _modelStatus =  ModelStatus.Error(IllegalStateException("new_context() failed"))
            return modelStatus
        }

        val batch = platformNewBatch(512, 0, 1)
        if (batch == 0L) {
            _modelStatus =  ModelStatus.Error(IllegalStateException("new_batch() failed"))
        }

        Logger.i { "Loaded model $model" }
        pointers = LlamainternalPointers(nativeModel, context, batch)
        _modelStatus = ModelStatus.Loaded
        _model = model
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
                        platformCompletionInit(
                            pointers.context,
                            pointers.batch,
                            prompt,
                            nlen
                        )
                    )
                    while (ncur.value <= nlen) {
                        val str =
                            platformCompletionLoop(pointers.context, pointers.batch, nlen, ncur)
                                ?: break
                        println(str)
                        emit(str)
                    }
                    platformKvCacheClear(pointers.context)
                }

                else -> {
                    Logger.i { "Model not loaded, noop..." }
                }
            }
        }.flowOn(runLoop)
    }
}