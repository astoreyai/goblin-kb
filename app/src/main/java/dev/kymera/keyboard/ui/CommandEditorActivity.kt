package dev.kymera.keyboard.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dev.kymera.keyboard.R
import dev.kymera.keyboard.commands.SlashAction
import dev.kymera.keyboard.commands.SlashCommand
import dev.kymera.keyboard.databinding.ActivityCommandEditorBinding
import dev.kymera.keyboard.databinding.DialogEditCommandBinding
import dev.kymera.keyboard.databinding.ItemCommandBinding
import dev.kymera.keyboard.settings.PreferencesManager

/**
 * Activity for managing custom slash commands.
 * Allows users to add, edit, and delete their own commands.
 */
class CommandEditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCommandEditorBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var adapter: CommandAdapter

    private var commands = mutableListOf<SlashCommand>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommandEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)
        loadCommands()
        setupUI()
    }

    private fun loadCommands() {
        commands = prefs.getCustomCommands().toMutableList()
    }

    private fun setupUI() {
        // Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Custom Commands"

        // RecyclerView
        adapter = CommandAdapter(
            commands = commands,
            onEdit = { command -> showEditDialog(command) },
            onDelete = { command -> deleteCommand(command) }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        // Swipe to delete
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val command = commands[position]
                deleteCommand(command)
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerView)

        // FAB
        binding.fabAdd.setOnClickListener {
            showEditDialog(null)
        }

        // Empty state
        updateEmptyState()
    }

    private fun updateEmptyState() {
        binding.emptyView.visibility = if (commands.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (commands.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showEditDialog(existingCommand: SlashCommand?) {
        val dialogBinding = DialogEditCommandBinding.inflate(layoutInflater)

        // Pre-fill if editing
        existingCommand?.let { cmd ->
            dialogBinding.editCommand.setText(cmd.command)
            dialogBinding.editDescription.setText(cmd.description)
            dialogBinding.editCategory.setText(cmd.category)
            when (val action = cmd.action) {
                is SlashAction.InsertText -> {
                    dialogBinding.radioInsertText.isChecked = true
                    dialogBinding.editValue.setText(action.text)
                }
                is SlashAction.InsertTemplate -> {
                    dialogBinding.radioInsertTemplate.isChecked = true
                    dialogBinding.editValue.setText(action.template)
                }
                is SlashAction.SendToLocalAgent -> {
                    dialogBinding.radioSendToLocalAgent.isChecked = true
                    dialogBinding.editValue.setText(action.command)
                }
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (existingCommand == null) "Add Command" else "Edit Command")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { _, _ ->
                val command = dialogBinding.editCommand.text.toString().trim()
                val description = dialogBinding.editDescription.text.toString().trim()
                val category = dialogBinding.editCategory.text.toString().trim().ifEmpty { "custom" }
                val value = dialogBinding.editValue.text.toString().trim()

                if (command.isBlank() || value.isBlank()) {
                    Toast.makeText(this, "Command and value are required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val action = when {
                    dialogBinding.radioInsertText.isChecked -> SlashAction.InsertText(value)
                    dialogBinding.radioInsertTemplate.isChecked -> SlashAction.InsertTemplate(value)
                    dialogBinding.radioSendToLocalAgent.isChecked -> SlashAction.SendToLocalAgent(value)
                    else -> SlashAction.InsertText(value)
                }

                val newCommand = SlashCommand(
                    command = if (command.startsWith("/")) command else "/$command",
                    description = description,
                    category = category,
                    action = action,
                    isBuiltIn = false
                )

                if (existingCommand != null) {
                    updateCommand(existingCommand, newCommand)
                } else {
                    addCommand(newCommand)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun addCommand(command: SlashCommand) {
        // Check for duplicates
        if (commands.any { it.command == command.command }) {
            Toast.makeText(this, "Command already exists", Toast.LENGTH_SHORT).show()
            return
        }

        commands.add(command)
        adapter.notifyItemInserted(commands.size - 1)
        saveCommands()
        updateEmptyState()
    }

    private fun updateCommand(oldCommand: SlashCommand, newCommand: SlashCommand) {
        val index = commands.indexOfFirst { it.command == oldCommand.command }
        if (index >= 0) {
            commands[index] = newCommand
            adapter.notifyItemChanged(index)
            saveCommands()
        }
    }

    private fun deleteCommand(command: SlashCommand) {
        val index = commands.indexOfFirst { it.command == command.command }
        if (index >= 0) {
            commands.removeAt(index)
            adapter.notifyItemRemoved(index)
            saveCommands()
            updateEmptyState()
        }
    }

    private fun saveCommands() {
        prefs.saveCustomCommands(commands)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // Adapter
    class CommandAdapter(
        private val commands: List<SlashCommand>,
        private val onEdit: (SlashCommand) -> Unit,
        private val onDelete: (SlashCommand) -> Unit
    ) : RecyclerView.Adapter<CommandAdapter.ViewHolder>() {

        class ViewHolder(val binding: ItemCommandBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemCommandBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val command = commands[position]

            holder.binding.textCommand.text = command.command
            holder.binding.textDescription.text = command.description
            holder.binding.textCategory.text = command.category

            holder.binding.root.setOnClickListener { onEdit(command) }
            holder.binding.btnDelete.setOnClickListener { onDelete(command) }
        }

        override fun getItemCount() = commands.size
    }
}
