package fr.skyhub.kit;

import fr.skyhub.SkyHubPlugin;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/** Stockage des kits FFA : la liste d'items disponibles (pool) et les kits 1/2/3 de chaque joueur. */
public final class KitManager {
    public static final int POOL_SIZE = 54;

    private final SkyHubPlugin plugin;
    private final File file;
    private final YamlConfiguration cfg;

    public KitManager(SkyHubPlugin plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "kits.yml");
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    private synchronized void save() {
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Impossible d'écrire kits.yml : " + e.getMessage());
        }
    }

    // ---------- (dé)sérialisation ----------
    private static String enc(ItemStack it) {
        return it == null || it.getType().isAir() ? "" : Base64.getEncoder().encodeToString(it.serializeAsBytes());
    }

    private static ItemStack dec(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(s));
        } catch (Exception e) {
            return null;
        }
    }

    private static List<String> encAll(ItemStack[] a) {
        List<String> l = new ArrayList<>();
        for (ItemStack it : a) l.add(enc(it));
        return l;
    }

    private static ItemStack[] decAll(List<String> l, int size) {
        ItemStack[] r = new ItemStack[size];
        for (int i = 0; i < l.size() && i < size; i++) r[i] = dec(l.get(i));
        return r;
    }

    // ---------- monde FFA ----------
    public boolean isFfa(World w) {
        String n = plugin.getConfig().getString("kits.world", "");
        if (n == null || n.isEmpty()) n = plugin.getConfig().getString("npcs.ffa.world", "ffa");
        return w.getName().equalsIgnoreCase(n);
    }

    // ---------- items disponibles ----------
    public ItemStack[] getPool() {
        return decAll(cfg.getStringList("pool"), POOL_SIZE);
    }

    public void savePool(ItemStack[] contents) {
        cfg.set("pool", encAll(contents));
        save();
    }

    private static ItemStack norm(ItemStack it) {
        ItemStack c = it.clone();
        c.setAmount(1);
        ItemMeta m = c.getItemMeta();
        if (m instanceof Damageable d && d.hasDamage()) {
            d.setDamage(0);
            c.setItemMeta(m);
        }
        return c;
    }

    /** Un item est autorisé s'il est dans la liste de l'admin (même quantité max, usure ignorée). */
    public boolean allowed(ItemStack it, ItemStack[] pool) {
        ItemStack n = norm(it);
        for (ItemStack p : pool) {
            if (p == null || p.getType().isAir()) continue;
            if (p.getAmount() >= it.getAmount() && norm(p).isSimilar(n)) return true;
        }
        return false;
    }

    // ---------- kits des joueurs ----------
    public void saveKit(UUID id, int n, ItemStack[] contents) {
        cfg.set("players." + id + ".kit" + n, encAll(contents));
        cfg.set("players." + id + ".last", n);
        save();
    }

    public ItemStack[] getKit(UUID id, int n) {
        List<String> l = cfg.getStringList("players." + id + ".kit" + n);
        return l.isEmpty() ? null : decAll(l, 41);
    }

    public int getLast(UUID id) {
        return cfg.getInt("players." + id + ".last", 0);
    }

    public void setLast(UUID id, int n) {
        cfg.set("players." + id + ".last", n);
        save();
    }

    /** Donne le kit n au joueur (remplace son inventaire). */
    public boolean equip(Player p, int n) {
        ItemStack[] kit = getKit(p.getUniqueId(), n);
        if (kit == null) return false;
        PlayerInventory inv = p.getInventory();
        ItemStack[] c = new ItemStack[inv.getSize()];
        System.arraycopy(kit, 0, c, 0, Math.min(kit.length, c.length));
        inv.setContents(c);
        setLast(p.getUniqueId(), n);
        p.updateInventory();
        return true;
    }
}
