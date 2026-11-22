package fr.skyhub;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Crée les mobs de téléportation (Wither Squelette, Lama, Breeze...) et gère les téléportations. */
public final class NpcManager {

    private final SkyHubPlugin plugin;
    private final HubWorld hub;
    private final NamespacedKey npcKey;
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public NpcManager(SkyHubPlugin plugin, HubWorld hub) {
        this.plugin = plugin;
        this.hub = hub;
        this.npcKey = new NamespacedKey(plugin, "npc_id");
    }

    // ------------------------------------------------------------------
    //  Infos
    // ------------------------------------------------------------------

    public Set<String> ids() {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("npcs");
        return s == null ? Set.of() : s.getKeys(false);
    }

    /** Retrouve l'id (sans tenir compte de la casse), ou null. */
    public String matchId(String input) {
        for (String id : ids()) {
            if (id.equalsIgnoreCase(input)) return id;
        }
        return null;
    }

    public String displayName(String id) {
        return plugin.getConfig().getString("npcs." + id + ".name", id);
    }

    /** Retourne l'id du mob si l'entité en est un, sinon null. */
    public String getNpcId(Entity entity) {
        return entity.getPersistentDataContainer().get(npcKey, PersistentDataType.STRING);
    }

    // ------------------------------------------------------------------
    //  Création / suppression
    // ------------------------------------------------------------------

    /** Supprime tous les anciens mobs et recrée ceux de la config. */
    public void spawnAll() {
        World world = hub.world();
        for (String id : ids()) loadChunk(id);

        for (Entity e : world.getEntities()) {
            if (getNpcId(e) != null) e.remove();
        }
        for (String id : ids()) spawn(id);
    }

    /** Recrée le mob "id" : l'ancien est supprimé. */
    public void spawn(String id) {
        World world = hub.world();
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("npcs." + id);
        if (s == null) return;

        loadChunk(id);
        for (Entity e : world.getEntities()) {
            if (id.equals(getNpcId(e))) e.remove();
        }

        Location loc = readLocation(world, s);
        EntityType type = Registry.ENTITY_TYPE.get(
                NamespacedKey.minecraft(s.getString("entity", "villager").toLowerCase(Locale.ROOT)));
        if (type == null) {
            plugin.getLogger().warning("Type de mob inconnu pour '" + id + "', villageois utilisé.");
            type = EntityType.VILLAGER;
        }

        Entity entity = world.spawnEntity(loc, type);
        if (!(entity instanceof LivingEntity mob)) {
            plugin.getLogger().warning("Le type de '" + id + "' n'est pas un mob vivant.");
            entity.remove();
            return;
        }

        mob.setAI(false);
        mob.setInvulnerable(true);
        mob.setSilent(true);
        mob.setPersistent(true);
        mob.setRemoveWhenFarAway(false);
        mob.setCollidable(false);
        mob.setCanPickupItems(false);
        mob.setRotation(loc.getYaw(), loc.getPitch());
        mob.customName(MiniMessage.miniMessage().deserialize(s.getString("name", id)));
        mob.setCustomNameVisible(true);
        mob.getPersistentDataContainer().set(npcKey, PersistentDataType.STRING, id);

        // Wither squelette sans épée, etc.
        if (s.getBoolean("clear-equipment", true)) {
            EntityEquipment eq = mob.getEquipment();
            if (eq != null) eq.clear();
        }
    }

    /** Enregistre la position du mob (face au spawn), supprime l'ancien et en pose un nouveau. */
    public void setLocation(String id, Location where) {
        Location spawn = hub.spawn();
        double dx = spawn.getX() - where.getX();
        double dz = spawn.getZ() - where.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

        String base = "npcs." + id + ".location.";
        plugin.getConfig().set(base + "x", where.getX());
        plugin.getConfig().set(base + "y", where.getY());
        plugin.getConfig().set(base + "z", where.getZ());
        plugin.getConfig().set(base + "yaw", yaw);
        plugin.getConfig().set(base + "pitch", 0.0);
        plugin.saveConfig();

        spawn(id);
    }

    private void loadChunk(String id) {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("npcs." + id);
        if (s == null) return;
        World world = hub.world();
        int cx = ((int) Math.floor(s.getDouble("location.x"))) >> 4;
        int cz = ((int) Math.floor(s.getDouble("location.z"))) >> 4;
        world.getChunkAt(cx, cz);
        world.addPluginChunkTicket(cx, cz, plugin);
    }

    // ------------------------------------------------------------------
    //  Téléportation
    // ------------------------------------------------------------------

    /** Téléporte le joueur vers le monde associé au mob. */
    public void teleport(Player player, String id) {
        long now = System.currentTimeMillis();
        Long last = cooldowns.get(player.getUniqueId());
        if (last != null && now - last < 1500) return;
        cooldowns.put(player.getUniqueId(), now);

        ConfigurationSection s = plugin.getConfig().getConfigurationSection("npcs." + id);
        if (s == null) return;

        String worldName = s.getString("world", "world");
        World target = resolveWorld(worldName);
        if (target == null) {
            player.sendMessage(plugin.message("world-missing", "%world%", worldName));
            return;
        }

        GameMode gm;
        try {
            gm = GameMode.valueOf(s.getString("gamemode", "SURVIVAL").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            gm = GameMode.SURVIVAL;
        }
        final GameMode gamemode = gm;

        player.sendMessage(plugin.message("teleporting", "%name%", displayName(id)));
        Location dest = target.getSpawnLocation().add(0.5, 0, 0.5);
        player.teleportAsync(dest).thenAccept(ok -> {
            if (ok) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.setGameMode(gamemode);
                    player.setFallDistance(0);
                });
            }
        });
    }

    private World resolveWorld(String name) {
        World w = Bukkit.getWorld(name);
        if (w != null) return w;
        File folder = new File(Bukkit.getWorldContainer(), name);
        if (folder.isDirectory()) {
            return new WorldCreator(name).createWorld();
        }
        return null;
    }

    private Location readLocation(World world, ConfigurationSection s) {
        return new Location(world,
                s.getDouble("location.x"),
                s.getDouble("location.y"),
                s.getDouble("location.z"),
                (float) s.getDouble("location.yaw"),
                (float) s.getDouble("location.pitch"));
    }
}
