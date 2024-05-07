package com.pinelab.llamakt

class LLM {

    /**
     * A native method that is implemented by the 'llamakt' native library,
     * which is packaged with this application.
     */
    external fun systemInfo(): String

    companion object {
        // Used to load the 'llamakt' library on application startup.
        init {
            System.loadLibrary("llamakt")
        }
    }
}