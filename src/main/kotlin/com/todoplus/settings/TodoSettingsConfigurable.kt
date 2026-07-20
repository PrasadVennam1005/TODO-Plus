package com.todoplus.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ColorPicker
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.GridLayout
import javax.swing.*

/**
 * Settings page for TODO++ configuration
 */
class TodoSettingsConfigurable : Configurable {

    private var settingsPanel: JPanel? = null
    
    // Priorities
    private val priorityListModel = DefaultListModel<TodoSettingsService.PriorityConfig>()
    private lateinit var priorityList: JBList<TodoSettingsService.PriorityConfig>
    
    // Ignored Dirs
    private val ignoredListModel = DefaultListModel<String>()
    private lateinit var ignoredList: JBList<String>

    // Issue Tracker
    private val issueUrlField = JTextField()
    private val issuePatternField = JTextField()
    
    // Completion Behavior
    private val markDoneRadioButton = JRadioButton("Mark as DONE in code (e.g. // DONE(...))")
    private val deleteCommentRadioButton = JRadioButton("Remove comment line completely from code")
    private val completionGroup = ButtonGroup()

    private var isModified = false

    override fun getDisplayName(): String = "TODO++"

    override fun createComponent(): JComponent? {
        settingsPanel = JBPanel<JBPanel<*>>(BorderLayout())
        
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)
        
        // --- Priority Settings ---
        val currentPriorities = TodoSettingsService.getInstance().getPriorities()
        priorityListModel.clear()
        currentPriorities.forEach { priorityListModel.addElement(it.copy()) }
        
        priorityList = JBList(priorityListModel).apply {
            cellRenderer = PriorityListRenderer()
            selectionMode = ListSelectionModel.SINGLE_SELECTION
        }
        
        val priorityDecorator = ToolbarDecorator.createDecorator(priorityList)
            .setAddAction { addPriority() }
            .setRemoveAction { removePriority() }
            .setMoveUpAction { movePriority(-1) }
            .setMoveDownAction { movePriority(1) }
            .setEditAction { editPriority() }
            
        val priorityPanel = priorityDecorator.createPanel()
        priorityPanel.border = BorderFactory.createTitledBorder("Priority Levels (Ordered High to Low)")
        mainPanel.add(priorityPanel)
        
        // --- Ignored Directories Settings ---
        val currentIgnored = TodoSettingsService.getInstance().getIgnoredDirectories()
        ignoredListModel.clear()
        currentIgnored.forEach { ignoredListModel.addElement(it) }
        
