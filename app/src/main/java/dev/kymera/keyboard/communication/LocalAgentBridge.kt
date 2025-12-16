package dev.kymera.keyboard.communication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import dev.kymera.keyboard.agents.CommandContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Bridge for bidirectional communication with local agent app (fallback).
 * Supports sending agent commands and receiving responses.
 *
 * Note: Primary agent integration is via Goblin Forge/Termux.
 * This bridge is for local Android app agent fallback.
 */
object LocalAgentBridge {

    // Local agent app package (fallback when Termux not available)
    private const val LOCAL_AGENT_PACKAGE = "dev.goblin.agent"

    // Actions we can send to local agent
    private const val ACTION_AI_REQUEST = "dev.goblin.agent.AI_REQUEST"
    private const val ACTION_VOICE_INPUT = "dev.goblin.agent.VOICE_INPUT"
    private const val ACTION_AGENT_REQUEST = "dev.goblin.agent.AGENT_REQUEST"
    private const val ACTION_CONTEXT_EXPAND = "dev.goblin.agent.CONTEXT_EXPAND"
    private const val ACTION_CONTEXT_RESET = "dev.goblin.agent.CONTEXT_RESET"

    // Actions we receive from local agent
    const val ACTION_AGENT_RESPONSE = "dev.kymera.keyboard.AGENT_RESPONSE"
    const val ACTION_INSERT_TEXT = "dev.kymera.keyboard.INSERT_TEXT"
    const val ACTION_REPLACE_TEXT = "dev.kymera.keyboard.REPLACE_TEXT"

    // Extras
    private const val EXTRA_TEXT = "text"
    private const val EXTRA_TYPE = "type"
    private const val EXTRA_CONTEXT = "context"
    private const val EXTRA_AGENT_ID = "agent_id"
    private const val EXTRA_REQUEST_ID = "request_id"
    private const val EXTRA_COMMAND = "command"
    private const val EXTRA_PARAMS = "params"
    private const val EXTRA_CONTEXT_JSON = "context_json"
    private const val EXTRA_RESPONSE_STATUS = "status"
    private const val EXTRA_RESPONSE_DATA = "data"

    // Response listener
    private var responseListener: ((AgentBridgeResponse) -> Unit)? = null

    // JSON serializer
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // Request ID counter
    private var requestCounter = 0L

    /**
     * Register a response listener to receive agent responses.
     */
    fun setResponseListener(listener: (AgentBridgeResponse) -> Unit) {
        responseListener = listener
    }

