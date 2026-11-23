package fr.skyhub.kit;

import fr.skyhub.SkyHubPlugin;
import fr.skyhub.ffa.FfaMode;
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

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] a) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Commande réservée aux joueurs.");
            return true;
        }

        // ---- /kit create <diamond|mace|crystal> : les op règlent les items d'UNE zone ----
        if (a.length >= 1 && a[0].equalsIgnoreCase("create")) {
            if (!p.hasPermission("skyhub.admin")) {
                msg(p, "<red>Permission refusée.");
                return true;
            }
            FfaMode m = a.length >= 2 ? FfaMode.parse(a[1]) : null;
            if (m == null) {
                msg(p, "<yellow>Usage : /kit create <diamond|mace|crystal>");
                return true;
            }
            KitHolder h = new KitHolder(true, m);
            Inventory inv = Bukkit.createInventory(h, KitManager.POOL_SIZE,
                    KitListener.c("<dark_red>Kit FFA " + m.label + " (admin)"));
            h.inv = inv;
            inv.setContents(kits.getPool(m));
            p.openInventory(inv);
            msg(p, "<gray>Zone " + m.colored() + "<gray> : mets / retire les items puis ferme l'inventaire pour sauvegarder.");
            return true;
        }

        if (!kits.isFfa(p.getWorld())) {
            msg(p, "<red>Les kits ne sont disponibles que dans le FFA.");
            return true;
        }
        FfaMode mode = kits.getMode(p.getUniqueId());
        if (mode == null) {
            msg(p, "<red>Choisis d'abord une zone avec le mob FFA du hub.");
            return true;
        }

        // ---- /kit : liste des items de ta zone ----
        if (a.length == 0) {
            KitHolder h = new KitHolder(false, mode);
            Inventory inv = Bukkit.createInventory(h, KitManager.POOL_SIZE,
                    KitListener.c("<gold>Kit FFA " + mode.label + " - items"));
            h.inv = inv;
            inv.setContents(kits.getPool(mode));
            p.openInventory(inv);
            msg(p, "<gray>Zone " + mode.colored() + "<gray> : clique sur un item pour le prendre, place-le où tu veux dans ton"
                    + " inventaire, puis <yellow>/kit set</yellow> (1 seul kit par arène).");
            return true;
        }

        // ---- /kit set : sauvegarde (ou remplace) ton UNIQUE kit de la zone ----
        if (a[0].equalsIgnoreCase("set")) {
            if (a.length >= 2 && !a[1].equals("1")) {
                msg(p, "<red>Tu n'as droit qu'à <white>1 kit par arène</white>. <gray>Utilise <yellow>/kit set</yellow>.");
                return true;
            }
            ItemStack[] pool = kits.getPool(mode);
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
                msg(p, "<red>" + bad + " item(s) ne sont pas dans le kit de la zone " + mode.label
                        + ". <gray>Retire-les puis refais /kit set.");
                return true;
            }
            boolean had = kits.hasKit(p.getUniqueId(), mode);
            kits.saveKit(p.getUniqueId(), mode, 1, contents);
            msg(p, "<green>Ton kit " + mode.label + (had ? " a été mis à jour" : " est sauvegardé")
                    + " ! <gray>Récupère-le avec <yellow>/kit load</yellow>.");
            return true;
        }

        // ---- /kit load (ou /kit 1) : équipe ton kit de la zone ----
        if (a[0].equalsIgnoreCase("load") || a[0].equals("1")) {
            if (!kits.equip(p, mode, 1)) {
                msg(p, "<red>Tu n'as pas encore de kit pour la zone " + mode.label
                        + ". <gray>Prends des items avec /kit puis /kit set.");
                return true;
            }
            msg(p, "<green>Kit " + mode.label + " équipé !");
            return true;
        }
        if (a[0].equals("2") || a[0].equals("3")) {
            msg(p, "<red>Tu n'as droit qu'à <white>1 kit par arène</white>.");
            return true;
        }
        msg(p, "<yellow>/kit <gray>(items)</gray> | /kit set | /kit load");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            out.addAll(List.of("set", "load"));
            if (s.hasPermission("skyhub.admin")) out.add("create");
            out.removeIf(x -> !x.startsWith(a[0].toLowerCase()));
        } else if (a.length == 2 && a[0].equalsIgnoreCase("create") && s.hasPermission("skyhub.admin")) {
            for (FfaMode m : FfaMode.values()) if (m.key.startsWith(a[1].toLowerCase())) out.add(m.key);
        }
        return out;
    }
}
