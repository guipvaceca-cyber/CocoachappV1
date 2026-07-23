# Walkthrough - Console de Statistiques Professionnelle

L'outil de statistiques a été entièrement repensé pour offrir une expérience de "scouting" professionnelle, optimisée pour une utilisation fluide en match.

## Changements majeurs

### 📺 Interface "Tablette" Landscape Forcée
- **Bascule Automatique** : Plus besoin de tourner manuellement votre téléphone. Cliquer sur l'icône de stats force immédiatement l'application en mode **Paysage**.
- **Retour à la normale** : L'orientation d'origine est restaurée automatiquement dès que vous fermez l'outil.

### 🏐 Modélisation Réaliste du Terrain (`StatTerrainTool.kt`)
- **Terrain Complet (18m x 9m)** : Visualisation des deux camps pour un meilleur repérage spatial.
- **Lignes Réglementaires** : Tracé du filet (épais), des lignes de fond et surtout des **lignes d'attaque (3 mètres)** dans chaque camp.
- **Proportions respectées** : La zone d'attaque (3m) et la zone de défense (6m) sont modélisées avec leurs dimensions réelles pour une saisie ultra-précise.

### 🎨 Design & Ergonomie de Saisie
- **Header Centré** : Le titre est centré pour une lecture claire, avec les sélecteurs de joueurs (titulaires) répartis uniformément juste en dessous.
- **Code Couleur par Action** :
    - **Bleu** : Service
    - **Orange** : Réception
    - **Rouge** : Attaque
    - **Violet** : Bloc
    - **Vert** : Défense
- **Heatmap Thématique** : La heatmap sur le terrain change de couleur en fonction de l'action sélectionnée.

### ⚡ Fonctionnalités Avancées
- **Feedback Flash** : Un flash blanc confirme visuellement chaque impact touché sur le terrain.
- **Système Undo (Annuler)** : Possibilité d'annuler les dernières saisies une par une en cas d'erreur de manipulation.
- **Compteur de volume** : Affichage en temps réel du total pour la catégorie sélectionnée (ex: "ATTAQUE : 18").

## Comment l'utiliser ?

1. Dans le **Cockpit Terrain**, cliquez sur l'icône de graphique.
2. Votre écran bascule en paysage.
3. Sélectionnez le **joueur** en haut et l'**action** à droite.
4. Marquez les impacts sur le **camp adverse** (moitié supérieure du terrain).
5. Utilisez la flèche de retour (en haut à droite) pour annuler une erreur ou la corbeille pour tout effacer.

---

> [!TIP]
> L'outil est particulièrement puissant pour identifier les zones de faiblesse adverse en réception ou les zones préférentielles de vos attaquants !
