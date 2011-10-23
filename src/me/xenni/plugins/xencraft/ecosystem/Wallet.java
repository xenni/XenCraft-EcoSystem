package me.xenni.plugins.xencraft.ecosystem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.logging.Level;

public final class Wallet
{
    private final static class WalletData implements Serializable
    {
        public Hashtable<String, Serializable> values = new Hashtable<String, Serializable>();
        public Hashtable<String, Serializable> customData = new Hashtable<String, Serializable>();

        public WalletData(Wallet instance)
        {
            for (Map.Entry<MoneySystem<?>, Serializable> customDatum : instance.customData.entrySet())
            {
                customData.put(customDatum.getKey().name, customDatum.getValue());
            }
            for(ValueStore<?> value : instance.values)
            {
                if (!customData.containsKey(value.moneySystem.name))
                {
                    values.put(value.moneySystem.name, value.getData());
                }
            }
        }
    }

    public final String name;

    private final ArrayList<ValueStore<?>> values = new ArrayList<ValueStore<?>>();
    private final Hashtable<MoneySystem<?>, Serializable> customData = new Hashtable<MoneySystem<?>, Serializable>();

    public Wallet(String wname)
    {
        name = wname;
    }

    public Wallet(String wname, Serializable data, XenCraftEcoSystemPlugin ecoSystemPlugin)
    {
        name = wname;

        if (data instanceof WalletData)
        {
            WalletData wdata = (WalletData)data;

            for (Map.Entry<String, Serializable> value : wdata.values.entrySet())
            {
                if (ecoSystemPlugin.moneySystems.containsKey(value.getKey()))
                {
                    values.add(ecoSystemPlugin.moneySystems.get(value.getKey()).createValueStore(value.getValue()));
                }
                else
                {
                    ecoSystemPlugin.getLogger().log("Unable to locate money system '" + value.getKey() + "' specified in wallet '" + name + "'.", Level.WARNING);
                }
            }

            for (Map.Entry<String, Serializable> customDatum : wdata.customData.entrySet())
            {
                if (ecoSystemPlugin.moneySystems.containsKey(customDatum.getKey()))
                {
                    customData.put(ecoSystemPlugin.moneySystems.get(customDatum.getKey()), customDatum.getValue());
                }
                else
                {
                    ecoSystemPlugin.getLogger().log("Unable to locate money system '" + customDatum.getKey() + "' specified in wallet '" + name + "'.", Level.WARNING);
                }
            }
        }
    }

    public Serializable getData()
    {
        return new WalletData(this);
    }

    @SuppressWarnings("unchecked")
    // To be honest, I can't really blame Java's type erasure for this one:
    // But it would just be too rude to ask the geniuses at Oracle to write a functional inference engine.
    public <A> ValueStore<A> getValueStoreForMoneySystem(MoneySystem<A> system)
    {
        for (ValueStore<?> store : values)
        {
            if (store.moneySystem == system)
            {
                return (ValueStore<A>)store;
            }
        }

        if (customData.containsKey(system))
        {
            Serializable data = customData.get(system);
            if (data != null)
            {
                ValueStore<A> wallet = system.getSpecialWallet(data);
                if (wallet != null)
                {
                    values.add(wallet);
                    return wallet;
                }
            }
        }

        if (initForMoneySystem(system))
        {
            return getValueStoreForMoneySystem(system);
        }
        else
        {
            return null;
        }
    }

    public Collection<MoneySystem<?>> availableMoneySystems()
    {
        ArrayList<MoneySystem<?>> available = new ArrayList<MoneySystem<?>>();
        available.addAll(customData.keySet());

        for (ValueStore<?> value : values)
        {
            if (!available.contains(value.moneySystem))
            {
                available.add(value.moneySystem);
            }
        }

        return available;
    }

    public boolean initForMoneySystem(MoneySystem<?> system)
    {
        for(ValueStore<?> testvalue : values)
        {
            if (testvalue.moneySystem == system)
            {
                return false;
            }
        }
        for (MoneySystem<?> testsystem : customData.keySet())
        {
            if (testsystem == system)
            {
                return false;
            }
        }

        if (system.isSpecialWalletName(name))
        {
            Serializable data = system.getSpecialWalletData(name, null);
            if (data == null)
            {
                return false;
            }

            customData.put(system, data);
        }
        else
        {
            ValueStore<?> store = system.createValueStore();
            if (store == null)
            {
                return false;
            }

            values.add(store);
        }

        return true;
    }
    public <A> boolean initDefault(ValueStore<A> value)
    {
        for(ValueStore<?> testvalue : values)
        {
            if (testvalue.moneySystem == value.moneySystem)
            {
                return false;
            }
        }
        for (MoneySystem<?> testsystem : customData.keySet())
        {
            if (testsystem == value.moneySystem)
            {
                return false;
            }
        }

        if (value.moneySystem.isSpecialWalletName(name))
        {
            Serializable data = value.moneySystem.getSpecialWalletData(name, value);
            if (data == null)
            {
                return false;
            }

            customData.put(value.moneySystem, data);
        }
        else
        {
            values.add(value);
        }

        return true;
    }
}
