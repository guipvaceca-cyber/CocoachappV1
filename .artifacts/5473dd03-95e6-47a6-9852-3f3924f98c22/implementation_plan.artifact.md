# Refonte de l'App Terrain (SessionCompanion)

L'objectif est de supprimer les lenteurs au lancement et d'uniformiser l'écran de terrain avec le nouveau design sombre et glassmorphism de l'application.

## User Review Required

> [!IMPORTANT]
> **Suppression du Launch Screen** : Je vais retirer totalement l'étape `TerrainLaunchScreen` pour un accès instantané aux outils de terrain.
>
> **Refonte Visuelle** : L'écran de terrain va passer sur le fond **Bleu Sombre profond** (`0xFF001529`) avec des cartes en verre. Le timer utilisera l'accent **Cyan** pour une meilleure visibilité.

## Proposed Changes

### [Component Name] Navigation (MainActivity)

#### [MODIFY] [MainActivity.kt](file:///C:/Users/guip3/AndroidStudioProjects/CoachApp/app/src/main/java/com/example/coachapp/MainActivity.kt)
1.  Supprimer la variable `isLaunchingTerrain`.
2.  Retirer le bloc `else if (isLaunchingTerrain)` qui affichait le `TerrainLaunchScreen`.
3.  Mettre à jour les clics de navigation pour qu'ils changent directement de destination sans passer par l'état "launching".

### [Component Name] Terrain UI (SessionCompanionScreen)

#### [MODIFY] [SessionCompanionScreen.kt](file:///C:/Users/guip3/AndroidStudioProjects/CoachApp/app/src/main/java/com/example/coachapp/ui/screens/SessionCompanionScreen.kt)
1.  **Uniformisation du Design** :
    - Fond : `Color(0xFF001529)`.
    - Cartes : `Color.White.copy(alpha = 0.12f)` avec bordures subtiles (Glassmorphism).
2.  **Header Premium** :
    - Alignement sur le style du Dashboard (Surface translucide, titre en blanc pur).
3.  **Timeline Dynamique** :
    - Amélioration visuelle de la barre latérale des étapes (points plus lumineux, connecteurs plus fins).
4.  **Timer High-Tech** :
    - Utilisation du **Bleu Cyan** (`0xFF00B4D8`) pour le cercle de progression.
    - Typographie plus large et plus lisible pour le décompte.
5.  **Nettoyage** :
    - Suppression de l'overlay "Scanner" qui ralentit le chargement visuel à l'entrée.

## Verification Plan

### Automated Tests
- Vérification de la compilation via `./gradlew :app:compileDebugKotlin`.

### Manual Verification
- Cliquer sur "Lancer l'entraînement" depuis le Dashboard : l'écran de terrain doit s'ouvrir instantanément.
- Vérifier la cohérence visuelle des couleurs (Bleu profond, Cyan, Glassmorphism).
