package com.gali.drawable

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile

class AppThemeResolver(private val project: Project) {
    fun findThemes(): List<AppTheme> {
        val base = project.guessProjectDir() ?: return listOf(AppTheme.None)
        val themes = linkedMapOf(AppTheme.None.name to AppTheme.None)

        VfsUtilCore.iterateChildrenRecursively(base, null) { file ->
            if (!file.isDirectory && file.name.endsWith(".xml") && file.parent?.name?.startsWith("values") == true) {
                val xmlFile = PsiManager.getInstance(project).findFile(file) as? XmlFile
                xmlFile?.rootTag?.findSubTags("style")
                    ?.filter { style -> style.getAttributeValue("name")?.contains("Theme", ignoreCase = true) == true }
                    ?.forEach { style ->
                        val name = style.getAttributeValue("name") ?: return@forEach
                        themes.putIfAbsent(name, AppTheme(name, name))
                    }
            }
            true
        }

        return themes.values.toList()
    }
}
