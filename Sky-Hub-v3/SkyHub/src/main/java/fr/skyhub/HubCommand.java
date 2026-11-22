package fr.skyhub;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /hub (/lobby) : retour au hub.
 * /l            : retour au hub.
 * /l sp [nom]   : (admin) pose le mob à ta position, l'ancien est supprimé.
 * /l reload     : (admin) recharge la config.
 */
public final class HubCommand implements CommandExecutor, TabCompleter {

    private final SkyHubPlugin plugin;
    private final HubWorld hub;
    private final NpcManager npcs;

    public HubCommand(SkyHubPlugin plugin, HubWorld hub, NpcManager npcs) {
        this.plugin = plugin;
        this.hub = hub;
        this.npcs = npcs;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        // /hub ou /l sans argument
        if (command.getName().equalsIgnoreCase("hub") || args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.message("players-only"));
                return true;
            }
            hub.sendToHub(player);
            player.sendMessage(plugin.message("hub-teleport"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "sp", "spawn" -> {
                if (!sender.hasPermission("skyhub.admin")) {
                    sender.sendMessage(plugin.message("no-permission"));
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(plugin.message("players-only"));
                    return true;
                }
                String ids = String.join(", ", npcs.ids());
                if (args.length < 2) {
                    player.sendMessage(plugin.message("usage-sp", "%ids%", ids));
                    return true;
                }
                String id = npcs.matchId(args[1]);
                if (id == null) {
                    player.sendMessage(plugin.message("npc-unknown", "%ids%", ids));
                    return true;
                }
                if (!hub.isHub(player.getWorld())) {
                    player.sendMessage(plugin.message("not-in-hub"));
                    return true;
                }
                npcs.setLocation(id, player.getLocation());
                player.sendMessage(plugin.message("npc-placed", "%name%", npcs.displayName(id)));
            }
            case "reload" -> {
                if (!sender.hasPermission("skyhub.admin")) {
                    sender.sendMessage(plugin.message("no-permission"));
                    return true;
                }
                plugin.reloadConfig();
                npcs.spawnAll();
                sender.sendMessage(plugin.message("reloaded"));
            }
            default -> sender.sendMessage(plugin.message("usage-sp", "%ids%", String.join(", ", npcs.ids())));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (!command.getName().equalsIgnoreCase("l") || !sender.hasPermission("skyhub.admin")) {
            return out;
        }
        if (args.length == 1) {
            for (String s : List.of("sp", "reload")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("sp") || args[0].equalsIgnoreCase("spawn"))) {
            for (String id : npcs.ids()) {
                if (id.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(id);
            }
        }
        return out;
    }
}
