package dev.kymera.keyboard.agents

import android.content.Context

/**
 * Definition of an agent that can be spawned from the keyboard.
 */
data class AgentDefinition(
    val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val capabilities: List<String>,
    val defaultPromptTemplate: String
)

/**
 * Registry for managing available agents.
 * Agents are specialized AI assistants that can be spawned from keyboard shortcuts.
 */
class AgentRegistry(private val context: Context) {

    private val agents = mutableMapOf<String, AgentDefinition>()
    private var activeAgentId: String? = null
    private var changeListener: ((AgentDefinition?) -> Unit)? = null

    companion object {
        // Pre-defined agent definitions
        val CODING = AgentDefinition(
            id = "coding",
            name = "Coding",
            icon = "C",
            color = "#2196F3",
            capabilities = listOf("code_gen", "debug", "refactor", "test", "review"),
            defaultPromptTemplate = "You are a coding assistant. Help with code generation, debugging, refactoring, and testing."
        )

        val RESEARCH = AgentDefinition(
            id = "research",
            name = "Research",
            icon = "R",
            color = "#9C27B0",
            capabilities = listOf("summarize", "cite", "compare", "synthesize", "literature"),
            defaultPromptTemplate = "You are a research assistant. Help with literature review, summarization, and synthesis."
        )

        val QUANT = AgentDefinition(
            id = "quant",
            name = "Quant",
            icon = "Q",
            color = "#4CAF50",
            capabilities = listOf("backtest", "scan", "analyze", "optimize", "signals"),
            defaultPromptTemplate = "You are a quantitative analyst. Help with backtesting, signal analysis, and portfolio optimization."
        )

        val ORCHESTRATOR = AgentDefinition(
            id = "orchestrator",
            name = "Master",
            icon = "M",
            color = "#FF9800",
            capabilities = listOf("delegate", "coordinate", "pipeline", "multi-agent"),
            defaultPromptTemplate = "You coordinate multiple agents to accomplish complex tasks."
        )

        val DEFAULT_AGENTS = listOf(CODING, RESEARCH, QUANT, ORCHESTRATOR)
    }

    init {
        // Register default agents
        DEFAULT_AGENTS.forEach { agents[it.id] = it }
    }

    /**
     * Set listener for agent changes.
     */
    fun setChangeListener(listener: (AgentDefinition?) -> Unit) {
        changeListener = listener
    }

    /**
     * Get agent by ID.
     */
    fun getAgent(id: String): AgentDefinition? = agents[id]

    /**
     * Get currently active agent.
     */
    fun getActiveAgent(): AgentDefinition? = activeAgentId?.let { agents[it] }

    /**
     * Get active agent ID.
     */
    fun getActiveAgentId(): String? = activeAgentId

    /**
     * Set active agent by ID.
     */
    fun setActiveAgent(id: String?) {
        if (id == null || agents.containsKey(id)) {
            activeAgentId = id
            changeListener?.invoke(getActiveAgent())
        }
    }

    /**
     * Cycle to next agent.
     */
    fun nextAgent() {
        val ids = agents.keys.toList()
        if (ids.isEmpty()) return

        val currentIndex = activeAgentId?.let { ids.indexOf(it) } ?: -1
        val nextIndex = (currentIndex + 1) % ids.size
        setActiveAgent(ids[nextIndex])
    }

    /**
     * Get all registered agents.
     */
    fun getAllAgents(): List<AgentDefinition> = agents.values.toList()

    /**
     * Register a custom agent.
     */
    fun registerAgent(agent: AgentDefinition) {
        agents[agent.id] = agent
    }

    /**
     * Unregister an agent.
     */
    fun unregisterAgent(id: String) {
        if (id !in DEFAULT_AGENTS.map { it.id }) {
            agents.remove(id)
            if (activeAgentId == id) {
                activeAgentId = null
                changeListener?.invoke(null)
            }
        }
    }

    /**
     * Check if an agent has a specific capability.
     */
    fun hasCapability(agentId: String, capability: String): Boolean {
        return agents[agentId]?.capabilities?.contains(capability) == true
    }
}
