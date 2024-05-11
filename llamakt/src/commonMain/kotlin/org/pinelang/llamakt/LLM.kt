package org.pinelang.llamakt

external fun nativeSystemInfo(): String

expect fun initLLM(): LLM

interface LLM {

    /**
     * A native method that is implemented by the 'llamakt' native library,
     * which is packaged with this application.
     */
    fun systemInfo(): String


}