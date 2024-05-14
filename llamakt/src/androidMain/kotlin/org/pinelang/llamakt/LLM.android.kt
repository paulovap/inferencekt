package org.pinelang.llamakt

class AndroidLLM: LLM() {
}
actual fun createLLM(): LLM = AndroidLLM()