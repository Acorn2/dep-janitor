package com.depjanitor.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.depjanitor.app.ui.theme.panelBorder

@Composable
fun PanelCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.panelBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    subtitle?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                content()
            },
        )
    }
}

@Composable
fun MetricPill(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.panelBorder),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        }
    }
}

@Composable
fun Badge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun GradientBar(label: String, value: String, progress: Float, colors: List<Color>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = MaterialTheme.colorScheme.onSurface)
            Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f), RoundedCornerShape(999.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .background(Brush.horizontalGradient(colors), RoundedCornerShape(999.dp))
                    .padding(vertical = 7.dp),
            )
        }
    }
}
