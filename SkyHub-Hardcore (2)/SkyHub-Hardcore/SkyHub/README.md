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

## FFA : 5 zones (Diamond Spear / Mace / Crystal / Cart PvP / Pot)
Cliquer sur le mob FFA ouvre un menu avec les 5 zones. Elles sont sur **la même map** mais chacune a son **spawn** (il n'y a plus de kits) :
- `/l ffa spawn <diamond|mace|crystal|cart|pot>` (admin, depuis le monde FFA) : définit le spawn de la zone là où tu es. Sans spawn défini, la zone utilise le spawn du monde.
- À la mort dans le FFA, tu réapparais au spawn de ta zone.

## Menu de modération `/menu` (ex-EnchantLimit, intégré)
`/menu` (alias `/enchantement`) : limites d'enchantements, pièces d'armure, limites d'objets, cooldowns, PvP / combat tag, équilibrage, PvP / Nether / End. Permission `enchantlimit.admin` (op), `enchantlimit.bypass` pour ignorer les règles.

**Actif uniquement dans le monde Hardcore** (`hardcore.world`, avec son Nether et son End). Partout ailleurs (hub, Event, FFA...), aucune limite, aucun cooldown et aucun combat tag ne s'applique.
- `/menu` édite toujours les réglages du monde Hardcore, d'où que tu sois.
- Quitter le monde Hardcore met fin au combat tag.
- Données : `plugins/SkyHub/limits.yml` (les réglages Hardcore existants sont conservés). Le plugin désactive l'ancien CombatTagPlugin s'il est présent.
- Retire l'ancien `Menu.jar` / `EnchantLimit.jar` des plugins : tout est dans SkyHub.

## Réapparition
Quand tu meurs, tu réapparais dans le monde où tu es mort (FFA, Event...) et pas dans le monde principal. Le nether / l'end d'un monde gardent le comportement normal (retour au monde de base).

## Zones protégées `/claim` (OP uniquement, sélection en créatif)
1. `/claim wand` (créatif + OP) : clic gauche = position 1, clic droit = position 2 (ou `/claim pos1` / `pos2` là où tu es).
2. `/claim create <nom>` crée la zone. Par défaut tout y est **interdit** pour les joueurs.
3. `/claim flag <nom> <place|break|windcharge|pvp> <allow|deny>` :
   - `place` : poser des blocs ; `windcharge` : utiliser des wind charges ; `pvp` : dégâts entre joueurs (autorisé par défaut) ;
   - `break` : casser n'importe quel bloc. Si c'est interdit, un joueur peut quand même casser **les blocs qu'il a posés**. Les blocs d'origine résistent aussi aux explosions.
4. `/claim save <nom>` enregistre les blocs de la zone (c'est l'état de référence).
5. `/claim restore <nom> <durée|off>` : la zone est reconstruite toute seule à cet intervalle (ex : `10m`).
6. `/claim expire <nom> <durée|off>` : chaque bloc posé par un joueur disparaît après ce délai (ex : `30s`) et le bloc d'origine revient.
7. `/claim rebuild <nom>` reconstruit tout de suite ; `/claim list`, `/claim info [nom]`, `/claim delete <nom>`.

- Un OP en créatif ignore les règles (pour construire l'arène sans que ses blocs disparaissent).
- Seuls les types de blocs sont sauvegardés : le contenu des coffres, panneaux, etc. n'est pas restauré.
- Sauvegarde et reconstruction se font par paquets (`claims.blocks-per-tick`) pour ne pas lagger. Zone max : `claims.max-volume`.
- Données : `plugins/SkyHub/claims.yml` et `plugins/SkyHub/claims/`.

## Hardcore : couleur et première arrivée
- Le mob et le titre HARDCORE sont en **dark red** (`glow-color: dark_red` dans `config.yml`).
- La **première fois** qu'un joueur entre dans le monde hardcore (via le mob), il apparaît à un endroit sûr tiré au hasard dans un rayon de **250 blocs** autour du spawn du monde (`hardcore.spawn-radius`). Ensuite il retrouve sa dernière position comme d'habitude.

## PNJ `/npc` (OP)
`/npc <mob> <interface|interaction> <nom> [couleur]` crée un PNJ là où tu es (mob au choix : villager, zombie, breeze...). La couleur du nom est un nom (`red`, `dark_red`, `gold`...) ou un code `#rrggbb` ; le `_` du nom devient un espace.
- **interface** : un clic ouvre une interface de **trade**. Ajoute des échanges avec `/npc trade <nom> add` : l'objet vendu dans la main principale, le prix dans la main secondaire. `/npc trade <nom> clear` les efface (12 max).
- **interaction** : un clic donne un objet ou téléporte.
  - `/npc item <nom> [once]` : l'objet tenu en main est donné à chaque clic (`once` = une seule fois par joueur) ;
  - `/npc teleport <nom>` : un clic téléporte à l'endroit où tu te trouves.
- `/npc mod ffa` pose le **Breeze « Changer de PvP »** : un clic ouvre le choix de la zone (Diamond Spear, Mace, Crystal, Cart PvP, Pot) sans repasser par le hub.
- `/npc move <nom>`, `/npc remove <nom>`, `/npc list`.
- Les PNJ sont invincibles, immobiles et sauvegardés dans `plugins/SkyHub/custom-npcs.yml`.
