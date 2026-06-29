package com.gali.preview

import java.awt.geom.Path2D
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class VectorPathParser(private val data: String) {
    private var index = 0
    private var command = ' '
    private var currentX = 0f
    private var currentY = 0f
    private var startX = 0f
    private var startY = 0f
    private var lastControlX = 0f
    private var lastControlY = 0f
    private var lastCommand = ' '

    fun parse(): Path2D.Float {
        val path = Path2D.Float(Path2D.WIND_NON_ZERO)
        while (skipSeparators()) {
            val char = data[index]
            if (char.isLetter()) {
                command = char
                index++
            }
            executeCommand(path, command)
            lastCommand = command
        }
        return path
    }

    private fun executeCommand(path: Path2D.Float, command: Char) {
        when (command) {
            'M', 'm' -> moveTo(path, command == 'm')
            'L', 'l' -> lineTo(path, command == 'l')
            'H', 'h' -> horizontalTo(path, command == 'h')
            'V', 'v' -> verticalTo(path, command == 'v')
            'C', 'c' -> cubicTo(path, command == 'c')
            'S', 's' -> smoothCubicTo(path, command == 's')
            'Q', 'q' -> quadTo(path, command == 'q')
            'T', 't' -> smoothQuadTo(path, command == 't')
            'A', 'a' -> arcTo(path, command == 'a')
            'Z', 'z' -> {
                path.closePath()
                currentX = startX
                currentY = startY
            }
            else -> index++
        }
    }

    private fun moveTo(path: Path2D.Float, relative: Boolean) {
        val firstX = nextNumber() ?: return
        val firstY = nextNumber() ?: return
        currentX = if (relative) currentX + firstX else firstX
        currentY = if (relative) currentY + firstY else firstY
        path.moveTo(currentX, currentY)
        startX = currentX
        startY = currentY
        while (hasNumberAhead()) {
            val x = nextNumber() ?: return
            val y = nextNumber() ?: return
            currentX = if (relative) currentX + x else x
            currentY = if (relative) currentY + y else y
            path.lineTo(currentX, currentY)
        }
    }

    private fun lineTo(path: Path2D.Float, relative: Boolean) {
        while (hasNumberAhead()) {
            val x = nextNumber() ?: return
            val y = nextNumber() ?: return
            currentX = if (relative) currentX + x else x
            currentY = if (relative) currentY + y else y
            path.lineTo(currentX, currentY)
        }
    }

    private fun horizontalTo(path: Path2D.Float, relative: Boolean) {
        while (hasNumberAhead()) {
            val x = nextNumber() ?: return
            currentX = if (relative) currentX + x else x
            path.lineTo(currentX, currentY)
        }
    }

    private fun verticalTo(path: Path2D.Float, relative: Boolean) {
        while (hasNumberAhead()) {
            val y = nextNumber() ?: return
            currentY = if (relative) currentY + y else y
            path.lineTo(currentX, currentY)
        }
    }

    private fun cubicTo(path: Path2D.Float, relative: Boolean) {
        while (hasNumberAhead()) {
            val x1 = nextNumber() ?: return
            val y1 = nextNumber() ?: return
            val x2 = nextNumber() ?: return
            val y2 = nextNumber() ?: return
            val x = nextNumber() ?: return
            val y = nextNumber() ?: return
            val controlX1 = if (relative) currentX + x1 else x1
            val controlY1 = if (relative) currentY + y1 else y1
            val controlX2 = if (relative) currentX + x2 else x2
            val controlY2 = if (relative) currentY + y2 else y2
            currentX = if (relative) currentX + x else x
            currentY = if (relative) currentY + y else y
            path.curveTo(controlX1, controlY1, controlX2, controlY2, currentX, currentY)
            lastControlX = controlX2
            lastControlY = controlY2
        }
    }

    private fun smoothCubicTo(path: Path2D.Float, relative: Boolean) {
        while (hasNumberAhead()) {
            val x2 = nextNumber() ?: return
            val y2 = nextNumber() ?: return
            val x = nextNumber() ?: return
            val y = nextNumber() ?: return
            val controlX1 = if (lastCommand == 'C' || lastCommand == 'c' || lastCommand == 'S' || lastCommand == 's') {
                2 * currentX - lastControlX
            } else {
                currentX
            }
            val controlY1 = if (lastCommand == 'C' || lastCommand == 'c' || lastCommand == 'S' || lastCommand == 's') {
                2 * currentY - lastControlY
            } else {
                currentY
            }
            val controlX2 = if (relative) currentX + x2 else x2
            val controlY2 = if (relative) currentY + y2 else y2
            currentX = if (relative) currentX + x else x
            currentY = if (relative) currentY + y else y
            path.curveTo(controlX1, controlY1, controlX2, controlY2, currentX, currentY)
            lastControlX = controlX2
            lastControlY = controlY2
        }
    }

    private fun quadTo(path: Path2D.Float, relative: Boolean) {
        while (hasNumberAhead()) {
            val x1 = nextNumber() ?: return
            val y1 = nextNumber() ?: return
            val x = nextNumber() ?: return
            val y = nextNumber() ?: return
            val controlX = if (relative) currentX + x1 else x1
            val controlY = if (relative) currentY + y1 else y1
            currentX = if (relative) currentX + x else x
            currentY = if (relative) currentY + y else y
            path.quadTo(controlX, controlY, currentX, currentY)
            lastControlX = controlX
            lastControlY = controlY
        }
    }

    private fun smoothQuadTo(path: Path2D.Float, relative: Boolean) {
        while (hasNumberAhead()) {
            val x = nextNumber() ?: return
            val y = nextNumber() ?: return
            val controlX = if (lastCommand == 'Q' || lastCommand == 'q' || lastCommand == 'T' || lastCommand == 't') {
                2 * currentX - lastControlX
            } else {
                currentX
            }
            val controlY = if (lastCommand == 'Q' || lastCommand == 'q' || lastCommand == 'T' || lastCommand == 't') {
                2 * currentY - lastControlY
            } else {
                currentY
            }
            currentX = if (relative) currentX + x else x
            currentY = if (relative) currentY + y else y
            path.quadTo(controlX, controlY, currentX, currentY)
            lastControlX = controlX
            lastControlY = controlY
        }
    }

    private fun arcTo(path: Path2D.Float, relative: Boolean) {
        while (hasNumberAhead()) {
            val radiusX = nextNumber() ?: return
            val radiusY = nextNumber() ?: return
            val angle = nextNumber() ?: return
            val largeArc = (nextNumber() ?: return) != 0f
            val sweep = (nextNumber() ?: return) != 0f
            val x = nextNumber() ?: return
            val y = nextNumber() ?: return
            val endX = if (relative) currentX + x else x
            val endY = if (relative) currentY + y else y
            drawArc(path, currentX, currentY, endX, endY, radiusX, radiusY, angle, largeArc, sweep)
            currentX = endX
            currentY = endY
        }
    }

    private fun drawArc(
        path: Path2D.Float,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        inputRadiusX: Float,
        inputRadiusY: Float,
        angle: Float,
        largeArc: Boolean,
        sweep: Boolean,
    ) {
        if (inputRadiusX == 0f || inputRadiusY == 0f || startX == endX && startY == endY) {
            path.lineTo(endX, endY)
            return
        }

        val radians = Math.toRadians(angle.toDouble())
        val cosAngle = cos(radians)
        val sinAngle = sin(radians)
        val dx = (startX - endX) / 2.0
        val dy = (startY - endY) / 2.0
        val x1 = cosAngle * dx + sinAngle * dy
        val y1 = -sinAngle * dx + cosAngle * dy
        var radiusX = abs(inputRadiusX.toDouble())
        var radiusY = abs(inputRadiusY.toDouble())

        val radiiScale = x1.pow(2) / radiusX.pow(2) + y1.pow(2) / radiusY.pow(2)
        if (radiiScale > 1.0) {
            val scale = sqrt(radiiScale)
            radiusX *= scale
            radiusY *= scale
        }

        val sign = if (largeArc == sweep) -1.0 else 1.0
        val numerator = radiusX.pow(2) * radiusY.pow(2) - radiusX.pow(2) * y1.pow(2) - radiusY.pow(2) * x1.pow(2)
        val denominator = radiusX.pow(2) * y1.pow(2) + radiusY.pow(2) * x1.pow(2)
        val centerScale = sign * sqrt(max(0.0, numerator / denominator))
        val centerX1 = centerScale * radiusX * y1 / radiusY
        val centerY1 = -centerScale * radiusY * x1 / radiusX
        val centerX = cosAngle * centerX1 - sinAngle * centerY1 + (startX + endX) / 2.0
        val centerY = sinAngle * centerX1 + cosAngle * centerY1 + (startY + endY) / 2.0

        val startVectorX = (x1 - centerX1) / radiusX
        val startVectorY = (y1 - centerY1) / radiusY
        val endVectorX = (-x1 - centerX1) / radiusX
        val endVectorY = (-y1 - centerY1) / radiusY
        var startAngle = vectorAngle(1.0, 0.0, startVectorX, startVectorY)
        var sweepAngle = vectorAngle(startVectorX, startVectorY, endVectorX, endVectorY)
        if (!sweep && sweepAngle > 0) sweepAngle -= 2 * PI
        if (sweep && sweepAngle < 0) sweepAngle += 2 * PI

        val segments = ceil(abs(sweepAngle) / (PI / 2)).toInt().coerceAtLeast(1)
        val segmentAngle = sweepAngle / segments
        repeat(segments) {
            val nextAngle = startAngle + segmentAngle
            arcSegmentTo(path, centerX, centerY, radiusX, radiusY, radians, startAngle, nextAngle)
            startAngle = nextAngle
        }
    }

    private fun arcSegmentTo(
        path: Path2D.Float,
        centerX: Double,
        centerY: Double,
        radiusX: Double,
        radiusY: Double,
        rotation: Double,
        startAngle: Double,
        endAngle: Double,
    ) {
        val delta = endAngle - startAngle
        val alpha = sin(delta) * (sqrt(4 + 3 * (tan(delta / 2) * tan(delta / 2))) - 1) / 3
        val sinStart = sin(startAngle)
        val cosStart = cos(startAngle)
        val sinEnd = sin(endAngle)
        val cosEnd = cos(endAngle)
        val x1 = radiusX * (cosStart - alpha * sinStart)
        val y1 = radiusY * (sinStart + alpha * cosStart)
        val x2 = radiusX * (cosEnd + alpha * sinEnd)
        val y2 = radiusY * (sinEnd - alpha * cosEnd)
        val x = radiusX * cosEnd
        val y = radiusY * sinEnd
        path.curveTo(
            mapX(centerX, centerY, x1, y1, rotation).toFloat(),
            mapY(centerX, centerY, x1, y1, rotation).toFloat(),
            mapX(centerX, centerY, x2, y2, rotation).toFloat(),
            mapY(centerX, centerY, x2, y2, rotation).toFloat(),
            mapX(centerX, centerY, x, y, rotation).toFloat(),
            mapY(centerX, centerY, x, y, rotation).toFloat(),
        )
    }

    private fun vectorAngle(ux: Double, uy: Double, vx: Double, vy: Double): Double {
        val sign = if (ux * vy - uy * vx < 0) -1.0 else 1.0
        val dot = ux * vx + uy * vy
        val length = sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy))
        return sign * acos((dot / length).coerceIn(-1.0, 1.0))
    }

    private fun mapX(centerX: Double, centerY: Double, x: Double, y: Double, rotation: Double): Double =
        centerX + cos(rotation) * x - sin(rotation) * y

    private fun mapY(centerX: Double, centerY: Double, x: Double, y: Double, rotation: Double): Double =
        centerY + sin(rotation) * x + cos(rotation) * y

    private fun hasNumberAhead(): Boolean {
        skipSeparators()
        return index < data.length && !data[index].isLetter()
    }

    private fun skipSeparators(): Boolean {
        while (index < data.length && (data[index].isWhitespace() || data[index] == ',')) {
            index++
        }
        return index < data.length
    }

    private fun nextNumber(): Float? {
        skipSeparators()
        if (index >= data.length) return null
        val start = index
        if (data[index] == '-' || data[index] == '+') index++
        while (index < data.length && data[index].isDigit()) index++
        if (index < data.length && data[index] == '.') {
            index++
            while (index < data.length && data[index].isDigit()) index++
        }
        if (index < data.length && (data[index] == 'e' || data[index] == 'E')) {
            index++
            if (index < data.length && (data[index] == '-' || data[index] == '+')) index++
            while (index < data.length && data[index].isDigit()) index++
        }
        return data.substring(start, index).toFloatOrNull()
    }
}
