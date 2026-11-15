# Suivi AAPS redesign

Un seul document de suivi, mis à jour le 15 novembre 2026. Il vit dans `docs/redesign/SUIVI.md` et dans le projet Claude (même contenu). Il remplace le backlog et le plan d'implémentation. En cas de contradiction avec `SPEC.md`, ce document l'emporte.

**Comment ça marche.** Une étape = un seul sujet = un commit = un test sur le téléphone. Statut : `À faire`, `En cours`, `Fait`. La première case de chaque étape est toujours le diagnostic, avant tout code. Flux : diagnostic, accord, commit (Gradle bloque sans commit), test rapide, test sur le téléphone.

**Déjà fait** : tout l'accueil v2 (cercle et ses créneaux, barre des consommables, graphique, tuiles), les feuilles Insuline et Basale, la barre du bas, le menu, la page Graphiques (titre, tailles Petit / Moyen / Grand), le panneau consommables de la pompe patch, le verrouillage portrait, le mode daltonien (réglage à 3 choix dans les Paramètres). Ce qui reste est ci-dessous.

## Étapes

| N° | Étape | Statut |
|---|---|---|
| 1 | Accueil : bugs | Fait |
| 1b | Interrupteur nouvelle / ancienne interface | Fait |
| 2 | Accueil : affichage | Fait |
| 3 | Accueil : défilement du graphique | Fait |
| 4 | Feuilles Insuline et Basale : barre du haut | Fait |
| 5 | Medtrum et barres de progression | Fait |
| 6 | Écran d'injection : texte | Fait |
| 7 | Boucle | Fait |
| 8 | Notifications | Fait |
| 9 | Page Graphiques : mode édition | Fait |
| 10 | Thème clair | Fait |
| 11 | Animations | Fait |
| 12 | Accueil : état de la boucle sur l'anneau | Code fait, à tester |
| 13 | Option « Noir pur » (nouvelle interface) | Code fait, à tester |
| 14 | Widget 4x2 : cercle et 6 infos | À faire |

Les 11 étapes sont validées sur le téléphone (15 nov.). Prochaine étape : 12 (choisie dans « Idées pour plus tard » le 15 nov.).

### 1. Accueil : bugs
- [x] Diagnostic de chaque bug (fichiers de `redesign/` seulement)
- [x] Changements de cible reliés sur le graphique (segment vertical, couleur de la cible en vigueur) ; vérifier aussi la page 2
- [x] « Glucides requis » affiché deux fois : trouver la duplication, n'en garder qu'une
- [x] Décalage entre les chevrons ∨ et ∧ de la barre des consommables (bouton 44 × 24 dp)
- [x] Revérifier sur le téléphone l'échelle au-dessus de 280 (déjà faite)

### 1b. Interrupteur nouvelle / ancienne interface
- [x] Accord sur la conception (12 nov.) : un réglage, pas un nouveau build
- [x] Réglage « Nouvelle interface » (activé par défaut, inclus dans l'export) dans Paramètres › Apparence
- [x] Dans le Menu de la nouvelle interface : ligne « Nouvelle interface » ; l'appui demande une confirmation et dit comment revenir
- [x] L'écran redémarre au changement ; interface classique = AAPS d'origine (écran principal, lignes des feuilles Insuline et Basale, pas de verrouillage portrait)
- [x] ~~La constante `RedesignFlags.ENABLED` reste l'interrupteur du build~~ : retirée le 14 nov. (elle faisait la même chose que le réglage éteint). Sans `ComposeMainActivity` (iOS, bureau), l'interface classique reste.

### 2. Accueil : affichage
- [x] Diagnostic : le cercle lit les données regroupées par 5 min (comme AAPS d'origine) ; avec une source à 1 min la valeur ne bougeait que toutes les 5 min. Le texte d'âge se mettait déjà à jour toutes les 30 s
- [x] Plus d'accès aux graphiques depuis l'accueil : retirer l'appui sur le graphique et sur le rond de glycémie. La seule entrée est l'icône Graphiques de la barre du bas. L'appui long (ligne d'inspection) reste
- [x] Cercle sans flèche de tendance : delta au centre, triangles sur l'anneau. Le nom accessible garde le sens de la tendance
- [x] Cercle : le texte d'âge (« il y a 1 min ») se met à jour chaque minute ; la valeur, les triangles et la couleur suivent chaque nouvelle lecture (selon la fréquence de ta source de glycémie). Le graphique reste à 5 min. Jamais d'animation sur une minuterie. Fait : le cercle prend la lecture la plus récente de la source quand elle est plus récente que le groupe de 5 min (valeur, couleur, âge) ; le delta et les triangles restent ceux d'AAPS (ils ont besoin des données regroupées)
- [x] Tuiles : texte centré à droite de l'icône (l'icône reste à gauche)
- [x] Ligne de sensibilité entre le graphique et les tuiles : 6 dp sous le graphique, espace flexible (minimum 12 dp) jusqu'aux tuiles. Vérifier que la page tient sans défiler sur 412 × 891 dp
- [x] Valeurs des bolus (au-dessus du triangle) et des glucides (en dessous), sur toutes les échelles (14 nov., test A8), une valeur qui chevauche une valeur déjà dessinée est cachée, pas de valeur pour les SMB

