package org.pinelang.llamakt

class AndroidLLM: LLM {
    override fun systemInfo(): String = nativeSystemInfo()
    companion object {
        // Used to load the 'llamakt' library on application startup.
        init {
            System.loadLibrary("llamakt")
        }
    }
}
actual fun initLLM(): LLM = AndroidLLM()