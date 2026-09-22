package com.pablovb019.renfenotifier.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pablovb019.renfenotifier.ui.theme.LocalSuccessColors

/** Tipo de estado representado por una [RenfeStatusBadge]. */
enum class BadgeType { SUCCESS, WARNING, ERROR, NEUTRAL }

/**
 * Insignia compacta de estado: fondo tintado con color del esquema (SUCCESS ->
 * verde success, WARNING -> tertiaryContainer, ERROR -> errorContainer,
 * NEUTRAL -> surfaceVariant), icono opcional y etiqueta.
 */
@Composable
fun RenfeStatusBadge(
    label: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    type: BadgeType = BadgeType.NEUTRAL,
) {
    val success = LocalSuccessColors.current
    val container = when (type) {
        BadgeType.SUCCESS -> success.container
        BadgeType.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
        BadgeType.ERROR -> MaterialTheme.colorScheme.errorContainer
        BadgeType.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
    }
    val content = when (type) {
        BadgeType.SUCCESS -> success.onContainer
        BadgeType.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
        BadgeType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        BadgeType.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .background(color = container, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = label,
            color = content,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}