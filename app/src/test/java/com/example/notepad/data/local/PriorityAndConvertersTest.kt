package com.example.notepad.data.local

import com.example.notepad.data.local.entity.Priority
import com.example.notepad.data.local.entity.ToDo
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Priority 列舉與 DatabaseConverters 單元測試（規範 4.2）
 */
class PriorityAndConvertersTest {

    private val converters = DatabaseConverters()

    @Test
    fun `Priority levels maintain HIGH greater than MEDIUM greater than LOW`() {
        assertEquals(3, Priority.HIGH.level)
        assertEquals(2, Priority.MEDIUM.level)
        assertEquals(1, Priority.LOW.level)
    }

    @Test
    fun `DatabaseConverters round trip works for all priorities`() {
        for (priority in Priority.entries) {
            val stored = converters.fromPriority(priority)
            val restored = converters.toPriority(stored)
            assertEquals(priority, restored)
        }
        assertEquals(Priority.MEDIUM, converters.toPriority(99))
    }

    @Test
    fun `ToDo defaults to MEDIUM priority`() {
        val todo = ToDo(title = "測試待辦")
        assertEquals(Priority.MEDIUM, todo.priority)
    }

    @Test
    fun `Priority fromString parses English and Chinese labels`() {
        assertEquals(Priority.HIGH, Priority.fromString("HIGH"))
        assertEquals(Priority.HIGH, Priority.fromString("高"))
        assertEquals(Priority.LOW, Priority.fromString("low"))
        assertEquals(Priority.LOW, Priority.fromString("低"))
        assertEquals(Priority.MEDIUM, Priority.fromString(null))
    }
}
