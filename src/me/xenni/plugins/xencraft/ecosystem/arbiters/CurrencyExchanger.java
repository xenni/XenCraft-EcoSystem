package me.xenni.plugins.xencraft.ecosystem.arbiters;

import me.xenni.plugins.xencraft.ecosystem.CurrencySystem;
import me.xenni.plugins.xencraft.ecosystem.MoneySystem;
import me.xenni.plugins.xencraft.ecosystem.XenCraftEcoSystemPlugin;
import me.xenni.plugins.xencraft.util.XenCraftLogger;
import org.bukkit.util.config.ConfigurationNode;

import java.util.List;
import java.util.Hashtable;
import java.util.logging.Level;

public class CurrencyExchanger
{
    private final Hashtable<CurrencySystem, Float> exchangeRates = new Hashtable<CurrencySystem, Float>();

    protected final XenCraftEcoSystemPlugin ecoSystemPlugin;

    public CurrencyExchanger(XenCraftEcoSystemPlugin plugin)
    {
        ecoSystemPlugin = plugin;
    }

    public void setPrimaryExchangeRate(CurrencySystem system, float rate)
    {
        exchangeRates.put(system, rate);
    }
    public Float getPrimaryExchangeRate(CurrencySystem system)
    {
        return exchangeRates.get(system);
    }

    private void loadCurrencyExchangeRates(List<ConfigurationNode> nodes, XenCraftLogger logger)
    {
        for (ConfigurationNode node : nodes)
        {
            String toname = node.getString("to");
            float rate = (float)node.getDouble("rate", -1);

            if (rate <= 0)
            {
                logger.log("Unable to load primary exchange rate to '" + toname + "': 'rate' must be present and greater than zero.", Level.SEVERE, 3);
                continue;
            }

            if (!ecoSystemPlugin.moneySystems.containsKey(toname))
            {
                logger.log("Unable to load primary exchange rate to '" + toname + "': 'to' MoneySystem was not found.", Level.SEVERE, 3);
                continue;
            }

            MoneySystem<?> to = ecoSystemPlugin.moneySystems.get(toname);

            if(!(to instanceof CurrencySystem))
            {
                logger.log("Unable to load primary exchange rate to '" + toname + "': 'to' MoneySystem is not a CurrencySystem.", Level.SEVERE, 3);
                continue;
            }

            logger.log("Setting primary exchange rate...", Level.FINE, 3);
            setPrimaryExchangeRate((CurrencySystem) to, rate);

            logger.log("Completed loading primary exchange rate to '" + toname + "'.", Level.FINE, 3);
        }
        logger.log("Completed loading primary exchange rates.", 3);
    }

    public void onEnable()
    {
        XenCraftLogger logger = ecoSystemPlugin.getLogger();

        logger.log("Loading primary exchange rates...", 3);
        loadCurrencyExchangeRates(ecoSystemPlugin.tryGetConfiguration("rates.yml").getNodeList("rates", null), logger);
    }

    public void onDisable()
    {
        exchangeRates.clear();
    }
}
