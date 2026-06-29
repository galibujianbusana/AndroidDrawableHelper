package com.gali.drawable

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import java.awt.Color

class DrawableXmlParser(
    private val project: Project,
    private val appTheme: AppTheme = AppTheme.None,
    repository: ResourceRepository = ResourceRepository.build(project),
    private val resolver: ResourceResolver = ResourceResolver(project, appTheme, repository),
) {
    fun parse(file: VirtualFile): DrawableModel {
        return try {
            val psiFile = PsiManager.getInstance(project).findFile(file) as? XmlFile
                ?: return DrawableModel.ParseError("Not an XML file")
            parseXmlFile(psiFile, file, emptySet())
        } catch (error: Exception) {
            DrawableModel.ParseError(error.message ?: error.javaClass.simpleName)
        }
    }

    internal fun parseXmlFile(xmlFile: XmlFile, file: VirtualFile, visiting: Set<String>): DrawableModel {
        if (file.path in visiting) {
            return DrawableModel.ParseError("Circular drawable reference")
        }

        val root = xmlFile.rootTag ?: return DrawableModel.ParseError("Missing root tag")
        return parseTag(root, file, visiting + file.path)
    }

    internal fun parseTag(tag: XmlTag, file: VirtualFile, visiting: Set<String>): DrawableModel = when (tag.name) {
        "shape" -> parseShape(tag)
        "selector" -> parseSelector(tag, file, visiting)
        "layer-list" -> parseLayerList(tag, file, visiting)
        "vector" -> parseVector(tag)
        else -> DrawableModel.Unsupported(tag.name)
    }

    private fun parseShape(tag: XmlTag): DrawableModel.Shape {
        val solid = tag.findFirstSubTag("solid")
        val stroke = tag.findFirstSubTag("stroke")
        val corners = tag.findFirstSubTag("corners")
        val size = tag.findFirstSubTag("size")
        val gradient = tag.findFirstSubTag("gradient")

        return DrawableModel.Shape(
            fillColor = colorAttribute(solid, "color")
                ?: colorAttribute(solid, "fillColor")
                ?: colorAttribute(tag, "fillColor")
                ?: colorAttribute(tag, "color"),
            strokeColor = colorAttribute(stroke, "color")
                ?: colorAttribute(stroke, "strokeColor")
                ?: colorAttribute(tag, "strokeColor"),
            strokeWidth = (dimensionAttribute(stroke, "width")
                .takeIf { it > 0 }
                ?: dimensionAttribute(tag, "strokeWidth")).toFloat(),
            cornerRadius = dimensionAttribute(corners, "radius").toFloat(),
            width = dimensionAttribute(size, "width").takeIf { it > 0 },
            height = dimensionAttribute(size, "height").takeIf { it > 0 },
            gradient = parseGradient(gradient),
        )
    }

    private fun parseSelector(tag: XmlTag, file: VirtualFile, visiting: Set<String>): DrawableModel.Selector {
        val item = tag.findSubTags("item").firstOrNull { it.getAttributeValue("state_pressed", ANDROID_NS) == null }
            ?: tag.findFirstSubTag("item")
        return DrawableModel.Selector(parseItemDrawable(item, file, visiting))
    }

    private fun parseLayerList(tag: XmlTag, file: VirtualFile, visiting: Set<String>): DrawableModel.LayerList {
        val layers = tag.findSubTags("item").mapNotNull { item ->
            parseItemDrawable(item, file, visiting)?.let { drawable ->
                Layer(
                    drawable = drawable,
                    left = dimensionAttribute(item, "left"),
                    top = dimensionAttribute(item, "top"),
                    right = dimensionAttribute(item, "right"),
                    bottom = dimensionAttribute(item, "bottom"),
                )
            }
        }
        return DrawableModel.LayerList(layers)
    }

    private fun parseVector(tag: XmlTag): DrawableModel.Vector {
        val viewportWidth = floatAttribute(tag, "viewportWidth").takeIf { it > 0f } ?: 24f
        val viewportHeight = floatAttribute(tag, "viewportHeight").takeIf { it > 0f } ?: 24f
        return DrawableModel.Vector(
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight,
            paths = collectVectorPaths(tag),
        )
    }

    private fun collectVectorPaths(tag: XmlTag): List<VectorPath> = tag.subTags.flatMap { child ->
        when (child.name) {
            "path" -> listOfNotNull(parseVectorPath(child))
            "group" -> collectVectorPaths(child)
            else -> emptyList()
        }
    }

    private fun parseVectorPath(tag: XmlTag): VectorPath? {
        val pathData = tag.getAndroidAttribute("pathData") ?: return null
        return VectorPath(
            pathData = pathData,
            fillColor = colorAttribute(tag, "fillColor"),
            strokeColor = colorAttribute(tag, "strokeColor"),
            strokeWidth = floatAttribute(tag, "strokeWidth"),
        )
    }

    private fun parseItemDrawable(item: XmlTag?, file: VirtualFile, visiting: Set<String>): DrawableModel? {
        item ?: return null
        val reference = item.getAndroidAttribute("drawable")
        if (!reference.isNullOrBlank()) {
            return resolver.resolveDrawable(file, reference, visiting)?.let { parse(it, visiting) }
                ?: DrawableModel.Unsupported(reference, "Drawable reference could not be resolved.")
        }
        return item.subTags.firstOrNull()?.let { parseTag(it, file, visiting) }
    }

    private fun parseGradient(tag: XmlTag?): Gradient? {
        tag ?: return null
        val start = colorAttribute(tag, "startColor") ?: return null
        val end = colorAttribute(tag, "endColor") ?: return null
        return Gradient(start, end, intAttribute(tag, "angle"))
    }

    private fun colorAttribute(tag: XmlTag?, localName: String): Color? {
        val raw = tag?.getAndroidAttribute(localName) ?: return null
        return resolver.resolveColor(raw)
    }

    private fun dimensionAttribute(tag: XmlTag?, localName: String): Int {
        val raw = tag?.getAndroidAttribute(localName) ?: return 0
        return resolver.resolveDimension(raw)
    }

    private fun intAttribute(tag: XmlTag?, localName: String): Int {
        val raw = tag?.getAndroidAttribute(localName) ?: return 0
        return raw.toIntOrNull() ?: 0
    }

    private fun floatAttribute(tag: XmlTag?, localName: String): Float {
        val raw = tag?.getAndroidAttribute(localName) ?: return 0f
        return raw.toFloatOrNull() ?: resolver.resolveDimension(raw).toFloat()
    }

    private fun XmlTag.getAndroidAttribute(localName: String): String? =
        getAttributeValue(localName, ANDROID_NS) ?: getAttributeValue(localName)

    private fun parse(file: VirtualFile, visiting: Set<String>): DrawableModel {
        val psiFile = PsiManager.getInstance(project).findFile(file) as? XmlFile
            ?: return DrawableModel.ParseError("Not an XML file")
        return parseXmlFile(psiFile, file, visiting)
    }

    companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
