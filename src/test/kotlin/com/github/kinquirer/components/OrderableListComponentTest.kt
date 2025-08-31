package com.github.kinquirer.components

import com.github.kinquirer.core.Choice
import com.github.kinquirer.core.KInquirerEvent.*
import com.github.kinquirer.core.toAnsi
import com.github.kinquirer.core.toAnsiStr
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OrderableListComponentTest {
    private lateinit var orderableList: OrderableListComponent<String>

    @BeforeEach
    fun setup() {
        orderableList = OrderableListComponent(
            message = "hello?",
            hint = "please select something",
            choices = mutableListOf(
                Choice("A", "1"),
                Choice("B", "2"),
                Choice("C", "3"),
                Choice("D", "4"),
            ),
        )
    }


    @Test
    fun `test wrapping cursor up`() {
        orderableList.onEventSequence {
            add(KeyPressUp)
        }
        val expected = buildString {
            append("?".toAnsi { bold(); fgGreen() })
            append(" ")
            append(orderableList.message.toAnsi { bold() })
            append(" ")
            appendLine(orderableList.hint.toAnsi { fgBrightBlack() })
            appendLine("   ◯ A")
            appendLine("   ◯ B")
            appendLine("   ◯ C")
            appendLine(" ❯ ".toAnsiStr { fgBrightCyan() } + "◯ D")
        }
        assertEquals(expected, orderableList.render())
    }

    @Test
    fun `test wrapping cursor down`() {
        orderableList.onEventSequence {
            add(KeyPressDown)
            add(KeyPressDown)
            add(KeyPressDown)
            add(KeyPressDown)
        }
        val expected = buildString {
            append("?".toAnsi { bold(); fgGreen() })
            append(" ")
            append(orderableList.message.toAnsi { bold() })
            append(" ")
            appendLine(orderableList.hint.toAnsi { fgBrightBlack() })
            appendLine(" ❯ ".toAnsiStr { fgBrightCyan() } + "◯ A")
            appendLine("   ◯ B")
            appendLine("   ◯ C")
            appendLine("   ◯ D")
        }
        assertEquals(expected, orderableList.render())
    }


    @Test
    fun `test orderable list select and move down`() {
        orderableList.onEventSequence {
            add(KeyPressUp)
            add(KeyPressSpace) // select D
            add(KeyPressDown) // move to A
            add(KeyPressSpace) // move A down
            add(KeyPressEnter)
        }
        val expectedValue = listOf("4", "2", "3", "1") // D, B, C, A
        assertEquals(expectedValue, orderableList.value())
        assertFalse(orderableList.isInteracting())
    }

}