package com.pablovb019.renfenotifier.feature.followups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.core.model.FollowUpLifecycle
import com.pablovb019.renfenotifier.core.network.model.FollowUpOut
import com.pablovb019.renfenotifier.ui.components.BadgeType
import com.pablovb019.renfenotifier.ui.components.RenfeStatusBadge
import com.pablovb019.renfenotifier.ui.theme.RenfeSpacing

/**
 * Fila horizontal de filtros por ciclo de vida. LazyRow (no una Row estática)
 * para que los 5 filtros —incluido el último— sean accesibles también a 2×.
 */
@Composable
fun FollowUpFilters(
    filter: LifecycleFilter,
    onFilterSelected: (LifecycleFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .testTag("followups_filters"),
        contentPadding = PaddingValues(horizontal = RenfeSpacing.screenMargin),
        horizontalArrangement = Arrangement.spacedBy(RenfeSpacing.sm),
    ) {
        items(items = LifecycleFilter.entries, key = { it.name }) { item ->
            FilterChip(
                selected = filter == item,
                onClick = { onFilterSelected(item) },
                label = { Text(text = filterLabel(item)) },
            )
        }
    }
}

/**
 * Tarjeta de seguimiento: ruta en jerarquía principal (nombre de estación con
 * fallback al código, máx. 2 líneas con elipsis), fecha en formato Europe/Madrid
 * y badge de ciclo de vida (ACTIVE→SUCCESS, PAUSED→WARNING, EXPIRED→ERROR,
 * DELETED→NEUTRAL). Un click abre el detalle.
 */
@Composable
fun FollowUpCard(
    followUp: FollowUpOut,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(R.string.followup_card_desc, followUp.followupId)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = description }
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(RenfeSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(RenfeSpacing.xs),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = routeLabel(followUp),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                RenfeStatusBadge(
                    label = lifecycleLabel(followUp.lifecycle),
                    icon = null,
                    type = lifecycleBadgeType(followUp.lifecycle),
                )
            }
            Text(
                text = stringResource(
                    R.string.followups_card_date,
                    MadridFormat.showTravelDate(followUp.travelDate),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = availabilityLabel(followUp.availability),
                    color = availabilityColor(followUp.availability),
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = alertLabel(followUp.alertState),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun routeLabel(followUp: FollowUpOut): String {
    val origin = followUp.originName ?: followUp.originCode
    val destination = followUp.destinationName ?: followUp.destinationCode
    return stringResource(R.string.followups_card_route, origin, destination)
}

@Composable
internal fun filterLabel(filter: LifecycleFilter): String = when (filter) {
    LifecycleFilter.ALL -> stringResource(R.string.followups_filter_all)
    LifecycleFilter.ACTIVE -> stringResource(R.string.followups_filter_active)
    LifecycleFilter.PAUSED -> stringResource(R.string.followups_filter_paused)
    LifecycleFilter.EXPIRED -> stringResource(R.string.followups_filter_expired)
    LifecycleFilter.DELETED -> stringResource(R.string.followups_filter_deleted)
}

@Composable
private fun lifecycleLabel(value: String): String = when (value) {
    FollowUpLifecycle.ACTIVE -> stringResource(R.string.followups_lifecycle_active)
    FollowUpLifecycle.PAUSED -> stringResource(R.string.followups_lifecycle_paused)
    FollowUpLifecycle.EXPIRED -> stringResource(R.string.followups_lifecycle_expired)
    FollowUpLifecycle.DELETED -> stringResource(R.string.followups_lifecycle_deleted)
    else -> value
}

@Composable
private fun availabilityLabel(value: String): String = when (value) {
    "available" -> stringResource(R.string.av_available)
    "unavailable" -> stringResource(R.string.av_none)
    else -> stringResource(R.string.av_unknown)
}

@Composable
private fun alertLabel(value: String): String = when (value) {
    "pending_alert" -> stringResource(R.string.followups_alert_pending)
    "acknowledged" -> stringResource(R.string.followups_alert_acknowledged)
    else -> stringResource(R.string.followups_alert_idle)
}

private fun lifecycleBadgeType(value: String): BadgeType = when (value) {
    FollowUpLifecycle.ACTIVE -> BadgeType.SUCCESS
    FollowUpLifecycle.PAUSED -> BadgeType.WARNING
    FollowUpLifecycle.EXPIRED -> BadgeType.ERROR
    else -> BadgeType.NEUTRAL
}

@Composable
private fun availabilityColor(value: String): androidx.compose.ui.graphics.Color = when (value) {
    "available" -> MaterialTheme.colorScheme.onPrimaryContainer
    "unavailable" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}