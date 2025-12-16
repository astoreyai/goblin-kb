package dev.kymera.keyboard.communication

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast

/**
 * Bridge for communication with Termux app.
 * Uses Termux's RUN_COMMAND intent to execute gforge commands.
 *
 * Requirements:
 * 1. Termux must be installed (com.termux)
 * 2. Termux:API plugin recommended for advanced features
 * 3. gforge must be installed in Termux (~/.local/bin/gforge)
 * 4. App must have com.termux.permission.RUN_COMMAND permission
 *
 * Setup in Termux:
 *   pkg install tmux git golang
 *   cd ~/projects/screener && make build-linux-arm64
 *   cp bin/gforge-linux-arm64 ~/.local/bin/gforge
 *   chmod +x ~/.local/bin/gforge
 */
object TermuxBridge {

    // Termux package and intent constants
    private const val TERMUX_PACKAGE = "com.termux"
    private const val TERMUX_SERVICE = "com.termux.app.RunCommandService"
    private const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"

    // Intent extras (Termux RUN_COMMAND API)
    private const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    private const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    private const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    private const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    private const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"

    // Permission required for RUN_COMMAND
    const val PERMISSION_RUN_COMMAND = "com.termux.permission.RUN_COMMAND"

    // gforge binary path in Termux
    private const val GFORGE_PATH = "/data/data/com.termux/files/home/.local/bin/gforge"
    private const val GFORGE_ALT_PATH = "/data/data/com.termux/files/usr/bin/gforge"

    // Default working directory (Termux home)
    private const val TERMUX_HOME = "/data/data/com.termux/files/home"

    // Session actions
    private const val SESSION_ACTION_SWITCH = 0  // Switch to session
    private const val SESSION_ACTION_NEW = 1     // Create new session

    // Track active goblin name
    private var activeGoblinName: String? = null

    /**
     * Check if Termux is installed.
     */
    fun isTermuxInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Check if we have permission to run Termux commands.
     */
    fun hasRunCommandPermission(context: Context): Boolean {
        return context.checkSelfPermission(PERMISSION_RUN_COMMAND) ==
                PackageManager.PERMISSION_GRANTED
    }

