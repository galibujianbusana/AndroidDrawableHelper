package com.gali.preview

import com.gali.drawable.DrawableModel
import com.gali.drawable.Gradient
import com.gali.drawable.Layer
import com.gali.drawable.VectorPath
import java.awt.BasicStroke
import java.awt.Color
import java.awt.GradientPaint
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.AffineTransform
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import javax.swing.ImageIcon

class DrawablePreviewRenderer {
    fun renderIcon(model: DrawableModel, size: Int): ImageIcon = ImageIcon(render(model, size))

    fun render(model: DrawableModel, size: Int): BufferedImage {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            configure(graphics)
            paintCheckerboard(graphics, size)
            paintModel(graphics, model, 8, 8, size - 16, size - 16)
        } finally {
            graphics.dispose()
        }
        return image
    }

    private fun paintModel(graphics: Graphics2D, model: DrawableModel, x: Int, y: Int, width: Int, height: Int) {
        when (model) {
            is DrawableModel.Shape -> paintShape(graphics, model, x, y, width, height)
            is DrawableModel.Selector -> model.item?.let { paintModel(graphics, it, x, y, width, height) } ?: paintPlaceholder(graphics, x, y, width, height)
            is DrawableModel.LayerList -> paintLayerList(graphics, model.layers, x, y, width, height)
            is DrawableModel.Vector -> paintVector(graphics, model, x, y, width, height)
            is DrawableModel.Unsupported -> paintPlaceholder(graphics, x, y, width, height)
            is DrawableModel.ParseError -> paintPlaceholder(graphics, x, y, width, height)
        }
    }

    private fun paintShape(graphics: Graphics2D, shape: DrawableModel.Shape, x: Int, y: Int, width: Int, height: Int) {
        val targetWidth = shape.width?.coerceAtMost(width) ?: width
        val targetHeight = shape.height?.coerceAtMost(height) ?: height
        val targetX = x + (width - targetWidth) / 2
        val targetY = y + (height - targetHeight) / 2
        val radius = shape.cornerRadius.coerceAtMost(minOf(targetWidth, targetHeight) / 2f)
        val rectangle = RoundRectangle2D.Float(
            targetX.toFloat(),
            targetY.toFloat(),
            targetWidth.toFloat(),
            targetHeight.toFloat(),
            radius * 2,
            radius * 2,
        )

        val fill = shape.gradient?.let { gradientPaint(it, targetX, targetY, targetWidth, targetHeight) }
        graphics.paint = fill ?: shape.fillColor ?: Color(0xE0E0E0)
        graphics.fill(rectangle)

        if (shape.strokeColor != null && shape.strokeWidth > 0f) {
            graphics.color = shape.strokeColor
            graphics.stroke = BasicStroke(shape.strokeWidth)
            graphics.draw(rectangle)
        }
    }

    private fun paintLayerList(graphics: Graphics2D, layers: List<Layer>, x: Int, y: Int, width: Int, height: Int) {
        if (layers.isEmpty()) {
            paintPlaceholder(graphics, x, y, width, height)
            return
        }

        layers.forEach { layer ->
            val layerX = x + layer.left
            val layerY = y + layer.top
            val layerWidth = (width - layer.left - layer.right).coerceAtLeast(1)
            val layerHeight = (height - layer.top - layer.bottom).coerceAtLeast(1)
            paintModel(graphics, layer.drawable, layerX, layerY, layerWidth, layerHeight)
        }
    }

    private fun paintVector(graphics: Graphics2D, vector: DrawableModel.Vector, x: Int, y: Int, width: Int, height: Int) {
        if (vector.paths.isEmpty()) {
            paintPlaceholder(graphics, x, y, width, height)
            return
        }

        val scale = minOf(width / vector.viewportWidth, height / vector.viewportHeight)
        val offsetX = x + (width - vector.viewportWidth * scale) / 2f
        val offsetY = y + (height - vector.viewportHeight * scale) / 2f
        val transform = AffineTransform().apply {
            translate(offsetX.toDouble(), offsetY.toDouble())
            scale(scale.toDouble(), scale.toDouble())
        }

        vector.paths.forEach { vectorPath -> paintVectorPath(graphics, vectorPath, transform) }
    }

    private fun paintVectorPath(graphics: Graphics2D, vectorPath: VectorPath, transform: AffineTransform) {
        val path = transform.createTransformedShape(VectorPathParser(vectorPath.pathData).parse())
        vectorPath.fillColor?.takeUnless { it.alpha == 0 }?.let { fillColor ->
            graphics.color = fillColor
            graphics.fill(path)
        }
        if (vectorPath.strokeColor != null && vectorPath.strokeWidth > 0f) {
            graphics.color = vectorPath.strokeColor
            graphics.stroke = BasicStroke(vectorPath.strokeWidth * transform.scaleX.toFloat())
            graphics.draw(path)
        }
    }

    private fun gradientPaint(gradient: Gradient, x: Int, y: Int, width: Int, height: Int): GradientPaint {
        return if (gradient.angle == 90 || gradient.angle == 270) {
            GradientPaint(x.toFloat(), y.toFloat(), gradient.startColor, x.toFloat(), (y + height).toFloat(), gradient.endColor)
        } else {
            GradientPaint(x.toFloat(), y.toFloat(), gradient.startColor, (x + width).toFloat(), y.toFloat(), gradient.endColor)
        }
    }

    private fun paintPlaceholder(graphics: Graphics2D, x: Int, y: Int, width: Int, height: Int) {
        graphics.color = Color(0xF5F5F5)
        graphics.fillRoundRect(x, y, width, height, 12, 12)
        graphics.color = Color(0x9E9E9E)
        graphics.stroke = BasicStroke(2f)
        graphics.drawRoundRect(x, y, width, height, 12, 12)
        graphics.drawLine(x + 10, y + 10, x + width - 10, y + height - 10)
        graphics.drawLine(x + width - 10, y + 10, x + 10, y + height - 10)
    }

    private fun paintCheckerboard(graphics: Graphics2D, size: Int) {
        val cell = 8
        for (row in 0 until size step cell) {
            for (column in 0 until size step cell) {
                graphics.color = if ((row / cell + column / cell) % 2 == 0) Color(0xFFFFFF) else Color(0xEEEEEE)
                graphics.fillRect(column, row, cell, cell)
            }
        }
    }

    private fun configure(graphics: Graphics2D) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    }
}
