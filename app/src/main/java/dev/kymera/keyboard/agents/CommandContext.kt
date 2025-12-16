package dev.kymera.keyboard.agents

import android.content.ClipboardManager
import android.content.Context
import android.view.inputmethod.InputConnection
import dev.kymera.keyboard.core.InputContext
import dev.kymera.keyboard.core.SemanticLayer

/**
 * Rich context for agent command execution.
 * Gathers all relevant context from the current input field.
 */
data class CommandContext(
    // Text context
    val selectedText: String?,
    val textBeforeCursor: String?,
    val textAfterCursor: String?,
    val cursorPosition: Int,

    // Clipboard
    val clipboardText: String?,

    // App context
    val inputContext: InputContext,
    val packageName: String?,
    val fieldHint: String?,

    // Agent context
    val activeAgentId: String?,
    val semanticLayer: SemanticLayer,

    // Memory
    val lastAgentOutput: String?,

    // Timestamp
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Get the most relevant text for an operation.
     * Priority: selected text > clipboard > text before cursor
     */
    fun getRelevantText(): String? {
        return selectedText?.takeIf { it.isNotBlank() }
            ?: clipboardText?.takeIf { it.isNotBlank() }
            ?: textBeforeCursor?.takeIf { it.isNotBlank() }
    }

    /**
     * Check if there's any text context available.
     */
    fun hasTextContext(): Boolean {
        return !selectedText.isNullOrBlank() ||
               !textBeforeCursor.isNullOrBlank() ||
               !clipboardText.isNullOrBlank()
    }

    /**
     * Get full surrounding text (before + after cursor).
     */
    fun getSurroundingText(): String {
        return buildString {
            textBeforeCursor?.let { append(it) }
            textAfterCursor?.let { append(it) }
        }
    }

    companion object {
        private const val MAX_CONTEXT_LENGTH = 2000

        /**
         * Build context from current input connection.
         */
        fun build(
            context: Context,
            inputConnection: InputConnection?,
            inputContext: InputContext,
            agentRegistry: AgentRegistry,
            semanticLayer: SemanticLayer,
            lastOutput: String?
        ): CommandContext {
            val ic = inputConnection

            // Get text around cursor
            val textBefore = ic?.getTextBeforeCursor(MAX_CONTEXT_LENGTH, 0)?.toString()
            val textAfter = ic?.getTextAfterCursor(MAX_CONTEXT_LENGTH, 0)?.toString()

            // Get selected text
            val selectedText = ic?.getSelectedText(0)?.toString()

            // Get cursor position
            val extracted = ic?.getExtractedText(
                android.view.inputmethod.ExtractedTextRequest(),
                0
            )
            val cursorPos = extracted?.selectionStart ?: 0

            // Get clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clipText = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()

            // Get field info
            val editorInfo = extracted?.text?.toString()

            return CommandContext(
                selectedText = selectedText,
                textBeforeCursor = textBefore,
                textAfterCursor = textAfter,
                cursorPosition = cursorPos,
                clipboardText = clipText,
                inputContext = inputContext,
                packageName = null, // Set by service
                fieldHint = null,
                activeAgentId = agentRegistry.getActiveAgentId(),
                semanticLayer = semanticLayer,
                lastAgentOutput = lastOutput
            )
        }
    }
}

/**
 * Result from an agent command execution.
 */
sealed class AgentResult {
    /** Insert text at cursor */
    data class Insert(val text: String) : AgentResult()

    /** Replace selected text */
    data class Replace(val text: String) : AgentResult()

    /** Show in popup/overlay */
    data class Display(val content: String, val title: String? = null) : AgentResult()

    /** Send to local agent app (fallback when Termux unavailable) */
    data class SendToLocalAgent(val text: String, val type: String) : AgentResult()

    /** Store in memory for later recall */
    data class StoreMemory(val content: String, val key: String? = null) : AgentResult()

    /** Error occurred */
    data class Error(val message: String) : AgentResult()

    /** No action needed */
    data object None : AgentResult()
}
