package me.xenni.plugins.xencraft.ecosystem.builtin.arbiters;

import me.xenni.plugins.xencraft.ecosystem.arbiters.MoneyConverter;

import java.util.List;

public final class MultiStepMoneyConverter<A, B> implements MoneyConverter<A, B>
{
    public final List<MoneyConverter<?, ?>> converters;

    public MultiStepMoneyConverter(List<MoneyConverter<?, ?>> lconverters)
    {
       converters = lconverters;
    }

    @SuppressWarnings("unchecked")   //Everybody loves type erasure! (Seriously, this is just a mess.)
    public B convert(A value)
    {
        if (value == null)
        {
            return null;
        }

        Object temp = value;
        for(MoneyConverter<?,?> converter : converters)
        {
            temp = ((MoneyConverter<Object, Object>)converter).convert(temp);
            if (temp == null)
            {
                return null;
            }
        }

        return (B)temp;
    }
}
