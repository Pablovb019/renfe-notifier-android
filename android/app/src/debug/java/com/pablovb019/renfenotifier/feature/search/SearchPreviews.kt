package com.pablovb019.renfenotifier.feature.search

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.pablovb019.renfenotifier.core.model.Availability
import com.pablovb019.renfenotifier.core.model.FollowUpMode
import com.pablovb019.renfenotifier.core.network.model.TrainOut
import com.pablovb019.renfenotifier.ui.theme.RenfeNotifierTheme
import java.time.LocalDate

private const val SEARCH_PREVIEW_DEVICE = "spec:width=1080px,height=2400px,dpi=480"

private val previewTrains = listOf(
    TrainOut(
        identifier = "8492",
        identity = "AVANT 8492",
        departure = "08:45",
        arrival = "09:31",
        price = "8,55 €",
        availability = Availability.AVAILABLE,
    ),
    TrainOut(
        identifier = "4186",
        identity = "REGIONAL 4186",
        departure = "12:10",
        arrival = "14:02",
        price = null,
        availability = Availability.NO_AVAILABILITY,
    ),
)

private fun previewUiState(withResults: Boolean) = SearchUiState(
    originQuery = "Zaragoza",
    destinationQuery = "Madrid",
    travelDate = LocalDate.of(2026, 9, 25),
    isSearching = false,
    searchStatus = if (withResults) "ok" else null,
    trains = if (withResults) previewTrains else emptyList(),
    mode = FollowUpMode.FIRST,
)

@Preview(
    name = "Búsqueda claro",
    widthDp = 360,
    heightDp = 800,
    device = SEARCH_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showSystemUi = true,
)
@Composable
private fun SearchPreviewFormLight() {
    SearchPreviewContent(withResults = false)
}

@Preview(
    name = "Búsqueda oscuro",
    widthDp = 360,
    heightDp = 800,
    device = SEARCH_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showSystemUi = true,
)
@Composable
private fun SearchPreviewFormDark() {
    SearchPreviewContent(withResults = false)
}

@Preview(
    name = "Búsqueda claro, fuente 2x",
    widthDp = 360,
    heightDp = 800,
    device = SEARCH_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    fontScale = 2f,
)
@Composable
private fun SearchPreviewResultsLightLargeFont() {
    SearchPreviewContent(withResults = true)
}

@Preview(
    name = "Búsqueda oscuro, fuente 2x",
    widthDp = 360,
    heightDp = 800,
    device = SEARCH_PREVIEW_DEVICE,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    fontScale = 2f,
)
@Composable
private fun SearchPreviewResultsDarkLargeFont() {
    SearchPreviewContent(withResults = true)
}

@Composable
private fun SearchPreviewContent(withResults: Boolean) {
    RenfeNotifierTheme {
        SearchContent(
            uiState = previewUiState(withResults),
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