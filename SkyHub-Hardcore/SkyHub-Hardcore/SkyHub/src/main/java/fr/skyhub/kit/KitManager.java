package fr.skyhub.kit;

import fr.skyhub.SkyHubPlugin;
import fr.skyhub.ffa.FfaMode;
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

/**
 * Kits du FFA, séparés par zone (Diamond Spear / Mace / Crystal) :
 * chaque zone a sa propre liste d'items et chaque joueur a ses kits 1/2/3 pour chaque zone.
 */
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
        return w != null && w.getName().equalsIgnoreCase(plugin.ffa().ffaWorldName());
    }

    // ---------- zone actuelle du joueur ----------
    public FfaMode getMode(UUID id) {
        return FfaMode.parse(cfg.getString("kitdata." + id + ".mode"));
    }

    public void setMode(UUID id, FfaMode m) {
        cfg.set("kitdata." + id + ".mode", m.key);
        save();
    }

    // ---------- items disponibles (par zone) ----------
    public ItemStack[] getPool(FfaMode m) {
        return decAll(cfg.getStringList("pools." + m.key), POOL_SIZE);
    }

    public void savePool(FfaMode m, ItemStack[] contents) {
        cfg.set("pools." + m.key, encAll(contents));
        save();
    }

    private static ItemStack norm(ItemStack it) {
        ItemStack c = it.clone();
        c.setAmount(1);
        ItemMeta meta = c.getItemMeta();
        if (meta instanceof Damageable d && d.hasDamage()) {
            d.setDamage(0);
            c.setItemMeta(meta);
        }
        return c;
    }

    /** Un item est autorisé s'il est dans la liste de CETTE zone (même quantité max, usure ignorée). */
    public boolean allowed(ItemStack it, ItemStack[] pool) {
        ItemStack n = norm(it);
        for (ItemStack p : pool) {
            if (p == null || p.getType().isAir()) continue;
            if (p.getAmount() >= it.getAmount() && norm(p).isSimilar(n)) return true;
        }
        return false;
    }

    // ---------- kits des joueurs (par zone) ----------
    private String base(UUID id, FfaMode m) {
        return "kitdata." + id + "." + m.key;
    }

    public void saveKit(UUID id, FfaMode m, int n, ItemStack[] contents) {
        cfg.set(base(id, m) + ".kit" + n, encAll(contents));
        cfg.set(base(id, m) + ".last", n);
        save();
    }

    public ItemStack[] getKit(UUID id, FfaMode m, int n) {
        List<String> l = cfg.getStringList(base(id, m) + ".kit" + n);
        return l.isEmpty() ? null : decAll(l, 41);
    }

    public int getLast(UUID id, FfaMode m) {
        return cfg.getInt(base(id, m) + ".last", 0);
    }

    /** Donne le kit n de la zone au joueur (remplace son inventaire). */
    public boolean equip(Player p, FfaMode m, int n) {
        ItemStack[] kit = getKit(p.getUniqueId(), m, n);
        if (kit == null) return false;
        PlayerInventory inv = p.getInventory();
        ItemStack[] c = new ItemStack[inv.getSize()];
        System.arraycopy(kit, 0, c, 0, Math.min(kit.length, c.length));
        inv.setContents(c);
        cfg.set(base(p.getUniqueId(), m) + ".last", n);
        save();
        p.updateInventory();
        return true;
    }

    public boolean hasKit(UUID id, FfaMode m) {
        return getKit(id, m, 1) != null;
    }

    /** Kit de la zone (un seul par arène), sinon inventaire vidé : on ne garde jamais les items d'une autre zone. */
    public void equipLast(Player p, FfaMode m) {
        var mm = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
        if (equip(p, m, 1)) {
            p.sendMessage(mm.deserialize("<gray>Ton kit " + m.label + " est équipé."));
            return;
        }
        p.getInventory().clear();
        p.sendMessage(mm.deserialize("<gray>Prends tes items avec <yellow>/kit</yellow>, puis <yellow>/kit set</yellow> (1 kit par arène)."));
    }
}
