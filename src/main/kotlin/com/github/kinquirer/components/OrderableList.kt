package com.github.kinquirer.components

import com.github.kinquirer.KInquirer
import com.github.kinquirer.components.MoveDirection.DOWN
import com.github.kinquirer.components.MoveDirection.UP
import com.github.kinquirer.core.Choice
import com.github.kinquirer.core.Component
import com.github.kinquirer.core.KInquirerEvent
import com.github.kinquirer.core.KInquirerEvent.KeyPressDown
import com.github.kinquirer.core.KInquirerEvent.KeyPressEnter
import com.github.kinquirer.core.KInquirerEvent.KeyPressSpace
import com.github.kinquirer.core.KInquirerEvent.KeyPressUp
import com.github.kinquirer.core.toAnsi
import kotlin.math.max
import kotlin.math.min

private enum class MoveDirection(val value: Int) {
    UP(-1), DOWN(1)
}

internal class OrderableListComponent<T>(
    val message: String,
    val hint: String,
    val choices: MutableList<Choice<T>>,
    private val pageSize: Int = Int.MAX_VALUE,
    private val viewOptions: CheckboxViewOptions = CheckboxViewOptions()
) : Component<List<T>> {

    private var selectedIndex = -1
    private var cursorIndex = 0
    private var interacting = true
    private var value: List<T> = emptyList()
    private var infoMessage = ""
    private var windowPageStartIndex = 0

    init {
        if (pageSize < choices.size) {
            infoMessage = "(move up and down to reveal more choices)"
        }
    }

    override fun value(): List<T> {
        return value
    }

    override fun isInteracting(): Boolean {
        return interacting
    }

    override fun onEvent(event: KInquirerEvent) {
        when (event) {
            is KeyPressUp -> {
                moveCursor(UP)
                updateChoices()
            }

            is KeyPressDown -> {
                moveCursor(DOWN)
                updateChoices()
            }

            is KeyPressSpace -> {
                when {
                    selectedIndex == -1 -> {
                        selectedIndex = cursorIndex
                    }

                    cursorIndex == selectedIndex -> {
                        selectedIndex = -1
                    }
                }
            }

            is KeyPressEnter -> {
                interacting = false
                value = choices.map { choice -> choice.data }
            }

            else -> {}
        }
    }

    private fun moveCursor(direction: MoveDirection) {
        val moved = (cursorIndex + direction.value + choices.size) % choices.size
        when (direction) {
            UP -> {
                if (cursorIndex == 0 && moved == choices.size - 1) { // wrapping up to bottom
                    windowPageStartIndex = max(0, choices.size - pageSize)
                } else if (moved < windowPageStartIndex) { // normal up
                    windowPageStartIndex = max(0, windowPageStartIndex - 1)
                }
            }

            DOWN -> {
                if (cursorIndex == choices.size - 1 && moved == 0) { // wrapping down to top
                    windowPageStartIndex = 0
                } else if (moved > windowPageStartIndex + pageSize - 1) { // normal down
                    windowPageStartIndex = min(choices.size - pageSize, windowPageStartIndex + 1)
                }
            }
        }
        cursorIndex = moved
    }

    private fun updateChoices() {
        if (selectedIndex != -1) {
            val temp = choices[selectedIndex]
            choices[selectedIndex] = choices[cursorIndex]
            choices[cursorIndex] = temp
            selectedIndex = cursorIndex
        }
    }

    override fun render(): String = buildString {
        // Question mark character
        append(viewOptions.questionMarkPrefix)
        append(" ")

        // Message
        append(message.toAnsi { bold() })
        append(" ")

        if (interacting) {
            // Hint
            if (hint.isNotBlank()) {
                append(hint.toAnsi { fgBrightBlack() })
            }
            appendLine()
            // Choices
            choices.forEachIndexed { index, choice ->
                appendListRow(index, choice)
            }
            // Info message
            if (infoMessage.isNotBlank()) {
                appendLine(infoMessage.toAnsi { fgBrightBlack() })
            }
        } else {
            // Results
            appendLine(
                choices.joinToString(", ") { choice -> choice.displayName }
                    .toAnsi { fgCyan(); bold() }
            )
        }
    }

    private fun StringBuilder.appendListRow(currentIndex: Int, choice: Choice<T>) {
        if (currentIndex in windowPageStartIndex until windowPageStartIndex + pageSize) {
            appendCursor(currentIndex)
            appendChoice(currentIndex, choice)
            appendLine()
        }
    }

    private fun StringBuilder.appendCursor(currentIndex: Int) {
        if (currentIndex == cursorIndex) {
            append(viewOptions.cursor)
        } else {
            append(viewOptions.nonCursor)
        }
        if (currentIndex == selectedIndex) {
            append(viewOptions.checked)
        } else {
            append(viewOptions.unchecked)
        }
    }

    private fun StringBuilder.appendChoice(currentIndex: Int, choice: Choice<T>) {
        if (currentIndex == selectedIndex) {
            append(choice.displayName.toAnsi { fgCyan(); bold() })
        } else {
            append(choice.displayName)
        }
    }

}


public fun KInquirer.promptOrderableList(
    message: String,
    choices: MutableList<String>,
    hint: String = "",
    pageSize: Int = 10,
    viewOptions: CheckboxViewOptions = CheckboxViewOptions()
): List<String> {
    return promptOrderableListObject(
        message = message,
        choices = choices.map { Choice(it, it) }.toMutableList(),
        hint = hint,
        pageSize = pageSize,
        viewOptions = viewOptions,
    )
}

public fun <T> KInquirer.promptOrderableListObject(
    message: String,
    choices: MutableList<Choice<T>>,
    hint: String = "",
    pageSize: Int = 10,
    viewOptions: CheckboxViewOptions = CheckboxViewOptions(),
): List<T> {
    return prompt(
        OrderableListComponent(
            message = message,
            hint = hint,
            choices = choices,
            pageSize = pageSize,
            viewOptions = viewOptions,
        )
    )
}