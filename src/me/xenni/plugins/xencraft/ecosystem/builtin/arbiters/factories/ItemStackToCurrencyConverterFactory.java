package me.xenni.plugins.xencraft.ecosystem.builtin.arbiters.factories;

import me.xenni.plugins.xencraft.ecosystem.arbiters.MoneyConverter;
import me.xenni.plugins.xencraft.ecosystem.arbiters.factories.MoneyConverterFactory;
import me.xenni.plugins.xencraft.ecosystem.MoneySystem;
import me.xenni.plugins.xencraft.ecosystem.XenCraftEcoSystemPlugin;
import me.xenni.plugins.xencraft.ecosystem.builtin.ItemStackMoneySystem;
import me.xenni.plugins.xencraft.ecosystem.builtin.arbiters.ItemStackToCurrencyConverter;
import org.bukkit.util.config.ConfigurationNode;

public final class ItemStackToCurrencyConverterFactory implements MoneyConverterFactory
{
    public MoneyConverter<?, ?> getMoneyConverter(XenCraftEcoSystemPlugin ecoSystemPlugin, ConfigurationNode node, MoneySystem<?> systema, MoneySystem<?> systemb)
    {
        if (ecoSystemPlugin.appraiser == null)
        {
            return null;
        }
        if (!(systema instanceof ItemStackMoneySystem))
        {
            return null;
        }
        if (systemb != ecoSystemPlugin.primaryCurrencySystem)
        {
            return null;
        }

        return new ItemStackToCurrencyConverter(ecoSystemPlugin);
    }
}
