package com.pablovb019.renfenotifier.feature.followups

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.core.model.FollowUpLifecycle
import com.pablovb019.renfenotifier.core.network.model.FollowUpOut
import com.pablovb019.renfenotifier.ui.theme.RenfeNotifierTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests instrumentales del contenido de la lista de seguimientos. */
@RunWith(AndroidJUnit4::class)
class FollowUpsContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val sampleFollowUp = FollowUpOut(
        followupId = "fu-1",
        originCode = "ZAR",
        originName = "Zaragoza-Delicias",
        destinationCode = "MAD",
        destinationName = "Madrid Puerta de Atocha",
        travelDate = "2026-09-25",
        mode = "specific",
        plazaH = false,
        specificTrainId = "AVANT 8492",
        lifecycle = FollowUpLifecycle.ACTIVE,
        availability = "available",
        alertState = "pending_alert",
        episode = 1,
        expiresAt = "2026-09-30T12:00:00+02:00",
    )

    private fun state(
        filter: LifecycleFilter = LifecycleFilter.ALL,
        items: List<FollowUpOut> = listOf(sampleFollowUp),
        error: String? = null,
        loading: Boolean = false,
    ) = FollowUpsUiState(loading = loading, error = error, filter = filter, items = items)

    private fun setFollowUpsContent(
        uiState: FollowUpsUiState,
        onFilterSelected: (LifecycleFilter) -> Unit = {},
        onRefresh: () -> Unit = {},
        onOpenDetail: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            RenfeNotifierTheme {
                FollowUpsContent(
                    uiState = uiState,
                    onFilterSelected = onFilterSelected,
                    onRefresh = onRefresh,
                    onOpenDetail = onOpenDetail,
                )
            }
        }
    }

    @Test
    fun losCincoFiltrosSonAccesibles() {
        val labels = listOf(
            R.string.followups_filter_all,
            R.string.followups_filter_active,
            R.string.followups_filter_paused,
            R.string.followups_filter_expired,
            R.string.followups_filter_deleted,
        )
        setFollowUpsContent(uiState = FollowUpsUiState())
        labels.forEach { labelRes ->
            val label = composeRule.activity.getString(labelRes)
            composeRule.onNodeWithTag("followups_filters")
                .performScrollToNode(hasText(label))
            composeRule.onNodeWithText(label).assertIsDisplayed()
        }
    }

    @Test
    fun clickEnFiltroInvocaCallbackUnaVez() {
        val selected = mutableListOf<LifecycleFilter>()
        setFollowUpsContent(
            uiState = state(filter = LifecycleFilter.ALL),
            onFilterSelected = { selected.add(it) },
        )
        val label = composeRule.activity.getString(R.string.followups_filter_active)
        composeRule.onNodeWithTag("followups_filters")
            .performScrollToNode(hasText(label))
        composeRule.onNodeWithText(label).performClick()
        assertEquals(listOf(LifecycleFilter.ACTIVE), selected)
    }

    @Test
    fun clickEnTarjetaAbreDetalleUnaVez() {
        val opened = mutableListOf<String>()
        setFollowUpsContent(uiState = state(), onOpenDetail = { opened.add(it) })
        val description = composeRule.activity.getString(R.string.followup_card_desc, "fu-1")
        composeRule.onNodeWithContentDescription(description).assertIsDisplayed().performClick()
        assertEquals(listOf("fu-1"), opened)
    }

    @Test
    fun estadoVacioMuestraMensaje() {
        setFollowUpsContent(uiState = FollowUpsUiState())
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.followups_empty),
        ).assertIsDisplayed()
    }

    @Test
    fun estadoErrorMuestraReintentarQueRecarga() {
        var refreshes = 0
        setFollowUpsContent(
            uiState = state(error = "Error de prueba"),
            onRefresh = { refreshes++ },
        )
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.common_retry),
        ).performClick()
        assertEquals(1, refreshes)
    }
}