    /**
     * Create a broadcast receiver for agent responses.
     */
    fun createResponseReceiver(): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                handleResponse(intent)
            }
        }
    }

    /**
     * Get the intent filter for the response receiver.
     */
    fun getResponseIntentFilter(): IntentFilter {
        return IntentFilter().apply {
            addAction(ACTION_AGENT_RESPONSE)
            addAction(ACTION_INSERT_TEXT)
            addAction(ACTION_REPLACE_TEXT)
        }
    }

    private fun handleResponse(intent: Intent) {
        val response = when (intent.action) {
            ACTION_AGENT_RESPONSE -> {
                AgentBridgeResponse(
                    requestId = intent.getStringExtra(EXTRA_REQUEST_ID) ?: "",
                    status = intent.getStringExtra(EXTRA_RESPONSE_STATUS) ?: "error",
                    data = intent.getStringExtra(EXTRA_RESPONSE_DATA),
                    action = ResponseAction.AGENT_RESULT
                )
            }
            ACTION_INSERT_TEXT -> {
                AgentBridgeResponse(
                    requestId = intent.getStringExtra(EXTRA_REQUEST_ID) ?: "",
                    status = "success",
                    data = intent.getStringExtra(EXTRA_TEXT),
                    action = ResponseAction.INSERT
                )
            }
            ACTION_REPLACE_TEXT -> {
                AgentBridgeResponse(
                    requestId = intent.getStringExtra(EXTRA_REQUEST_ID) ?: "",
                    status = "success",
                    data = intent.getStringExtra(EXTRA_TEXT),
                    action = ResponseAction.REPLACE
                )
            }
            else -> null
        }

        response?.let { responseListener?.invoke(it) }
    }

    /**
     * Send AI request to local agent app.
     */
    fun sendAiRequest(
        context: Context,
        text: String,
        type: String,
        inputContext: String
    ) {
        val intent = Intent(ACTION_AI_REQUEST).apply {
            setPackage(LOCAL_AGENT_PACKAGE)
            putExtra(EXTRA_TEXT, text)
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_CONTEXT, inputContext)
        }
        context.sendBroadcast(intent)
    }

    /**
     * Send agent request to local agent app with full context.
     *
     * @param context Android context
     * @param agentId The agent to invoke (coding, research, quant, orchestrator)
     * @param command The command to execute
     * @param params Additional parameters
     * @param commandContext The full command context
     * @return The request ID for tracking the response
     */
    fun sendAgentRequest(
        context: Context,
        agentId: String,
        command: String,
        params: Map<String, String> = emptyMap(),
        commandContext: CommandContext? = null
    ): String {
        val requestId = generateRequestId()

        val intent = Intent(ACTION_AGENT_REQUEST).apply {
            setPackage(LOCAL_AGENT_PACKAGE)
            putExtra(EXTRA_REQUEST_ID, requestId)
            putExtra(EXTRA_AGENT_ID, agentId)
            putExtra(EXTRA_COMMAND, command)
            putExtra(EXTRA_PARAMS, json.encodeToString(params))
            commandContext?.let {
                putExtra(EXTRA_CONTEXT_JSON, json.encodeToString(it.toSerializable()))
            }
        }
        context.sendBroadcast(intent)

        return requestId
    }

    /**
     * Expand context window (add more context to agent).
     */
    fun expandContext(context: Context, additionalContext: String) {
        val intent = Intent(ACTION_CONTEXT_EXPAND).apply {
            setPackage(LOCAL_AGENT_PACKAGE)
            putExtra(EXTRA_TEXT, additionalContext)
        }
        context.sendBroadcast(intent)
    }

    /**
     * Reset context (clear agent context window).
     */
    fun resetContext(context: Context) {
        val intent = Intent(ACTION_CONTEXT_RESET).apply {
            setPackage(LOCAL_AGENT_PACKAGE)
        }
        context.sendBroadcast(intent)
    }

    /**
     * Request voice input from local agent.
     */
    fun requestVoiceInput(context: Context) {
        val intent = Intent(ACTION_VOICE_INPUT).apply {
            setPackage(LOCAL_AGENT_PACKAGE)
        }
        context.sendBroadcast(intent)
    }

    /**
     * Check if local agent app is installed.
     */
    fun isLocalAgentInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(LOCAL_AGENT_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun generateRequestId(): String {
        return "kb_${System.currentTimeMillis()}_${++requestCounter}"
    }
}

/**
 * Response from agent bridge.
 */
data class AgentBridgeResponse(
    val requestId: String,
    val status: String,  // "success", "error", "pending"
    val data: String?,
    val action: ResponseAction
)

/**
 * Action to take with response data.
 */
enum class ResponseAction {
    INSERT,       // Insert text at cursor
    REPLACE,      // Replace selection
    AGENT_RESULT  // Full agent result (may include metadata)
}

/**
 * Serializable version of CommandContext for IPC.
 */
@Serializable
data class SerializableCommandContext(
    val selectedText: String? = null,
    val textBeforeCursor: String? = null,
    val textAfterCursor: String? = null,
    val cursorPosition: Int = 0,
    val clipboardText: String? = null,
    val inputContext: String = "GENERAL",
    val packageName: String? = null,
    val fieldHint: String? = null,
    val activeAgentId: String? = null,
    val semanticLayer: String = "TEXT",
    val lastAgentOutput: String? = null,
    val timestamp: Long = 0
)

/**
 * Extension to convert CommandContext to serializable form.
 */
fun CommandContext.toSerializable() = SerializableCommandContext(
    selectedText = selectedText,
    textBeforeCursor = textBeforeCursor,
    textAfterCursor = textAfterCursor,
    cursorPosition = cursorPosition,
    clipboardText = clipboardText,
    inputContext = inputContext.name,
    packageName = packageName,
    fieldHint = fieldHint,
    activeAgentId = activeAgentId,
    semanticLayer = semanticLayer.name,
    lastAgentOutput = lastAgentOutput,
    timestamp = timestamp
)
