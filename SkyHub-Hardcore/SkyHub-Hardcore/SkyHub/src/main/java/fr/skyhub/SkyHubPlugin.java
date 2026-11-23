package fr.skyhub;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import fr.skyhub.hardcore.DataManager;
import fr.skyhub.kit.KitCommand;
import fr.skyhub.kit.KitListener;
import fr.skyhub.kit.KitManager;
import fr.skyhub.claim.ClaimCommand;
import fr.skyhub.claim.ClaimListener;
import fr.skyhub.claim.ClaimManager;
import fr.skyhub.ffa.FfaModes;
import fr.skyhub.menu.MenuModule;
import fr.skyhub.npc.CustomNpcManager;
import fr.skyhub.npc.NpcCommand;
import fr.skyhub.hardcore.GuiListener;
import fr.skyhub.hardcore.HardcoreCommand;
import fr.skyhub.hardcore.PlayerListener;
import fr.skyhub.hardcore.UnbanCommand;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SkyHubPlugin extends JavaPlugin {

    private HubWorld hubWorld;
    private NpcManager npcManager;
    private WorldManager worldManager;
    private KitManager kits;
    private FfaModes ffa;
    private MenuModule menu;
    private ClaimManager claimManager;
    private CustomNpcManager customNpcs;
    private LocationStore locations;
    private DataManager hardcoreData;
    private final Set<UUID> awaiting = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        // Ajoute automatiquement les nouvelles options à un ancien config.yml
        getConfig().options().copyDefaults(true);
        saveConfig();

        locations = new LocationStore(this);

        hubWorld = new HubWorld(this);
        hubWorld.load();

        worldManager = new WorldManager(this, hubWorld);
        worldManager.loadSaved();

        npcManager = new NpcManager(this, hubWorld);
        npcManager.spawnAll();

        var pm = getServer().getPluginManager();
        pm.registerEvents(new JoinListener(this, hubWorld), this);
        pm.registerEvents(new LobbyProtectionListener(this, hubWorld), this);
        pm.registerEvents(new NpcListener(npcManager), this);
        pm.registerEvents(new InventoryManager(this), this);
        pm.registerEvents(new RespawnListener(this), this);

        // Gamerules indépendantes par monde
        WorldGameRules gameRules = new WorldGameRules(this);
        pm.registerEvents(gameRules, this);
        gameRules.applyAll();

        // Kits FFA
        ffa = new FfaModes(this);
        pm.registerEvents(ffa, this);
        kits = new KitManager(this);
        pm.registerEvents(new KitListener(this, kits), this);
        KitCommand kitCommand = new KitCommand(this, kits);
        getCommand("kit").setExecutor(kitCommand);
        getCommand("kit").setTabCompleter(kitCommand);

        // Zones protégées (/claim)
        claimManager = new ClaimManager(this);
        claimManager.start();
        pm.registerEvents(new ClaimListener(claimManager), this);
        ClaimCommand claimCommand = new ClaimCommand(claimManager);
        getCommand("claim").setExecutor(claimCommand);
        getCommand("claim").setTabCompleter(claimCommand);

        // PNJ personnalisés (/npc)
        customNpcs = new CustomNpcManager(this);
        pm.registerEvents(customNpcs, this);
        NpcCommand npcCommand = new NpcCommand(customNpcs);
        getCommand("npc").setExecutor(npcCommand);
        getCommand("npc").setTabCompleter(npcCommand);
        customNpcs.start();

        // Menu de modération (/menu) : réglages séparés par monde
        menu = new MenuModule(this);
        menu.enable();

        HubCommand command = new HubCommand(this, hubWorld, npcManager, worldManager);
        getCommand("hub").setExecutor(command);
        getCommand("l").setExecutor(command);
        getCommand("l").setTabCompleter(command);

        // ---- Module Hardcore (actif uniquement dans le monde hardcore) ----
        hardcoreData = new DataManager(this);
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new GuiListener(this), this);
        UnbanCommand unban = new UnbanCommand(this);
        getCommand("unban").setExecutor(unban);
        getCommand("unban").setTabCompleter(unban);
        getCommand("hardcore").setExecutor(new HardcoreCommand(this));

        getLogger().info("SkyHub v" + getPluginMeta().getVersion() + " activé.");
    }

    @Override
    public void onDisable() {
        if (hardcoreData != null) hardcoreData.save();
        if (menu != null) menu.disable();
        if (claimManager != null) claimManager.shutdown();
        if (customNpcs != null) customNpcs.save();
    }

    public KitManager kits() {
        return kits;
    }

    public FfaModes ffa() {
        return ffa;
    }

    public HubWorld hub() {
        return hubWorld;
    }

    public DataManager data() {
        return hardcoreData;
    }

    public Set<UUID> awaiting() {
        return awaiting;
    }

    /** Vrai si le monde est le monde hardcore (ou son nether / end : world, world_nether, world_the_end). */
    public boolean isHardcoreWorld(World w) {
        if (w == null) return false;
        String base = getConfig().getString("hardcore.world", "world").toLowerCase(Locale.ROOT);
        String n = w.getName().toLowerCase(Locale.ROOT);
        return n.equals(base) || n.equals(base + "_nether") || n.equals(base + "_the_end");
    }

    public NpcManager npcManager() {
        return npcManager;
    }

    public LocationStore locations() {
        return locations;
    }

    /** Récupère un message de la config (MiniMessage) avec des remplacements clé/valeur. */
    public Component message(String key, String... replacements) {
        String raw = getConfig().getString("messages." + key, "<red>Message manquant : " + key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            raw = raw.replace(replacements[i], replacements[i + 1]);
        }
        return MiniMessage.miniMessage().deserialize(raw);
    }
}
