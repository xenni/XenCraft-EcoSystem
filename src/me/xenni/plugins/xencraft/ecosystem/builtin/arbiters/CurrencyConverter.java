package me.xenni.plugins.xencraft.ecosystem.builtin.arbiters;

import me.xenni.plugins.xencraft.ecosystem.arbiters.MoneyConverter;
import me.xenni.plugins.xencraft.ecosystem.XenCraftEcoSystemPlugin;
import me.xenni.plugins.xencraft.ecosystem.CurrencySystem;

public final class CurrencyConverter implements MoneyConverter<Float, Float>
{
    public final XenCraftEcoSystemPlugin xenCraftEcoSystemPlugin;
    public final CurrencySystem currencyA;
    public final CurrencySystem currencyB;

    public CurrencyConverter(XenCraftEcoSystemPlugin plugin, CurrencySystem ca, CurrencySystem cb)
    {
        xenCraftEcoSystemPlugin = plugin;
        currencyA = ca;
        currencyB = cb;
    }

    public Float convert(Float value)
    {
        Float arate = xenCraftEcoSystemPlugin.exchanger.getPrimaryExchangeRate(currencyA);
        if (arate == null)
        {
            return null;
        }
        Float brate = xenCraftEcoSystemPlugin.exchanger.getPrimaryExchangeRate(currencyB);
        if (brate == null)
        {
            return null;
        }

        return ((value / arate) * brate);
    }
}
