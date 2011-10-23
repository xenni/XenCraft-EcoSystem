package me.xenni.plugins.xencraft.ecosystem.arbiters;

public interface MoneyConverter<A , B>
{
    public B convert(A value);
}
