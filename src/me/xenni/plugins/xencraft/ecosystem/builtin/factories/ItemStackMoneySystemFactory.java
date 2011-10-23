package me.xenni.plugins.xencraft.ecosystem.builtin.factories;

import me.xenni.plugins.xencraft.ecosystem.MoneySystem;
import me.xenni.plugins.xencraft.ecosystem.factories.MoneySystemFactory;
import me.xenni.plugins.xencraft.ecosystem.XenCraftEcoSystemPlugin;
import me.xenni.plugins.xencraft.ecosystem.builtin.ItemStackMoneySystem;
import me.xenni.plugins.xencraft.util.ItemStackUtil;
import org.bukkit.util.config.ConfigurationNode;

public final class ItemStackMoneySystemFactory implements MoneySystemFactory
{
    public MoneySystem<?> getMoneySystem(XenCraftEcoSystemPlugin ecoSystemPlugin, ConfigurationNode node)
    {
        ItemStackUtil.ItemStackCoalesceMode coalesceMode;
        String strmode = node.getString("stackmode", "single");
        if (strmode.equalsIgnoreCase("single"))
        {
            coalesceMode = ItemStackUtil.ItemStackCoalesceMode.ONE_STACK;
        }
        else if (strmode.equalsIgnoreCase("64"))
        {
            coalesceMode = ItemStackUtil.ItemStackCoalesceMode.STACKS_OF_64;
        }
        else if (strmode.equalsIgnoreCase("natural"))
        {
            coalesceMode = ItemStackUtil.ItemStackCoalesceMode.STACKS_OF_MATERIAL_MAX_STACK_SIZE;
        }
        else
        {
            return null;
        }

        return new ItemStackMoneySystem(node.getString("name"), coalesceMode, node.getBoolean("unique", true));
    }
}
