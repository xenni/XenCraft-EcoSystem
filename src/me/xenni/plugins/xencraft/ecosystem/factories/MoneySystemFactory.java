package me.xenni.plugins.xencraft.ecosystem.factories;

import me.xenni.plugins.xencraft.ecosystem.MoneySystem;
import me.xenni.plugins.xencraft.ecosystem.XenCraftEcoSystemPlugin;
import org.bukkit.util.config.ConfigurationNode;


public interface MoneySystemFactory
{
    public MoneySystem<?> getMoneySystem(XenCraftEcoSystemPlugin ecoSystemPlugin, ConfigurationNode config);
}