### 3. Accueil : défilement du graphique
- [x] Diagnostic (`RedesignHomeGraph`, `RedesignHomeGraphScale.kt` ; aucune donnée nouvelle, le graphique lit déjà les 24 h). Les heures du bas gardent la grille de « maintenant » pendant le défilement ; les points restent comptés depuis la dernière lecture ; une zone reculée n'a pas de point creux
- [x] Glissement horizontal dans les 24 dernières heures, pour les échelles 3, 6, 12 et 18 h (en 24 h tout est visible)
- [x] Un changement d'échelle ramène toujours la zone sur maintenant, sans bouton « maintenant ». Une nouvelle lecture ne déplace pas une zone reculée
- [x] Pendant le glissement l'axe des valeurs ne bouge pas ; il s'adapte à la zone visible après le relâchement, comme les graphiques déjà dans l'app
- [x] Gestes : le glissement fait défiler, l'appui long inspecte ; vérifier que l'un ne déclenche pas l'autre

### 4. Feuilles Insuline et Basale : barre du haut
- [x] Diagnostic : comparer à `sheet-insulin.png` et `sheet-basal.png` (pastille « IOB » ou « TBR » suivie d'une ligne, dans un cadre arrondi ; pour Insuline une seconde ligne glycémie, IOB, COB ; pour Basale le bouton « Annuler la basale temp. » juste dessous). Trouvé : ligne en bloc gris sans pastille, texte centré, les trois valeurs en couleur ; sur Insuline elle était sous la ligne glycémie / IOB / COB (la maquette la met au-dessus) ; bouton Annuler sans couleur ni croix. En français la pastille IOB est « IA » (texte d'origine traduit)
- [x] Correction, par le paramètre `topInfo` déjà en place (pas de nouveau point d'accroche si possible)

### 5. Medtrum et barres de progression
- [x] Diagnostic. Le code Medtrum est dans le module de la pompe : je dis la cause et le fichier avant de toucher quoi que ce soit ; changement minimal sur l'affichage seulement, jamais de code de pompe, noté dans `UPSTREAM_TOUCHES.md`
- [x] Indicateur à ronds bloqué sur la 3ᵉ étape à la dernière étape de l'assistant de changement de pompe
- [x] Point en fin de barre de progression : faire d'abord le tour de toutes les barres de l'app (écran, barre, point oui / non). La barre des consommables est correcte, celle de l'assistant Medtrum ne l'est pas (cause probable : l'indicateur d'arrêt de la barre Material)
- [x] Traductions du menu changement de pompe : tableau « actuel → proposé » à valider avant toute modification

Résultat du diagnostic (12 nov.) :

- **Indicateur à ronds** : le dessin (`StepProgressIndicator`, core/ui) est correct. L'index vient de `MedtrumPatchViewModel.updateWizardPage` : une étape absente de la liste des pages donne `indexOf = -1`, ramené à 0 (premier rond). Exemple trouvé : « Terminé » après une désactivation va à `COMPLETE`, qui n'est pas dans la liste de désactivation (3 pages). Je n'ai pas trouvé de chemin qui reste sur le **3ᵉ** rond : **pas de changement**, il me faut le chemin exact (désactivation seule ? désactivation puis nouveau patch ? reprise d'activation ?). C'est du code du module pompe : je ne le touche pas sans la cause sûre.
- **Point en fin de barre** : tour de toutes les barres (`LinearProgressIndicator`). Seule notre barre des consommables enlève le point (`drawStopIndicator = {}`). Toutes les autres barres à valeur connue ont le point de Material 3 : écran de démarrage (`AapsAppRoot`), activité de la pompe (`PumpActivityDialog`), objectifs (`ExamBottomSheet`, `NtpProgressDialog`, `ObjectivesScreen`), Carelevo, Combo v2 (vue d'ensemble et appairage), EOPatch, **Medtrum (amorçage)**, historique, puces profil et cible temp., bandeau de l'accueil d'origine, `OverviewScreen`, voyants d'état, cartes profil et cible temp., statistiques. Sans point (barres sans valeur) : `AapsAppRoot` (2ᵉ), `PumpActivityDialog` (2ᵉ), Open Humans. **Changé** : Medtrum amorçage seulement (affichage, `PrimeStep.kt`), comme la barre des consommables. Les autres : à décider.
Décisions (12 nov.) et changements faits (affichage seulement, détail dans `UPSTREAM_TOUCHES.md`) :

- **Ronds** : le bug est dans l'assistant d'**activation** (5 ronds). La liste finit par une page `COMPLETE` jamais affichée : le bouton de « Activation terminée » ferme l'assistant. Le dernier écran restait donc sur le 4ᵉ rond. Correction dans `updateWizardPage` : sur `ACTIVATE_COMPLETE`, le dernier rond s'allume. L'assistant de désactivation (3 ronds) était déjà correct.
- **Point en fin de barre** : enlevé de **toutes** les barres à valeur connue (nouvelle interface et pages d'origine, 18 barres). Écran d'amorçage Medtrum : le rond qui tourne sous la barre est enlevé, la barre montre déjà la progression.
- **Traductions** : tableau ci-dessous validé, plus `retract_needle` (« Retirez »), `medtrum_deactivate_pump_confirm` (plus de « !! »), `cancel_sure`, et « patch » en minuscule partout. Les textes sont dans `app/src/main/res/values-fr-rFR/strings_redesign.xml`. À proposer aussi sur Crowdin.

- **Traductions** (tableau d'origine, avant validation). Attention : une chaîne du fork ne peut pas remplacer une chaîne Crowdin dans le même module (ressource en double). Le remplacement doit vivre dans un module plus haut (par ex. `app/src/main/res/values-fr-rFR/strings_redesign.xml`).

| Clé | Actuel | Proposé |
|---|---|---|
| `change_patch_label` | Changer le Patch | Changer de patch |
| `step_prepare_patch` | Activer le Patch | Activer un nouveau patch |
| `step_prepare_patch_connect` | Connecter et remplir | Connexion et remplissage |
| `step_select_insulin` | Sélectionner l'insuline | Choisir l'insuline |
| `step_prime` | Amorcer | Amorçage |
| `step_priming` | Amorçage | Amorçage en cours… |
| `step_priming_complete` | Amorçage terminé | (inchangé) |
| `step_attach` | Fixer le Patch | Poser le patch |
| `step_activate` | En cours d'activation... | Activation en cours… |
| `step_activate_complete` | Activation effectuée | Activation terminée |
| `step_deactivate` | Désactiver le Patch | Désactiver le patch |
| `step_deactivating` | Désactivation en cours... | Désactivation en cours… |
| `step_deactivate_complete` | Patch désactivé | (inchangé) |
| `step_retry_activation` | Activation en cours | Reprise de l'activation |
| `deactivate_sure` | Êtes-vous sûr de vouloir désactiver le Patch actuel ? | Voulez-vous vraiment désactiver le patch actuel ? |

### 6. Écran d'injection : texte
- [x] Diagnostic : la chaîne est `goingtodeliver` de `core/ui` (fichier Crowdin), utilisée par la fenêtre d'activité de la pompe et la montre. Une chaîne du fork ne peut pas la remplacer dans le même module (ressource en double) : le remplacement est dans `app/src/main/res/values-fr-rFR/strings_redesign.xml` (les ressources de l'app passent avant celles des modules). À proposer aussi sur Crowdin
- [x] « x unités vont être injectées » devient « Injection de x Unités »

Règle des traductions : on ne modifie jamais les chaînes d'origine (Crowdin les réécrit). Les remplacements vivent dans un fichier du fork, comme `strings_redesign.xml`. Pour qu'une traduction arrive un jour dans AAPS, il faut aussi la proposer sur Crowdin ([mode d'emploi](https://androidaps.readthedocs.io/en/latest/SupportingAaps/Translations.html)).

### 7. Boucle
- [x] Diagnostic (slot Boucle, badge ①) : le slot montrait encore la légende (« Fermée »…) ; avec une suggestion, l'appui ouvrait la feuille d'acceptation et il n'y avait pas d'appui long. Choix de sécurité : l'appui ouvre toujours la feuille d'origine qui demande avant d'envoyer (pas d'acceptation en un seul appui)
- [x] Icône seule, sans texte ; la couleur indique le mode. Les noms accessibles gardent le texte complet
- [x] Geste : boucle ouverte avec une suggestion en attente → appui = accepter la suggestion, appui long = changer le mode. Sans suggestion en attente → appui = changer le mode. Pas de chevron : le badge ① suffit

### 8. Notifications
- [x] Diagnostic (interface seulement : aucune logique d'alarme, de pompe ou de base touchée ; les données viennent des états existants). Vérifier d'abord comment AAPS affiche déjà les alarmes urgentes (son, écran plein) pour que le popup ne fasse pas doublon. Trouvé : le niveau URGENT d'AAPS sonne déjà et prend tout l'écran (`NotificationLevel.URGENT`) : **pas de popup ajouté** (il ferait doublon). Types : URGENT = alerte (rouge) ; IMPORTANT et NORMAL = avertissement (jaune) ; LOW, INFO, ANNOUNCEMENT = info (bleu). Couleurs = teintes rouge / jaune / bleu du redesign, chiffre dans la couleur claire ou foncée de la teinte (un chiffre blanc ne se lit pas sur du jaune). Aperçu : une carte 5 s en haut de l'accueil à l'arrivée d'une notification (pas au lancement), appui = feuille des notifications
- [x] Badge coloré selon le type : info bleu, avertissement jaune, alerte rouge. Le badge prend la couleur du type le plus grave présent
- [x] ~~Popup pour les alertes graves (pompe déconnectée, etc.), seulement pour les notifications d'importance « urgente » qui existent déjà, et seulement si les alarmes d'origine ne le font pas déjà~~ : abandonné, le niveau URGENT d'AAPS le fait déjà (voir diagnostic)
- [x] Aperçu du message pour toutes les notifications

### 9. Page Graphiques : mode édition
- [x] Diagnostic d'abord, pour chaque action (menu des séries, ajouter, supprimer, monter / descendre, taille, restaurer pour ✕) : le code d'origine l'offre-t-il, par quel appel, quels points d'accroche ? Si monter / descendre n'existe pas dans 4.0 et demande plus qu'un paramètre par défaut, s'arrêter et me le dire. Trouvé : tout passe par `GraphConfig` et `GraphViewModel.updateGraphConfig` (public) ; monter / descendre = changer l'ordre de la liste `secondaryGraphs`, aucun code d'origine nouveau. Points d'accroche : un calque par graphique (`GraphsRedesignLayout.blockOverlay`), `locked`, pas de ligne de hauteur dans les menus du redesign (voir UPSTREAM_TOUCHES). Le texte « Ce graphique ne peut pas être supprimé » n'est pas ajouté au menu d'origine du graphique BG (il faudrait un autre point d'accroche) ; le BG et IOB n'ont simplement pas de bouton supprimer
- [x] Pilule (✕) Mode édition (+) (✓), graphiques atténués et verrouillés, contrôles sur chaque graphique, Retour = annuler, nouveau graphique en bas en Petit
- [x] Optionnel : même échelle de glycémie que l'accueil sur la page 2 (seulement par un paramètre par défaut sur `BgGraphCompose`, sinon on abandonne). **Abandonné** : il faut un paramètre sur `BgGraphCompose` et un autre point d'accroche dans `GraphsSection` pour le passer

### 10. Thème clair
- [x] Diagnostic : tour des couleurs de toutes les pages en thème clair. Fait dans le code seulement : les couleurs illisibles sur blanc sont la basale temp. (`#00FFFF`) et les couleurs fixes des séries de la page Graphiques (cyan BGI / déviations, jaunes, menthe, vert citron) et des barres de déviation (vert vif, jaune UAM), identiques dans les deux thèmes. Les surfaces du redesign ont déjà leurs valeurs claires (`LightRedesignColors`). Le tour visuel écran par écran reste à faire sur le téléphone (maquettes « thème clair »). La pilule et les contrôles du mode édition restent sombres dans les deux thèmes (couleurs de la SPEC 4.3)
- [x] Page Graphiques : basale en cyan clair, point de la légende « Déviations », barres vertes de déviations
- [x] Couleur de la basale temporaire (`#00FFFF`, proposition `#0B6A70`)
- [x] Surfaces du redesign en thème clair ; vérifier les valeurs de la palette (déjà faite)

### 11. Animations
- [x] Diagnostic : aucune animation avant. Toutes sont déclenchées par un changement réel (ouverture de la barre, nouvelle lecture, changement de page), jamais par une minuterie ; « Supprimer les animations » d'Android les rend instantanées. Durées dans `RedesignMotion.kt` : tiroir 300 ms, page 250 ms, nouvelle lecture 200 ms (fondu de la valeur, couleur et arc de tendance qui tournent, nouveau point qui apparaît)
- [x] Barre des consommables : ouverture en grand de type tiroir
- [x] Animations courtes sur chaque nouvelle lecture (fondu de la valeur, arc de tendance, nouveau point), jamais sur une minuterie
- [x] Relever la limite de 200 ms pour les transitions de page

### 12. Accueil : état de la boucle sur l'anneau
- [x] Diagnostic : l'anneau est dessiné dans `RedesignBgCircle` (cercle complet à 22 % de la couleur de la plage, puis l'arc de tendance et ses triangles par-dessus). Le mode de la boucle est déjà dans `RedesignTilesState.runningMode` (créneau Boucle) : aucun nouveau point d'accroche. Couleur et motif dans `RedesignLoopRing.kt`, partagés par le créneau Boucle, le cercle et le widget (étape 14). Attention : dans le thème, « Suspendue » est **jaune** (comme l'icône du créneau Boucle), pas rouge ; j'ai suivi l'icône
- [x] L'anneau prend la couleur et le motif de l'icône de la boucle, atténué (50 %) pour que la valeur et l'arc de tendance restent devant. Les tirets sont étirés pour tomber juste sur le tour du cercle. Superbolus : orange, plein :
  - Fermée : vert, plein
  - Arrêt glycémie basse : violet, tirets
  - Ouverte : bleu, tirets
  - Suspendue : rouge, longs tirets
  - Désactivée : rouge, points
  - Pompe déconnectée : gris, points
- [x] La valeur et l'arc de tendance gardent la couleur de la plage de glycémie ; l'anneau ne montre jamais la plage
- [x] Suggestion en attente en boucle ouverte : rien de plus sur l'anneau, le badge ① suffit
- [x] Couleurs lues dans le thème (mêmes que l'icône de la boucle), aussi en thème clair. Le mode daltonien ne change que les couleurs de la glycémie, pas celles de la boucle
- [x] Le nom accessible du cercle dit le mode de la boucle (« …. Boucle : Fermée »)

### 13. Option « Noir pur » (nouvelle interface)
- [x] Diagnostic : où sont les couleurs sombres du redesign, quelles surfaces sont appuyables, et comment le réglage « Nouvelle interface » (étape 1b) est branché dans Paramètres › Apparence. Trouvé : couleurs sombres dans `RedesignColors.kt` ; le fond de l'accueil est la surface Material (transparent partout) ; la barre du bas a sa propre couleur (`RedesignBottomBarStyle`). Fait : `PureBlackRedesignColors` et `RedesignPureBlackTheme` autour de `RedesignMainScreen` (fichier du fork, l'activité n'est pas touchée). Le réglage s'applique sans redémarrer
- [x] Réglage « Noir pur » dans Paramètres › Apparence (éteint par défaut, inclus dans l'export), thème sombre seulement
- [x] Nouvelle interface seulement : les écrans d'origine ne changent pas
- [x] Fond de page `#000000` ; ce qu'on ne peut pas appuyer reste noir (zone du graphique, ligne de sensibilité)
- [x] Règle : surface plus claire = appuyable. Tuiles, barre des consommables, choix d'échelle, barre du bas et feuilles prennent une surface plus claire qu'aujourd'hui ; les tuiles teintées gardent leur teinte, en plus clair. Le choix d'échelle : seule la case choisie a un fond (plus clair)
- [x] Vérifier la lisibilité du texte coloré sur les surfaces plus claires, aussi en mode daltonien. Test `RedesignPureBlackTest` : contraste ≥ 4,5 pour chaque teinte. Le mode daltonien ne change que les couleurs de la glycémie, pas les tuiles
- [ ] Sur le téléphone : traînées au défilement du graphique (OLED)

### 14. Widget 4x2 : cercle et 6 infos
Maquette : `mockup/png/widget-4x2.png` (358 × 195 dp, mesuré sur le S26+).
- [ ] Diagnostic : d'où vient le widget actuel avec le cercle (AAPS 4 d'origine ou le fork), comment il est dessiné et mis à jour, et comment le remplacer sans toucher au code d'origine (sinon : second widget « nouvelle interface » à côté). Vérifier les vraies tailles du 4x2 sur le téléphone
- [ ] 3 colonnes : au centre un carré de toute la hauteur pour le cercle (environ 183 dp, 93 % de l'app) ; à gauche et à droite 3 tuiles égales
- [ ] Cercle comme l'accueil : delta, valeur, âge, arc de tendance ; anneau de l'étape 12 (couleur et motif de la boucle)
- [ ] Tuiles : icône de l'accueil en haut (en plus grand), valeur seule dessous, pas de libellé. Gauche : Profil, Insuline active, Boucle. Droite : Glucides, Basale (temp.), Cible. Boucle et cible en bas comme sur l'accueil ; couleurs des tuiles de l'accueil (cible orange si cible temp.)
- [ ] À décider : afficher en petit la durée restante de la basale temp.
- [ ] Lecture seule : un appui ouvre l'app, aucune action dans le widget
- [ ] L'âge de la valeur est toujours visible ; mise à jour à chaque nouvelle lecture
- [ ] Noms accessibles complets (par ex. « Insuline active 5,42 U »)
- [ ] Thème sombre, thème clair et mode daltonien

## Corrections après le test sur le téléphone (14 et 15 nov.)
- [x] A8 : valeurs des bolus et des glucides sur toutes les échelles ; une valeur qui chevauche une autre est cachée
- [x] A10 : la cible du graphique de l'accueil suit la cible ajustée par la boucle, comme le créneau Cible
- [x] A11 : la carte des consommables s'ouvre et se ferme en un seul mouvement, comme la carte d'origine
- [x] D2 : en français, la pastille de basale temp. dit « Temp. » au lieu de « TBR »
- [x] D3 : Medtrum, le dernier bolus lu à la connexion ne change plus la progression d'un nouveau bolus (voir `UPSTREAM_TOUCHES.md`)
- [x] Audit 3 : plus de caractère parasite après le tiret du créneau Cible

## Idées pour plus tard
- Demander le mode daltonien à AAPS par **issues, pas par PR** : AAPS refuse les PR faites avec l'IA (`CONTRIBUTING.md`, « AI-generated contributions »). Deux issues séparées : le réglage à 3 choix, puis les couleurs « très bas / très haut ». Y mettre l'analyse et les captures comme contexte, et laisser le code aux mainteneurs. Vérifier master d'abord
- Cadran Garmin (nécessite un autre langage de programmation c'est un projet à part). Point de départ : [buessow/garmin](https://github.com/buessow/garmin) (« AAPS Glucose Applications », source du cadran AAPS Glucose Watch, données lues par le plugin Garmin d'AAPS). À vérifier d'abord : la licence et si ma montre est prise en charge

## Existe déjà 
- AOD and android auto: GlucoDataHandler
