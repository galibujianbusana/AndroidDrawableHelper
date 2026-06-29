package com.gali.drawable

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile

object DrawableSelectionResolver {
    private const val XML_EXTENSION = "xml"

    fun resolve(event: AnActionEvent): List<VirtualFile> {
        val project = event.project ?: return emptyList()
        val files = event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)?.toList()
            ?: event.getData(CommonDataKeys.VIRTUAL_FILE)?.let(::listOf)
            ?: event.getData(CommonDataKeys.PSI_FILE)?.virtualFile?.let(::listOf)
            ?: emptyList()

        return resolve(project, files)
    }

    fun resolve(project: Project, files: Collection<VirtualFile>): List<VirtualFile> {
        val index = ProjectFileIndex.getInstance(project)
        return files.asSequence()
            .flatMap { file -> candidatesFrom(file).asSequence() }
            .filter { file -> isXmlDrawableFile(file) && index.isInContent(file) }
            .distinctBy { it.path }
            .sortedBy { it.path }
            .toList()
    }

    fun isDrawableDirectory(file: VirtualFile): Boolean =
        file.isDirectory && isDrawableDirectoryName(file.name)

    fun isXmlDrawableFile(file: VirtualFile): Boolean =
        !file.isDirectory && file.extension.equals(XML_EXTENSION, ignoreCase = true) &&
            file.parent?.let(::isDrawableDirectory) == true

    private fun candidatesFrom(file: VirtualFile): List<VirtualFile> = when {
        isXmlDrawableFile(file) -> listOf(file)
        isDrawableDirectory(file) -> file.children
            .filter { child -> !child.isDirectory && child.extension.equals(XML_EXTENSION, ignoreCase = true) }
        else -> emptyList()
    }

    private fun isDrawableDirectoryName(name: String): Boolean =
        name == "drawable" || name.startsWith("drawable-")
}
