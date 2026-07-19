# Walkthrough - Ajout du Sifflet Tactique

Le module "Scoreur" du Cockpit Terrain dispose désormais d'un sifflet intégré avec contrôle du volume.

## Modifications effectuées

### Cockpit Terrain (Match Live)
#### [MatchDashboardScreen.kt](file:///C:/Users/guip3/AndroidStudioProjects/CoachApp/app/src/main/java/com/example/coachapp/ui/screens/MatchDashboardScreen.kt)

- **Sifflet en en-tête** : L'icône de sifflet et le curseur de volume ont été déplacés dans l'en-tête de la section "SCOREUR". Ils sont désormais parfaitement alignés à droite du titre.
- **Accessibilité permanente** : Le sifflet est visible même quand la section du scoreur est repliée, permettant un accès instantané.
- **Contrôle du volume** : Un `Slider` compact (70dp) permet de régler la puissance sonore.
- **Audio réel** : Intégration d'un véritable son de sifflet (police/arbitre) de 33 Ko.
- **Nettoyage UI** : Suppression de l'ancienne barre de sifflet qui chevauchait les noms d'équipe dans le contenu du scoreur.

### Ressources
- **Audio** : Le fichier [whistle.mp3](file:///C:/Users/guip3/AndroidStudioProjects/CoachApp/app/src/main/res/raw/whistle.mp3) est opérationnel.

## Comment utiliser le sifflet ?
1. Rendez-vous dans le **Cockpit Terrain** (Match Live).
2. Repérez le module **Scoreur**.
3. En haut à droite du scoreur, vous trouverez le curseur de volume et l'icône du sifflet.
4. Cliquez sur l'icône pour déclencher le son.

> [!IMPORTANT]
> **Action requise** : Vous devez remplacer le fichier [whistle.mp3](file:///C:/Users/guip3/AndroidStudioProjects/CoachApp/app/src/main/res/raw/whistle.mp3) factice par un véritable enregistrement de sifflet pour que la fonctionnalité soit pleinement opérationnelle.

## Vérification technique
- Le build Kotlin est passé avec succès.
- Les composants UI ont été intégrés sans perturber l'agencement existant du scoreur.
