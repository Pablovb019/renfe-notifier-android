package com.pablovb019.renfenotifier.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.core.model.FollowUpLifecycle
import com.pablovb019.renfenotifier.core.network.model.EpisodeOut
import com.pablovb019.renfenotifier.core.network.model.FollowUpDetailOut
import com.pablovb019.renfenotifier.feature.diagnostics.DiagnosticsContent
import com.pablovb019.renfenotifier.feature.diagnostics.DiagnosticsUiState
import com.pablovb019.renfenotifier.feature.followups.FollowUpDetailContent
import com.pablovb019.renfenotifier.feature.followups.FollowUpDetailUiState
import com.pablovb019.renfenotifier.feature.followups.FollowUpsContent
import com.pablovb019.renfenotifier.feature.followups.FollowUpsUiState
import com.pablovb019.renfenotifier.feature.home.HomeContent
import com.pablovb019.renfenotifier.feature.home.HomeUiState
import com.pablovb019.renfenotifier.feature.pairing.PairingContent
import com.pablovb019.renfenotifier.feature.pairing.PairingUiState
import com.pablovb019.renfenotifier.feature.search.SearchContent
import com.pablovb019.renfenotifier.feature.search.SearchUiState
import com.pablovb019.renfenotifier.ui.theme.RenfeNotifierTheme
import com.pablovb019.renfenotifier.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regresión del rediseño: cada pantalla (Content público) se renderiza con
 * estados y callbacks falsos, sin ViewModels, sin OTP y sin consultas a Renfe.
 * No espera red: solo verifica que el árbol clave de cada pantalla compone y
 * que los elementos esenciales son accesibles/visibles.
 */
@RunWith(AndroidJUnit4::class)
class RedesignRegressionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun text(resId: Int): String = composeRule.activity.getString(resId)

    private val sampleDetail = FollowUpDetailOut(
        followupId = "fu-1",
        originCode = "ZAR",
        originName = "Zaragoza-Delicias",
        destinationCode = "MAD",
        destinationName = "Madrid Puerta de Atocha",
        travelDate = "2026-09-25",
        mode = "specific",
        plazaH = true,
        specificTrainId = "AVANT 8492|11:08|12:13",
        lifecycle = FollowUpLifecycle.ACTIVE,
        availability = "available",
        alertState = "pending_alert",
        episode = 2,
        expiresAt = "2026-09-30T12:00:00+02:00",
        episodes = listOf(
            EpisodeOut(
                episodeId = 1,
                episode = 1,
                observedAt = "2026-09-22T10:00:00+02:00",
                trainIds = listOf("AVANT 8492", "AVANT 8482"),
            ),
            EpisodeOut(
                episodeId = 2,
                episode = 2,
                observedAt = "2026-09-23T08:30:00+02:00",
                trainIds = listOf("AVANT 8492"),
            ),
        ),
    )

    @Test
    fun regresionHomeSeRenderizaConFakes() {
        composeRule.setContent {
            RenfeNotifierTheme {
                HomeContent(uiState = HomeUiState(loading = false, isPaired = true))
            }
        }
        composeRule.onNodeWithText(text(R.string.home_paired)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.home_search_button)).assertIsDisplayed()
    }

    @Test
    fun regresionSearchSeRenderizaConFakes() {
        composeRule.setContent {
            RenfeNotifierTheme {
                SearchContent(
                    uiState = SearchUiState(),
                    onOriginQueryChange = {},
                    onOriginSelected = {},
                    onDestinationQueryChange = {},
                    onDestinationSelected = {},
                    onDateSelected = {},
                    onPlazaHChange = {},
                    onSearch = {},
                    onModeSelected = {},
                    onTrainSelected = {},
                    onCreateFollowUp = {},
                    onCreatedAccepted = {},
                )
            }
        }
        composeRule.onNodeWithText(text(R.string.search_origin)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.search_button))
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun regresionFollowUpsSeRenderizaConFakes() {
        composeRule.setContent {
            RenfeNotifierTheme {
                FollowUpsContent(
                    uiState = FollowUpsUiState(),
                    onFilterSelected = {},
                    onRefresh = {},
                    onOpenDetail = {},
                )
            }
        }
        composeRule.onNodeWithTag("followups_filters").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.followups_empty)).assertIsDisplayed()
    }

    @Test
    fun regresionDetalleSeRenderizaConFakes() {
        composeRule.setContent {
            RenfeNotifierTheme {
                FollowUpDetailContent(
                    uiState = FollowUpDetailUiState(loading = false, detail = sampleDetail),
                    onRefresh = {},
                    onPause = {},
                    onResume = {},
                    onRenew = {},
                    onAcknowledge = {},
                    onDelete = {},
                )
            }
        }
        val expected = composeRule.activity.getString(
            R.string.followups_last_check,
            "23/09/2026 08:30",
        )
        composeRule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun regresionDiagnosticsSeRenderizaConFakes() {
        composeRule.setContent {
            RenfeNotifierTheme {
                DiagnosticsContent(
                    uiState = DiagnosticsUiState(),
                    themeMode = ThemeMode.SYSTEM,
                    themeLoading = false,
                    themeSaveError = null,
                    onSelectTheme = {},
                    onSetAlertsEnabled = {},
                    onSendTest = {},
                )
            }
        }
        composeRule.onNodeWithText(text(R.string.diag_section_appearance)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.diag_section_alerts)).assertIsDisplayed()
    }

    @Test
    fun regresionPairingSeRenderizaConFakes() {
        composeRule.setContent {
            RenfeNotifierTheme {
                PairingContent(
                    uiState = PairingUiState(),
                    onCodeChange = {},
                    onClaim = {},
                )
            }
        }
        composeRule.onNodeWithText(text(R.string.pairing_hint)).assertIsDisplayed()
    }
}