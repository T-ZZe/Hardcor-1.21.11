package fr.skyhub.kit;

import fr.skyhub.SkyHubPlugin;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class KitCommand implements CommandExecutor, TabCompleter {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final SkyHubPlugin plugin;
    private final KitManager kits;

    public KitCommand(SkyHubPlugin plugin, KitManager kits) {
        this.plugin = plugin;
        this.kits = kits;
    }

    private static void msg(Player p, String s) {
        p.sendMessage(MM.deserialize(s));
    }

    private static int slotNumber(String s) {
        try {
            int n = Integer.parseInt(s);
            return n >= 1 && n <= 3 ? n : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        // ---- /kit create : les op ajoutent / retirent des items disponibles ----
        if (a.length >= 1 && a[0].equalsIgnoreCase("create")) {
            if (!p.hasPermission("skyhub.admin")) {
                msg(p, "<red>Permission refusée.");
                return true;
            }
            KitHolder h = new KitHolder(true);
            Inventory inv = Bukkit.createInventory(h, KitManager.POOL_SIZE, KitListener.c("<dark_red>Kit FFA - édition (admin)"));
            h.inv = inv;
            inv.setContents(kits.getPool());
            p.openInventory(inv);
            msg(p, "<gray>Mets / retire les items puis ferme l'inventaire pour sauvegarder.");
            return true;
        }

        if (!kits.isFfa(p.getWorld())) {
            msg(p, "<red>Les kits ne sont disponibles que dans le FFA.");
            return true;
        }

        // ---- /kit : liste des items disponibles ----
        if (a.length == 0) {
            KitHolder h = new KitHolder(false);
            Inventory inv = Bukkit.createInventory(h, KitManager.POOL_SIZE, KitListener.c("<gold>Kit FFA - items disponibles"));
            h.inv = inv;
            inv.setContents(kits.getPool());
            p.openInventory(inv);
            msg(p, "<gray>Clique sur un item pour le prendre, place-le où tu veux dans ton inventaire,"
                    + " puis <yellow>/kit set 1|2|3</yellow> pour sauvegarder.");
            return true;
        }

        // ---- /kit set <1|2|3> : sauvegarde l'inventaire actuel ----
        if (a[0].equalsIgnoreCase("set")) {
            int n = a.length >= 2 ? slotNumber(a[1]) : -1;
            if (n < 0) {
                msg(p, "<yellow>Usage : /kit set <1|2|3>");
                return true;
            }
            ItemStack[] pool = kits.getPool();
            ItemStack[] contents = p.getInventory().getContents();
            int count = 0;
            int bad = 0;
            for (ItemStack it : contents) {
                if (it == null || it.getType().isAir()) continue;
                count++;
                if (!kits.allowed(it, pool)) bad++;
            }
            if (count == 0) {
                msg(p, "<red>Ton inventaire est vide : prends des items avec /kit d'abord.");
                return true;
            }
            if (bad > 0) {
                msg(p, "<red>" + bad + " item(s) de ton inventaire ne sont pas dans la liste du kit."
                        + " <gray>Retire-les puis refais /kit set " + n + ".");
                return true;
            }
            kits.saveKit(p.getUniqueId(), n, contents);
            msg(p, "<green>Kit " + n + " sauvegardé ! <gray>Utilise <yellow>/kit " + n + "</yellow> pour le récupérer.");
            return true;
        }

        // ---- /kit <1|2|3> : équipe le kit ----
        int n = slotNumber(a[0]);
        if (n < 0) {
            msg(p, "<yellow>/kit <1|2|3> | /kit set <1|2|3> | /kit");
            return true;
        }
        if (!kits.equip(p, n)) {
            msg(p, "<red>Tu n'as pas encore de kit " + n + ". <gray>Prends des items avec /kit puis /kit set " + n + ".");
            return true;
        }
        msg(p, "<green>Kit " + n + " équipé !");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            out.addAll(List.of("1", "2", "3", "set"));
            if (s.hasPermission("skyhub.admin")) out.add("create");
            out.removeIf(x -> !x.startsWith(a[0].toLowerCase()));
        } else if (a.length == 2 && a[0].equalsIgnoreCase("set")) {
            out.addAll(List.of("1", "2", "3"));
        }
        return out;
    }
}
