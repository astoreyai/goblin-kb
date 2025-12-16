package dev.kymera.keyboard.core

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.speech.RecognizerIntent
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import dev.kymera.keyboard.agents.*
import dev.kymera.keyboard.commands.SlashCommandRegistry
import dev.kymera.keyboard.communication.TermuxBridge
import dev.kymera.keyboard.layouts.LayoutManager
import dev.kymera.keyboard.settings.PreferencesManager
import dev.kymera.keyboard.ui.ThemeManager

/**
 * Main InputMethodService for the KB developer keyboard.
 *
 * Coordinates between:
 * - LayoutManager: Handles layout switching based on context
 * - ContextDetector: Determines input context (terminal, code, general)
 * - SlashCommandRegistry: Manages slash command execution
 * - KeyboardView: Renders the keyboard UI
 */
class KBKeyboardService : InputMethodService() {

    // Lazy-loaded components (following ky/ pattern)
    private val prefs by lazy { PreferencesManager(this) }
    private val contextDetector by lazy { ContextDetector() }
    private val layoutManager by lazy { LayoutManager(this) }
    private val commandRegistry by lazy { SlashCommandRegistry(this) }
    private val themeManager by lazy { ThemeManager(this) }
    private val modifiers by lazy {
        ModifierState().apply {
            setChangeListener { modifier, active ->
                when (modifier) {
                    ModifierState.Modifier.CTRL -> keyboardView?.setCtrlState(active)
                    ModifierState.Modifier.SHIFT -> keyboardView?.setShiftState(active)
                    else -> {}
                }
            }
        }
    }

    // Agentic components
    private val agentRegistry by lazy { AgentRegistry(this) }
    private val commandBus by lazy {
        AgentCommandBus(this).apply {
            setResponseListener { response ->
                handleAgentResponse(response)
            }
        }
    }

    private var keyboardView: KBKeyboardView? = null
    private var currentContext = InputContext.GENERAL
    private var splitModeEnabled: Boolean? = null  // null = auto, true = split, false = none
    private var currentLayer = SemanticLayer.TEXT
    private var isQueryActive = false

    override fun onCreateInputView(): View {
        keyboardView = KBKeyboardView(this).apply {
            setLayoutManager(layoutManager)
            setCommandRegistry(commandRegistry)
            setThemeManager(themeManager)
            setKeyListener(::onKeyPressed)
            setRowHeight(prefs.keyHeight)
        }
        return keyboardView!!
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)

        // Detect context and switch layout
        currentContext = contextDetector.detectContext(attribute)
        keyboardView?.setContext(currentContext)

