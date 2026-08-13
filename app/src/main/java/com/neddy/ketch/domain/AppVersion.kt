package com.neddy.ketch.domain

/**
 * Release versions are dotted numbers, tagged "v2.4" and built as "2.4".
 * Comparing them as text would put 2.10 before 2.9, so they are compared
 * component by component instead.
 */
object AppVersion {

    /** True when [candidate] is a newer release than [current]. */
    fun isNewer(candidate: String, current: String): Boolean =
        compare(parse(candidate), parse(current)) > 0

    /** "v2.4" and "2.4 " both read as 2.4; anything unparsable becomes zero. */
    fun parse(version: String): List<Int> =
        version.trim()
            .removePrefix("v")
            .removePrefix("V")
            .takeWhile { it.isDigit() || it == '.' }
            .split('.')
            .mapNotNull { it.toIntOrNull() }

    private fun compare(a: List<Int>, b: List<Int>): Int {
        val size = maxOf(a.size, b.size)
        for (index in 0 until size) {
            val left = a.getOrElse(index) { 0 }
            val right = b.getOrElse(index) { 0 }
            if (left != right) return left.compareTo(right)
        }
        return 0
    }
}
