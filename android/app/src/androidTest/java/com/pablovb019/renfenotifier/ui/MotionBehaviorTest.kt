package com.pablovb019.renfenotifier.ui

import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.core.model.FollowUpLifecycle
import com.pablovb019.renfenotifier.core.network.model.EpisodeOut
import com.pablovb019.renfenotifier.core.network.model.FollowUpDetailOut
import com.pablovb019.renfenotifier.core.network.model.FollowUpOut
import com.pablovb019.renfenotifier.feature.followups.FollowUpDetailContent
import com.pablovb019.renfenotifier.feature.followups.FollowUpDetailUiState
import com.pablovb019.renfenotifier.feature.followups.FollowUpsContent
import com.pablovb019.renfenotifier.feature.followups.FollowUpsUiState
import com.pablovb019.renfenotifier.ui.theme.RenfeMotion
import com.pablovb019.renfenotifier.ui.theme.RenfeNotifierTheme
import com.pablovb019.renfenotifier.ui.theme.renfeSpring
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Comportamiento del movimiento: duraciones cortas y transiciones sin doble callback. */
@RunWith(AndroidJUnit4::class)
class MotionBehaviorTest {

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
    fun duracionesDeMovimientoCortasYOrdenadas() {
        assertTrue("Short=${RenfeMotion.Short}", RenfeMotion.Short in 100..200)
        assertTrue("Normal=${RenfeMotion.Normal}", RenfeMotion.Normal in 150..250)
        assertTrue("Medium=${RenfeMotion.Medium}", RenfeMotion.Medium in 200..400)
        assertTrue(RenfeMotion.Short < RenfeMotion.Normal)
        assertTrue(RenfeMotion.Normal < RenfeMotion.Medium)
    }

    @Test
    fun resorteRenfeSuaveYDeRigidezMedia() {
        val springSpec = renfeSpring()
        assertEquals("Rigidez debe ser StiffnessMedium", Spring.StiffnessMedium, springSpec.stiffness, 1e-3f)
        assertTrue(
            "Damping debe ser 0.9, era ${springSpec.dampingRatio}",
            abs(springSpec.dampingRatio - 0.9f) < 1e-3f,
        )
    }

    @Test
    fun crossfadeDeCargaConservaOrdenYNoDuplicaCallback() {
        val opened = mutableListOf<String>()
        val items = listOf(
            sampleFollowUp,
            sampleFollowUp.copy(
                followupId = "fu-2",
                specificTrainId = "AVANT 8482",
            ),
        )
        composeRule.setContent {
            RenfeNotifierTheme {
                var state by remember { mutableStateOf(FollowUpsUiState(loading = true)) }
                Column {
                    Button(onClick = { state = FollowUpsUiState(items = items) }) {
                        Text("cargar")
                    }
                    FollowUpsContent(
                        uiState = state,
                        onFilterSelected = {},
                        onRefresh = {},
                        onOpenDetail = { opened.add(it) },
                    )
                }
            }
        }

        composeRule.onNodeWithTag("renfe_loading_state").assertIsDisplayed()
        composeRule.onNodeWithText("cargar").performClick()

        val firstDesc = composeRule.activity.getString(R.string.followup_card_desc, "fu-1")
        val secondDesc = composeRule.activity.getString(R.string.followup_card_desc, "fu-2")
        composeRule.onNodeWithContentDescription(firstDesc).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(secondDesc).assertIsDisplayed()

        val firstTop = composeRule.onNodeWithContentDescription(firstDesc).getBoundsInRoot().top
        val secondTop = composeRule.onNodeWithContentDescription(secondDesc).getBoundsInRoot().top
        assertTrue(
            "fu-1 (top=$firstTop) debe quedar por encima de fu-2 (top=$secondTop)",
            firstTop < secondTop,
        )

        composeRule.onNodeWithContentDescription(firstDesc).performClick()
        assertEquals(listOf("fu-1"), opened)
    }

    @Test
    fun detalleCruzaDeCargaAContenido() {
        composeRule.setContent {
            RenfeNotifierTheme {
                var state by remember {
                    mutableStateOf(FollowUpDetailUiState(loading = true))
                }
                Column {
                    Button(onClick = {
                        state = FollowUpDetailUiState(loading = false, detail = sampleDetail)
                    }) {
                        Text("cargar")
                    }
                    FollowUpDetailContent(
                        uiState = state,
                        onRefresh = {},
                        onPause = {},
                        onResume = {},
                        onRenew = {},
                        onAcknowledge = {},
                        onDelete = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag("renfe_loading_state").assertIsDisplayed()
        composeRule.onNodeWithText("cargar").performClick()

        val header = composeRule.activity.getString(R.string.followups_last_check, "23/09/2026 08:30")
        composeRule.onNodeWithText(header).assertIsDisplayed()
    }
}