        // Load appropriate layout
        val layoutName = when (currentContext) {
            InputContext.TERMINAL -> "terminal"
            InputContext.CODE -> "code"
            InputContext.AGENT_CHAT -> "code"
            InputContext.GENERAL -> "qwerty"
        }
        layoutManager.loadLayout(layoutName)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        keyboardView?.invalidate()
    }

    override fun onFinishInput() {
        super.onFinishInput()
        modifiers.resetAll()
        keyboardView?.reset()
    }

    private fun onKeyPressed(action: KeyAction) {
        when (action) {
            // Basic text actions
            is KeyAction.InsertText -> commitText(action.text)
            is KeyAction.InsertPaired -> commitPairedText(action.open, action.close)
            is KeyAction.Delete -> handleDelete()
            is KeyAction.Enter -> handleEnter()

            // Commands and layouts
            is KeyAction.SlashCommand -> handleSlashCommand(action.command)
            is KeyAction.SwitchLayout -> handleLayoutSwitch(action.layout)
            is KeyAction.SendToLocalAgent -> sendToLocalAgent(action.text, action.type)
            is KeyAction.ControlKey -> handleControlKey(action.key)

            // Theme and layout cycling
            is KeyAction.NextTheme -> themeManager.nextTheme()
            is KeyAction.NextLayout -> layoutManager.nextLayout()
            is KeyAction.ShowLayoutPicker -> {} // TODO: Show picker dialog
            is KeyAction.ShowThemePicker -> {} // TODO: Show picker dialog

            // Voice and split
            is KeyAction.VoiceInput -> startVoiceInput()
            is KeyAction.ToggleSplit -> toggleSplitMode()

            // === AGENTIC ACTIONS ===
            is KeyAction.SpawnAgent -> handleSpawnAgent(action.agentId, action.withSelection)
            is KeyAction.AgentCommand -> handleAgentCommand(action.command, action.params)
            is KeyAction.ExpandContext -> handleExpandContext()
            is KeyAction.ResetContext -> handleResetContext()
            is KeyAction.RecallMemory -> handleRecallMemory()
            is KeyAction.GenerateDoc -> handleGenerateDoc()
            is KeyAction.ExecuteRun -> handleExecuteRun()
            is KeyAction.ExecuteSimulate -> handleExecuteSimulate()
            is KeyAction.RefineSelection -> handleRefineSelection()
            is KeyAction.AgentStep -> handleAgentStep()
            is KeyAction.HoldToQuery -> handleHoldToQuery(action.isStart)
            is KeyAction.SwitchLayer -> handleSwitchLayer(action.layer)
            is KeyAction.NextAgent -> agentRegistry.nextAgent()

            // === GOBLIN FORGE ACTIONS ===
            is KeyAction.GforgeSpawn -> handleGforgeSpawn(action.goblinName, action.agent, action.project)
            is KeyAction.GforgeAttach -> handleGforgeAttach(action.goblinName)
            is KeyAction.GforgeKill -> handleGforgeKill(action.goblinName)
            is KeyAction.GforgeLogs -> handleGforgeLogs(action.goblinName, action.lines)
            is KeyAction.GforgeTop -> handleGforgeTop()
            is KeyAction.GforgeTask -> handleGforgeTask(action.task, action.goblinName)
            is KeyAction.GforgeRun -> handleGforgeRun(action.template)
            is KeyAction.GforgeList -> handleGforgeList()
            is KeyAction.GforgeStatus -> handleGforgeStatus()
            is KeyAction.GforgeDiff -> handleGforgeDiff(action.goblinName, action.staged)
        }
    }

    private fun startVoiceInput() {
        // Use Android's built-in voice input
        val voiceIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(voiceIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice input unavailable", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleSplitMode() {
        val view = keyboardView ?: return
        // Cycle through: AUTO -> SPLIT -> NONE -> AUTO
        val currentMode = when {
            splitModeEnabled == null -> KBKeyboardView.SplitMode.AUTO
            splitModeEnabled == true -> KBKeyboardView.SplitMode.SPLIT
            else -> KBKeyboardView.SplitMode.NONE
        }
        val nextMode = when (currentMode) {
            KBKeyboardView.SplitMode.AUTO -> KBKeyboardView.SplitMode.SPLIT
            KBKeyboardView.SplitMode.SPLIT -> KBKeyboardView.SplitMode.NONE
            KBKeyboardView.SplitMode.NONE -> KBKeyboardView.SplitMode.AUTO
        }
        splitModeEnabled = when (nextMode) {
            KBKeyboardView.SplitMode.AUTO -> null
            KBKeyboardView.SplitMode.SPLIT -> true
            KBKeyboardView.SplitMode.NONE -> false
        }
        view.setSplitMode(nextMode)
    }

    private fun commitText(text: String) {
        // Apply shift modifier if active (BeeRaider-style sticky shift)
        val finalText = modifiers.applyShift(text)
        currentInputConnection?.commitText(finalText, 1)
        // Reset sticky modifiers after key use
        modifiers.onKeyUsed()
        if (prefs.hapticEnabled) {
            keyboardView?.performHapticFeedback()
        }
    }

    private fun commitPairedText(open: String, close: String) {
        currentInputConnection?.apply {
            beginBatchEdit()
            // Commit open character, cursor ends after it
            commitText(open, 1)
            // Commit close character, cursor ends after it
            commitText(close, 1)
            // Move cursor back by length of close string to position between
            val extractedText = getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
            val cursorPos = extractedText?.selectionStart ?: 0
            if (cursorPos >= close.length) {
                setSelection(cursorPos - close.length, cursorPos - close.length)
            }
            endBatchEdit()
        }
    }

    private fun handleDelete() {
        currentInputConnection?.deleteSurroundingText(1, 0)
    }

    private fun handleEnter() {
        currentInputConnection?.commitText("\n", 1)
    }

    private fun handleLayoutSwitch(layout: String) {
        // Handle sticky shift toggle for *_shift layouts
        if (layout.endsWith("_shift")) {
            modifiers.toggle(ModifierState.Modifier.SHIFT)
            // Don't actually switch layout - just toggle shift state
            return
        }
        layoutManager.loadLayout(layout)
    }

    private fun handleSlashCommand(command: String) {
        val result = commandRegistry.execute(command, currentContext)
        when (result) {
            is CommandResult.InsertText -> commitText(result.text)
            is CommandResult.SendToLocalAgent -> sendToLocalAgent(result.text, result.type)
            is CommandResult.ShowPicker -> keyboardView?.showCommandPicker(result.commands)
            is CommandResult.NotFound -> {} // Silently ignore
        }
    }

    private fun sendToLocalAgent(text: String, type: String) {
        // Broadcast to local agent app (fallback when Termux unavailable)
        val intent = android.content.Intent("dev.goblin.agent.AI_REQUEST").apply {
            putExtra("text", text)
            putExtra("type", type)
            putExtra("context", currentContext.name)
        }
        sendBroadcast(intent)
    }

    private fun handleControlKey(key: String) {
        val ic = currentInputConnection ?: return

        when (key) {
            // Modifier key toggle
            "Ctrl" -> {
                modifiers.toggle(ModifierState.Modifier.CTRL)
                return
            }

            // Arrow keys with optional Ctrl modifier for word navigation
            "Left" -> {
                val meta = modifiers.getMetaState()
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT, 0, meta))
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_LEFT, 0, meta))
                modifiers.onKeyUsed()
                return
            }
            "Right" -> {
                val meta = modifiers.getMetaState()
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT, 0, meta))
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_RIGHT, 0, meta))
                modifiers.onKeyUsed()
                return
            }
            "Up" -> {
                val meta = modifiers.getMetaState()
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP, 0, meta))
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP, 0, meta))
                modifiers.onKeyUsed()
                return
            }
            "Down" -> {
                val meta = modifiers.getMetaState()
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, 0, meta))
                ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_DOWN, 0, meta))
                modifiers.onKeyUsed()
                return
            }

            // Navigation keys
            "Home" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_HOME))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_HOME))
                return
            }
            "End" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MOVE_END))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MOVE_END))
                return
            }
            "PageUp" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_UP))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_PAGE_UP))
                return
            }
            "PageDown" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_PAGE_DOWN))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_PAGE_DOWN))
                return
            }

            // Escape key
            "Esc" -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ESCAPE))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ESCAPE))
                return
            }

            // Terminal control sequences
            "C-c" -> commitText("\u0003")
            "C-d" -> commitText("\u0004")
            "C-z" -> commitText("\u001a")
            "C-l" -> commitText("\u000c")
            "C-r" -> commitText("\u0012")
            "C-a" -> commitText("\u0001")
            "C-e" -> commitText("\u0005")
            "C-w" -> commitText("\u0017")
            "Tab" -> commitText("\t")
        }
    }

    // === AGENTIC HANDLERS ===

    private fun handleSpawnAgent(agentId: String, withSelection: Boolean) {
        agentRegistry.setActiveAgent(agentId)
        val agent = agentRegistry.getAgent(agentId) ?: return

        Toast.makeText(this, "Agent: ${agent.name}", Toast.LENGTH_SHORT).show()

        if (withSelection) {
            // If there's selected text, send it to the agent
            val context = buildCommandContext()
            if (context.hasTextContext()) {
                val request = AgentRequest(
                    agentId = agentId,
                    command = "analyze",
                    context = context
                )
                commandBus.sendRequestAsync(request)
            }
        }
    }

    private fun handleAgentCommand(command: String, params: Map<String, String>) {
        val agentId = agentRegistry.getActiveAgentId() ?: run {
            Toast.makeText(this, "No active agent", Toast.LENGTH_SHORT).show()
            return
        }

        val request = AgentRequest(
            agentId = agentId,
            command = command,
            context = buildCommandContext(),
            params = params
        )
        commandBus.sendRequestAsync(request)
    }

    private fun handleExpandContext() {
        // Send expand context command to active agent
        val agentId = agentRegistry.getActiveAgentId() ?: "orchestrator"
        val request = AgentRequest(
            agentId = agentId,
            command = "expand_context",
            context = buildCommandContext()
        )
        commandBus.sendRequestAsync(request)
        Toast.makeText(this, "Expanding context...", Toast.LENGTH_SHORT).show()
    }

    private fun handleResetContext() {
        commandBus.clearMemory()
        Toast.makeText(this, "Context reset", Toast.LENGTH_SHORT).show()
    }

    private fun handleRecallMemory() {
        val lastOutput = commandBus.getLastOutput()
        if (lastOutput != null) {
            currentInputConnection?.commitText(lastOutput, 1)
        } else {
            Toast.makeText(this, "No memory to recall", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleGenerateDoc() {
        val agentId = agentRegistry.getActiveAgentId() ?: "coding"
        val request = AgentRequest(
            agentId = agentId,
            command = "generate_doc",
            context = buildCommandContext()
        )
        commandBus.sendRequestAsync(request)
        Toast.makeText(this, "Generating docs...", Toast.LENGTH_SHORT).show()
    }

    private fun handleExecuteRun() {
        val context = buildCommandContext()
        sendToLocalAgent(context.getRelevantText() ?: "", "execute")
        Toast.makeText(this, "Executing...", Toast.LENGTH_SHORT).show()
    }

    private fun handleExecuteSimulate() {
        val agentId = agentRegistry.getActiveAgentId() ?: "quant"
        val request = AgentRequest(
            agentId = agentId,
            command = "simulate",
            context = buildCommandContext()
        )
        commandBus.sendRequestAsync(request)
        Toast.makeText(this, "Simulating...", Toast.LENGTH_SHORT).show()
    }

    private fun handleRefineSelection() {
        val agentId = agentRegistry.getActiveAgentId() ?: "coding"
        val request = AgentRequest(
            agentId = agentId,
            command = "refine",
            context = buildCommandContext()
        )
        commandBus.sendRequestAsync(request)
        Toast.makeText(this, "Refining...", Toast.LENGTH_SHORT).show()
    }

    private fun handleAgentStep() {
        val agentId = agentRegistry.getActiveAgentId() ?: "orchestrator"
        val request = AgentRequest(
            agentId = agentId,
            command = "next_step",
            context = buildCommandContext()
        )
        commandBus.sendRequestAsync(request)
    }

    private fun handleHoldToQuery(isStart: Boolean) {
        isQueryActive = isStart
        if (isStart) {
            // Start voice or show query overlay
            startVoiceInput()
        }
    }

    private fun handleSwitchLayer(layer: SemanticLayer) {
        currentLayer = layer
        Toast.makeText(this, "Layer: ${layer.name}", Toast.LENGTH_SHORT).show()
        // Could switch layout based on layer
        when (layer) {
            SemanticLayer.CODE -> layoutManager.loadLayout("code")
            SemanticLayer.SHELL -> layoutManager.loadLayout("terminal")
            SemanticLayer.TEXT -> layoutManager.loadLayout("qwerty")
            else -> {} // Keep current layout
        }
    }

    private fun buildCommandContext(): CommandContext {
        return CommandContext.build(
            context = this,
            inputConnection = currentInputConnection,
            inputContext = currentContext,
            agentRegistry = agentRegistry,
            semanticLayer = currentLayer,
            lastOutput = commandBus.getLastOutput()
        )
    }

    // === GOBLIN FORGE HANDLERS ===

    private fun handleGforgeSpawn(name: String, agent: String, project: String) {
        if (!TermuxBridge.isTermuxInstalled(this)) {
            // Fall back to local agent system
            handleSpawnAgent(agent, true)
            return
        }
        TermuxBridge.spawnGoblin(this, name, agent, project)
    }

    private fun handleGforgeAttach(name: String?) {
        if (!TermuxBridge.isTermuxInstalled(this)) {
            Toast.makeText(this, "Termux required for attach", Toast.LENGTH_SHORT).show()
            return
        }
        TermuxBridge.attachGoblin(this, name)
    }

    private fun handleGforgeKill(name: String?) {
        if (!TermuxBridge.isTermuxInstalled(this)) {
            // Fall back to reset context
            handleResetContext()
            return
        }
        TermuxBridge.killGoblin(this, name)
    }

    private fun handleGforgeLogs(name: String?, lines: Int) {
        if (!TermuxBridge.isTermuxInstalled(this)) {
            handleRecallMemory()
            return
        }
        TermuxBridge.showLogs(this, name, lines)
    }

    private fun handleGforgeTop() {
        if (!TermuxBridge.isTermuxInstalled(this)) {
            Toast.makeText(this, "Termux required for dashboard", Toast.LENGTH_SHORT).show()
            return
        }
        TermuxBridge.launchTop(this)
    }

    private fun handleGforgeTask(task: String, goblinName: String?) {
        if (!TermuxBridge.isTermuxInstalled(this)) {
            // Fall back to agent command
            handleAgentCommand(task, emptyMap())
            return
        }
        TermuxBridge.sendTask(this, task, goblinName)
    }

    private fun handleGforgeRun(template: String) {
        if (!TermuxBridge.isTermuxInstalled(this)) {
            when (template) {
                "build" -> handleExecuteRun()
                "test" -> handleExecuteSimulate()
                else -> Toast.makeText(this, "Termux required", Toast.LENGTH_SHORT).show()
            }
            return
        }
        TermuxBridge.runTemplate(this, template)
    }

    private fun handleGforgeList() {
        if (!TermuxBridge.isTermuxInstalled(this)) {
            Toast.makeText(this, "Termux required", Toast.LENGTH_SHORT).show()
            return
        }
        TermuxBridge.listGoblins(this)
    }

    private fun handleGforgeStatus() {
        if (!TermuxBridge.isTermuxInstalled(this)) {
            Toast.makeText(this, "Termux required", Toast.LENGTH_SHORT).show()
            return
        }
        TermuxBridge.showStatus(this)
    }

    private fun handleGforgeDiff(name: String?, staged: Boolean) {
        if (!TermuxBridge.isTermuxInstalled(this)) {
            Toast.makeText(this, "Termux required", Toast.LENGTH_SHORT).show()
            return
        }
        TermuxBridge.showDiff(this, name, staged)
    }

    private fun handleAgentResponse(response: AgentResponse) {
        when (val result = response.result) {
            is AgentResult.Insert -> {
                currentInputConnection?.commitText(result.text, 1)
            }
            is AgentResult.Replace -> {
                // Delete selected text first, then insert
                currentInputConnection?.apply {
                    commitText("", 1) // Clear selection
                    commitText(result.text, 1)
                }
            }
            is AgentResult.Display -> {
                Toast.makeText(this, result.content, Toast.LENGTH_LONG).show()
            }
            is AgentResult.SendToLocalAgent -> {
                sendToLocalAgent(result.text, result.type)
            }
            is AgentResult.StoreMemory -> {
                // Already stored by command bus
            }
            is AgentResult.Error -> {
                Toast.makeText(this, "Error: ${result.message}", Toast.LENGTH_SHORT).show()
            }
            is AgentResult.None -> {}
        }
    }

    /**
     * Called by KeyboardBroadcastReceiver to set mode externally.
     */
    fun setMode(mode: String) {
        currentContext = InputContext.valueOf(mode.uppercase())
        keyboardView?.setContext(currentContext)
        layoutManager.loadLayout(mode.lowercase())
    }

    /**
     * Called by KeyboardBroadcastReceiver to insert text.
     */
    fun insertText(text: String) {
        commitText(text)
    }

    companion object {
        var instance: KBKeyboardService? = null
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        commandBus.initialize()
    }

    override fun onDestroy() {
        commandBus.shutdown()
        instance = null
        super.onDestroy()
    }
}
