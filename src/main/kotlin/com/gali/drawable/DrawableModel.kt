package com.gali.drawable

import java.awt.Color

sealed class DrawableModel {
    data class Shape(
        val fillColor: Color? = null,
        val strokeColor: Color? = null,
        val strokeWidth: Float = 0f,
        val cornerRadius: Float = 0f,
        val width: Int? = null,
        val height: Int? = null,
        val gradient: Gradient? = null,
    ) : DrawableModel()

    data class Selector(
        val item: DrawableModel?,
        val note: String = "Selector preview shows the default or first item.",
    ) : DrawableModel()

    data class LayerList(
        val layers: List<Layer>,
        val note: String = "Layer-list preview is partially supported.",
    ) : DrawableModel()

    data class Vector(
        val viewportWidth: Float,
        val viewportHeight: Float,
        val paths: List<VectorPath>,
    ) : DrawableModel()

    data class Unsupported(
        val tagName: String,
        val reason: String = "This drawable type is not supported yet.",
    ) : DrawableModel()

    data class ParseError(val message: String) : DrawableModel()
}

data class Gradient(
    val startColor: Color,
    val endColor: Color,
    val angle: Int = 0,
)

data class Layer(
    val drawable: DrawableModel,
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
)

data class VectorPath(
    val pathData: String,
    val fillColor: Color? = null,
    val strokeColor: Color? = null,
    val strokeWidth: Float = 0f,
)

data class PreviewItem(
    val name: String,
    val path: String,
    val model: DrawableModel,
)
