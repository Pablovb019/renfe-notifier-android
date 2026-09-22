package com.pablovb019.renfenotifier.feature.followups

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pablovb019.renfenotifier.R
import com.pablovb019.renfenotifier.core.model.FollowUpLifecycle
import com.pablovb019.renfenotifier.core.network.model.EpisodeOut
import com.pablovb019.renfenotifier.core.network.model.FollowUpDetailOut
import com.pablovb019.renfenotifier.ui.theme.RenfeNotifierTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests instrumentales del contenido del detalle de un seguimiento. */
@RunWith(AndroidJUnit4::class)
class FollowUpDetailContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

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

    private fun state(
        detail: FollowUpDetailOut = sampleDetail,
        actionInProgress: Boolean = false,
        actionError: String? = null,
    ) = FollowUpDetailUiState(
        loading = false,
        actionInProgress = actionInProgress,
        actionError = actionError,
        detail = detail,
        deleted = false,
    )

    private fun setDetailContent(
        uiState: FollowUpDetailUiState,
        onPause: () -> Unit = {},
        onResume: () -> Unit = {},
        onRenew: () -> Unit = {},
        onAcknowledge: () -> Unit = {},
        onDelete: () -> Unit = {},
    ) {
        composeRule.setContent {
            RenfeNotifierTheme {
                FollowUpDetailContent(
                    uiState = uiState,
                    onRefresh = {},
                    onPause = onPause,
                    onResume = onResume,
                    onRenew = onRenew,
                    onAcknowledge = onAcknowledge,
                    onDelete = onDelete,
                )
            }
        }
    }

    private fun text(resId: Int): String = composeRule.activity.getString(resId)

    @Test
    fun lasAccionesEstanSeparadas() {
        setDetailContent(uiState = state())
        val deleteLabel = text(R.string.followups_delete)
        composeRule.onNodeWithText(text(R.string.followups_pause))
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.followups_renew))
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.followups_acknowledge))
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(deleteLabel).performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithText(text(R.string.followups_pause)).assertCountEquals(1)
        composeRule.onAllNodesWithText(text(R.string.followups_acknowledge)).assertCountEquals(1)
        composeRule.onAllNodesWithText(deleteLabel).assertCountEquals(1)
    }

    @Test
    fun pausarInvocaCallbackUnaVez() {
        var pauses = 0
        setDetailContent(uiState = state(), onPause = { pauses++ })
        composeRule.onNodeWithText(text(R.string.followups_pause))
            .performScrollTo().performClick()
        assertEquals(1, pauses)
    }

    @Test
    fun cicloPausadoMuestraReanudar() {
        val paused = sampleDetail.copy(lifecycle = FollowUpLifecycle.PAUSED)
        var resumes = 0
        setDetailContent(uiState = state(detail = paused), onResume = { resumes++ })
        val pause = text(R.string.followups_pause)
        val resume = text(R.string.followups_resume)
        composeRule.onNodeWithText(resume).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(resume).performClick()
        assertEquals(1, resumes)
        composeRule.onNodeWithText(pause).assertDoesNotExist()
    }

    @Test
    fun confirmarAvisoNoElimina() {
        var acknowledges = 0
        var deletes = 0
        setDetailContent(
            uiState = state(),
            onAcknowledge = { acknowledges++ },
            onDelete = { deletes++ },
        )
        composeRule.onNodeWithText(text(R.string.followups_acknowledge))
            .performScrollTo().performClick()
        assertEquals(1, acknowledges)
        assertEquals(0, deletes)
    }

    @Test
    fun cancelarBorradoNoElimina() {
        val deletes = mutableListOf<Unit>()
        setDetailContent(uiState = state(), onDelete = { deletes.add(Unit) })
        val deleteLabel = composeRule.activity.getString(R.string.followups_delete)
        val dialogMessage = composeRule.activity.getString(R.string.followups_delete_dialog_message)

        composeRule.onNodeWithText(deleteLabel).performScrollTo().performClick()
        composeRule.onNodeWithText(dialogMessage).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.dialog_cancel)).performClick()
        assertEquals(0, deletes.size)
        composeRule.onNodeWithText(dialogMessage).assertDoesNotExist()

        composeRule.onNodeWithText(deleteLabel).performScrollTo().performClick()
        composeRule.onNodeWithText(text(R.string.followups_delete_confirm)).performClick()
        assertEquals(1, deletes.size)
    }

    @Test
    fun accionEnCursoDeshabilitaLosBotones() {
        setDetailContent(uiState = state(actionInProgress = true))
        composeRule.onNodeWithText(text(R.string.followups_pause))
            .performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.followups_renew))
            .performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.followups_acknowledge))
            .performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText(text(R.string.followups_delete))
            .performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun cabeceraMuestraUltimaComprobacionValidaYNumeroEpisodios() {
        setDetailContent(uiState = state())
        val expected = composeRule.activity.getString(
            R.string.followups_last_check,
            "23/09/2026 08:30",
        )
        val count = composeRule.activity.getString(R.string.followups_episode_count, 2)
        composeRule.onNodeWithText(expected).assertIsDisplayed()
        composeRule.onNodeWithText(count).assertIsDisplayed()
    }
}