package me.xenni.plugins.xencraft.ecosystem.factories;

import me.xenni.plugins.xencraft.ecosystem.CurrencySystem;
import me.xenni.plugins.xencraft.ecosystem.MoneySystem;
import me.xenni.plugins.xencraft.ecosystem.XenCraftEcoSystemPlugin;
import org.bukkit.util.config.ConfigurationNode;

public final class CurrencySystemFactory implements MoneySystemFactory
{
    public MoneySystem<?> getMoneySystem(XenCraftEcoSystemPlugin ecoSystemPlugin, ConfigurationNode node)
    {
        return new CurrencySystem(
            node.getString("pluralname"),
            node.getString("singularname"),
            node.getString("symbol"),
            node.getBoolean("symbolispostfix", false),
            (byte)node.getInt("fractionaldigits", 0)
        );
    }
}
