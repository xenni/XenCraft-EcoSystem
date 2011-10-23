package me.xenni.plugins.xencraft.ecosystem.builtin.arbiters.factories;

import me.xenni.plugins.xencraft.ecosystem.XenCraftEcoSystemPlugin;
import me.xenni.plugins.xencraft.ecosystem.arbiters.CurrencyExchanger;
import org.bukkit.util.config.ConfigurationNode;

public final class CurrencyExchangerFactory implements me.xenni.plugins.xencraft.ecosystem.arbiters.factories.CurrencyExchangerFactory
{
    public CurrencyExchanger getCurrencyExchanger(XenCraftEcoSystemPlugin ecoSystemPlugin, ConfigurationNode config)
    {
        if (ecoSystemPlugin.primaryCurrencySystem == null)
        {
            return null;
        }

        return new CurrencyExchanger(ecoSystemPlugin);
    }
}
