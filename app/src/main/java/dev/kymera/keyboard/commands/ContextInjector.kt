package dev.kymera.keyboard.commands

import android.content.ClipboardManager
import android.content.Context
import android.view.inputmethod.InputConnection
import dev.kymera.keyboard.agents.CommandContext
import dev.kymera.keyboard.core.InputContext
import dev.kymera.keyboard.core.SemanticLayer

/**
 * Extracts context from the input field for command execution.
 * Provides access to selected text, clipboard, cursor position, and surrounding text.
 */
class ContextInjector(private val context: Context) {

    private val clipboardManager by lazy {
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    }

    // Configuration
    private var maxContextChars = 500  // Max characters to extract around cursor

    /**
     * Extract full context from the current input connection.
     *
     * @param inputConnection The active input connection
     * @param inputContext The detected input context (CODE, TERMINAL, etc.)
     * @param packageName The package name of the app being typed into
     * @param fieldHint The hint text of the field (if any)
     * @param activeAgentId The currently active agent ID (if any)
     * @param semanticLayer The current semantic layer
     * @param lastAgentOutput The last output from an agent (for memory recall)
     * @return Full CommandContext for command execution
     */
    fun extractContext(
        inputConnection: InputConnection?,
        inputContext: InputContext,
        packageName: String?,
        fieldHint: String? = null,
        activeAgentId: String? = null,
        semanticLayer: SemanticLayer = SemanticLayer.TEXT,
        lastAgentOutput: String? = null
    ): CommandContext {
        val ic = inputConnection

        // Extract selected text
        val selectedText = ic?.getSelectedText(0)?.toString()

        // Extract text before cursor
        val textBeforeCursor = ic?.getTextBeforeCursor(maxContextChars, 0)?.toString()

        // Extract text after cursor
        val textAfterCursor = ic?.getTextAfterCursor(maxContextChars, 0)?.toString()

        // Calculate cursor position (approximate - based on text before cursor)
        val cursorPosition = textBeforeCursor?.length ?: 0

        // Get clipboard text
        val clipboardText = getClipboardText()

        return CommandContext(
            selectedText = selectedText,
            textBeforeCursor = textBeforeCursor,
            textAfterCursor = textAfterCursor,
            cursorPosition = cursorPosition,
            clipboardText = clipboardText,
            inputContext = inputContext,
            packageName = packageName,
            fieldHint = fieldHint,
            activeAgentId = activeAgentId,
            semanticLayer = semanticLayer,
            lastAgentOutput = lastAgentOutput,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Get the current clipboard text.
     */
    fun getClipboardText(): String? {
        return try {
            if (clipboardManager.hasPrimaryClip()) {
                clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get selected text from input connection.
     */
    fun getSelectedText(inputConnection: InputConnection?): String? {
        return inputConnection?.getSelectedText(0)?.toString()
    }

    /**
     * Get text around cursor (for context in prompts).
     *
     * @param inputConnection The active input connection
     * @param beforeChars Chars to extract before cursor
     * @param afterChars Chars to extract after cursor
     * @return Pair of (textBefore, textAfter)
     */
    fun getTextAroundCursor(
        inputConnection: InputConnection?,
        beforeChars: Int = 200,
        afterChars: Int = 200
    ): Pair<String?, String?> {
        val before = inputConnection?.getTextBeforeCursor(beforeChars, 0)?.toString()
        val after = inputConnection?.getTextAfterCursor(afterChars, 0)?.toString()
        return before to after
    }

    /**
     * Get the current line (text from last newline to next newline).
     */
    fun getCurrentLine(inputConnection: InputConnection?): String? {
        val before = inputConnection?.getTextBeforeCursor(500, 0)?.toString() ?: return null
        val after = inputConnection?.getTextAfterCursor(500, 0)?.toString() ?: ""

        // Find last newline in before text
        val lineStart = before.lastIndexOf('\n').let { if (it >= 0) it + 1 else 0 }

        // Find first newline in after text
        val lineEnd = after.indexOf('\n').let { if (it >= 0) it else after.length }

        return before.substring(lineStart) + after.substring(0, lineEnd)
    }

    /**
     * Get surrounding function/block context (for code).
     * Looks for opening/closing braces to find the enclosing block.
     */
    fun getCodeBlockContext(inputConnection: InputConnection?, maxChars: Int = 1000): String? {
        val before = inputConnection?.getTextBeforeCursor(maxChars, 0)?.toString() ?: return null
        val after = inputConnection?.getTextAfterCursor(maxChars, 0)?.toString() ?: ""

        // Find opening brace depth to locate function/block start
        var braceDepth = 0
        var blockStart = before.length

        for (i in before.indices.reversed()) {
            when (before[i]) {
                '{' -> {
                    braceDepth++
                    if (braceDepth == 1) {
                        // Found the opening brace of our block
                        // Look back for function signature
                        blockStart = before.lastIndexOf('\n', i).let { if (it >= 0) it + 1 else 0 }
                        break
                    }
                }
                '}' -> braceDepth--
            }
        }

        // Find closing brace
        braceDepth = 0
        var blockEnd = 0

        for (i in after.indices) {
            when (after[i]) {
                '}' -> {
                    braceDepth++
                    if (braceDepth == 1) {
                        blockEnd = i + 1
                        break
                    }
                }
                '{' -> braceDepth--
            }
        }

        if (blockEnd == 0) blockEnd = after.length

        return before.substring(blockStart) + after.substring(0, blockEnd)
    }

    /**
     * Set the maximum context characters to extract.
     */
    fun setMaxContextChars(chars: Int) {
        maxContextChars = chars.coerceIn(100, 2000)
    }

    /**
     * Inject placeholder values into a template string.
     *
     * Supported placeholders:
     * - {{selection}} - Selected text
     * - {{clipboard}} - Clipboard content
     * - {{line}} - Current line
     * - {{before}} - Text before cursor
     * - {{after}} - Text after cursor
     * - {{block}} - Surrounding code block
     *
     * @param template The template string with placeholders
     * @param inputConnection The active input connection
     * @return The template with placeholders replaced
     */
    fun injectPlaceholders(
        template: String,
        inputConnection: InputConnection?
    ): String {
        var result = template

        // Lazy evaluation - only extract what's needed
        if (result.contains("{{selection}}")) {
            result = result.replace("{{selection}}", getSelectedText(inputConnection) ?: "")
        }

        if (result.contains("{{clipboard}}")) {
            result = result.replace("{{clipboard}}", getClipboardText() ?: "")
        }

        if (result.contains("{{line}}")) {
            result = result.replace("{{line}}", getCurrentLine(inputConnection) ?: "")
        }

        if (result.contains("{{before}}") || result.contains("{{after}}")) {
            val (before, after) = getTextAroundCursor(inputConnection)
            result = result.replace("{{before}}", before ?: "")
            result = result.replace("{{after}}", after ?: "")
        }

        if (result.contains("{{block}}")) {
            result = result.replace("{{block}}", getCodeBlockContext(inputConnection) ?: "")
        }

        return result
    }
}
