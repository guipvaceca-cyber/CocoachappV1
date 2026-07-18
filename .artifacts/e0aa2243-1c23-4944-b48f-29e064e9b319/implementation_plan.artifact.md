# Fix Team and Player Loading in Cockpit and Calendar

The user reports that only "M15F" and "Adversaire" are visible in the terrain cockpit (MatchDashboard), while their "real teams" (club teams from Supabase) are missing.

## Analysis
- `MainActivity.kt` performs a merge of local teams and club teams (`allTeams`), but it only passes this merged list to `DashboardScreen`.
- `MatchDashboardScreen` (used for "Cockpit Terrain") only receives `viewModel.seasonConfig`, which contains only local teams.
- `SeasonCalendarScreen` performs its own partial merge, often ignoring local data or re-fetching club data inefficiently.
- Players are also not consistently merged across screens.

## User Review Required
> [!IMPORTANT]
> All screens will now show both your locally created teams/players and the official ones from your club. If there are duplicates (same ID), the club version will take priority.

## Proposed Changes

### [MainActivity](file:///C:/Users/guip3/AndroidStudioProjects/CoachApp/app/src/main/java/com/example/coachapp/MainActivity.kt)
- [MODIFY] Centralize the merge logic for both `allTeams` and `allPlayers`.
- [MODIFY] Pass the merged data to all screens via `seasonConfig.copy(teams = allTeams, players = allPlayers)`.

### [MatchDashboardScreen](file:///C:/Users/guip3/AndroidStudioProjects/CoachApp/app/src/main/java/com/example/coachapp/ui/screens/MatchDashboardScreen.kt)
- [MODIFY] Ensure it uses the `teams` and `players` from the provided `seasonConfig`. (Actually it already does, it just needs the correct `seasonConfig`).

### [SeasonCalendarScreen](file:///C:/Users/guip3/AndroidStudioProjects/CoachApp/app/src/main/java/com/example/coachapp/ui/screens/SeasonCalendarScreen.kt)
- [MODIFY] Remove redundant local merge logic.
- [MODIFY] Use `seasonConfig.teams` and `seasonConfig.players` directly as they will be pre-merged by `MainActivity`.

### [Other Screens]
- [MODIFY] Apply the same logic to `TeamHubScreen`, `InsightsScreen`, etc.

## Verification Plan

### Manual Verification
1. Login with a coach account that has official club teams.
2. Go to **Dashboard** and verify official teams are visible in "Prêt pour le terrain".
3. Navigate to **Terrain** (Cockpit) and verify the dropdown contains both official teams and local teams.
4. Go to **Saison** (Calendar) and verify the filters include all teams.
