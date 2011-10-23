package me.xenni.plugins.xencraft.ecosystem;

import me.xenni.plugins.xencraft.util.XenCraftLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.util.config.ConfigurationNode;

import java.io.IOException;
import java.io.Serializable;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

public final class CurrencySystem extends MoneySystem<Float>
{
    protected final static class CurrencySystemPlayerListener extends PlayerListener
    {
        private final XenCraftEcoSystemPlugin ecoSystemPlugin;
        private final ArrayList<ValueStore<?>> defaultWalletValues = new ArrayList<ValueStore<?>>();
        private final ArrayList<MoneySystem<?>> defaultWalletSystems = new ArrayList<MoneySystem<?>>();

        public CurrencySystemPlayerListener(XenCraftEcoSystemPlugin plugin, ValueStore<Float> initialPlayerBalance)
        {
            ecoSystemPlugin = plugin;

            if (initialPlayerBalance != null)
            {
                defaultWalletValues.add(initialPlayerBalance);
            }

            ecoSystemPlugin.getServer().getPluginManager().registerEvent(Event.Type.PLAYER_JOIN, this, Event.Priority.Normal, ecoSystemPlugin);
        }

        public void onPlayerJoin(PlayerJoinEvent event)
        {
            Player player = event.getPlayer();
            try
            {
                ecoSystemPlugin.getWallet("player." + player.getName(), defaultWalletValues, defaultWalletSystems);
            }
            catch (IOException ex)
            {
                XenCraftLogger logger = ecoSystemPlugin.getLogger();
                logger.log("A failure occurred initializing \"" + player.getName() + "\"'s wallet.", Level.WARNING);
                logger.log("Message: " + ex.getMessage(), Level.INFO, 1);
                player.sendRawMessage("[EcoSystem] WARNING: A failure occurred loading your wallet data.");
                if (!player.isOp())
                {
                    player.sendRawMessage("[EcoSystem]          You may want to report this to an administrator.");
                }
            }
        }
    }

    public static final class CurrencyValueStore extends ValueStore<Float>
    {
        private Float value;

        public CurrencyValueStore(CurrencySystem system, Serializable data)
        {
            super(system, data);
        }

        public boolean setValue(Float newval)
        {
            value = moneySystem.normalizeValue(newval);
            return true;
        }
        public Float getValue()
        {
            return value;
        }
    }

    public final String singularName;
    public final String symbol;
    public final boolean symbolIsPostfix;
    public final byte fractionalDigits;
    private String wholeShortFormat;
    private String fractionalShortFormat;
    private String oneLongValue;
    private String wholeLongFormat;
    private String fractionalLongFormat;
    private CurrencySystemPlayerListener playerListener;

    public CurrencySystem(String pluralname, String singularname, String csymbol, boolean symbolispostfix, byte fractionaldigits)
    {
        super(pluralname, true);

        singularName = singularname;
        symbol = csymbol;
        symbolIsPostfix = symbolispostfix;
        fractionalDigits = fractionaldigits;

        if (fractionaldigits < 0 || fractionaldigits > 4)
        {
            throw new Error("Parameter 'fractionaldigits' invalid. Expected a non-negative value not greater than 4.");
        }

        oneLongValue = ("1 " + singularName);
        wholeLongFormat = ("%.0f " + name);
        wholeShortFormat = (symbolIsPostfix ? ("%.0f" + symbol) : (symbol + "%.0f"));

        if (fractionaldigits == 0)
        {
            fractionalShortFormat = wholeShortFormat;
            fractionalLongFormat = wholeLongFormat;
        }
        else
        {
            fractionalShortFormat = (symbolIsPostfix ? ("%." + fractionaldigits + "f" + symbol) : (symbol + "%." + fractionaldigits + "f"));
            fractionalLongFormat = ("%." + fractionaldigits + "f " + name);
        }
    }

    public void initialize(ConfigurationNode node, XenCraftEcoSystemPlugin plugin)
    {
        if (node.getBoolean("includeinwallet", false))
        {
            ValueStore<Float> initialPlayerBalance = createValueStore();
            initialPlayerBalance.setValue((float)node.getDouble("initialplayerbalance", 0.0));

            playerListener = new CurrencySystemPlayerListener(plugin, initialPlayerBalance);
        }
    }

    public boolean isValidRepresentation(String rep)
    {
        return (
            rep.matches(oneLongValue) ||
            rep.matches("-?\\d+(\\.\\d+)? " + name) ||
            (symbolIsPostfix ?
                (rep.endsWith(symbol) && rep.substring(0, rep.length() - symbol.length()).matches("^-?\\d+(\\.\\d+)?$")) :
                (rep.startsWith(symbol) && rep.substring(symbol.length()).matches("^-?\\d+(\\.\\d+)?$"))
            )
        );
    }

    public boolean isValueDeficit(Float value)
    {
        return (value != null && value < 0);
    }

    public boolean isValueNothing(Float value)
    {
        return (value == null || value == 0);
    }

    public Float negate(Float value)
    {
        return (value == null ? 0 : (-1 * value));
    }

    public Float add(Float a, Float b)
    {
        return (a + b);
    }

    public Float subtract(Float a, Float b)
    {
        return (a - b);
    }

    public ValueStore<Float> createValueStore(Serializable data)
    {
        return new CurrencyValueStore(this, data);
    }

    public Float parseRepresentation(String rep)
    {
        if (rep.startsWith(symbol))
        {
            return Float.parseFloat(rep.substring(symbol.length()));
        }
        else
        {
            return Float.parseFloat(rep.substring(0, rep.length() - symbol.length()));
        }
    }

    public Float normalizeValue(Float value)
    {
        if (value == null)
        {
            return 0f;
        }

        if (fractionalDigits == 0)
        {
            return new Float(Math.floor(value.doubleValue()));
        }
        else
        {
            double factor = Math.pow(10, fractionalDigits);
            return new Float(Math.floor(value.doubleValue() * factor) / factor);
        }
    }

    public String getShortRepresentation(Float value)
    {
        if (value == null)
        {
            return getShortRepresentation(0f);
        }
        return ((Math.floor(value) == value) ? String.format(wholeShortFormat, value) : String.format(fractionalShortFormat, value));
    }

    public String getLongRepresentation(Float value)
    {
        if (value == null)
        {
            return getLongRepresentation(0f);
        }
        else if (value == 1)
        {
            return oneLongValue;
        }
        else
        {
            return ((Math.floor(value) == value) ? String.format(wholeLongFormat, value) : String.format(fractionalLongFormat, value));
        }
    }
}