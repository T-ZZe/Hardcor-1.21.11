package fr.skyhub.menu;

import fr.skyhub.InventoryManager;
import fr.skyhub.SkyHubPlugin;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Limites et réglages du menu, SÉPARÉS PAR MONDE.
 * world, world_nether et world_the_end partagent les mêmes réglages (clé = nom du monde de base).
 */
public class LimitManager {

    public static final int NO_PIECE_LIMIT = WorldLimits.NO_PIECE_LIMIT;

    private final SkyHubPlugin plugin;
    private final File file;
    private final Map<String, WorldLimits> worlds = new HashMap<>();

    public LimitManager(SkyHubPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "limits.yml");
    }

    public static String key(World w) {
        return InventoryManager.group(w);
    }

    public WorldLimits of(World w) {
        return of(key(w));
    }

    public WorldLimits of(String key) {
        String k = key.toLowerCase(Locale.ROOT);
        return worlds.computeIfAbsent(k, x -> new WorldLimits(this::save));
    }

    /** Mondes connus (chargés + ceux qui ont des réglages sauvegardés). */
    public Set<String> knownKeys() {
        Set<String> s = new TreeSet<>(worlds.keySet());
        for (World w : plugin.getServer().getWorlds()) s.add(key(w));
        return s;
    }

    public void load() {
        worlds.clear();
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = cfg.getConfigurationSection("worlds");
        if (sec == null) return;
        for (String k : sec.getKeys(false)) {
            WorldLimits wl = new WorldLimits(this::save);
            wl.load(sec.getConfigurationSection(k));
            worlds.put(k.toLowerCase(Locale.ROOT), wl);
        }
    }

    public synchronized void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (var e : worlds.entrySet()) {
            e.getValue().write(cfg.createSection("worlds." + e.getKey()));
        }
        try {
            plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Impossible de sauvegarder limits.yml : " + ex.getMessage());
        }
    }
}
