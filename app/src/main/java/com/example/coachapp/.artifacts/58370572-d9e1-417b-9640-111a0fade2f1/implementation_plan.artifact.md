# Implementation Plan - Team Color for Dashboard Cards

The user wants to replace the black background of the first card in the "Prêt pour le terrain" section with the background color of the associated team (defined in "Ma Saison").

## Proposed Changes

### [Dashboard UI]

#### [MODIFY] [DashboardScreen.kt](file:///C:/Users/guip3/AndroidStudioProjects/CoachApp/app/src/main/java/com/example/coachapp/ui/screens/DashboardScreen.kt)

- Update `ReadySessionCard` to use `team?.color` as the `containerColor` when `isFront` is true.
- If `team?.color` is not available, fallback to `Color.Black` (current behavior).
- Ensure text and icons remain readable on the team color background (all team colors are currently saturated, so white text should be fine).
- Hide the small team color indicator circle when the whole card background already uses that color to avoid redundancy.

## Verification Plan

### Automated Tests
- No automated tests planned for this UI tweak.

### Manual Verification
- Deploy the app to a device/emulator.
- Ensure at least one team has a color assigned.
- Go to the Dashboard and verify that the "PRÊT POUR LE TERRAIN" card for that team uses the team's color instead of black.
- Verify that text and icons are still legible.
