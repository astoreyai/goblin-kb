package dev.kymera.keyboard.core

import android.view.inputmethod.EditorInfo

/**
 * Detects input context based on package name and editor attributes.
 * Enables automatic layout switching for different app types.
 */
class ContextDetector {

    // Terminal apps that should use terminal layout
    private val terminalApps = setOf(
        "com.termux",
        "jackpal.androidterm",
        "yarolegovich.materialterminal",
        "com.offsec.nethunter.kex",
        "com.sonelli.juicessh"
    )

    // Code editor apps that should use code layout
    private val codeEditorApps = setOf(
        "com.aide.ui",
        "com.droidide",
        "io.spck",
        "com.foxdebug.acode",
        "com.rhmsoft.edit",
        "com.jecelyin.editor2",
        "ru.iiec.pydroid3",
        "org.nickeditor",
        "com.microsoft.vscode"  // VS Code Server via web
    )

    // KYMERA app package
    private val kymeraPackage = "dev.kymera.app"

    /**
     * Detect context from EditorInfo provided by the input field.
     */
    fun detectContext(editorInfo: EditorInfo?): InputContext {
        if (editorInfo == null) return InputContext.GENERAL

        val packageName = editorInfo.packageName ?: return InputContext.GENERAL

        return when {
            // KYMERA app with specific field detection
            packageName == kymeraPackage -> detectKymeraContext(editorInfo)

            // Terminal apps
            packageName in terminalApps -> InputContext.TERMINAL

            // Code editors
            packageName in codeEditorApps -> InputContext.CODE

            // Check input type hints
            isCodeInputType(editorInfo) -> InputContext.CODE

            // Default
            else -> InputContext.GENERAL
        }
    }

    private fun detectKymeraContext(editorInfo: EditorInfo): InputContext {
        // Check field hint or ID for context
        val hint = editorInfo.hintText?.toString()?.lowercase() ?: ""
        val fieldId = editorInfo.fieldId

        return when {
            hint.contains("terminal") -> InputContext.TERMINAL
            hint.contains("code") || hint.contains("editor") -> InputContext.CODE
            hint.contains("chat") -> InputContext.KYMERA_CHAT
            else -> InputContext.KYMERA_CHAT
        }
    }

    private fun isCodeInputType(editorInfo: EditorInfo): Boolean {
        // Check if field suggests code input
        val inputType = editorInfo.inputType

        // No autocorrect + no suggestions often indicates code
        val noAutoCorrect = (inputType and EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
        val isMultiLine = (inputType and EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE) != 0

        // Check hint text for code-related keywords
        val hint = editorInfo.hintText?.toString()?.lowercase() ?: ""
        val codeHints = listOf("code", "script", "json", "yaml", "xml", "sql", "command")
        val hasCodeHint = codeHints.any { hint.contains(it) }

        return hasCodeHint || (noAutoCorrect && isMultiLine)
    }

    /**
     * Check if a package should use a specific context.
     * Called during settings to let user override auto-detection.
     */
    fun getPackageContext(packageName: String): InputContext? {
        return when (packageName) {
            in terminalApps -> InputContext.TERMINAL
            in codeEditorApps -> InputContext.CODE
            kymeraPackage -> InputContext.KYMERA_CHAT
            else -> null
        }
    }
}
