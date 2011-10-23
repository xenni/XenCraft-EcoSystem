package me.xenni.plugins.xencraft.ecosystem;

import java.io.Serializable;

public abstract class ValueStore<V>
{
    public final MoneySystem<V> moneySystem;

    protected ValueStore(MoneySystem<V> system)
    {
        moneySystem = system;
    }

    public ValueStore(MoneySystem<V> system, Serializable data)
    {
        this(system);

        if (data != null)
        {
            loadData(data);
        }
    }

    public abstract V getValue();
    public abstract boolean setValue(V value);

    @SuppressWarnings("unchecked")
    public boolean setAnyValue(Object value)
    {
        return setValue((V)value);
    }

    public boolean addValue(V value)
    {
        return setValue(moneySystem.add(getValue(), value));
    }
    public boolean subtractValue(V value)
    {
        return setValue(moneySystem.subtract(getValue(), value));
    }

    @SuppressWarnings("unchecked")
    public boolean addAny(Object value)
    {
        return addValue((V)value);
    }
    @SuppressWarnings("unchecked")
    public boolean subtractAny(Object value)
    {
        return subtractValue((V)value);
    }

    public Serializable getData()
    {
        return moneySystem.getShortRepresentation(getValue());
    }
    protected void loadData(Serializable data)
    {
        setValue(moneySystem.parseRepresentation((String)data));
    }

    public boolean getIsDeficit()
    {
        return moneySystem.isValueDeficit(getValue());
    }
    public boolean getIsNothing()
    {
        return moneySystem.isValueNothing(getValue());
    }

    public boolean equals(ValueStore<V> other)
    {
        return (
            other.moneySystem == moneySystem &&
            moneySystem.isValueNothing(moneySystem.subtract(getValue(), other.getValue()))
        );
    }

    @SuppressWarnings("unchecked")
    public boolean equalsAny(ValueStore<?> other)
    {
        return equals((ValueStore<V>)other);
    }

    public Integer compare(ValueStore<V> other)
    {
        if (other.moneySystem != moneySystem)
        {
            return null;
        }

        V val = moneySystem.subtract(getValue(), other.getValue());
        return (moneySystem.isValueNothing(val) ? 0 : (moneySystem.isValueDeficit(val) ? -1 : 1));
    }

    @SuppressWarnings("unchecked")
    public Integer compareAny(ValueStore<?> other)
    {
        return compare((ValueStore<V>)other);
    }

    public String toString(boolean longRepresentation)
    {
        return (longRepresentation ? moneySystem.getLongRepresentation(getValue()) : moneySystem.getShortRepresentation(getValue()));
    }

    public String toString()
    {
        return toString(false);
    }
}