    /**
     * Run a gforge command in Termux.
     *
     * @param context Android context
     * @param args Command arguments (e.g., ["spawn", "coder", "--agent", "claude"])
     * @param background Run in background (true) or foreground with terminal (false)
     * @param workdir Working directory for the command
     */
    fun runGforgeCommand(
        context: Context,
        args: List<String>,
        background: Boolean = true,
        workdir: String = TERMUX_HOME
    ): Boolean {
        if (!isTermuxInstalled(context)) {
            Toast.makeText(context, "Termux not installed", Toast.LENGTH_SHORT).show()
            return false
        }

        val intent = Intent(ACTION_RUN_COMMAND).apply {
            setClassName(TERMUX_PACKAGE, TERMUX_SERVICE)
            putExtra(EXTRA_COMMAND_PATH, GFORGE_PATH)
            putExtra(EXTRA_ARGUMENTS, args.toTypedArray())
            putExtra(EXTRA_WORKDIR, workdir)
            putExtra(EXTRA_BACKGROUND, background)
            if (!background) {
                putExtra(EXTRA_SESSION_ACTION, SESSION_ACTION_SWITCH)
            }
        }

        return try {
            context.startService(intent)
            true
        } catch (e: SecurityException) {
            Toast.makeText(context, "Termux permission denied", Toast.LENGTH_SHORT).show()
            false
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to run gforge: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    // === HIGH-LEVEL GOBLIN OPERATIONS ===

    /**
     * Spawn a new goblin (agent instance).
     *
     * @param name Goblin name (e.g., "coder", "reviewer")
     * @param agent Agent type (claude, codex, gemini, ollama)
     * @param project Project path (relative to Termux home or absolute)
     */
    fun spawnGoblin(
        context: Context,
        name: String,
        agent: String = "claude",
        project: String = "."
    ): Boolean {
        val args = mutableListOf("spawn", name, "--agent", agent)
        if (project != ".") {
            args.addAll(listOf("--project", project))
        }

        val success = runGforgeCommand(context, args, background = true)
        if (success) {
            activeGoblinName = name
            Toast.makeText(context, "Spawning goblin: $name ($agent)", Toast.LENGTH_SHORT).show()
        }
        return success
    }

    /**
     * Attach to a goblin's tmux session (opens Termux).
     */
    fun attachGoblin(context: Context, name: String? = null): Boolean {
        val goblinName = name ?: activeGoblinName
        if (goblinName == null) {
            Toast.makeText(context, "No active goblin", Toast.LENGTH_SHORT).show()
            return false
        }

        // Attach opens Termux terminal
        return runGforgeCommand(
            context,
            listOf("attach", goblinName),
            background = false  // Need foreground for tmux attach
        )
    }

    /**
     * Stop a goblin gracefully.
     */
    fun stopGoblin(context: Context, name: String? = null): Boolean {
        val goblinName = name ?: activeGoblinName ?: return false
        val success = runGforgeCommand(context, listOf("stop", goblinName))
        if (success && goblinName == activeGoblinName) {
            activeGoblinName = null
        }
        return success
    }

    /**
     * Kill a goblin and cleanup resources.
     */
    fun killGoblin(context: Context, name: String? = null): Boolean {
        val goblinName = name ?: activeGoblinName
        if (goblinName == null) {
            Toast.makeText(context, "No active goblin to kill", Toast.LENGTH_SHORT).show()
            return false
        }

        val success = runGforgeCommand(context, listOf("kill", goblinName))
        if (success) {
            Toast.makeText(context, "Killed: $goblinName", Toast.LENGTH_SHORT).show()
            if (goblinName == activeGoblinName) {
                activeGoblinName = null
            }
        }
        return success
    }

    /**
     * Show goblin logs.
     */
    fun showLogs(context: Context, name: String? = null, lines: Int = 50): Boolean {
        val goblinName = name ?: activeGoblinName
        if (goblinName == null) {
            Toast.makeText(context, "No active goblin", Toast.LENGTH_SHORT).show()
            return false
        }

        // Logs need foreground to display
        return runGforgeCommand(
            context,
            listOf("logs", goblinName, "-n", lines.toString()),
            background = false
        )
    }

    /**
     * Launch gforge TUI dashboard.
     */
    fun launchTop(context: Context): Boolean {
        return runGforgeCommand(
            context,
            listOf("top"),
            background = false  // TUI needs foreground
        )
    }

    /**
     * Send a task to a goblin.
     */
    fun sendTask(context: Context, task: String, goblinName: String? = null): Boolean {
        val name = goblinName ?: activeGoblinName
        if (name == null) {
            Toast.makeText(context, "No active goblin for task", Toast.LENGTH_SHORT).show()
            return false
        }

        val success = runGforgeCommand(
            context,
            listOf("task", task, "--goblin", name)
        )
        if (success) {
            Toast.makeText(context, "Task → $name", Toast.LENGTH_SHORT).show()
        }
        return success
    }

    /**
     * Run a template command (build, test, dev).
     */
    fun runTemplate(context: Context, template: String): Boolean {
        return runGforgeCommand(context, listOf("run", template))
    }

    /**
     * List all goblins (opens Termux to show).
     */
    fun listGoblins(context: Context): Boolean {
        return runGforgeCommand(
            context,
            listOf("list"),
            background = false
        )
    }

    /**
     * Show system status.
     */
    fun showStatus(context: Context): Boolean {
        return runGforgeCommand(
            context,
            listOf("status"),
            background = false
        )
    }

    /**
     * Show diff for a goblin.
     */
    fun showDiff(context: Context, name: String? = null, staged: Boolean = false): Boolean {
        val goblinName = name ?: activeGoblinName ?: return false
        val args = mutableListOf("diff", goblinName)
        if (staged) args.add("--staged")
        return runGforgeCommand(context, args, background = false)
    }

    // === STATE MANAGEMENT ===

    /**
     * Get the currently active goblin name.
     */
    fun getActiveGoblin(): String? = activeGoblinName

    /**
     * Set the active goblin (for UI tracking).
     */
    fun setActiveGoblin(name: String?) {
        activeGoblinName = name
    }

    /**
     * Clear the active goblin.
     */
    fun clearActiveGoblin() {
        activeGoblinName = null
    }

    // === ADVANCED OPERATIONS ===

    /**
     * Scan for installed agents.
     */
    fun scanAgents(context: Context): Boolean {
        return runGforgeCommand(
            context,
            listOf("agents", "scan"),
            background = false
        )
    }

    /**
     * List available agents.
     */
    fun listAgents(context: Context): Boolean {
        return runGforgeCommand(
            context,
            listOf("agents", "list"),
            background = false
        )
    }

    /**
     * Initialize gforge configuration.
     */
    fun initConfig(context: Context): Boolean {
        return runGforgeCommand(context, listOf("config", "init"))
    }

    /**
     * Open Termux directly (fallback when gforge not available).
     */
    fun openTermux(context: Context): Boolean {
        if (!isTermuxInstalled(context)) {
            Toast.makeText(context, "Termux not installed", Toast.LENGTH_SHORT).show()
            return false
        }

        val intent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
        return if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } else {
            false
        }
    }
}
