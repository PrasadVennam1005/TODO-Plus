package com.todoplus.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ColorPicker
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.*
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
    private val issueUrlField = JTextField(25)
    private val issuePatternField = JTextField(25)
    
    // Task Completion Behavior Settings
    private val markDoneRadioButton = JRadioButton("Mark as DONE in code (e.g. // DONE(...))")
    private val deleteCommentRadioButton = JRadioButton("Remove comment line completely from code")
    private val completionGroup = ButtonGroup()

    // Scanning Limits
    private val maxFileSizeSpinner = JSpinner(SpinnerNumberModel(5, 1, 100, 1))

    // GitHub REST Integration
    private val githubTokenField = JPasswordField(25)
    private val githubOwnerField = JTextField(25)
    private val githubRepoField = JTextField(25)

    // Jira REST Integration
    private val jiraUrlField = JTextField(25)
    private val jiraEmailField = JTextField(25)
    private val jiraTokenField = JPasswordField(25)
    private val jiraProjectField = JTextField(25)

    // Webhooks
    private val slackWebhookField = JTextField(25)
    private val discordWebhookField = JTextField(25)

    // AI Assistant
    private val aiProviderComboBox = JComboBox(arrayOf("GEMINI", "OPENAI", "CLAUDE", "OLLAMA"))
    private val aiApiKeyField = JPasswordField(25)
    private val aiModelField = JTextField(25)
    private val aiEndpointField = JTextField(25)
    private val enablePreCommitCheck = JCheckBox("Inspect unlinked TODOs and prompt for Jira tickets before Git commit")

    private var isModified = false

    override fun getDisplayName(): String = "TODO++"

    private fun createFormPanel(title: String, rows: List<Pair<String, JComponent>>): JPanel {
        val panel = JPanel(GridBagLayout())
        if (title.isNotBlank()) {
            panel.border = BorderFactory.createTitledBorder(title)
        }
        val gbc = GridBagConstraints()
        gbc.insets = Insets(4, 6, 4, 6)
        rows.forEachIndexed { index, (labelText, component) ->
            gbc.gridx = 0
            gbc.gridy = index
            gbc.weightx = 0.0
            gbc.anchor = GridBagConstraints.WEST
            gbc.fill = GridBagConstraints.NONE
            panel.add(JLabel(labelText), gbc)

            gbc.gridx = 1
            gbc.gridy = index
            gbc.weightx = 1.0
            gbc.anchor = GridBagConstraints.WEST
            gbc.fill = GridBagConstraints.HORIZONTAL
            panel.add(component, gbc)
        }
        return panel
    }

    override fun createComponent(): JComponent? {
        settingsPanel = JBPanel<JBPanel<*>>(BorderLayout())
        
        val mainPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(5)
        }
        
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
            
            val formPanel = createFormPanel("", listOf(
                "Issue URL Template:" to issueUrlField,
                "Issue ID Pattern (Regex):" to issuePatternField
            ))
            
            val hintLabel = JLabel("<html><small>Use <b>{id}</b> placeholder in URL. Example: https://github.com/user/repo/issues/<b>{id}</b></small></html>")
            hintLabel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            
            add(formPanel, BorderLayout.CENTER)
            add(hintLabel, BorderLayout.SOUTH)
        }
        mainPanel.add(issuePanel)

        // --- GitHub REST Export Settings ---
        val githubPanel = createFormPanel("GitHub REST API Integration", listOf(
            "Personal Access Token:" to githubTokenField,
            "Repository Owner / Org:" to githubOwnerField,
            "Repository Name:" to githubRepoField
        ))
        mainPanel.add(githubPanel)

        // --- Jira REST Export Settings ---
        val jiraPanel = createFormPanel("Jira Cloud REST API Integration", listOf(
            "Jira Base URL:" to jiraUrlField,
            "Account Email:" to jiraEmailField,
            "API Token:" to jiraTokenField,
            "Project Key (e.g. PROJ):" to jiraProjectField
        ))
        mainPanel.add(jiraPanel)

        // --- Webhook Alerts Settings ---
        val webhookPanel = createFormPanel("Overdue Webhook Alert Endpoints", listOf(
            "Slack Webhook URL:" to slackWebhookField,
            "Discord Webhook URL:" to discordWebhookField
        ))
        mainPanel.add(webhookPanel)

        // --- Scanning Limits Settings ---
        val scanPanel = createFormPanel("Scanning Performance Limits", listOf(
            "Max File Size to Scan (MB):" to maxFileSizeSpinner
        ))
        mainPanel.add(scanPanel)
        
        // --- Task Completion Behavior Settings ---
        completionGroup.add(markDoneRadioButton)
        completionGroup.add(deleteCommentRadioButton)
        
        val completionPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createTitledBorder("Task Completion Action")
            add(markDoneRadioButton)
            add(deleteCommentRadioButton)
        }
        mainPanel.add(completionPanel)

        // --- AI Jira Assistant Settings ---
        val aiPanel = createFormPanel("AI Jira Ticket Assistant", listOf(
            "AI Provider:" to aiProviderComboBox,
            "API Key:" to aiApiKeyField,
            "Model Name:" to aiModelField,
            "Custom Endpoint (for Ollama / Proxy):" to aiEndpointField,
            "Pre-Commit Check:" to enablePreCommitCheck
        ))
        mainPanel.add(aiPanel)

        // --- Load Settings ---
        reset()

        val scrollPane = com.intellij.ui.components.JBScrollPane(
            mainPanel,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        )
        settingsPanel?.add(scrollPane, BorderLayout.CENTER)
        
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
            val color = javax.swing.JColorChooser.showDialog(panel, "Choose Color", Color.GRAY)
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
            val color = javax.swing.JColorChooser.showDialog(panel, "Choose Color for ${current.name}", current.getColor())
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

        // Check scanning limits
        if ((maxFileSizeSpinner.value as Int) != settings.getState().maxFileSizeMb) return true

        // Check GitHub settings
        if (String(githubTokenField.password) != settings.getState().githubToken) return true
        if (githubOwnerField.text != settings.getState().githubRepoOwner) return true
        if (githubRepoField.text != settings.getState().githubRepoName) return true

        // Check Jira settings
        if (jiraUrlField.text != settings.getState().jiraBaseUrl) return true
        if (jiraEmailField.text != settings.getState().jiraEmail) return true
        if (String(jiraTokenField.password) != settings.getState().jiraApiToken) return true
        if (jiraProjectField.text != settings.getState().jiraProjectKey) return true

        // Check Webhooks
        if (slackWebhookField.text != settings.getState().slackWebhookUrl) return true
        if (discordWebhookField.text != settings.getState().discordWebhookUrl) return true

        // Check AI Settings
        if (aiProviderComboBox.selectedItem != settings.getState().aiProvider) return true
        if (String(aiApiKeyField.password) != settings.getState().aiApiKey) return true
        if (aiModelField.text != settings.getState().aiModelName) return true
        if (aiEndpointField.text != settings.getState().aiCustomEndpoint) return true
        if (enablePreCommitCheck.isSelected != settings.getState().enablePreCommitTodoCheck) return true
        
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
        settings.getState().maxFileSizeMb = maxFileSizeSpinner.value as Int

        settings.getState().githubToken = String(githubTokenField.password).trim()
        settings.getState().githubRepoOwner = githubOwnerField.text.trim()
        settings.getState().githubRepoName = githubRepoField.text.trim()

        settings.getState().jiraBaseUrl = jiraUrlField.text.trim()
        settings.getState().jiraEmail = jiraEmailField.text.trim()
        settings.getState().jiraApiToken = String(jiraTokenField.password).trim()
        settings.getState().jiraProjectKey = jiraProjectField.text.trim()

        settings.getState().slackWebhookUrl = slackWebhookField.text.trim()
        settings.getState().discordWebhookUrl = discordWebhookField.text.trim()

        settings.getState().aiProvider = (aiProviderComboBox.selectedItem as? String) ?: "GEMINI"
        settings.getState().aiApiKey = String(aiApiKeyField.password).trim()
        settings.getState().aiModelName = aiModelField.text.trim()
        settings.getState().aiCustomEndpoint = aiEndpointField.text.trim()
        settings.getState().enablePreCommitTodoCheck = enablePreCommitCheck.isSelected

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
        maxFileSizeSpinner.value = settings.getState().maxFileSizeMb

        githubTokenField.text = settings.getState().githubToken
        githubOwnerField.text = settings.getState().githubRepoOwner
        githubRepoField.text = settings.getState().githubRepoName

        jiraUrlField.text = settings.getState().jiraBaseUrl
        jiraEmailField.text = settings.getState().jiraEmail
        jiraTokenField.text = settings.getState().jiraApiToken
        jiraProjectField.text = settings.getState().jiraProjectKey

        slackWebhookField.text = settings.getState().slackWebhookUrl
        discordWebhookField.text = settings.getState().discordWebhookUrl

        aiProviderComboBox.selectedItem = settings.getState().aiProvider
        aiApiKeyField.text = settings.getState().aiApiKey
        aiModelField.text = settings.getState().aiModelName
        aiEndpointField.text = settings.getState().aiCustomEndpoint
        enablePreCommitCheck.isSelected = settings.getState().enablePreCommitTodoCheck

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
