# Implementation Plan - Ajout d'un Sifflet au Scoreur

L'objectif est d'ajouter une icône de sifflet dans le module "Scoreur" du cockpit terrain. Cette icône permettra de déclencher un son de sifflet avec un volume réglable via un curseur dédié.

## User Review Required

> [!IMPORTANT]
> Cette modification nécessite l'ajout d'un fichier audio nommé `whistle.mp3` dans le dossier `app/src/main/res/raw/`.
> Si ce dossier n'existe pas, il devra être créé. Sans ce fichier, l'application pourrait planter ou ne produire aucun son.

> [!NOTE]
> Le volume sera contrôlé par un curseur (`Slider`) placé juste à côté de l'icône du sifflet pour un accès rapide pendant le match.

## Proposed Changes

### UI Components & Screens

#### [MODIFY] [MatchDashboardScreen.kt](file:///C:/Users/guip3/AndroidStudioProjects/CoachApp/app/src/main/java/com/example/coachapp/ui/screens/MatchDashboardScreen.kt)

- **Gestion du son** :
    - Initialisation d'un `MediaPlayer` avec le fichier `R.raw.whistle`.
    - Gestion du cycle de vie avec `DisposableEffect` pour libérer les ressources du `MediaPlayer`.
- **Intégration UI** :
    - Dans la fonction `VolleyScorer`, ajouter un `Box` englobant pour positionner les contrôles du sifflet en haut à droite.
    - Ajouter un `Row` contenant :
        - Un `Slider` compact pour régler le volume (0.0 à 1.0).
        - Un `IconButton` avec l'icône `Icons.Default.Campaign` (ou un sifflet si disponible) pour déclencher le son.
- **Logique de lecture** :
    - La fonction de lecture appliquera le volume sélectionné au `MediaPlayer` avant de lancer le `start()`.

## Verification Plan

### Automated Tests
- N/A (Interaction matérielle son/UI)

### Manual Verification
1. Lancer l'application et accéder au "Cockpit Terrain".
2. Ouvrir le "Scoreur".
3. Vérifier la présence de l'icône de sifflet et du curseur de volume en haut à droite.
4. Ajuster le volume et cliquer sur le sifflet pour vérifier la sortie sonore.
5. S'assurer que le son s'arrête correctement et peut être relancé immédiatement.
