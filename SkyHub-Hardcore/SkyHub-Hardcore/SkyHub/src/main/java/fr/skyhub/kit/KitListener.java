package fr.skyhub.kit;

import fr.skyhub.SkyHubPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public final class KitListener implements Listener {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final SkyHubPlugin plugin;
    private final KitManager kits;

    public KitListener(SkyHubPlugin plugin, KitManager kits) {
        this.plugin = plugin;
        this.kits = kits;
    }

    public static Component c(String s) {
        return MM.deserialize("<!italic>" + s);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof KitHolder h)) return;
        if (h.admin) return; // les op modifient librement la liste
        if (!(e.getWhoClicked() instanceof Player p)) return;

        if (e.getClickedInventory() == e.getView().getTopInventory()) {
            // Clic dans la liste : on prend une COPIE de l'item
            e.setCancelled(true);
            ItemStack cursor = e.getCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                p.setItemOnCursor(null); // reposer un item dans la liste = le jeter
                return;
            }
            ItemStack it = e.getCurrentItem();
            if (it == null || it.getType().isAir()) return;
            if (e.isShiftClick()) p.getInventory().addItem(it.clone());
            else p.setItemOnCursor(it.clone());
        } else if (e.isShiftClick() || e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            // pas de shift-clic de ton inventaire vers la liste
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof KitHolder h) || h.admin) return;
        int size = e.getView().getTopInventory().getSize();
        if (e.getRawSlots().stream().anyMatch(s -> s < size)) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof KitHolder h)) return;
        if (!(e.getPlayer() instanceof Player p)) return;
        if (h.admin) {
            kits.savePool(e.getInventory().getContents());
            p.sendMessage(MM.deserialize("<green>Liste des items du kit sauvegardée."));
        } else {
            p.setItemOnCursor(null); // pas de copie perdue sur le curseur
        }
    }

    /** Respawn dans le FFA : le dernier kit utilisé est redonné automatiquement. */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (!plugin.getConfig().getBoolean("kits.give-on-respawn", true)) return;
        Player p = e.getPlayer();
        if (!kits.isFfa(e.getRespawnLocation().getWorld())) return;
        int last = kits.getLast(p.getUniqueId());
        if (last < 1) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (p.isOnline() && kits.isFfa(p.getWorld()) && kits.equip(p, last)) {
                p.sendMessage(MM.deserialize("<gray>Kit " + last + " rééquipé."));
            }
        }, 2L);
    }
}
