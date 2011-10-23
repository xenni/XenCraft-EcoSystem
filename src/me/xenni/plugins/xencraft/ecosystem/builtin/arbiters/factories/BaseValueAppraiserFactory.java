package me.xenni.plugins.xencraft.ecosystem.builtin.arbiters.factories;

import me.xenni.plugins.xencraft.ecosystem.XenCraftEcoSystemPlugin;
import me.xenni.plugins.xencraft.ecosystem.arbiters.ItemAppraiser;
import me.xenni.plugins.xencraft.ecosystem.arbiters.factories.ItemAppraiserFactory;
import me.xenni.plugins.xencraft.ecosystem.builtin.arbiters.BaseValueAppraiser;
import org.bukkit.util.config.ConfigurationNode;

public final class BaseValueAppraiserFactory implements ItemAppraiserFactory
{
    public ItemAppraiser getItemAppraiser(XenCraftEcoSystemPlugin ecoSystemPlugin, ConfigurationNode node)
    {
        if (ecoSystemPlugin.primaryCurrencySystem == null)
        {
            return null;
        }

        return new BaseValueAppraiser(ecoSystemPlugin, (float)node.getDouble("currencyscale", 1.0));
    }
}
