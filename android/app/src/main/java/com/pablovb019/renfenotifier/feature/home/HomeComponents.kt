package com.pablovb019.renfenotifier.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.ui.components.BadgeType
import com.pablovb019.renfenotifier.ui.components.RenfeStatusBadge
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Encabezado de la pantalla de inicio: titulo grande, maximo 2 lineas.
 */
@Composable
fun HomeHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

/**
 * Insignia de estado "Dispositivo vinculado" cuando ya hay emparejamiento.
 */
@Composable
fun HomePairedBadge(modifier: Modifier = Modifier) {
    RenfeStatusBadge(
        label = stringResource(R.string.home_paired),
        icon = Icons.Filled.CheckCircle,
        type = BadgeType.SUCCESS,
        modifier = modifier,
    )
}

/**
 * Hora local y version, en tipo y color discretos.
 */
@Composable
fun HomeMeta(
    now: LocalDateTime?,
    versionName: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        now?.let {
            Text(
                text = stringResource(R.string.home_clock, it.format(HOUR_FORMAT)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Text(
            text = stringResource(R.string.home_version, versionName),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/**
 * Aviso de permisos de notificacion denegados, con acceso a los ajustes del sistema.
 */
@Composable
fun HomeNotificationNotice(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.home_notifs_denied_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.home_notifs_denied_body),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onOpenSettings) {
            Text(text = stringResource(R.string.home_notifs_open_settings))
        }
    }
}

private val HOUR_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")