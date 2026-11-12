package com.mutantcat.dailydiet

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 覆盖 [com.mutantcat.dailydiet.data.repository.FoodRepository.escapeLike] 的转义规则。
 * 该函数是 private，这里用同样的实现做契约测试，避免 LIKE 通配符回归。
 *
 * 断言统一用 buildString + repeat 构造期望值，避免在源码里数反斜杠。
 */
class FoodSearchEscapeTest {

    private val bs = '\\'.code.toChar()
    private val pct = '%'
    private val underscore = '_'

    private fun escapeLike(query: String): String = query
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

    /** n 个连续反斜杠。 */
    private fun backslashes(n: Int): String = buildString { repeat(n) { append(bs) } }

    @Test
    fun `plain query is untouched`() {
        assertEquals("馒头", escapeLike("馒头"))
    }

    @Test
    fun `wildcards are escaped`() {
        assertEquals(backslashes(1) + pct, escapeLike(pct.toString()))
        assertEquals(backslashes(1) + underscore, escapeLike(underscore.toString()))
        assertEquals("100" + backslashes(1) + pct, escapeLike("100$pct"))
    }

    @Test
    fun `backslash is escaped first so it cannot double escape`() {
        // 1 个反斜杠 -> 2 个
        assertEquals(backslashes(2), escapeLike(backslashes(1)))
        // 1 个反斜杠 + % -> 先扩成 2 个反斜杠，再给 % 加 1 个前缀，共 3 个
        assertEquals(backslashes(3) + pct, escapeLike(backslashes(1) + pct))
    }

    @Test
    fun `round trip leaves no bare wildcard`() {
        val escaped = escapeLike("a${underscore}b${pct}c${backslashes(1)}d")
        var index = 0
        while (index < escaped.length) {
            if (escaped[index] == bs) {
                index += 2
                continue
            }
            require(escaped[index] != pct && escaped[index] != underscore) {
                "escapeLike 漏掉了裸通配符: $escaped"
            }
            index += 1
        }
    }
}
