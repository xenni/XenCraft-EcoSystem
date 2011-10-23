package me.xenni.plugins.xencraft.ecosystem.arbiters.factories;

import me.xenni.plugins.xencraft.ecosystem.XenCraftEcoSystemPlugin;
import me.xenni.plugins.xencraft.ecosystem.arbiters.ItemAppraiser;
import org.bukkit.util.config.ConfigurationNode;

public interface ItemAppraiserFactory
{
    public ItemAppraiser getItemAppraiser(XenCraftEcoSystemPlugin plugin, ConfigurationNode config);
}
