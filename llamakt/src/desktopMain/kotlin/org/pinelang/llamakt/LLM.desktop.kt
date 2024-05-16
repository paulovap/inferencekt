package org.pinelang.llamakt

class JvmLLM(override val staticPath: String = "/home/paulo/Downloads/Phi-3-mini-4k-instruct-q4.gguf") : LLM() {
}
actual fun createLLM(): LLM = JvmLLM()