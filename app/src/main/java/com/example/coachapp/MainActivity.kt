package com.example.coachapp

import androidx.activity.SystemBarStyle
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
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coachapp.data.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.coachapp.ui.CoachViewModel
import com.example.coachapp.ui.screens.*
import com.example.coachapp.ui.theme.CoachAppTheme
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
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
    val presidentViewModel: com.example.coachapp.ui.president.PresidentViewModel = viewModel(
        factory = com.example.coachapp.ui.president.PresidentViewModelFactory(
            com.example.coachapp.data.repository.PresidentRepository(com.example.coachapp.data.SupabaseManager.client)
        )
    )
    val playerViewModel: com.example.coachapp.ui.player.PlayerViewModel = viewModel(
        factory = com.example.coachapp.ui.player.PlayerViewModelFactory(
            com.example.coachapp.data.repository.PresidentRepository(com.example.coachapp.data.SupabaseManager.client)
        )
    )
    val trainingViewModel: com.example.coachapp.ui.training.TrainingViewModel = viewModel(
        factory = com.example.coachapp.ui.training.TrainingViewModelFactory(
            com.example.coachapp.data.repository.TrainingRepository(com.example.coachapp.data.SupabaseManager.client)
        )
    )

    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.DASHBOARD) }

    if (!viewModel.isLoggedIn) {
        LoginScreen(
            isLoggingIn = viewModel.isAuthLoading,
            errorMessage = viewModel.authError,
            onLogin = { email, pass -> viewModel.login(email, pass) },
            onSignUp = { email, pass -> viewModel.signUp(email, pass) },
            onDismissError = { viewModel.clearAuthError() }
        )
    } else if (viewModel.isFetchingProfile) {
        // Écran d'attente pendant que le profil Patrick Robin descend de Supabase
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("Récupération de votre profil...", style = MaterialTheme.typography.bodyMedium)
            }
        }
    } else if (!viewModel.seasonConfig.isOnboardingCompleted) {
        OnboardingScreen(
            initialConfig = viewModel.seasonConfig,
            pendingInvitations = viewModel.pendingInvitations,
            onCompleted = { config, intentions ->
                intentions.forEach { viewModel.preRegisterFormation(it) }
                viewModel.completeOnboarding(config)
            }
        )
    } else {
        val navSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = Color(0xFF001529),
            navigationBarContentColor = Color.White,
            navigationRailContainerColor = Color(0xFF001529),
            navigationRailContentColor = Color.White
        )

        val itemColors = NavigationSuiteDefaults.itemColors(
            navigationBarItemColors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF00B4D8),
                selectedTextColor = Color(0xFF00B4D8),
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                indicatorColor = Color.Transparent
            ),
            navigationRailItemColors = NavigationRailItemDefaults.colors(
                selectedIconColor = Color(0xFF00B4D8),
                selectedTextColor = Color(0xFF00B4D8),
                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                unselectedTextColor = Color.White.copy(alpha = 0.6f),
                indicatorColor = Color.Transparent
            )
        )

        // Merge local teams and official club teams for consistent UI (colors, names)
        val presidentState by presidentViewModel.uiState.collectAsState()
        
        val allTeams = remember(viewModel.seasonConfig.teams, presidentState) {
            val localTeams = viewModel.seasonConfig.teams
            val customColors = persistenceManager.loadTeamColors() // Refresh on every state change
            val clubTeams = if (presidentState is com.example.coachapp.ui.president.PresidentUiState.Success) {
                val userId = com.example.coachapp.data.SupabaseManager.auth.currentUserOrNull()?.id
                (presidentState as com.example.coachapp.ui.president.PresidentUiState.Success).collectifs
                    .filter { detail -> detail.rattachements.any { it.coachId == userId } }
                    .map { detail ->
                        val teamId = detail.collectif.id
                        val colorInt = customColors[teamId]
                        com.example.coachapp.data.Team(
                            id = teamId,
                            name = detail.collectif.nom,
                            color = if (colorInt != null) Color(colorInt) else Color(0xFF2196F3),
                            format = when(detail.collectif.format) {
                                "2x2" -> com.example.coachapp.data.TeamFormat.TWO_TWO
                                "3x3" -> com.example.coachapp.data.TeamFormat.THREE_THREE
                                "4x4" -> com.example.coachapp.data.TeamFormat.FOUR_FOUR
                                else -> com.example.coachapp.data.TeamFormat.SIX_SIX
                            }
                        )
                    }
            } else emptyList()
            
            // Merge: prioritizes club teams if IDs overlap, then adds local ones
            val merged = (clubTeams + localTeams).distinctBy { it.id }
            merged
        }

        val allPlayers = remember(viewModel.seasonConfig.players, presidentState) {
            val localPlayers = viewModel.seasonConfig.players
            val clubPlayers = if (presidentState is com.example.coachapp.ui.president.PresidentUiState.Success) {
                (presidentState as com.example.coachapp.ui.president.PresidentUiState.Success).collectifs.flatMap { detail ->
                    detail.joueurs.map { j ->
                        com.example.coachapp.data.Player(
                            id = j.id,
                            teamId = detail.collectif.id,
                            firstName = j.prenom,
                            lastName = j.nom,
                            number = 0,
                            position = j.poste ?: "",
                            vivierId = j.vivierJoueurId
                        )
                    }
                }
            } else emptyList()
            
            (clubPlayers + localPlayers).distinctBy { it.id }
        }

        val clubEventsState by presidentViewModel.clubEvents.collectAsState()

        val mergedSeasonConfig = remember(viewModel.seasonConfig, allTeams, allPlayers, clubEventsState) {
            viewModel.seasonConfig.copy(
                teams = allTeams, 
                players = allPlayers,
                clubEvents = clubEventsState
            )
        }

        NavigationSuiteScaffold(
            layoutType = NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfo()),
            containerColor = Color(0xFF001529), // Match background
            navigationSuiteColors = navSuiteColors,
            navigationSuiteItems = {
                // Main Home Item
                item(
                    icon = { Icon(painterResource(R.drawable.ic_home), contentDescription = "Accueil") },
                    label = { Text("Accueil") },
                    selected = currentDestination == AppDestinations.DASHBOARD && viewModel.selectedResource == null,
                    colors = itemColors,
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
                        colors = itemColors,
                        onClick = { currentDestination = AppDestinations.COACH_SPACE }
                    )
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color(0xFF001529)
            ) { innerPadding ->
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
                            AppDestinations.DASHBOARD -> {
                                DashboardScreen(
                                    modifier = modifier,
                                    flashResults = viewModel.flashResults,
                                    globalResults = viewModel.globalResults,
                                    seasonConfig = mergedSeasonConfig,
                                    onUpdateAttendance = { id, status -> viewModel.updateClubEventAttendance(id, status) },
                                    onNavigate = { dest ->
                                        when(dest) {
                                            "CALENDAR" -> currentDestination = AppDestinations.SEASON_CALENDAR
                                            "TEAM_HUB" -> currentDestination = AppDestinations.TEAM_HUB
                                            "COACH_SPACE" -> currentDestination = AppDestinations.COACH_SPACE
                                            "COMPANION" -> {
                                                currentDestination = AppDestinations.SESSION_COMPANION
                                            }
                                            "MATCH" -> {
                                                currentDestination = AppDestinations.MATCH_BOARD
                                            }
                                            "INSIGHTS" -> currentDestination = AppDestinations.INSIGHTS
                                            "PROFILE" -> currentDestination = AppDestinations.PROFILE
                                            "COPLAYER_PLANNING" -> currentDestination = AppDestinations.COPLAYER_PLANNING
                                            "DIAGNOSTIC_FLASH" -> {
                                                viewModel.currentAssessmentType = com.example.coachapp.data.AssessmentType.FLASH
                                                currentDestination = AppDestinations.DIAGNOSTIC
                                            }
                                            "PREPARER" -> {
                                                viewModel.selectedSessionIdForBuilder = null
                                                currentDestination = AppDestinations.SESSION_BUILDER
                                            }
                                            else -> if (dest.startsWith("COMPANION_")) {
                                                viewModel.selectedSessionIdForBuilder = dest.removePrefix("COMPANION_")
                                                currentDestination = AppDestinations.SESSION_COMPANION
                                            }
                                        }
                                    }
                                )
                            }
                            AppDestinations.SEASON_CALENDAR -> SeasonCalendarScreen(
                            modifier = modifier,
                            persistenceManager = persistenceManager,
                            seasonConfig = mergedSeasonConfig,
                            viewModel = viewModel,
                            trainingViewModel = trainingViewModel,
                            onUseHelp = { viewModel.useHelp() },
                            helpUsageCount = viewModel.getHelpUsageCountThisMonth(),
                            onUpdateAttendance = { id, status -> viewModel.updateClubEventAttendance(id, status) },
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
                        AppDestinations.TEAM_HUB -> {
                            TeamHubScreen(
                                modifier = modifier,
                                viewModel = presidentViewModel,
                                trainingViewModel = trainingViewModel,
                                seasonConfig = mergedSeasonConfig,
                                onUpdateConfig = { viewModel.updateSeasonConfig(it) }
                            )
                        }
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
                                    val team = mergedSeasonConfig.teams.find { it.id == trainingSession.teamId }
                                    SessionCompanionScreen(
                                        modifier = modifier,
                                        session = trainingSession,
                                        teamName = team?.name ?: "Inconnu",
                                        persistenceManager = persistenceManager,
                                        onUpdateSession = { viewModel.updateSeasonConfig(mergedSeasonConfig.copy(plannedTrainings = mergedSeasonConfig.plannedTrainings.map { s -> if (s.id == it.id) it else s })) },
                                        onFinish = { 
                                            viewModel.currentAssessmentType = com.example.coachapp.data.AssessmentType.FLASH
                                            currentDestination = AppDestinations.DIAGNOSTIC 
                                        },
                                        onBack = { currentDestination = AppDestinations.DASHBOARD },
                                        onPushSession = { 
                                            trainingViewModel.pushSession(it, onSuccess = {}, onError = {})
                                        }
                                    )
                                }
                                else -> { 
                                    MatchDashboardScreen(
                                        modifier = modifier,
                                        seasonConfig = mergedSeasonConfig,
                                        persistenceManager = persistenceManager,
                                        onUpdatePlayer = { viewModel.updatePlayer(it) }
                                    )
                                }
                            }
                        }
                        AppDestinations.MATCH_BOARD -> MatchDashboardScreen(
                            modifier = modifier,
                            seasonConfig = mergedSeasonConfig,
                            persistenceManager = persistenceManager,
                            onUpdatePlayer = { viewModel.updatePlayer(it) }
                        )
                        AppDestinations.SESSION_BUILDER -> SessionBuilderScreen(
                            modifier = modifier,
                            seasonConfig = mergedSeasonConfig,
                            initialSessionId = viewModel.selectedSessionIdForBuilder,
                            onUpdateSession = { viewModel.updateSeasonConfig(mergedSeasonConfig.copy(plannedTrainings = mergedSeasonConfig.plannedTrainings.map { s -> if (s.id == it.id) it else s })) },
                            onPushSession = {
                                trainingViewModel.pushSession(it, onSuccess = {}, onError = {})
                            },
                            onBack = { currentDestination = AppDestinations.SEASON_CALENDAR }
                        )
                        AppDestinations.INSIGHTS -> InsightsScreen(
                            modifier = modifier,
                            seasonConfig = mergedSeasonConfig,
                            onNavigate = { dest ->
                                when (dest) {
                                    "COACH_SPACE_VESTIAIRE" -> {
                                        viewModel.coachSpaceTab = 0
                                        currentDestination = AppDestinations.COACH_SPACE
                                    }
                                }
                            }
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
                            seasonConfig = mergedSeasonConfig,
                            userRole = viewModel.userRole,
                            isCoachCde = viewModel.isCoachCde,
                            isStageOpen = viewModel.isStageOpen,                 // ← nouveau
                            cdeAssignments = viewModel.cdeAssignments,
                            vivierPrincipal = viewModel.vivierPrincipal,    // ← ajoute
                            vivierInferieur = viewModel.vivierInferieur,
                            slotsPersistes = viewModel.slotsPersistes,          // ← nouveau
                            bancPersiste = viewModel.bancPersiste,               // ← nouveau
                            selectionAlerteMessage = viewModel.selectionAlerteMessage, // ← nouveau
                            onOuverture = { categorie ->                         // ← nouveau
                                val quota = com.example.coachapp.ui.screens.quotaParCategorie(categorie)
                                viewModel.creerOuChargerConvocation(categorie, quota)
                            },
                            onSlotChange = { index, type, joueur ->
                                viewModel.sauvegarderSlot(index, type, joueur)
                            },
                            onSauvegarder = { principal, banc ->
                                viewModel.sauvegarderSelection(principal, banc)
                                viewModel.finaliserConvocation()
                            },
                            onEnvoyerSelection = { categorie ->                  // ← nouveau
                                viewModel.envoyerSelection(categorie)
                            },
                            onUpdateConfig = { viewModel.updateSeasonConfig(it) },
                            onLogout = { viewModel.logout() },
                            onNavigateToPresident = { currentDestination = AppDestinations.PRESIDENT_DASHBOARD },
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
                                    onUpdateSession = { viewModel.updateSeasonConfig(mergedSeasonConfig.copy(plannedTrainings = mergedSeasonConfig.plannedTrainings.map { s -> if (s.id == it.id) it else s })) }
                                )
                            } else {
                                currentDestination = AppDestinations.SEASON_CALENDAR
                            }
                        }
                        AppDestinations.PRESIDENT_DASHBOARD -> {
                            PresidentDashboardScreen(
                                viewModel = presidentViewModel,
                                onBack = { currentDestination = AppDestinations.PROFILE }
                            )
                        }
                        AppDestinations.COPLAYER_PLANNING -> {
                            CoPlayerPlanningScreen(
                                viewModel = playerViewModel,
                                modifier = modifier
                            )
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
    PRESIDENT_DASHBOARD("Gestion Club", R.drawable.ic_home),
    COPLAYER_PLANNING("Mon Planning", R.drawable.ic_home),
}
