# Plan d'Optimisation Visuelle du Dashboard (Version Corrigée)

Ce plan vise à élever la qualité visuelle de l'écran d'accueil (`DashboardScreen`) en affinant les effets de "glassmorphism", en améliorant la hiérarchie visuelle et en ajoutant des micro-interactions.

## Changements Proposés

### 1. Fond d'Écran
- **Décision** : Le fond bleu uni actuel (`Color(0xFF001529)`) est conservé tel quel selon la demande utilisateur.

### 2. Raffinement du Glassmorphism
- **Bordures Lumineuses** : Appliquer une bordure plus fine (`0.5.dp`) avec un blanc très transparent mais plus contrasté sur les bords supérieurs pour simuler le reflet du verre.
- **Cohérence des Formes** : Harmoniser les rayons de courbure (`RoundedCornerShape`) à 24.dp pour les sections majeures (ContextualCard) et 16.dp ou 20.dp pour les éléments secondaires.

### 3. Carte Contextuelle Dynamique
- **Effet de "Glow" Subtil** : Ajouter une lueur interne légère (`Box` avec ombre ou dégradé) qui change de couleur selon l'état (ex: lueur rouge pour le mode "FIELD").
- **Transitions Fluides** : Utiliser `animateColorAsState` pour que le passage d'un état à l'autre (ex: PREP vers FIELD) se fasse via un fondu de couleur élégant.

### 4. Grille de Modules Modernisée
- **Aura d'Icône** : Ajouter un dégradé radial très doux derrière chaque icône de module pour lui donner du relief par rapport à la carte.
- **Micro-interactions** : Ajouter un effet de feedback visuel (légère réduction de taille au clic) sur les `ModuleCard`.

### 5. Titres de Section et Hiérarchie
- **Accents Visuels** : Accompagner chaque titre de section d'une petite barre verticale de couleur d'accent (VolleyBlueLight ou Cyan) à gauche du texte.
- **Typographie** : Uniformiser les styles pour une meilleure lisibilité.

## Fichiers à Modifier

#### [MODIFY] [DashboardScreen.kt](file:///C:/Users/guip3/AndroidStudioProjects/CoachApp/app/src/main/java/com/example/coachapp/ui/screens/DashboardScreen.kt)
- Mise à jour de `ModuleCard` (aura, interactions).
- Refonte de `ContextualCard` (animations, glow).
- Ajout d'un composant `SectionHeader` réutilisable.
- Raffinement global des styles de `Card`.

## Plan de Vérification

### Vérification Manuelle
- Déploiement sur appareil pour valider les contrastes.
- Test des transitions d'état du dashboard.
- Vérification du feedback tactile sur les modules.
