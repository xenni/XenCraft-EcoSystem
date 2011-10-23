package me.xenni.plugins.xencraft.ecosystem.builtin.arbiters;

import me.xenni.plugins.xencraft.ecosystem.arbiters.MoneyConverter;
import me.xenni.plugins.xencraft.ecosystem.XenCraftEcoSystemPlugin;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;

public final class ItemStackToCurrencyConverter implements MoneyConverter<Collection<ItemStack>, Float>
{
    public final XenCraftEcoSystemPlugin ecoSystemPlugin;

    public ItemStackToCurrencyConverter(XenCraftEcoSystemPlugin plugin)
    {
        ecoSystemPlugin = plugin;
    }

    public Float convert(Collection<ItemStack> value)
    {
        float total = 0;
        for (ItemStack stack : value)
        {
            Float rate = ecoSystemPlugin.appraiser.appraise(stack);
            if (rate != null)
            {
                total += rate;
            }
        }

        return total;
    }
}
