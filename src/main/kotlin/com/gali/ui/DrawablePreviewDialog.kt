package com.gali.ui

import com.gali.MyMessageBundle
import com.gali.drawable.AppTheme
import com.gali.drawable.DrawableModel
import com.gali.drawable.PreviewItem
import com.gali.preview.DrawablePreviewRenderer
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.ide.projectView.ProjectView
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.Action
import javax.swing.BorderFactory
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

class DrawablePreviewDialog(
    project: Project,
    files: List<VirtualFile>,
    themes: List<AppTheme>,
    initialTheme: AppTheme,
    items: List<PreviewItem>,
    reloadItems: (AppTheme) -> List<PreviewItem>,
) : DialogWrapper(project) {
    private val panel = DrawablePreviewPanel(project, files, themes, initialTheme, items, reloadItems)

    init {
        title = MyMessageBundle.message("dialog.previewDrawables.title")
        init()
    }

    override fun createCenterPanel(): JComponent = panel

    override fun createActions(): Array<Action> = arrayOf(okAction)
}

class DrawablePreviewPanel(
    private val project: Project,
    private val files: List<VirtualFile>,
    private val themes: List<AppTheme>,
    initialTheme: AppTheme,
    initialItems: List<PreviewItem>,
    private val reloadItems: (AppTheme) -> List<PreviewItem>,
) : JBPanel<DrawablePreviewPanel>(BorderLayout()) {
    private val renderer = DrawablePreviewRenderer()
    private val grid = JPanel(GridLayout(0, 3, JBUI.scale(12), JBUI.scale(12)))
    private val sizeSelector = JComboBox(arrayOf(48, 96, 160)).apply { selectedItem = 96 }
    private val themeSelector = JComboBox(themes.toTypedArray()).apply { selectedItem = initialTheme }
    private val searchField = JBTextField().apply {
        emptyText.text = MyMessageBundle.message("dialog.previewDrawables.search.placeholder")
        columns = 18
    }
    private val summaryLabel = JBLabel()
    private var items: List<PreviewItem> = initialItems

    init {
        border = JBUI.Borders.empty(12)
        preferredSize = Dimension(JBUI.scale(760), JBUI.scale(520))

        add(createToolbar(), BorderLayout.NORTH)
        add(ScrollPaneFactory.createScrollPane(grid, true), BorderLayout.CENTER)
        rebuildCards(96)

        sizeSelector.addActionListener {
            rebuildCards(sizeSelector.selectedItem as Int)
        }
        themeSelector.addActionListener {
            items = reloadItems(themeSelector.selectedItem as AppTheme)
            rebuildCards(sizeSelector.selectedItem as Int)
        }
        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(event: DocumentEvent) = rebuildCards(sizeSelector.selectedItem as Int)
            override fun removeUpdate(event: DocumentEvent) = rebuildCards(sizeSelector.selectedItem as Int)
            override fun changedUpdate(event: DocumentEvent) = rebuildCards(sizeSelector.selectedItem as Int)
        })
    }

    private fun createToolbar(): JComponent {
        updateSummary()
        return JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = JBUI.Borders.emptyBottom(10)
            add(summaryLabel, BorderLayout.WEST)
            add(JBPanel<JBPanel<*>>().apply {
                add(JBLabel(MyMessageBundle.message("dialog.previewDrawables.search")))
                add(searchField)
                add(JBLabel(MyMessageBundle.message("dialog.previewDrawables.theme")))
                add(themeSelector)
                add(JBLabel(MyMessageBundle.message("dialog.previewDrawables.size")))
                add(sizeSelector)
            }, BorderLayout.EAST)
        }
    }

    private fun rebuildCards(size: Int) {
        val visibleItems = filteredItems()
        updateSummary(visibleItems)
        grid.removeAll()
        visibleItems.forEach { item -> grid.add(createCard(item, size)) }
        grid.revalidate()
        grid.repaint()
    }

    private fun filteredItems(): List<PreviewItem> = items.filter { item ->
        FuzzyNameMatcher.matches(item.name.removeSuffix(".xml"), searchField.text) ||
            FuzzyNameMatcher.matches(item.name, searchField.text)
    }

    private fun updateSummary(visibleItems: List<PreviewItem> = filteredItems()) {
        val previewable = visibleItems.count { it.model !is DrawableModel.Unsupported && it.model !is DrawableModel.ParseError }
        val warnings = visibleItems.size - previewable
        summaryLabel.text = if (items.isEmpty()) {
            MyMessageBundle.message("dialog.previewDrawables.empty")
        } else {
            MyMessageBundle.message("dialog.previewDrawables.summary", visibleItems.size, previewable, warnings)
        }
    }

    private fun createCard(item: PreviewItem, size: Int): JComponent {
        val icon = renderer.renderIcon(item.model, JBUI.scale(size))
        val status = statusText(item.model)
        return JBPanel<JBPanel<*>>(BorderLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border()),
                JBUI.Borders.empty(10),
            )
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(event: MouseEvent) {
                    selectDrawableInProjectView(item)
                }
            })
            add(JBLabel(icon).apply {
                horizontalAlignment = SwingUtilities.CENTER
                preferredSize = Dimension(JBUI.scale(180), JBUI.scale(size + 8))
            }, BorderLayout.NORTH)
            add(JBPanel<JBPanel<*>>(BorderLayout()).apply {
                add(JBLabel(item.name), BorderLayout.NORTH)
                add(JBLabel(status).apply { foreground = JBColor.GRAY }, BorderLayout.CENTER)
            }, BorderLayout.CENTER)
            toolTipText = item.path
        }
    }

    private fun selectDrawableInProjectView(item: PreviewItem) {
        val file = files.firstOrNull { it.path == item.path } ?: return
        FileEditorManager.getInstance(project).openFile(file, true, true)
        ProjectView.getInstance(project).select(null, file, true)
    }

    private fun statusText(model: DrawableModel): String = when (model) {
        is DrawableModel.Shape -> "shape"
        is DrawableModel.Selector -> model.note
        is DrawableModel.LayerList -> model.note
        is DrawableModel.Vector -> "vector"
        is DrawableModel.Unsupported -> MyMessageBundle.message("preview.unsupported", model.tagName)
        is DrawableModel.ParseError -> MyMessageBundle.message("preview.parseError", model.message)
    }
}
