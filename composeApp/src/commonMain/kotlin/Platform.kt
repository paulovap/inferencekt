package org.pinelang.pineai

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform