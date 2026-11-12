# Suivi AAPS redesign

Un seul document de suivi, mis à jour le 12 novembre 2026. Il vit dans `docs/redesign/SUIVI.md` et dans le projet Claude (même contenu). Il remplace le backlog et le plan d'implémentation. En cas de contradiction avec `SPEC.md`, ce document l'emporte.

**Comment ça marche.** Une étape = un seul sujet = un commit = un test sur le téléphone. Statut : `À faire`, `En cours`, `Fait`. La première case de chaque étape est toujours le diagnostic, avant tout code. Flux : diagnostic, accord, commit (Gradle bloque sans commit), test rapide, test sur le téléphone.

**Déjà fait** : tout l'accueil v2 (cercle et ses créneaux, barre des consommables, graphique, tuiles), les feuilles Insuline et Basale, la barre du bas, le menu, la page Graphiques (titre, tailles Petit / Moyen / Grand), le panneau consommables de la pompe patch, le verrouillage portrait, le mode daltonien (réglage à 3 choix dans les Paramètres). Ce qui reste est ci-dessous.

## Étapes

| N° | Étape | Statut |
|---|---|---|
| 1 | Accueil : bugs | En cours (code fait, test sur le téléphone) |
| 1b | Interrupteur nouvelle / ancienne interface | En cours (code fait, test sur le téléphone) |
| 2 | Accueil : affichage | À faire |
| 3 | Accueil : défilement du graphique | À faire |
| 4 | Feuilles Insuline et Basale : barre du haut | À faire |
| 5 | Medtrum et barres de progression | À faire |
| 6 | Écran d'injection : texte | À faire |
| 7 | Boucle | À faire |
| 8 | Notifications | À faire |
| 9 | Page Graphiques : mode édition | À faire |
| 10 | Thème clair | À faire |
| 11 | Animations | À faire (en dernier) |

Les étapes sont dans l'ordre où on les fait : on prend la première `À faire`. Les étapes 1, 2 et 3 touchent le même code (graphique et cercle de l'accueil) et s'enchaînent : l'étape 3 demande que l'étape 2 soit finie.

### 1. Accueil : bugs
- [x] Diagnostic de chaque bug (fichiers de `redesign/` seulement)
- [ ] Changements de cible reliés sur le graphique (segment vertical, couleur de la cible en vigueur) ; vérifier aussi la page 2
- [ ] « Glucides requis » affiché deux fois : trouver la duplication, n'en garder qu'une
- [ ] Décalage entre les chevrons ∨ et ∧ de la barre des consommables (bouton 44 × 24 dp)
- [ ] Revérifier sur le téléphone l'échelle au-dessus de 280 (déjà faite)

### 1b. Interrupteur nouvelle / ancienne interface
- [x] Accord sur la conception (12 nov.) : un réglage, pas un nouveau build
- [ ] Réglage « Nouvelle interface » (activé par défaut, inclus dans l'export) dans Paramètres › Apparence
- [ ] Dans le Menu de la nouvelle interface : ligne « Nouvelle interface » ; l'appui demande une confirmation et dit comment revenir
- [ ] L'écran redémarre au changement ; interface classique = AAPS d'origine (écran principal, lignes des feuilles Insuline et Basale, pas de verrouillage portrait)
- [ ] La constante `RedesignFlags.ENABLED` reste l'interrupteur du build (false = jamais de nouvelle interface)

### 2. Accueil : affichage
- [ ] Diagnostic
- [ ] Plus d'accès aux graphiques depuis l'accueil : retirer l'appui sur le graphique et sur le rond de glycémie. La seule entrée est l'icône Graphiques de la barre du bas. L'appui long (ligne d'inspection) reste
- [ ] Cercle sans flèche de tendance : delta au centre, triangles sur l'anneau. Le nom accessible garde le sens de la tendance
- [ ] Cercle : le texte d'âge (« il y a 1 min ») se met à jour chaque minute ; la valeur, les triangles et la couleur suivent chaque nouvelle lecture (selon la fréquence de ta source de glycémie). Le graphique reste à 5 min. Jamais d'animation sur une minuterie
- [ ] Tuiles : texte centré à droite de l'icône (l'icône reste à gauche)
- [ ] Ligne de sensibilité entre le graphique et les tuiles : 6 dp sous le graphique, espace flexible (minimum 12 dp) jusqu'aux tuiles. Vérifier que la page tient sans défiler sur 412 × 891 dp
- [ ] Valeurs des bolus (au-dessus du triangle) et des glucides (en dessous), échelles 3 h et 6 h seulement, pas de valeur pour les SMB

### 3. Accueil : défilement du graphique
- [ ] Diagnostic (`RedesignHomeGraph`, `RedesignHomeGraphScale.kt` ; aucune donnée nouvelle, le graphique lit déjà les 24 h)
- [ ] Glissement horizontal dans les 24 dernières heures, pour les échelles 3, 6, 12 et 18 h (en 24 h tout est visible)
- [ ] Un changement d'échelle ramène toujours la zone sur maintenant, sans bouton « maintenant ». Une nouvelle lecture ne déplace pas une zone reculée
- [ ] Pendant le glissement l'axe des valeurs ne bouge pas ; il s'adapte à la zone visible après le relâchement, comme les graphiques déjà dans l'app
- [ ] Gestes : le glissement fait défiler, l'appui long inspecte ; vérifier que l'un ne déclenche pas l'autre

