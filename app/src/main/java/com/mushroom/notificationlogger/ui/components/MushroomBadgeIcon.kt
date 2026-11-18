package com.mushroom.notificationlogger.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 小さな赤い傘に白い斑点をあしらった控えめなきのこモチーフアイコン。
 */
@Composable
fun MushroomBadgeIcon(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height

        // 軸 (Stem)
        val stemWidth = w * 0.30f
        val stemHeight = h * 0.36f
        val stemLeft = (w - stemWidth) / 2f
        val stemTop = h * 0.52f
        drawRoundRect(
            color = Color(0xFFEFE4D3),
            topLeft = Offset(stemLeft, stemTop),
            size = Size(stemWidth, stemHeight),
            cornerRadius = CornerRadius(stemWidth * 0.45f, stemWidth * 0.45f)
        )

        // 傘の下のわずかな影
        drawRect(
            color = Color(0xFFDFD0BA),
            topLeft = Offset(stemLeft, stemTop),
            size = Size(stemWidth, h * 0.06f)
        )

        // 赤い傘 (Cap)
        val capPath = Path().apply {
            val left = w * 0.12f
            val right = w * 0.88f
            val bottom = h * 0.56f
            val top = h * 0.12f
            moveTo(left, bottom)
            arcTo(
                rect = Rect(left = left, top = top, right = right, bottom = bottom + (bottom - top) * 0.85f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            quadraticTo(right, bottom + h * 0.05f, right - w * 0.08f, bottom + h * 0.05f)
            lineTo(left + w * 0.08f, bottom + h * 0.05f)
            quadraticTo(left, bottom + h * 0.05f, left, bottom)
            close()
        }
        drawPath(path = capPath, color = Color(0xFFD64035))

        // 白い斑点 (White spots)
        drawCircle(
            color = Color.White,
            radius = w * 0.068f,
            center = Offset(w * 0.37f, h * 0.33f)
        )
        drawCircle(
            color = Color.White,
            radius = w * 0.054f,
            center = Offset(w * 0.62f, h * 0.31f)
        )
        drawCircle(
            color = Color.White,
            radius = w * 0.058f,
            center = Offset(w * 0.53f, h * 0.46f)
        )
        drawCircle(
            color = Color.White,
            radius = w * 0.042f,
            center = Offset(w * 0.27f, h * 0.47f)
        )
    }
}
