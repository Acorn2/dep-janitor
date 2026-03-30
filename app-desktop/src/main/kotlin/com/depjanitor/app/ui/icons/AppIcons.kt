package com.depjanitor.app.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {
    val Home: ImageVector by lazy {
        icon("Home") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
            ) {
                moveTo(4f, 10.5f)
                lineTo(12f, 4f)
                lineTo(20f, 10.5f)
                moveTo(6.5f, 9.5f)
                lineTo(6.5f, 19f)
                lineTo(17.5f, 19f)
                lineTo(17.5f, 9.5f)
                moveTo(10f, 19f)
                lineTo(10f, 13.5f)
                lineTo(14f, 13.5f)
                lineTo(14f, 19f)
            }
        }
    }

    val Insights: ImageVector by lazy {
        icon("Insights") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
            ) {
                moveTo(5f, 19f)
                lineTo(19f, 19f)
                moveTo(7f, 16f)
                lineTo(10f, 12.5f)
                lineTo(13f, 14f)
                lineTo(17f, 8f)
                moveTo(15f, 8f)
                lineTo(17f, 8f)
                lineTo(17f, 10f)
            }
        }
    }

    val Settings: ImageVector by lazy {
        icon("Settings") {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(11f, 2.8f)
                lineTo(13f, 2.8f)
                lineTo(13.4f, 4.6f)
                curveTo(14.0f, 4.8f, 14.5f, 5.0f, 15.0f, 5.3f)
                lineTo(16.6f, 4.3f)
                lineTo(18f, 5.7f)
                lineTo(17f, 7.3f)
                curveTo(17.3f, 7.8f, 17.5f, 8.4f, 17.7f, 9f)
                lineTo(19.5f, 9.4f)
                lineTo(19.5f, 11.4f)
                lineTo(17.7f, 11.8f)
                curveTo(17.5f, 12.4f, 17.3f, 13f, 17f, 13.5f)
                lineTo(18f, 15.1f)
                lineTo(16.6f, 16.5f)
                lineTo(15f, 15.5f)
                curveTo(14.5f, 15.8f, 14f, 16f, 13.4f, 16.2f)
                lineTo(13f, 18f)
                lineTo(11f, 18f)
                lineTo(10.6f, 16.2f)
                curveTo(10f, 16f, 9.5f, 15.8f, 9f, 15.5f)
                lineTo(7.4f, 16.5f)
                lineTo(6f, 15.1f)
                lineTo(7f, 13.5f)
                curveTo(6.7f, 13f, 6.5f, 12.4f, 6.3f, 11.8f)
                lineTo(4.5f, 11.4f)
                lineTo(4.5f, 9.4f)
                lineTo(6.3f, 9f)
                curveTo(6.5f, 8.4f, 6.7f, 7.8f, 7f, 7.3f)
                lineTo(6f, 5.7f)
                lineTo(7.4f, 4.3f)
                lineTo(9f, 5.3f)
                curveTo(9.5f, 5f, 10f, 4.8f, 10.6f, 4.6f)
                close()
                moveTo(12f, 8.2f)
                curveTo(9.9f, 8.2f, 8.2f, 9.9f, 8.2f, 12f)
                curveTo(8.2f, 14.1f, 9.9f, 15.8f, 12f, 15.8f)
                curveTo(14.1f, 15.8f, 15.8f, 14.1f, 15.8f, 12f)
                curveTo(15.8f, 9.9f, 14.1f, 8.2f, 12f, 8.2f)
                close()
            }
        }
    }

    val Moon: ImageVector by lazy {
        icon("Moon") {
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.EvenOdd,
            ) {
                moveTo(14.6f, 3.3f)
                curveTo(10.6f, 3.8f, 7.5f, 7.2f, 7.5f, 11.4f)
                curveTo(7.5f, 16f, 11.3f, 19.8f, 15.9f, 19.8f)
                curveTo(17f, 19.8f, 18.1f, 19.6f, 19f, 19.2f)
                curveTo(17.6f, 20.5f, 15.7f, 21.2f, 13.7f, 21.2f)
                curveTo(9f, 21.2f, 5.2f, 17.4f, 5.2f, 12.7f)
                curveTo(5.2f, 8.7f, 8f, 5.2f, 11.9f, 4.3f)
                curveTo(12.8f, 4.1f, 13.7f, 4f, 14.6f, 4.1f)
                close()
            }
        }
    }

    val Sun: ImageVector by lazy {
        icon("Sun") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
            ) {
                moveTo(12f, 3.5f)
                lineTo(12f, 5.5f)
                moveTo(12f, 18.5f)
                lineTo(12f, 20.5f)
                moveTo(3.5f, 12f)
                lineTo(5.5f, 12f)
                moveTo(18.5f, 12f)
                lineTo(20.5f, 12f)
                moveTo(6f, 6f)
                lineTo(7.4f, 7.4f)
                moveTo(16.6f, 16.6f)
                lineTo(18f, 18f)
                moveTo(16.6f, 7.4f)
                lineTo(18f, 6f)
                moveTo(6f, 18f)
                lineTo(7.4f, 16.6f)
            }
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
            ) {
                moveTo(15.5f, 12f)
                curveTo(15.5f, 13.9f, 13.9f, 15.5f, 12f, 15.5f)
                curveTo(10.1f, 15.5f, 8.5f, 13.9f, 8.5f, 12f)
                curveTo(8.5f, 10.1f, 10.1f, 8.5f, 12f, 8.5f)
                curveTo(13.9f, 8.5f, 15.5f, 10.1f, 15.5f, 12f)
            }
        }
    }

    val Refresh: ImageVector by lazy {
        icon("Refresh") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
            ) {
                moveTo(19f, 8f)
                lineTo(19f, 4f)
                lineTo(15f, 4f)
                moveTo(5f, 16f)
                lineTo(5f, 20f)
                lineTo(9f, 20f)
                moveTo(18.2f, 11f)
                curveTo(17.8f, 8f, 15.2f, 5.8f, 12f, 5.8f)
                curveTo(9.7f, 5.8f, 7.7f, 6.9f, 6.5f, 8.6f)
                moveTo(5.8f, 13f)
                curveTo(6.2f, 16f, 8.8f, 18.2f, 12f, 18.2f)
                curveTo(14.3f, 18.2f, 16.3f, 17.1f, 17.5f, 15.4f)
            }
        }
    }

    val PlayCircle: ImageVector by lazy {
        icon("PlayCircle") {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                fill = null,
            ) {
                moveTo(12f, 3.8f)
                curveTo(16.5f, 3.8f, 20.2f, 7.5f, 20.2f, 12f)
                curveTo(20.2f, 16.5f, 16.5f, 20.2f, 12f, 20.2f)
                curveTo(7.5f, 20.2f, 3.8f, 16.5f, 3.8f, 12f)
                curveTo(3.8f, 7.5f, 7.5f, 3.8f, 12f, 3.8f)
                close()
            }
            path(fill = SolidColor(Color.Black)) {
                moveTo(10f, 8.5f)
                lineTo(16f, 12f)
                lineTo(10f, 15.5f)
                close()
            }
        }
    }
}

private inline fun icon(
    name: String,
    block: ImageVector.Builder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply(block).build()
