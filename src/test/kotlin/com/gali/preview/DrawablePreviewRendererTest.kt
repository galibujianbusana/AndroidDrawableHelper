package com.gali.preview

import com.gali.drawable.DrawableModel
import com.gali.drawable.VectorPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.awt.Color

class DrawablePreviewRendererTest {
    private val renderer = DrawablePreviewRenderer()

    @Test
    fun rendersShapeImageAtRequestedSize() {
        val image = renderer.render(DrawableModel.Shape(fillColor = Color.RED, cornerRadius = 8f), 96)

        assertEquals(96, image.width)
        assertEquals(96, image.height)
        assertTrue(image.getRGB(48, 48) != 0)
    }

    @Test
    fun rendersVectorImageAtRequestedSize() {
        val image = renderer.render(
            DrawableModel.Vector(
                viewportWidth = 16f,
                viewportHeight = 16f,
                paths = listOf(VectorPath("M1,1h14v14h-14z", fillColor = Color.BLACK)),
            ),
            64,
        )

        assertEquals(64, image.width)
        assertEquals(64, image.height)
        assertTrue(image.getRGB(32, 32) != 0)
    }

    @Test
    fun rendersUnsupportedDrawableAsPlaceholder() {
        val image = renderer.render(DrawableModel.Unsupported("vector"), 48)

        assertEquals(48, image.width)
        assertEquals(48, image.height)
        assertTrue(image.getRGB(24, 24) != 0)
    }
}
