package dev.kymera.keyboard.communication

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.kymera.keyboard.core.KBKeyboardService

/**
 * Receives broadcasts from external apps for keyboard control.
 *
 * Supported actions:
 * - SET_MODE: Switch keyboard layout (terminal, code, general)
 * - INSERT_TEXT: Insert text at cursor
 * - UPDATE_COMMANDS: Reload custom commands
 */
class KeyboardBroadcastReceiver : BroadcastReceiver() {

    companion object {
        // Actions
        const val ACTION_SET_MODE = "dev.kymera.keyboard.SET_MODE"
        const val ACTION_INSERT_TEXT = "dev.kymera.keyboard.INSERT_TEXT"
        const val ACTION_UPDATE_COMMANDS = "dev.kymera.keyboard.UPDATE_COMMANDS"

        // Extras
        const val EXTRA_MODE = "mode"
        const val EXTRA_TEXT = "text"
        const val EXTRA_COMMANDS = "commands"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val service = KBKeyboardService.instance ?: return

        when (intent.action) {
            ACTION_SET_MODE -> {
                val mode = intent.getStringExtra(EXTRA_MODE) ?: return
                service.setMode(mode)
            }

            ACTION_INSERT_TEXT -> {
                val text = intent.getStringExtra(EXTRA_TEXT) ?: return
                service.insertText(text)
            }

            ACTION_UPDATE_COMMANDS -> {
                // Trigger command registry reload
                // Commands can be sent as JSON in EXTRA_COMMANDS
                // or just trigger reload from preferences
            }
        }
    }
}
