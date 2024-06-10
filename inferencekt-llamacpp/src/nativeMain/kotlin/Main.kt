import kotlinx.cinterop.toKString
import llamakt.hi


fun main() {
    val l = hi()?.toKString()
    println("value $l")
}