        ignoredList = JBList(ignoredListModel).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
        }
        
        val ignoredDecorator = ToolbarDecorator.createDecorator(ignoredList)
            .setAddAction { addIgnoredDirectory() }
            .setRemoveAction { removeIgnoredDirectory() }
            .disableUpDownActions()
            
        val ignoredPanel = ignoredDecorator.createPanel()
        ignoredPanel.border = BorderFactory.createTitledBorder("Ignored Directories (e.g. build, node_modules)")
        mainPanel.add(ignoredPanel)
        
        // --- Issue Tracker Settings ---
        val issuePanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder("Issue Tracker Integration")
            
            val formPanel = JPanel(GridLayout(2, 2, 5, 5))
            formPanel.add(JLabel("Issue URL Template:"))
            formPanel.add(issueUrlField)
            formPanel.add(JLabel("Issue ID Pattern (Regex):"))
            formPanel.add(issuePatternField)
            
            val hintLabel = JLabel("<html><small>Use <b>{id}</b> placeholder in URL. Example: https://github.com/user/repo/issues/<b>{id}</b></small></html>")
            hintLabel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            
            add(formPanel, BorderLayout.CENTER)
            add(hintLabel, BorderLayout.SOUTH)
        }
        mainPanel.add(issuePanel)
        
        // --- Task Completion Behavior Settings ---
        completionGroup.add(markDoneRadioButton)
        completionGroup.add(deleteCommentRadioButton)
        
        val completionPanel = JPanel(GridLayout(2, 1, 5, 5)).apply {
            border = BorderFactory.createTitledBorder("Task Completion Action")
            add(markDoneRadioButton)
            add(deleteCommentRadioButton)
        }
        mainPanel.add(completionPanel)

        // --- Load Settings ---
        val settings = TodoSettingsService.getInstance()
        issueUrlField.text = settings.getState().issueUrlTemplate
        issuePatternField.text = settings.getState().issuePattern
        if (settings.getState().completionBehavior == TodoSettingsService.BEHAVIOR_DELETE_COMMENT) {
            deleteCommentRadioButton.isSelected = true
        } else {
            markDoneRadioButton.isSelected = true
        }

        settingsPanel?.add(mainPanel, BorderLayout.CENTER)
        
        return settingsPanel
    }

    private fun addPriority() {
        val panel = settingsPanel ?: return
        val name = Messages.showInputDialog(
            panel,
            "Enter priority name:",
            "Add Priority",
            Messages.getQuestionIcon(),
            "",
            object : InputValidator {
                override fun checkInput(inputString: String?): Boolean {
                    return !inputString.isNullOrBlank() && 
                           !priorityListModel.elements().toList().any { it.name.equals(inputString, ignoreCase = true) }
                }
                override fun canClose(inputString: String?): Boolean = checkInput(inputString)
            }
        )
        
        if (name != null) {
            val color = try {
                com.intellij.ui.ColorChooserService.getInstance().showDialog(panel, "Choose Color", Color.GRAY, true, emptyList(), false)
            } catch (e: Throwable) {
                javax.swing.JColorChooser.showDialog(panel, "Choose Color", Color.GRAY)
            }
            if (color != null) {
                priorityListModel.addElement(TodoSettingsService.PriorityConfig(name.uppercase(), color.rgb))
                isModified = true
            }
        }
    }
    
    private fun removePriority() {
        val index = priorityList.selectedIndex
        if (index != -1) {
            priorityListModel.remove(index)
            isModified = true
        }
    }
    
    private fun movePriority(direction: Int) {
        val index = priorityList.selectedIndex
        if (index != -1) {
            val newIndex = index + direction
            if (newIndex >= 0 && newIndex < priorityListModel.size()) {
                val item = priorityListModel.remove(index)
                priorityListModel.add(newIndex, item)
                priorityList.selectedIndex = newIndex
                isModified = true
            }
        }
    }
    
    private fun editPriority() {
        val panel = settingsPanel ?: return
        val index = priorityList.selectedIndex
        if (index != -1) {
            val current = priorityListModel.get(index)
            val color = try {
                com.intellij.ui.ColorChooserService.getInstance().showDialog(panel, "Choose Color for ${current.name}", current.getColor(), true, emptyList(), false)
            } catch (e: Throwable) {
                javax.swing.JColorChooser.showDialog(panel, "Choose Color for ${current.name}", current.getColor())
            }
            if (color != null) {
                current.colorRgb = color.rgb
                priorityList.repaint() // Force refresh
                isModified = true
            }
        }
    }

    private fun addIgnoredDirectory() {
        val panel = settingsPanel ?: return
        val dir = Messages.showInputDialog(
            panel,
            "Enter directory name to ignore (e.g. node_modules):",
            "Add Ignored Directory",
            Messages.getQuestionIcon(),
            "",
            object : InputValidator {
                override fun checkInput(inputString: String?): Boolean {
                    return !inputString.isNullOrBlank() && 
                           !ignoredListModel.elements().toList().contains(inputString.trim())
                }
                override fun canClose(inputString: String?): Boolean = checkInput(inputString)
            }
        )
        
        if (dir != null) {
            ignoredListModel.addElement(dir.trim())
            isModified = true
        }
    }
    
    private fun removeIgnoredDirectory() {
        val index = ignoredList.selectedIndex
        if (index != -1) {
            ignoredListModel.remove(index)
            isModified = true
        }
    }

    override fun isModified(): Boolean {
        if (isModified) return true
        
        val settings = TodoSettingsService.getInstance()
        
        // Check priorities
        val storedPriorities = settings.getPriorities()
        if (storedPriorities.size != priorityListModel.size()) return true
        for (i in 0 until storedPriorities.size) {
            if (storedPriorities[i] != priorityListModel.get(i)) return true
        }
        
        // Check ignored dirs
        val storedIgnored = settings.getIgnoredDirectories()
        if (storedIgnored.size != ignoredListModel.size()) return true
        for (i in 0 until storedIgnored.size) {
            if (storedIgnored[i] != ignoredListModel.get(i)) return true
        }

        // Check issue settings
        if (issueUrlField.text != settings.getState().issueUrlTemplate) return true
        if (issuePatternField.text != settings.getState().issuePattern) return true
        
        // Check completion behavior
        val selectedBehavior = if (deleteCommentRadioButton.isSelected) TodoSettingsService.BEHAVIOR_DELETE_COMMENT else TodoSettingsService.BEHAVIOR_MARK_DONE
        if (selectedBehavior != settings.getState().completionBehavior) return true

        return false
    }

    override fun apply() {
        val settings = TodoSettingsService.getInstance()
        
        val newPriorities = priorityListModel.elements().toList()
        settings.setPriorities(newPriorities)
        
        val newIgnored = ignoredListModel.elements().toList()
        settings.setIgnoredDirectories(newIgnored)
        
        settings.getState().issueUrlTemplate = issueUrlField.text.trim()
        settings.getState().issuePattern = issuePatternField.text.trim()
        settings.getState().completionBehavior = if (deleteCommentRadioButton.isSelected) TodoSettingsService.BEHAVIOR_DELETE_COMMENT else TodoSettingsService.BEHAVIOR_MARK_DONE
        
        isModified = false
    }

    override fun reset() {
        val settings = TodoSettingsService.getInstance()
        
        val currentPriorities = settings.getPriorities()
        priorityListModel.clear()
        currentPriorities.forEach { priorityListModel.addElement(it.copy()) }
        
        val currentIgnored = settings.getIgnoredDirectories()
        ignoredListModel.clear()
        currentIgnored.forEach { ignoredListModel.addElement(it) }
        
        issueUrlField.text = settings.getState().issueUrlTemplate
        issuePatternField.text = settings.getState().issuePattern
        if (settings.getState().completionBehavior == TodoSettingsService.BEHAVIOR_DELETE_COMMENT) {
            deleteCommentRadioButton.isSelected = true
        } else {
            markDoneRadioButton.isSelected = true
        }
        
        isModified = false
    }
    
    // Custom renderer for the list
    private class PriorityListRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            if (value is TodoSettingsService.PriorityConfig) {
                text = value.name
                icon = ColorIcon(value.getColor())
            }
            return component
        }
    }
    
    // Helper for color icon
    private class ColorIcon(private val color: Color) : Icon {
        override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
            g.color = color
            g.fillRect(x, y, iconWidth, iconHeight)
            g.color = Color.GRAY
            g.drawRect(x, y, iconWidth, iconHeight)
        }
        override fun getIconWidth(): Int = 16
        override fun getIconHeight(): Int = 16
    }
}
