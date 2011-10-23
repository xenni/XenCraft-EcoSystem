package me.xenni.plugins.xencraft.ecosystem.builtin.arbiters.factories;

import me.xenni.plugins.xencraft.ecosystem.*;
import me.xenni.plugins.xencraft.ecosystem.arbiters.MoneyConverter;
import me.xenni.plugins.xencraft.ecosystem.arbiters.factories.MoneyConverterFactory;
import me.xenni.plugins.xencraft.ecosystem.builtin.arbiters.CurrencyConverter;
import me.xenni.plugins.xencraft.ecosystem.CurrencySystem;
import org.bukkit.util.config.ConfigurationNode;

public final class CurrencyConverterFactory implements MoneyConverterFactory
{
    public MoneyConverter<?, ?> getMoneyConverter(
            XenCraftEcoSystemPlugin ecoSystemPlugin, ConfigurationNode config,
            MoneySystem<?> a, MoneySystem<?> b
    )
    {
        if (!(a instanceof CurrencySystem) || !(b instanceof CurrencySystem))
        {
            return null;
        }

        return new CurrencyConverter(ecoSystemPlugin, (CurrencySystem)a, (CurrencySystem)b);
    }
}
