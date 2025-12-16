package dev.kymera.keyboard.core

/**
 * Unified state machine for keyboard modifiers (Ctrl, Shift, Alt, Meta).
 *
 * Supports three modifier behaviors:
 * - NORMAL: Modifier released after use (standard behavior)
 * - STICKY: Modifier stays active for one keypress, then resets (BeeRaider-style)
 * - LOCKED: Modifier stays active until toggled off (Caps Lock style)
 *
 * Thread-safe for use from IME callbacks.
 */
class ModifierState {

    enum class Modifier {
        CTRL, SHIFT, ALT, META
    }

    enum class Mode {
        NORMAL,  // Released immediately after use
        STICKY,  // Active for one keypress
        LOCKED   // Active until toggled off
    }

    data class State(
        val active: Boolean = false,
        val mode: Mode = Mode.STICKY
    )

    private val states = mutableMapOf(
        Modifier.CTRL to State(),
        Modifier.SHIFT to State(),
        Modifier.ALT to State(),
        Modifier.META to State()
    )

    private var changeListener: ((Modifier, Boolean) -> Unit)? = null

    /**
     * Set a listener to be notified when any modifier state changes.
     */
    fun setChangeListener(listener: (Modifier, Boolean) -> Unit) {
        changeListener = listener
    }

    /**
     * Toggle a modifier on/off.
     * Returns the new active state.
     */
    fun toggle(modifier: Modifier): Boolean {
        val current = states[modifier] ?: State()
        val newActive = !current.active
        states[modifier] = current.copy(active = newActive)
        changeListener?.invoke(modifier, newActive)
        return newActive
    }

    /**
     * Set a modifier to a specific state.
     */
    fun set(modifier: Modifier, active: Boolean) {
        val current = states[modifier] ?: State()
        if (current.active != active) {
            states[modifier] = current.copy(active = active)
            changeListener?.invoke(modifier, active)
        }
    }

    /**
     * Check if a modifier is currently active.
     */
    fun isActive(modifier: Modifier): Boolean {
        return states[modifier]?.active ?: false
    }

    /**
     * Get the current mode for a modifier.
     */
    fun getMode(modifier: Modifier): Mode {
        return states[modifier]?.mode ?: Mode.STICKY
    }

    /**
     * Set the behavior mode for a modifier.
     */
    fun setMode(modifier: Modifier, mode: Mode) {
        val current = states[modifier] ?: State()
        states[modifier] = current.copy(mode = mode)
    }

    /**
     * Called after a key press to potentially reset sticky modifiers.
     * Call this after handling any non-modifier key.
     */
    fun onKeyUsed() {
        for ((modifier, state) in states) {
            if (state.active && state.mode == Mode.STICKY) {
                states[modifier] = state.copy(active = false)
                changeListener?.invoke(modifier, false)
            }
        }
    }

    /**
     * Reset all modifiers to inactive.
     */
    fun resetAll() {
        for ((modifier, state) in states) {
            if (state.active) {
                states[modifier] = state.copy(active = false)
                changeListener?.invoke(modifier, false)
            }
        }
    }

    /**
     * Get Android KeyEvent meta state for current modifiers.
     */
    fun getMetaState(): Int {
        var meta = 0
        if (isActive(Modifier.CTRL)) meta = meta or android.view.KeyEvent.META_CTRL_ON
        if (isActive(Modifier.SHIFT)) meta = meta or android.view.KeyEvent.META_SHIFT_ON
        if (isActive(Modifier.ALT)) meta = meta or android.view.KeyEvent.META_ALT_ON
        if (isActive(Modifier.META)) meta = meta or android.view.KeyEvent.META_META_ON
        return meta
    }

    /**
     * Apply shift transformation to text if shift is active.
     * For single characters, uppercases them.
     */
    fun applyShift(text: String): String {
        return if (isActive(Modifier.SHIFT) && text.length == 1) {
            text.uppercase()
        } else {
            text
        }
    }
}
