package fr.skyhub.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EnchantCommand implements CommandExecutor, TabCompleter {

    private final GuiManager gui;
    private final LimitManager limits;

    public EnchantCommand(GuiManager gui, LimitManager limits) {
        this.gui = gui;
        this.limits = limits;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(Component.text("Commande reservee aux joueurs.", NamedTextColor.RED));
            return true;
        }
        if (!p.hasPermission("enchantlimit.admin")) {
            p.sendMessage(Component.text("Tu n'as pas la permission.", NamedTextColor.RED));
            return true;
        }
        // /menu = réglages du monde où tu es ; /menu <monde> = réglages d'un autre monde
        if (args.length >= 1) gui.setEditing(p, args[0]);
        else gui.resetEditing(p);
        gui.openMain(p);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, @NotNull String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            for (String k : limits.knownKeys()) if (k.startsWith(a[0].toLowerCase(Locale.ROOT))) out.add(k);
        }
        return out;
    }
}
