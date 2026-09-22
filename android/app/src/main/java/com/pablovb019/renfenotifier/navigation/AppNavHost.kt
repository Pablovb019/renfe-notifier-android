package com.pablovb019.renfenotifier.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pablovb019.renfenotifier.feature.diagnostics.DiagnosticsScreen
import com.pablovb019.renfenotifier.feature.followups.FollowUpDetailScreen
import com.pablovb019.renfenotifier.feature.followups.FollowUpsScreen
import com.pablovb019.renfenotifier.feature.home.HomeScreen
import com.pablovb019.renfenotifier.feature.pairing.PairingScreen
import com.pablovb019.renfenotifier.feature.search.SearchScreen
import com.pablovb019.renfenotifier.ui.theme.RenfeMotion
import com.pablovb019.renfenotifier.ui.theme.ThemeViewModel
import kotlinx.coroutines.flow.StateFlow

/** Destinos de la aplicación. Ampliado con emparejamiento en paso 21. */
object Destinations {
    const val HOME = "home"
    const val PAIRING = "pairing"
    const val SEARCH = "search"
    const val FOLLOWUPS = "followups"
    const val FOLLOWUP_DETAIL = "followup/{followupId}"
    const val FOLLOWUP_DETAIL_ARG = "followupId"
    const val DIAGNOSTICS = "diagnostics"

    fun followupDetail(followupId: String) = "followup/$followupId"
}

/**
 * Grafo de navegación. La pantalla HOME decide internamente si el usuario no
 * está emparejado, mostrando el CTA de emparejamiento, y ofrece la búsqueda
 * y el listado de seguimientos. Tras emparejar con éxito, se navega a HOME
 * limpiando la pila.
 *
 * [pendingFollowupId] permite abrir directamente el detalle de un seguimiento
 * al pulsar la notificación ("abrir"), una vez por evento emitido.
 */
@Composable
fun AppNavHost(
    pendingFollowupId: StateFlow<String?>? = null,
    themeViewModel: ThemeViewModel,
) {
    val navController = rememberNavController()

    if (pendingFollowupId != null) {
        LaunchedEffect(pendingFollowupId) {
            pendingFollowupId.collect { followupId ->
                if (!followupId.isNullOrEmpty()) {
                    navController.navigate(Destinations.followupDetail(followupId)) {
                        popUpTo(Destinations.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Destinations.HOME,
        enterTransition = {
            fadeIn(animationSpec = tween(RenfeMotion.Short)) +
                slideInHorizontally(animationSpec = tween(RenfeMotion.Short)) { it / 16 }
        },
        exitTransition = {
            fadeOut(animationSpec = tween(RenfeMotion.Short))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(RenfeMotion.Short))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(RenfeMotion.Short)) +
                slideOutHorizontally(animationSpec = tween(RenfeMotion.Short)) { it / 16 }
        },
    ) {
        composable(Destinations.HOME) {
            HomeScreen(
                onNavigateToPairing = {
                    navController.navigate(Destinations.PAIRING) {
                        launchSingleTop = true
                    }
                },
                onNavigateToSearch = {
                    navController.navigate(Destinations.SEARCH) {
                        launchSingleTop = true
                    }
                },
                onNavigateToFollowUps = {
                    navController.navigate(Destinations.FOLLOWUPS) {
                        launchSingleTop = true
                    }
                },
                onNavigateToDiagnostics = {
                    navController.navigate(Destinations.DIAGNOSTICS) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Destinations.PAIRING) {
            PairingScreen(
                onPaired = {
                    navController.navigate(Destinations.HOME) {
                        popUpTo(Destinations.HOME) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Destinations.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable(Destinations.FOLLOWUPS) {
            FollowUpsScreen(
                onBack = { navController.popBackStack() },
                onOpenDetail = { followupId ->
                    navController.navigate(Destinations.followupDetail(followupId)) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Destinations.DIAGNOSTICS) {
            DiagnosticsScreen(
                onBack = { navController.popBackStack() },
                themeViewModel = themeViewModel,
            )
        }
        composable(
            route = Destinations.FOLLOWUP_DETAIL,
            arguments = listOf(
                navArgument(Destinations.FOLLOWUP_DETAIL_ARG) {
                    type = NavType.StringType
                },
            ),
        ) { entry ->
            val followupId = entry.arguments?.getString(Destinations.FOLLOWUP_DETAIL_ARG).orEmpty()
            FollowUpDetailScreen(
                followupId = followupId,
                onBack = { navController.popBackStack() },
                onDeleted = { navController.popBackStack() },
            )
        }
    }
}