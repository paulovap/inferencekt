package org.pinelang.llamakt

import kotlinx.coroutines.flow.Flow

sealed class Role {
    data object User: Role()
    data object Assistant: Role()
    data object System: Role()
}
data class Chat(val role: Role, val content: String)

data class Content(val chat: List<Chat>)

data class InferenceParams(
    val temperature: Double = 0.7,
    val topK: Int = 50,
    val topP: Double = 0.95,
    val maxOutputTokens: Int = 256,
    val stopSequences: List<String> = listOf()
)

interface GenText {
    suspend fun generateText(content: Content): Flow<String>
}

sealed class ModelStatus {
    data object Idle: ModelStatus()
    data object Loaded: ModelStatus()
    data class Error(val exception: Exception): ModelStatus()
}

interface Model
{
    val modelName: String
    val modelPath: String
    val isLocal: Boolean
    val config: InferenceParams

    fun formatContent(content: Content): String
}


interface InferenceEngine {
    val model: Model?
    val modelStatus: ModelStatus

    suspend fun loadModel(model: Model): ModelStatus
    suspend fun generateText(prompt: String): Flow<String>
}

class LocalInferenceLoader(
    private val model: Model,
    private val inferenceEngine: InferenceEngine
): GenText {
    val modelStatus get() = inferenceEngine.modelStatus

    suspend fun loadModel(): ModelStatus {
        if (inferenceEngine.modelStatus != ModelStatus.Idle) {
            return ModelStatus.Error(IllegalStateException("Model already Loaded"))
        }
        return inferenceEngine.loadModel(model)
    }

    override suspend fun generateText(content: Content): Flow<String> {
        return inferenceEngine.generateText(model.formatContent(content))
    }
}
