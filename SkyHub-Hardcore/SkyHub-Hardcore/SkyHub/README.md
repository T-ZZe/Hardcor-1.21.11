# SkyHub

Plugin **Paper 1.21.11** (Java 21) : à chaque connexion, le joueur arrive dans un **monde void**, sur une **île flottante enfermée dans une sphère noire**. Trois mobs permettent de rejoindre les différents modes :

| Mob | Nom | Destination par défaut |
|-----|-----|------------------------|
| Wither Squelette (sans épée) | **Hardcore** | monde normal (`world`) |
| Lama | **Event** | monde `event` |
| Breeze | **FFA** | monde `ffa` |

Clic droit sur un mob = téléportation.

## Commandes
| Commande | Description | Permission |
|----------|-------------|------------|
| `/hub`, `/lobby`, `/l` | Retour au hub | tout le monde |
| `/l sp hardcore` / `event` / `ffa` | Pose le mob à ta position (il regarde vers le spawn). **L'ancien est supprimé.** | `skyhub.admin` |
| `/l reload` | Recharge la config et recrée les mobs | `skyhub.admin` |

## Permissions
- `skyhub.build` : casser / poser des blocs dans le hub (**modos uniquement**, op par défaut).
- `skyhub.admin` : `/l sp` et `/l reload`.

Exemple avec LuckPerms : `/lp user <pseudo> permission set skyhub.build true`

## Installation
1. `mvn package` (ou jar depuis l'onglet *Actions* de GitHub).
2. Dépose le jar dans `plugins/`.
3. Crée/importe les mondes `event` et `ffa` (ex. Multiverse), ou change les noms dans `config.yml`.
4. Au premier lancement, le monde `hub`, l'île et la sphère sont générés.

## Configuration
`plugins/SkyHub/config.yml` : monde, île, sphère (rayon, épaisseur, matériau, lumière), type de mob (`entity`), nom, destination et position de chaque mob, messages (MiniMessage).

## Position sauvegardée
Quand un joueur quitte un monde (Hardcore, Event, FFA) pour le hub (`/hub`, `/l`, ou connexion au serveur), sa position est enregistrée dans `plugins/SkyHub/locations.yml`. En revenant par le mob, il est remis au même endroit. Si le joueur meurt, sa position dans ce monde est oubliée. Désactivable avec `lobby.save-positions: false`.

## Notes
- Pour régénérer le hub, arrête le serveur et supprime le dossier du monde `hub`.
- Les inventaires sont partagés entre les mondes (utilise Multiverse-Inventories pour les séparer).
- Pour un vrai mode Hardcore, le monde de destination doit lui-même être en hardcore.

## Module Hardcore (intégré)
Tout est actif **uniquement dans le monde hardcore** (`hardcore.world` dans `config.yml`, `world_nether` / `world_the_end` inclus).

- Mourir dans le monde hardcore = **banni du monde hardcore** pendant 5 jours réels (`hardcore.ban-hours`). Tu n'es PAS banni du serveur : tu réapparais au hub avec le message de ban et tu peux jouer à Event / FFA. Si tu essaies de rejoindre le hardcore (mob, téléportation, connexion), tu es refusé / renvoyé au hub avec le message. Mourir ailleurs ne bannit pas.
- Seule la blacklist (ban définitif) bloque la connexion au serveur.
- Les sanctions (PV, taille, dégâts, reach) ne sont actives que dans le monde hardcore : elles sont retirées au retour au hub / dans un autre monde et réappliquées à l'entrée.
- `/hardcore` (alias `/hardcor`) : menu admin (durée, sanctions, blacklist) — utilisable depuis n'importe quel monde.
- `/unban <pseudo>` lève le ban (et retire de la blacklist) ; `/unban all` lève tous les bans temporaires.
- Permissions : `hardcore.admin` (op), `hardcore.bypass` (pas de ban à la mort).
- Données : `plugins/SkyHub/hardcore-data.yml`.

## Ouvrir / fermer des mondes
| Commande | Effet |
|----------|-------|
| `/l open event` / `/l open ffa` | Crée (ou recharge) le monde **flat** du mode (nom = `npcs.<mode>.world`) |
| `/l open <nom>` | Pareil avec n'importe quel nom de monde (lettres, chiffres, `_`, `-`) |
| `/l close <event\|ffa\|nom>` | Renvoie les joueurs au hub puis décharge le monde |
| `/l close` ou `/l close all` | Ferme tous les mondes ouverts avec `/l open` |

- Le hub, le monde principal et le monde hardcore ne peuvent jamais être fermés.
- Tant qu'un monde est fermé, cliquer sur son mob affiche « fermé ».
- Les mondes ouverts sont rouverts au redémarrage (`worlds.yml`). Fermer garde le monde sur le disque : le rouvrir retrouve les constructions. Pour repartir de zéro, supprime son dossier serveur éteint.

## Mobs du hub
Chaque mob est agrandi (`scale`), brille dans sa couleur (`glow-color`) et a un gros titre flottant (`title`, `title-scale`) dans `config.yml`.

## Inventaires séparés
Chaque monde garde son équipement et son XP (`inventories.separate` dans `config.yml`). `world`, `world_nether` et `world_the_end` partagent le même inventaire. Les données sont dans `plugins/SkyHub/inventories/<uuid>.yml`. Un monde jamais visité commence avec un inventaire vide.

## Gamerules par monde
`/gamerule <règle> <valeur> [monde]` ne change que le monde où tu te trouves (ou celui indiqué en dernier argument). Les autres mondes gardent leurs valeurs, même après un redémarrage (`gamerules.yml`). Valable pour tous les mondes (event, ffa, hub...). Désactivable avec `gamerules.per-world: false`. Seule la commande tapée par un joueur est gérée (pas la console).

## Kits FFA
| Commande | Qui | Effet |
|----------|-----|-------|
| `/kit create` | op (`skyhub.admin`) | Ouvre la liste des items disponibles : ajoute / retire des items puis ferme pour sauvegarder (54 emplacements) |
| `/kit` | joueurs, dans le FFA | Ouvre la liste : clic = prendre une copie de l'item (shift+clic = direct dans l'inventaire). Pose-le où tu veux dans ton inventaire / armure |
| `/kit set 1\|2\|3` | joueurs, dans le FFA | Sauvegarde ton inventaire actuel comme kit 1, 2 ou 3 (seuls les items de la liste sont acceptés) |
| `/kit 1\|2\|3` | joueurs, dans le FFA | Équipe ton kit sauvegardé, exactement comme tu l'as rangé |

Au respawn dans le FFA, le dernier kit utilisé est redonné (`kits.give-on-respawn`). Données : `plugins/SkyHub/kits.yml`.
