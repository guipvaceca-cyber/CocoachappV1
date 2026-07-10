package com.example.coachapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.PersistenceManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coachapp.ui.CoachViewModel
import com.example.coachapp.ui.screens.*
import com.example.coachapp.ui.theme.CoachAppTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CoachAppTheme {
                CoachAppApp()
            }
        }
    }
}

@Composable
fun CoachAppApp(viewModel: CoachViewModel = viewModel()) {
    val persistenceManager = remember { PersistenceManager(viewModel.getApplication()) }
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.DASHBOARD) }
    var isLaunchingTerrain by remember { mutableStateOf(false) }

    if (!viewModel.isLoggedIn) {
        LoginScreen(
            isLoggingIn = viewModel.isAuthLoading,
            errorMessage = viewModel.authError,
            onLogin = { email, pass -> viewModel.login(email, pass) },
            onSignUp = { email, pass -> viewModel.signUp(email, pass) },
            onDismissError = { viewModel.clearAuthError() }
        )
    } else if (!viewModel.seasonConfig.isOnboardingCompleted) {
        OnboardingScreen(onCompleted = { config ->
            viewModel.completeOnboarding(config)
        })
    } else if (isLaunchingTerrain) {
        TerrainLaunchScreen(onAnimationFinish = { isLaunchingTerrain = false })
    } else {
        NavigationSuiteScaffold(
            navigationSuiteItems = {
                // Main Home Item
                item(
                    icon = { Icon(painterResource(R.drawable.ic_home), contentDescription = "Accueil") },
                    label = { Text("Accueil") },
                    selected = currentDestination == AppDestinations.DASHBOARD && viewModel.selectedResource == null,
                    onClick = {
                        currentDestination = AppDestinations.DASHBOARD
                        viewModel.clearSelectedResource()
                        viewModel.selectedTool = null
                        viewModel.selectedSessionIdForBuilder = null
                    }
                )

                // SOS LABO Button - Only visible on specific screens
                val showSos = currentDestination == AppDestinations.SESSION_COMPANION || currentDestination == AppDestinations.MATCH_BOARD
                if (showSos) {
                    item(
                        icon = { 
                            Badge(containerColor = MaterialTheme.colorScheme.error) {
                                Icon(Icons.Default.PriorityHigh, contentDescription = "SOS", tint = Color.White)
                            }
                        },
                        label = { Text("SOS LABO", color = MaterialTheme.colorScheme.error) },
                        selected = false,
                        onClick = { currentDestination = AppDestinations.COACH_SPACE }
                    )
                }
            }
        ) {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                val modifier = Modifier.padding(innerPadding)
                
                if (viewModel.selectedResource != null) {
                    BackHandler { viewModel.clearSelectedResource() }
                    LaboDetailScreen(
                        resource = viewModel.selectedResource!!,
                        onBack = { viewModel.clearSelectedResource() },
                        onTestNow = {
                            val nextSession = viewModel.seasonConfig.plannedTrainings.firstOrNull { !it.isValidated }
                            if (nextSession != null) {
                                viewModel.selectedSessionIdForBuilder = nextSession.id
                                viewModel.updateSeasonConfig(viewModel.seasonConfig.copy(
                                    plannedTrainings = viewModel.seasonConfig.plannedTrainings.map { 
                                        if (it.id == nextSession.id) it.copy(drills = viewModel.selectedResource!!.title) else it 
                                    }
                                ))
                                currentDestination = AppDestinations.SESSION_BUILDER
                            }
                            viewModel.clearSelectedResource()
                        },
                        modifier = modifier
                    )
                } else {
                    AnimatedContent<AppDestinations>(
                        targetState = currentDestination,
                        transitionSpec = {
                            val isTerrain = targetState == AppDestinations.SESSION_COMPANION || targetState == AppDestinations.MATCH_BOARD
                            
                            if (isTerrain) {
                                (fadeIn(animationSpec = tween(600)) + scaleIn(initialScale = 0.5f, animationSpec = tween(600)))
                                    .togetherWith(fadeOut(animationSpec = tween(300)))
                            } else {
                                (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.9f, animationSpec = tween(400)))
                                    .togetherWith(fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 1.1f, animationSpec = tween(200)))
                            }
                        },
                        label = "ScreenTransition"
                    ) { targetDestination ->
                        when (targetDestination) {
                            AppDestinations.DASHBOARD -> DashboardScreen(
                            modifier = modifier,
                            flashResults = viewModel.flashResults,
                            globalResults = viewModel.globalResults,
                            seasonConfig = viewModel.seasonConfig,
                            onNavigate = { dest ->
                                when(dest) {
                                    "CALENDAR" -> currentDestination = AppDestinations.SEASON_CALENDAR
                                    "TEAM_HUB" -> currentDestination = AppDestinations.TEAM_HUB
                                    "COACH_SPACE" -> currentDestination = AppDestinations.COACH_SPACE
                                    "COMPANION" -> {
                                        isLaunchingTerrain = true
                                        currentDestination = AppDestinations.SESSION_COMPANION
                                    }
                                    "MATCH" -> {
                                        isLaunchingTerrain = true
                                        currentDestination = AppDestinations.MATCH_BOARD
                                    }
                                    "INSIGHTS" -> currentDestination = AppDestinations.INSIGHTS
                                    "PROFILE" -> currentDestination = AppDestinations.PROFILE
                                    "DIAGNOSTIC_FLASH" -> {
                                        viewModel.currentAssessmentType = com.example.coachapp.data.AssessmentType.FLASH
                                        currentDestination = AppDestinations.DIAGNOSTIC
                                    }
                                    "PREPARER" -> {
                                        viewModel.selectedSessionIdForBuilder = null
                                        currentDestination = AppDestinations.SESSION_BUILDER
                                    }
                                    else -> if (dest.startsWith("COMPANION_")) {
                                        isLaunchingTerrain = true
                                        viewModel.selectedSessionIdForBuilder = dest.removePrefix("COMPANION_")
                                        currentDestination = AppDestinations.SESSION_COMPANION
                                    }
                                }
                            }
                        )
                        AppDestinations.SEASON_CALENDAR -> SeasonCalendarScreen(
                            modifier = modifier,
                            persistenceManager = persistenceManager,
                            seasonConfig = viewModel.seasonConfig,
                            viewModel =viewModel,
                            onUseHelp = { viewModel.useHelp() },
                            helpUsageCount = viewModel.getHelpUsageCountThisMonth(),
                            onNavigateToPreparer = { session ->
                                if (!viewModel.seasonConfig.plannedTrainings.any { it.id == session.id }) {
                                    viewModel.updateSeasonConfig(viewModel.seasonConfig.copy(
                                        plannedTrainings = viewModel.seasonConfig.plannedTrainings + session
                                    ))
                                }
                                viewModel.selectedSessionIdForBuilder = session.id
                                currentDestination = AppDestinations.SESSION_BUILDER 
                            },
                            onViewRecap = { 
                                viewModel.selectedSessionForRecap = it
                                currentDestination = AppDestinations.SESSION_RECAP
                            }
                        )
                        AppDestinations.TEAM_HUB -> TeamHubScreen(
                            modifier = modifier,
                            seasonConfig = viewModel.seasonConfig,
                            onUpdateConfig = { viewModel.updateSeasonConfig(it) },
                            onUpdatePlayer = { viewModel.updatePlayer(it) },
                            onDeletePlayer = { viewModel.deletePlayer(it) },
                            onAddAssessment = { id, assessment -> viewModel.addPlayerAssessment(id, assessment) }
                        )
                        AppDestinations.COACH_SPACE -> CoachSpaceScreen(
                            modifier = modifier,
                            viewModel = viewModel,
                            onResourceClick = { viewModel.selectedResource = it }
                        )
                        AppDestinations.SESSION_COMPANION -> {
                            val today = LocalDate.now()
                            val trainingSession = if (viewModel.selectedSessionIdForBuilder != null) {
                                viewModel.seasonConfig.plannedTrainings.find { it.id == viewModel.selectedSessionIdForBuilder }
                            } else {
                                viewModel.seasonConfig.plannedTrainings.find { it.date == today }
                            }

                            when {
                                trainingSession != null -> {
                                    val team = viewModel.seasonConfig.teams.find { it.id == trainingSession.teamId }
                                    SessionCompanionScreen(
                                        modifier = modifier,
                                        session = trainingSession,
                                        teamName = team?.name ?: "Inconnu",
                                        persistenceManager = persistenceManager,
                                        onUpdateSession = { viewModel.updateSeasonConfig(viewModel.seasonConfig.copy(plannedTrainings = viewModel.seasonConfig.plannedTrainings.map { s -> if (s.id == it.id) it else s })) },
                                        onFinish = { 
                                            viewModel.currentAssessmentType = com.example.coachapp.data.AssessmentType.FLASH
                                            currentDestination = AppDestinations.DIAGNOSTIC 
                                        },
                                        onBack = { currentDestination = AppDestinations.DASHBOARD }
                                    )
                                }
                                else -> { 
                                    MatchDashboardScreen(
                                        modifier = modifier,
                                        seasonConfig = viewModel.seasonConfig,
                                        persistenceManager = persistenceManager,
                                        onUpdatePlayer = { viewModel.updatePlayer(it) }
                                    )
                                }
                            }
                        }
                        AppDestinations.MATCH_BOARD -> MatchDashboardScreen(
                            modifier = modifier,
                            seasonConfig = viewModel.seasonConfig,
                            persistenceManager = persistenceManager,
                            onUpdatePlayer = { viewModel.updatePlayer(it) }
                        )
                        AppDestinations.SESSION_BUILDER -> SessionBuilderScreen(
                            modifier = modifier,
                            seasonConfig = viewModel.seasonConfig,
                            initialSessionId = viewModel.selectedSessionIdForBuilder,
                            onUpdateSession = { viewModel.updateSeasonConfig(viewModel.seasonConfig.copy(plannedTrainings = viewModel.seasonConfig.plannedTrainings.map { s -> if (s.id == it.id) it else s })) }
                        )
                        AppDestinations.INSIGHTS -> InsightsScreen(
                            modifier = modifier,
                            seasonConfig = viewModel.seasonConfig
                        )
                        AppDestinations.DIAGNOSTIC -> AssessmentScreen(
                            modifier = modifier,
                            type = viewModel.currentAssessmentType,
                            onResultCalculated = { type, results, note ->
                                viewModel.updateResults(type, results, note)
                                currentDestination = AppDestinations.DASHBOARD
                            }
                        )
                        AppDestinations.PROFILE -> ProfileScreen(
                            modifier = modifier,
                            history = viewModel.history,
                            seasonConfig = viewModel.seasonConfig,
                            userRole = viewModel.userRole,
                            onUpdateConfig = { viewModel.updateSeasonConfig(it) },
                            onLogout = { viewModel.logout() },
                            onNavigateToGlobalAssessment = {
                                viewModel.currentAssessmentType = com.example.coachapp.data.AssessmentType.GLOBAL
                                currentDestination = AppDestinations.DIAGNOSTIC
                            }
                        )
                        AppDestinations.SESSION_RECAP -> {
                            if (viewModel.selectedSessionForRecap != null) {
                                SessionRecapScreen(
                                    session = viewModel.selectedSessionForRecap!!,
                                    persistenceManager = persistenceManager,
                                    onBack = { currentDestination = AppDestinations.SEASON_CALENDAR },
                                    onRepeat = { 
                                        viewModel.selectedSessionIdForBuilder = viewModel.selectedSessionForRecap!!.id
                                        currentDestination = AppDestinations.SESSION_BUILDER 
                                    },
                                    onExportToLabo = { currentDestination = AppDestinations.COACH_SPACE },
                                    onUpdateSession = { viewModel.updateSeasonConfig(viewModel.seasonConfig.copy(plannedTrainings = viewModel.seasonConfig.plannedTrainings.map { s -> if (s.id == it.id) it else s })) }
                                )
                            } else {
                                currentDestination = AppDestinations.SEASON_CALENDAR
                            }
                        }
                    }
                }
            }
        }
    }
}
}

enum class AppDestinations(
    val label: String,
    val icon: Int,
) {
    DASHBOARD("Tableau", R.drawable.ic_home),
    SEASON_CALENDAR("Saison", R.drawable.ic_home),
    TEAM_HUB("Collectifs", R.drawable.ic_home),
    COACH_SPACE("Espace Coachs", R.drawable.ic_home),
    SESSION_COMPANION("Terrain", R.drawable.ic_home),
    MATCH_BOARD("Match", R.drawable.ic_home),
    SESSION_BUILDER("Préparateur", R.drawable.ic_home),
    INSIGHTS("Analyses", R.drawable.ic_home),
    DIAGNOSTIC("Diagnostic", R.drawable.ic_favorite),
    PROFILE("Profil", R.drawable.ic_home),
    SESSION_RECAP("Bilan", R.drawable.ic_home),
}
