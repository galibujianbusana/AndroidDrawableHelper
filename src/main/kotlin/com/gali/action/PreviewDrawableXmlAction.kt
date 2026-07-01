package com.gali.action

import com.gali.MyMessageBundle
import com.gali.drawable.AppTheme
import com.gali.drawable.AppThemeResolver
import com.gali.drawable.DrawableSelectionResolver
import com.gali.drawable.DrawableXmlParser
import com.gali.drawable.PreviewItem
import com.gali.drawable.ResourceRepository
import com.gali.ui.DrawablePreviewDialog
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile

class PreviewDrawableXmlAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(event: AnActionEvent) {
        val files = readResolvedFiles(event)
        event.presentation.isEnabledAndVisible = files.isNotEmpty()
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        val files = readResolvedFiles(event)
        if (files.isEmpty()) {
            Messages.showInfoMessage(
                project,
                MyMessageBundle.message("dialog.previewDrawables.empty"),
                MyMessageBundle.message("dialog.previewDrawables.title"),
            )
            return
        }

        val themes = ApplicationManager.getApplication().runReadAction<List<AppTheme>> {
            AppThemeResolver(project).findThemes()
        }
        val repository = ApplicationManager.getApplication().runReadAction<ResourceRepository> {
            ResourceRepository.build(project)
        }
        val initialTheme = themes.firstOrNull() ?: AppTheme.None
        val items = parseItems(project, files, initialTheme, repository)
        DrawablePreviewDialog(project, files, themes, initialTheme, items) { theme ->
            parseItems(project, files, theme, repository)
        }.show()
    }

    private fun readResolvedFiles(event: AnActionEvent): List<VirtualFile> =
        ApplicationManager.getApplication().runReadAction<List<VirtualFile>> {
            DrawableSelectionResolver.resolve(event)
        }

    private fun parseItems(
        project: Project,
        files: List<VirtualFile>,
        appTheme: AppTheme,
        repository: ResourceRepository,
    ): List<PreviewItem> {
        val parser = DrawableXmlParser(project, appTheme, repository)
        return ApplicationManager.getApplication().runReadAction<List<PreviewItem>> {
            files.map { file ->
                PreviewItem(
                    name = file.name,
                    path = file.path,
                    model = parser.parse(file),
                )
            }
        }
    }
}
