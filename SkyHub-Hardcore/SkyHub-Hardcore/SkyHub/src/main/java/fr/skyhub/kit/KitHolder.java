package fr.skyhub.kit;

import fr.skyhub.ffa.FfaMode;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class KitHolder implements InventoryHolder {
    public final boolean admin;
    public final FfaMode mode;
    public Inventory inv;

    public KitHolder(boolean admin, FfaMode mode) {
        this.admin = admin;
        this.mode = mode;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
