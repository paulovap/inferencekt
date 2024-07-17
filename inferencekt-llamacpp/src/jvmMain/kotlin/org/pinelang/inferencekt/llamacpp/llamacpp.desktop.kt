package org.pinelang.inferencekt.llamacpp

import org.pinelang.inferencekt.Content
import org.pinelang.inferencekt.InferenceParams
import org.pinelang.inferencekt.Model
import org.pinelang.inferencekt.Role

actual fun platformCreateDefaultModel(): Model {
    return DesktopModel()
}

// /data/data/org.pinelang.pineai/files/Phi-3-mini-4k-instruct-q4.gguf
class DesktopModel(
    override val modelName: String = "Phi-3-mini-4k-instruct-q4.gguf",
    override val modelPath: String = "/home/paulo/Downloads/$modelName",
    override val isLocal: Boolean = true,
    override val config: InferenceParams = InferenceParams()
): Model {

    override fun formatContent(content: Content): String {
        // <|user|>\nHow to explain Internet for a medieval knight?\n<|end|>\n<|assistant|>
        val stringBuffer = StringBuffer()
        content.chat.forEach {
            when (it.role) {
                Role.User -> stringBuffer.append("<|user|>\n")
                Role.Assistant -> stringBuffer.append("<|assistant|>\n")
                Role.System -> stringBuffer.append("<|system|>\n")
            }
            stringBuffer.append(it.content)
            stringBuffer.append("\n<|end|>")
        }
        //TODO: move the leading part to make it optional
        stringBuffer.append("\n<|assistant|>")
        return stringBuffer.toString()
    }
}