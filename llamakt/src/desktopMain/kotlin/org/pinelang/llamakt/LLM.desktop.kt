package org.pinelang.llamakt

class JvmLLM : LLM() {
}
actual fun createLLM(): LLM = JvmLLM()