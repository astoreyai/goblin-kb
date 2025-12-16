package dev.kymera.keyboard.agents

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Request to execute an agent command.
 */
data class AgentRequest(
    val id: String = UUID.randomUUID().toString(),
    val agentId: String,
    val command: String,
    val context: CommandContext,
    val params: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Response from an agent command.
 */
data class AgentResponse(
    val requestId: String,
    val agentId: String,
    val result: AgentResult,
    val executionTimeMs: Long,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Async bidirectional command bus for agent communication.
 * Supports both local execution and remote local agent app communication.
 */
class AgentCommandBus(private val context: Context) {

    companion object {
        // Broadcast actions for local agent communication
        const val ACTION_AGENT_REQUEST = "dev.kymera.keyboard.AGENT_REQUEST"
        const val ACTION_AGENT_RESPONSE = "dev.kymera.keyboard.AGENT_RESPONSE"

        // Intent extras
        const val EXTRA_REQUEST_ID = "request_id"
        const val EXTRA_AGENT_ID = "agent_id"
        const val EXTRA_COMMAND = "command"
        const val EXTRA_CONTEXT_TEXT = "context_text"
        const val EXTRA_CONTEXT_SELECTION = "context_selection"
        const val EXTRA_PARAMS = "params"
        const val EXTRA_RESULT_TYPE = "result_type"
        const val EXTRA_RESULT_TEXT = "result_text"
        const val EXTRA_ERROR = "error"

        // Timeout for remote requests
        const val REQUEST_TIMEOUT_MS = 30000L
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val mainHandler = Handler(Looper.getMainLooper())

    // Pending requests waiting for response
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<AgentResponse>>()

    // Event flows
    private val _responses = MutableSharedFlow<AgentResponse>(replay = 1)
    val responses: SharedFlow<AgentResponse> = _responses

    // Last output for memory recall
    private var lastOutput: String? = null

    // Response listener
    private var responseListener: ((AgentResponse) -> Unit)? = null

    // Broadcast receiver for local agent responses
    private val responseReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action == ACTION_AGENT_RESPONSE) {
                handleRemoteResponse(intent)
            }
        }
    }

    private var receiverRegistered = false

    /**
     * Initialize the command bus.
     */
    fun initialize() {
        if (!receiverRegistered) {
            val filter = IntentFilter(ACTION_AGENT_RESPONSE)
            context.registerReceiver(responseReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            receiverRegistered = true
        }
    }

    /**
     * Shutdown the command bus.
     */
    fun shutdown() {
        if (receiverRegistered) {
            context.unregisterReceiver(responseReceiver)
            receiverRegistered = false
        }
        scope.cancel()
        pendingRequests.clear()
    }

    /**
     * Set response listener for handling results.
     */
    fun setResponseListener(listener: (AgentResponse) -> Unit) {
        responseListener = listener
    }

    /**
     * Send a request and wait for response (suspending).
     */
    suspend fun sendRequest(request: AgentRequest): AgentResponse {
        val deferred = CompletableDeferred<AgentResponse>()
        pendingRequests[request.id] = deferred

        // Send to local agent
        sendToLocalAgent(request)

        // Wait with timeout
        return try {
            withTimeout(REQUEST_TIMEOUT_MS) {
                deferred.await()
            }
        } catch (e: TimeoutCancellationException) {
            pendingRequests.remove(request.id)
            AgentResponse(
                requestId = request.id,
                agentId = request.agentId,
                result = AgentResult.Error("Request timed out"),
                executionTimeMs = REQUEST_TIMEOUT_MS
            )
        }
    }

    /**
     * Send a request without waiting (fire and forget with callback).
     */
    fun sendRequestAsync(request: AgentRequest, callback: ((AgentResponse) -> Unit)? = null) {
        scope.launch {
            val response = sendRequest(request)
            callback?.invoke(response)
            responseListener?.invoke(response)

            // Store successful output for memory
            if (response.result is AgentResult.Insert || response.result is AgentResult.Replace) {
                lastOutput = when (val r = response.result) {
                    is AgentResult.Insert -> r.text
                    is AgentResult.Replace -> r.text
                    else -> null
                }
            }
        }
    }

    /**
     * Get the last agent output (for memory recall).
     */
    fun getLastOutput(): String? = lastOutput

    /**
     * Clear memory.
     */
    fun clearMemory() {
        lastOutput = null
    }

    private fun sendToLocalAgent(request: AgentRequest) {
        val intent = Intent(ACTION_AGENT_REQUEST).apply {
            setPackage("dev.goblin.agent")
            putExtra(EXTRA_REQUEST_ID, request.id)
            putExtra(EXTRA_AGENT_ID, request.agentId)
            putExtra(EXTRA_COMMAND, request.command)
            putExtra(EXTRA_CONTEXT_TEXT, request.context.getRelevantText())
            putExtra(EXTRA_CONTEXT_SELECTION, request.context.selectedText)
            // Params as JSON string
            if (request.params.isNotEmpty()) {
                putExtra(EXTRA_PARAMS, request.params.entries.joinToString(";") { "${it.key}=${it.value}" })
            }
        }
        context.sendBroadcast(intent)
    }

    private fun handleRemoteResponse(intent: Intent) {
        val requestId = intent.getStringExtra(EXTRA_REQUEST_ID) ?: return
        val agentId = intent.getStringExtra(EXTRA_AGENT_ID) ?: return
        val resultType = intent.getStringExtra(EXTRA_RESULT_TYPE) ?: "none"
        val resultText = intent.getStringExtra(EXTRA_RESULT_TEXT)
        val error = intent.getStringExtra(EXTRA_ERROR)

        val result: AgentResult = when {
            error != null -> AgentResult.Error(error)
            resultType == "insert" && resultText != null -> AgentResult.Insert(resultText)
            resultType == "replace" && resultText != null -> AgentResult.Replace(resultText)
            resultType == "display" && resultText != null -> AgentResult.Display(resultText)
            else -> AgentResult.None
        }

        val response = AgentResponse(
            requestId = requestId,
            agentId = agentId,
            result = result,
            executionTimeMs = 0 // Could calculate from stored timestamp
        )

        // Complete pending request
        pendingRequests.remove(requestId)?.complete(response)

        // Emit to flow
        scope.launch {
            _responses.emit(response)
        }

        // Notify listener
        mainHandler.post {
            responseListener?.invoke(response)
        }
    }

    /**
     * Simulate a local response (for testing or local agent execution).
     */
    fun simulateResponse(requestId: String, agentId: String, result: AgentResult) {
        val response = AgentResponse(
            requestId = requestId,
            agentId = agentId,
            result = result,
            executionTimeMs = 0
        )
        pendingRequests.remove(requestId)?.complete(response)
        scope.launch { _responses.emit(response) }
        responseListener?.invoke(response)
    }
}
