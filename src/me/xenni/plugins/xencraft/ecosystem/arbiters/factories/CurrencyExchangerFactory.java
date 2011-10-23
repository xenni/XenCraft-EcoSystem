package me.xenni.plugins.xencraft.ecosystem.arbiters.factories;

import me.xenni.plugins.xencraft.ecosystem.XenCraftEcoSystemPlugin;
import me.xenni.plugins.xencraft.ecosystem.arbiters.CurrencyExchanger;
import org.bukkit.util.config.ConfigurationNode;

public interface CurrencyExchangerFactory
{
    public CurrencyExchanger getCurrencyExchanger(XenCraftEcoSystemPlugin plugin, ConfigurationNode config);
}
