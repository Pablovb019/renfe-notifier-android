package com.pablovb019.renfenotifier.feature.followups

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.core.network.ApiModule
import com.pablovb019.renfenotifier.ui.components.RenfeErrorState
import com.pablovb019.renfenotifier.ui.components.RenfeLoadingState
import com.pablovb019.renfenotifier.ui.components.RenfeScreenScaffold
import com.pablovb019.renfenotifier.ui.theme.RenfeMotion
import com.pablovb019.renfenotifier.ui.theme.RenfeSpacing

/**
 * Listado de seguimientos con filtro por ciclo de vida. Tocar un elemento abre
 * su detalle; al volver, el listado se recarga (fuente de verdad = backend).
 */
@Composable
fun FollowUpsScreen(
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FollowUpsViewModel = viewModel {
        val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            ?: error("Application no disponible para el ViewModel de seguimientos")
        ApiModule.init(app)
        FollowUpsViewModel(api = ApiModule.api())
    },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }

    RenfeScreenScaffold(
        title = stringResource(R.string.followups_title),
        onBack = onBack,
        modifier = modifier,
    ) { innerPadding ->
        FollowUpsContent(
            uiState = uiState,
            onFilterSelected = viewModel::onFilterSelected,
            onRefresh = viewModel::load,
            onOpenDetail = onOpenDetail,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun FollowUpsContent(
    uiState: FollowUpsUiState,
    onFilterSelected: (LifecycleFilter) -> Unit,
    onRefresh: () -> Unit,
    onOpenDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        FollowUpFilters(
            filter = uiState.filter,
            onFilterSelected = onFilterSelected,
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = RenfeSpacing.screenMargin),
            verticalArrangement = Arrangement.spacedBy(RenfeSpacing.sm),
        ) {
            Crossfade(
                targetState = uiState.loading,
                animationSpec = tween(RenfeMotion.Normal),
            ) { isLoading ->
                if (isLoading) {
                    RenfeLoadingState(modifier = Modifier.padding(vertical = RenfeSpacing.xxxl))
                } else {
                    uiState.error?.let { error ->
                        RenfeErrorState(
                            icon = Icons.Filled.Warning,
                            title = stringResource(R.string.followups_load_error),
                            text = error,
                            onRetry = onRefresh,
                            modifier = Modifier.padding(vertical = RenfeSpacing.lg),
                        )
                    }

                    if (uiState.isEmpty) {
                        Text(
                            text = stringResource(R.string.followups_empty),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(RenfeSpacing.sm)) {
                        items(uiState.items, key = { it.followupId }) { followUp ->
                            FollowUpCard(
                                followUp = followUp,
                                onClick = { onOpenDetail(followUp.followupId) },
                            )
                        }
                    }
                }
            }
        }
    }
}