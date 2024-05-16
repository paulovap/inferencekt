package org.pinelang.llamakt

class AndroidLLM(override val staticPath: String = "/data/data/org.pinelang.pineai/files/tinyllama-1.1b-chat-v0.3.Q4_0.gguf") : LLM() {
}
actual fun createLLM(): LLM = AndroidLLM()