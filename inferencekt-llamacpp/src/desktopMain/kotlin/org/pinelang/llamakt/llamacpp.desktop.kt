package org.pinelang.llamakt

actual fun createDefaultModel(): Model {
    return DesktopModel()
}
// /data/data/org.pinelang.pineai/files/Phi-3-mini-4k-instruct-q4.gguf
class DesktopModel(
    override val modelName: String = "Phi-3-mini-4k-instruct-q4.gguf",
    override val modelPath: String = "/home/paulo/Downloads/$modelName",
    override val isLocal: Boolean = true,
    override val config: InferenceParams = InferenceParams()): Model {

    override fun formatContent(content: Content): String {
        val stringBuffer = StringBuffer()
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