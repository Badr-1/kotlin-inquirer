package com.github.kinquirer.components

import com.github.kinquirer.KInquirer
import com.github.kinquirer.core.Choice
import com.github.kinquirer.core.Component
import com.github.kinquirer.core.KInquirerEvent
import com.github.kinquirer.core.KInquirerEvent.KeyPressDown
import com.github.kinquirer.core.KInquirerEvent.KeyPressEnter
import com.github.kinquirer.core.KInquirerEvent.KeyPressSpace
import com.github.kinquirer.core.KInquirerEvent.KeyPressUp
import com.github.kinquirer.core.toAnsi


internal class OrderableListComponent<T>(
    val message: String,
    val hint: String,
    val choices: MutableList<Choice<T>>,
    private val viewOptions: CheckboxViewOptions = CheckboxViewOptions()
) : Component<List<T>> {

    private var selectedIndex = -1
    private var cursorIndex = 0
    private var interacting = true
    private var value: List<T> = emptyList()
    private var errorMessage = ""
    private var infoMessage = ""

    override fun value(): List<T> {
        return value
    }

    override fun isInteracting(): Boolean {
        return interacting
    }

    override fun onEvent(event: KInquirerEvent) {
        errorMessage = ""
        when (event) {
            is KeyPressUp -> {
                cursorIndex = (cursorIndex - 1).wrap()
                updateChoices()
            }

            is KeyPressDown -> {
                cursorIndex = (cursorIndex + 1).wrap()
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

                    else -> {
                        errorMessage =
                            "You can only select one item at a time. Deselect the current item before selecting another."
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

    private fun Int.wrap(): Int = (this + choices.size) % choices.size

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
            // Error message
            if (errorMessage.isNotBlank()) {
                appendLine(errorMessage.toAnsi { bold(); fgRed() })
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
        appendCursor(currentIndex)
        appendChoice(currentIndex, choice)
        appendLine()
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
    viewOptions: CheckboxViewOptions = CheckboxViewOptions()
): List<String> {
    return promptOrderableListObject(
        message = message,
        choices = choices.map { Choice(it, it) }.toMutableList(),
        hint = hint,
        viewOptions = viewOptions,
    )
}

public fun <T> KInquirer.promptOrderableListObject(
    message: String,
    choices: MutableList<Choice<T>>,
    hint: String = "",
    viewOptions: CheckboxViewOptions = CheckboxViewOptions(),
): List<T> {
    return prompt(
        OrderableListComponent(
            message = message,
            hint = hint,
            choices = choices,
            viewOptions = viewOptions,
        )
    )
}