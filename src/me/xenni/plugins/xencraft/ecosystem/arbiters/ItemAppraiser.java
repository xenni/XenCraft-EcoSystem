package me.xenni.plugins.xencraft.ecosystem.arbiters;

import me.xenni.plugins.xencraft.ecosystem.XenCraftEcoSystemPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class ItemAppraiser
{
    public final XenCraftEcoSystemPlugin ecoSystemPlugin;

    protected ItemAppraiser(XenCraftEcoSystemPlugin plugin)
    {
        ecoSystemPlugin = plugin;
    }

    public void onEnable()
    {
    }

    public abstract Float appraise(ItemStack stack);

    public Float appraise(ItemStack stack, Player context)
    {
        return appraise(stack);
    }

    public void onDisable()
    {
    }
}
