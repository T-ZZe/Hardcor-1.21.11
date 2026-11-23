package fr.skyhub.kit;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class KitHolder implements InventoryHolder {
    public final boolean admin;
    public Inventory inv;

    public KitHolder(boolean admin) {
        this.admin = admin;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
