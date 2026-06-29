package com.gali.drawable

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import java.awt.Color

class ResourceRepository private constructor(
    private val colors: Map<String, String>,
    private val styles: Map<String, StyleResource>,
) {
    fun resolveColorResource(name: String): Color? = colors[name]?.let(::parseHexColor)

    fun resolveThemeColor(themeName: String, attributeName: String, visitedAttributes: Set<String> = emptySet()): Color? {
        val normalizedAttribute = normalizeAttributeName(attributeName) ?: return null
        if (themeName.isBlank() || normalizedAttribute in visitedAttributes) return null
        return resolveThemeColorFromStyle(
            styleName = themeName,
            attributeName = normalizedAttribute,
            visitedStyles = emptySet(),
            visitedAttributes = visitedAttributes + normalizedAttribute,
        )
    }

    private fun resolveThemeColorFromStyle(
        styleName: String,
        attributeName: String,
        visitedStyles: Set<String>,
        visitedAttributes: Set<String>,
    ): Color? {
        if (styleName in visitedStyles) return null
        val style = styles[styleName] ?: return null
        val value = style.items[attributeName]
        if (!value.isNullOrBlank()) {
            resolveColorValue(value, styleName, visitedAttributes)?.let { return it }
        }
        return style.parent?.let { parent ->
            resolveThemeColorFromStyle(parent, attributeName, visitedStyles + styleName, visitedAttributes)
        }
    }

    private fun resolveColorValue(value: String, themeName: String, visitedAttributes: Set<String>): Color? {
        val normalized = value.trim()
        return when {
            normalized.startsWith("#") -> parseHexColor(normalized)
            normalized.startsWith("@color/") -> resolveColorResource(normalized.substringAfter("@color/"))
            normalized.startsWith("@android:color/") -> resolveColorResource(normalized.substringAfter("@android:color/"))
            normalized.startsWith("?attr/") -> resolveThemeColor(themeName, normalized.substringAfter("?attr/"), visitedAttributes)
            normalized.startsWith("?attrs/") -> resolveThemeColor(themeName, normalized.substringAfter("?attrs/"), visitedAttributes)
            normalized.startsWith("?android:attr/") -> resolveThemeColor(themeName, normalized.substringAfter("?android:attr/"), visitedAttributes)
            normalized.startsWith("?") -> resolveThemeColor(themeName, normalized.removePrefix("?"), visitedAttributes)
            else -> null
        }
    }

    private fun normalizeAttributeName(name: String?): String? = name
        ?.trim()
        ?.removePrefix("?")
        ?.removePrefix("attr/")
        ?.removePrefix("attrs/")
        ?.removePrefix("android:attr/")
        ?.removePrefix("android:")
        ?.takeIf { it.isNotBlank() }

    companion object {
        fun build(project: Project): ResourceRepository {
            val base = project.guessProjectDir() ?: return ResourceRepository(emptyMap(), emptyMap())
            val colors = linkedMapOf<String, String>()
            val styles = linkedMapOf<String, StyleResource>()

            VfsUtilCore.iterateChildrenRecursively(base, null) { file ->
                if (!file.isDirectory && file.name.endsWith(".xml") && file.parent?.name?.startsWith("values") == true) {
                    val xmlFile = PsiManager.getInstance(project).findFile(file) as? XmlFile
                    xmlFile?.rootTag?.let { root ->
                        root.findSubTags("color").forEach { color ->
                            val name = color.getAttributeValue("name")
                            val value = color.value.text.trim()
                            if (!name.isNullOrBlank() && value.isNotBlank()) {
                                colors.putIfAbsent(name, value)
                            }
                        }
                        root.findSubTags("style").forEach { style ->
                            val name = style.getAttributeValue("name") ?: return@forEach
                            val items = style.findSubTags("item").mapNotNull { item ->
                                val itemName = normalizeStaticAttributeName(item.getAttributeValue("name")) ?: return@mapNotNull null
                                val itemValue = item.value.text.trim().takeIf { it.isNotBlank() } ?: return@mapNotNull null
                                itemName to itemValue
                            }.toMap()
                            styles.putIfAbsent(
                                name,
                                StyleResource(
                                    parent = normalizeStyleName(style.getAttributeValue("parent")),
                                    items = items,
                                ),
                            )
                        }
                    }
                }
                true
            }
            return ResourceRepository(colors, styles)
        }

        private fun normalizeStaticAttributeName(name: String?): String? = name
            ?.trim()
            ?.removePrefix("?")
            ?.removePrefix("attr/")
            ?.removePrefix("attrs/")
            ?.removePrefix("android:attr/")
            ?.removePrefix("android:")
            ?.takeIf { it.isNotBlank() }

        private fun normalizeStyleName(name: String?): String? = name
            ?.trim()
            ?.removePrefix("@style/")
            ?.takeIf { it.isNotBlank() }

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
    }
}

data class StyleResource(
    val parent: String?,
    val items: Map<String, String>,
)
