# Walkthrough - Supabase Upsert API Fix

I have fixed the build error `No parameter with name 'onConflict' found` by migrating the `upsert` calls to the Supabase Postgrest-kt 3.0+ DSL syntax.

## Changes Made

### Supabase API Migration
The newer version of the Supabase Kotlin SDK moved optional parameters like `onConflict` and `ignoreDuplicates` into a trailing configuration lambda.

#### [CdeVivierParser.kt](file:///C:/Users/guip3/AndroidStudioProjects/CoachApp/app/src/main/java/com/example/coachapp/data/CdeVivierParser.kt)
Updated the `syncVersSupabase` function to use the new DSL:
```diff
- .upsert(rows, onConflict = "num_licence,saison")
+ .upsert(rows) {
+     onConflict = "num_licence,saison"
+ }
```

#### [CoachViewModel.kt](file:///C:/Users/guip3/AndroidStudioProjects/CoachApp/app/src/main/java/com/example/coachapp/ui/CoachViewModel.kt)
Updated multiple `upsert` calls. For single `Map` objects, I wrapped them in `listOf()` to ensure correct type inference with the new API signature:
```diff
- .upsert(row, onConflict = "convocation_id,type_tableau,ordre")
+ .upsert(listOf(row)) {
+     onConflict = "convocation_id,type_tableau,ordre"
+ }
```

#### [TrainingRepository.kt](file:///C:/Users/guip3/AndroidStudioProjects/CoachApp/app/src/main/java/com/example/coachapp/data/repository/TrainingRepository.kt)
Updated the `ignoreDuplicates` parameter:
```diff
- .upsert(presences, ignoreDuplicates = true)
+ .upsert(presences) {
+     ignoreDuplicates = true
+ }
```

## Verification Results

### Automated Tests
- Ran `./gradlew :app:compileBetaKotlin`.
- **Result**: The errors in `CdeVivierParser.kt`, `CoachViewModel.kt`, and `TrainingRepository.kt` related to `upsert` are resolved.
- Note: Other compilation errors in `AnalyseSeanceDrawer.kt` and `SessionRecapScreen.kt` were detected but are unrelated to the Supabase API changes and appear to be pre-existing or caused by unresolved references in other parts of the project.

### Manual Verification
- Verified with `analyze_file` that the modified files no longer report "No parameter with name" errors.
