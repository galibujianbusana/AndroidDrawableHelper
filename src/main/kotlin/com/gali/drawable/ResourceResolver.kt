package com.gali.drawable

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Color
import kotlin.math.roundToInt

class ResourceResolver(
    private val project: Project,
    private val appTheme: AppTheme = AppTheme.None,
    private val repository: ResourceRepository = ResourceRepository.build(project),
) {
    fun resolveDrawable(contextFile: VirtualFile, reference: String, visiting: Set<String>): VirtualFile? {
        if (!reference.startsWith("@drawable/")) return null
        if (visiting.size > MAX_REFERENCE_DEPTH) return null

        val name = reference.substringAfter("@drawable/").substringBefore('?')
        val resDirectory = findResDirectory(contextFile) ?: return null
        return resDirectory.children
            .asSequence()
            .filter { it.isDirectory && (it.name == "drawable" || it.name.startsWith("drawable-")) }
            .flatMap { it.children.asSequence() }
            .firstOrNull { !it.isDirectory && it.nameWithoutExtension == name && it.extension.equals("xml", ignoreCase = true) }
    }

    fun resolveColor(value: String): Color? {
        val normalized = value.trim()
        return when {
            normalized.startsWith("#") -> parseHexColor(normalized)
            normalized.startsWith("@color/") -> repository.resolveColorResource(normalized.substringAfter("@color/"))
            normalized.startsWith("@android:color/") -> repository.resolveColorResource(normalized.substringAfter("@android:color/"))
            normalized.startsWith("?attr/") -> repository.resolveThemeColor(appTheme.name, normalized.substringAfter("?attr/"))
            normalized.startsWith("?attrs/") -> repository.resolveThemeColor(appTheme.name, normalized.substringAfter("?attrs/"))
            normalized.startsWith("?android:attr/") -> repository.resolveThemeColor(appTheme.name, normalized.substringAfter("?android:attr/"))
            normalized.startsWith("?") -> repository.resolveThemeColor(appTheme.name, normalized.removePrefix("?"))
            else -> null
        }
    }

    fun resolveDimension(value: String): Int {
        val normalized = value.trim()
        val number = normalized.takeWhile { it.isDigit() || it == '.' || it == '-' }.toFloatOrNull() ?: return 0
        return number.roundToInt()
    }

    private fun findResDirectory(file: VirtualFile): VirtualFile? {
        var current = file.parent
        while (current != null) {
            if (current.name == "res") return current
            current = current.parent
        }
        return null
    }

    private fun parseHexColor(value: String): Color? {
        val hex = value.removePrefix("#")
        return try {
            when (hex.length) {
                3 -> Color(
                    hex[0].toString().repeat(2).toInt(16),
                    hex[1].toString().repeat(2).toInt(16),
                    hex[2].toString().repeat(2).toInt(16),
                )
                4 -> Color(
                    hex[1].toString().repeat(2).toInt(16),
                    hex[2].toString().repeat(2).toInt(16),
                    hex[3].toString().repeat(2).toInt(16),
                    hex[0].toString().repeat(2).toInt(16),
                )
                6 -> Color(hex.toInt(16))
                8 -> Color(
                    hex.substring(2, 4).toInt(16),
                    hex.substring(4, 6).toInt(16),
                    hex.substring(6, 8).toInt(16),
                    hex.substring(0, 2).toInt(16),
                )
                else -> null
            }
        } catch (_: NumberFormatException) {
            null
        }
    }

    companion object {
        private const val MAX_REFERENCE_DEPTH = 12
    }
}
