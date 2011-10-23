package me.xenni.plugins.xencraft.ecosystem;

import me.xenni.plugins.xencraft.ecosystem.arbiters.MoneyConverter;
import org.bukkit.util.config.ConfigurationNode;

import java.io.Serializable;

public abstract class MoneySystem<V>
{
    public final String name;
    public final boolean isRepresentationUnique;

    public MoneySystem(String cname, boolean uniqueRepresentation)
    {
        name = cname;
        isRepresentationUnique = uniqueRepresentation;
    }

    public final MoneyConverter<V,V> getSelfConverter()
    {
        return new MoneyConverter<V,V>()
        {
            public V convert(V value) { return value; }
        };
    }

    public ValueStore<V> getSpecialWallet(Serializable data)
    {
        return null;
    }
    public Serializable getSpecialWalletData(String name, ValueStore<V> def)
    {
        return null;
    }
    public boolean isSpecialWalletName(String name)
    {
        return false;
    }

    public void initialize(ConfigurationNode config, XenCraftEcoSystemPlugin plugin)
    {
    }
    public boolean isValidRepresentation(String rep)
    {
        return (parseRepresentation(rep) != null);
    }
    public abstract V parseRepresentation(String rep);
    public abstract boolean isValueDeficit(V value);
    public abstract boolean isValueNothing(V value);
    public abstract V negate(V value);
    public abstract V add(V a, V b);
    public V subtract(V a, V b)
    {
        return add(a, negate(b));
    }

    public abstract ValueStore<V> createValueStore(Serializable data);

    public ValueStore<V> createValueStore()
    {
        return createValueStore(null);
    }

    public V normalizeValue(V value)
    {
        return value;
    }

    public String getShortRepresentation(V value)
    {
        return getLongRepresentation(value);
    }

    public String getLongRepresentation(V value)
    {
        return normalizeValue(value).toString();
    }
}
