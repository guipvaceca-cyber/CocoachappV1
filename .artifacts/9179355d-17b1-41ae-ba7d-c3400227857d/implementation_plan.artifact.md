# Plan d'Amélioration du Spider Chart (Radar Chart)

Ce plan vise à rendre l'analyse de compétences plus lisible, esthétique et riche en informations, en passant d'un graphique simple à un véritable outil de pilotage.

## 1. Améliorations Graphiques ("Pimp")

### Lisibilité et Contraste
- **Labels Lumineux** : Passer les intitulés en blanc éclatant (`Color.White`) avec une légère ombre portée pour qu'ils ressortent sur le fond bleu nuit.
- **Grille "Glass"** : Utiliser des lignes blanches très fines à opacité variable (`0.1f` à `0.3f`) pour la grille de fond, renforçant l'aspect technologique.

### Esthétique "Premium"
- **Zones Dégradées** : Remplacer les aplats de couleur par des dégradés radiaux ou linéaires (`Brush`) pour donner du volume aux surfaces de score.
- **Points de Données (Markers)** : Ajouter des petits cercles lumineux à chaque sommet des polygones pour marquer précisément les scores.
- **Animations Avancées** : Faire "pousser" les branches du radar depuis le centre au chargement avec un effet d'élasticité.

## 2. Améliorations Conceptuelles

### Comparaison Augmentée
- **Zone Cible (Target)** : Afficher en pointillé très discret un profil "Coach Expert" (score de 4.5/5 partout) pour situer ses marges de progression.
- **Légende Interactive** : Ajouter une légende claire en bas du graphique : "Flash (Bleu) - Diagnostic de séance" vs "Global (Violet) - Équilibre de carrière".

### Données Contextuelles
- **Scores chiffrés** : Afficher le score moyen à côté de chaque label (ex: "Technique (4.2)").

## Fichiers à Modifier

#### [MODIFY] [RadarChart.kt](file:///C:/Users/guip3/AndroidStudioProjects/CoachApp/app/src/main/java/com/example/coachapp/ui/components/RadarChart.kt)
- Refonte complète de la fonction `Canvas`.
- Implémentation des nouvelles animations et dégradés.
- Correction des couleurs de texte.

## Plan de Vérification

### Vérification Manuelle
- Vérifier la lisibilité des textes sur fond sombre.
- Valider la fluidité de l'animation au déploiement de la carte.
- S'assurer que les deux couches (Flash/Global) sont bien distinctes.