### 4. Feuilles Insuline et Basale : barre du haut
- [ ] Diagnostic : comparer à `sheet-insulin.png` et `sheet-basal.png` (pastille « IOB » ou « TBR » suivie d'une ligne, dans un cadre arrondi ; pour Insuline une seconde ligne glycémie, IOB, COB ; pour Basale le bouton « Annuler la basale temp. » juste dessous)
- [ ] Correction, par le paramètre `topInfo` déjà en place (pas de nouveau point d'accroche si possible)

### 5. Medtrum et barres de progression
- [ ] Diagnostic. Le code Medtrum est dans le module de la pompe : je dis la cause et le fichier avant de toucher quoi que ce soit ; changement minimal sur l'affichage seulement, jamais de code de pompe, noté dans `UPSTREAM_TOUCHES.md`
- [ ] Indicateur à ronds bloqué sur la 3ᵉ étape à la dernière étape de l'assistant de changement de pompe
- [ ] Point en fin de barre de progression : faire d'abord le tour de toutes les barres de l'app (écran, barre, point oui / non). La barre des consommables est correcte, celle de l'assistant Medtrum ne l'est pas (cause probable : l'indicateur d'arrêt de la barre Material)
- [ ] Traductions du menu changement de pompe : tableau « actuel → proposé » à valider avant toute modification

### 6. Écran d'injection : texte
- [ ] Diagnostic : trouver la chaîne source avant de choisir le mécanisme
- [ ] « x unités vont être injectées » devient « Injection de x Unités »

Règle des traductions : on ne modifie jamais les chaînes d'origine (Crowdin les réécrit). Les remplacements vivent dans un fichier du fork, comme `strings_redesign.xml`. Pour qu'une traduction arrive un jour dans AAPS, il faut aussi la proposer sur Crowdin ([mode d'emploi](https://androidaps.readthedocs.io/en/latest/SupportingAaps/Translations.html)).

### 7. Boucle
- [ ] Diagnostic (slot Boucle, badge ①)
- [ ] Icône seule, sans texte ; la couleur indique le mode. Les noms accessibles gardent le texte complet
- [ ] Geste : boucle ouverte avec une suggestion en attente → appui = accepter la suggestion, appui long = changer le mode. Sans suggestion en attente → appui = changer le mode. Pas de chevron : le badge ① suffit

### 8. Notifications
- [ ] Diagnostic (interface seulement : aucune logique d'alarme, de pompe ou de base touchée ; les données viennent des états existants). Vérifier d'abord comment AAPS affiche déjà les alarmes urgentes (son, écran plein) pour que le popup ne fasse pas doublon
- [ ] Badge coloré selon le type : info bleu, avertissement jaune, alerte rouge. Le badge prend la couleur du type le plus grave présent
- [ ] Popup pour les alertes graves (pompe déconnectée, etc.), seulement pour les notifications d'importance « urgente » qui existent déjà, et seulement si les alarmes d'origine ne le font pas déjà
- [ ] Aperçu du message pour toutes les notifications

### 9. Page Graphiques : mode édition
- [ ] Diagnostic d'abord, pour chaque action (menu des séries, ajouter, supprimer, monter / descendre, taille, restaurer pour ✕) : le code d'origine l'offre-t-il, par quel appel, quels points d'accroche ? Si monter / descendre n'existe pas dans 4.0 et demande plus qu'un paramètre par défaut, s'arrêter et me le dire
- [ ] Pilule (✕) Mode édition (+) (✓), graphiques atténués et verrouillés, contrôles sur chaque graphique, Retour = annuler, nouveau graphique en bas en Petit
- [ ] Optionnel : même échelle de glycémie que l'accueil sur la page 2 (seulement par un paramètre par défaut sur `BgGraphCompose`, sinon on abandonne)

### 10. Thème clair
- [ ] Diagnostic : tour des couleurs de toutes les pages en thème clair
- [ ] Page Graphiques : basale en cyan clair, point de la légende « Déviations », barres vertes de déviations
- [ ] Couleur de la basale temporaire (`#00FFFF`, proposition `#0B6A70`)
- [ ] Surfaces du redesign en thème clair ; vérifier les valeurs de la palette (déjà faite)

### 11. Animations
- [ ] Diagnostic
- [ ] Barre des consommables : ouverture en grand de type tiroir
- [ ] Animations courtes sur chaque nouvelle lecture (fondu de la valeur, arc de tendance, nouveau point), jamais sur une minuterie
- [ ] Relever la limite de 200 ms pour les transitions de page

## Idées pour plus tard (chacune demande sa propre spec)
- [ ] Demander le mode daltonien à AAPS par **issues, pas par PR** : AAPS refuse les PR faites avec l'IA (`CONTRIBUTING.md`, « AI-generated contributions »). Deux issues séparées : le réglage à 3 choix, puis les couleurs « très bas / très haut ». Y mettre l'analyse et les captures comme contexte, et laisser le code aux mainteneurs. Vérifier master d'abord
- [ ] Traductions : passe sur toute l'application, écran par écran (tableau « actuel → proposé » validé avant chaque écran)
- [ ] Option « Noir pur en mode sombre » (ajoute une préférence dans les Paramètres d'origine)
- [ ] Widgets (le 4x2 en premier)
- [ ] Cadran Garmin
- [ ] Compatibilité Android Auto
- [ ] Écran toujours allumé (AOD) pour voir la glycémie
- [ ] Indice visuel de l'état de la boucle dans le cercle (fermée / ouverte / déconnectée)
