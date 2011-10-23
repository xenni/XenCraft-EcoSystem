package me.xenni.plugins.xencraft.ecosystem.arbiters.factories;

import me.xenni.plugins.xencraft.ecosystem.MoneySystem;
import me.xenni.plugins.xencraft.ecosystem.XenCraftEcoSystemPlugin;
import me.xenni.plugins.xencraft.ecosystem.arbiters.MoneyConverter;
import org.bukkit.util.config.ConfigurationNode;

public interface MoneyConverterFactory
{
    public MoneyConverter<?, ?> getMoneyConverter(XenCraftEcoSystemPlugin ecoSystemPlugin, ConfigurationNode config, MoneySystem<?> a, MoneySystem<?> b);
}
