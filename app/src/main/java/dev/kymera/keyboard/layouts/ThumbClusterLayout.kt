package dev.kymera.keyboard.layouts

import dev.kymera.keyboard.core.KeyAction
import dev.kymera.keyboard.core.SemanticLayer

/**
 * Position of a thumb cluster (LEFT or RIGHT).
 */
enum class ClusterPosition {
    LEFT, RIGHT
}

/**
 * Size variant for thumb keys.
 */
enum class ThumbKeySize {
    SMALL,    // 0.75x normal width
    STANDARD, // 1.0x normal width
    LARGE     // 1.5x normal width
}

/**
 * A key in the thumb cluster.
 */
data class ThumbKey(
    val id: String,
    val label: String,
    val action: KeyAction,
    val size: ThumbKeySize = ThumbKeySize.STANDARD,
    val color: String? = null,  // Override theme color (hex string)
    val icon: String? = null    // Optional icon instead of label
)

/**
 * A thumb cluster configuration.
 */
data class ThumbCluster(
    val position: ClusterPosition,
    val keys: List<ThumbKey>,
    val arcRadius: Float = 100f,  // Arc curvature in dp (0 = flat row)
    val keySpacing: Float = 4f    // Spacing between keys in dp
) {
    /**
     * Get total width multiplier for layout calculation.
     */
    fun getTotalWidthMultiplier(): Float {
        return keys.sumOf { key ->
            when (key.size) {
                ThumbKeySize.SMALL -> 0.75
                ThumbKeySize.STANDARD -> 1.0
                ThumbKeySize.LARGE -> 1.5
            }
        }.toFloat()
    }
}

/**
 * Pre-defined thumb cluster configurations for JARVIS layout.
 */
object ThumbClusters {
    /**
     * Left thumb cluster - Mode switching and context controls.
     * MODE: Switch semantic layer
     * QUERY: Hold-to-query (push-to-talk for AI)
     * MEM: Recall last agent output
     * DOC: Generate documentation
     */
    val LEFT = ThumbCluster(
        position = ClusterPosition.LEFT,
        keys = listOf(
            ThumbKey(
                id = "mode",
                label = "MODE",
                action = KeyAction.SwitchLayer(SemanticLayer.TEXT),
                color = "#2196F3"
            ),
            ThumbKey(
                id = "query",
                label = "QUERY",
                action = KeyAction.HoldToQuery(true),
                color = "#9C27B0"
            ),
            ThumbKey(
                id = "mem",
                label = "MEM",
                action = KeyAction.RecallMemory,
                color = "#FF9800"
            ),
            ThumbKey(
                id = "doc",
                label = "DOC",
                action = KeyAction.GenerateDoc,
                color = "#607D8B"
            )
        )
    )

    /**
     * Right thumb cluster - Execution and refinement controls.
     * RUN: Execute current code/cell
     * SIM: Trigger simulation/backtest
     * REFINE: Refine/optimize selection
     * STEP: Advance agent pipeline
     */
    val RIGHT = ThumbCluster(
        position = ClusterPosition.RIGHT,
        keys = listOf(
            ThumbKey(
                id = "run",
                label = "RUN",
                action = KeyAction.ExecuteRun,
                color = "#4CAF50"  // Green for execute
            ),
            ThumbKey(
                id = "sim",
                label = "SIM",
                action = KeyAction.ExecuteSimulate,
                color = "#00BCD4"
            ),
            ThumbKey(
                id = "refine",
                label = "REFINE",
                action = KeyAction.RefineSelection,
                color = "#E91E63"
            ),
            ThumbKey(
                id = "step",
                label = "STEP",
                action = KeyAction.AgentStep,
                color = "#FF5722"
            )
        )
    )

    /**
     * Simple left cluster for basic split mode.
     */
    val LEFT_SIMPLE = ThumbCluster(
        position = ClusterPosition.LEFT,
        keys = listOf(
            ThumbKey("shift", "SHIFT", KeyAction.SwitchLayout("qwerty_shift")),
            ThumbKey("space", "SPACE", KeyAction.InsertText(" "), size = ThumbKeySize.LARGE)
        )
    )

    /**
     * Simple right cluster for basic split mode.
     */
    val RIGHT_SIMPLE = ThumbCluster(
        position = ClusterPosition.RIGHT,
        keys = listOf(
            ThumbKey("space", "SPACE", KeyAction.InsertText(" "), size = ThumbKeySize.LARGE),
            ThumbKey("enter", "ENTER", KeyAction.Enter),
            ThumbKey("del", "DEL", KeyAction.Delete)
        )
    )
}

/**
 * Agent row key definition for JARVIS layout.
 */
data class AgentRowKey(
    val id: String,
    val label: String,
    val agentId: String?,  // null for non-agent keys like CTX+/-
    val action: KeyAction,
    val color: String
)

/**
 * Agent row configuration.
 */
object AgentRow {
    /**
     * Left half agent row: AG1-AG4 and CTX+
     */
    val LEFT_KEYS = listOf(
        AgentRowKey("ag1", "C", "coding", KeyAction.SpawnAgent("coding"), "#2196F3"),
        AgentRowKey("ag2", "R", "research", KeyAction.SpawnAgent("research"), "#9C27B0"),
        AgentRowKey("ag3", "Q", "quant", KeyAction.SpawnAgent("quant"), "#4CAF50"),
        AgentRowKey("agm", "M", "orchestrator", KeyAction.SpawnAgent("orchestrator"), "#FF9800"),
        AgentRowKey("ctx+", "CTX+", null, KeyAction.ExpandContext, "#00BCD4")
    )

    /**
     * Right half agent row: CTX- and function keys
     */
    val RIGHT_KEYS = listOf(
        AgentRowKey("ctx-", "CTX-", null, KeyAction.ResetContext, "#F44336"),
        AgentRowKey("f1", "F1", null, KeyAction.ControlKey("F1"), "#757575"),
        AgentRowKey("f2", "F2", null, KeyAction.ControlKey("F2"), "#757575"),
        AgentRowKey("f3", "F3", null, KeyAction.ControlKey("F3"), "#757575"),
        AgentRowKey("esc", "ESC", null, KeyAction.ControlKey("Esc"), "#FF5722")
    )
}
