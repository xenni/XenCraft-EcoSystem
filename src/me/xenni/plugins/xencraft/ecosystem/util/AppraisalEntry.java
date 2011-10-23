package me.xenni.plugins.xencraft.ecosystem.util;

import java.util.Hashtable;
import java.util.Map;

public final class AppraisalEntry
{
    public final float base;
    public final boolean durabilityScale;
    public final Hashtable<Byte, Float> dataBonuses;

    public AppraisalEntry(float abase, boolean durabilityscale, Map<Byte, Float> databonuses)
    {
        base = abase;
        durabilityScale = durabilityscale;
        dataBonuses = (databonuses == null ? new Hashtable<Byte, Float>() : new Hashtable<Byte, Float>(databonuses));
    }
    public AppraisalEntry(float abase, boolean durabilityscale)
    {
        this(abase, durabilityscale, null);
    }
    public AppraisalEntry(float abase, Map<Byte, Float> databonuses)
    {
        this(abase, false, databonuses);
    }
    public AppraisalEntry(float abase)
    {
        this(abase, false, null);
    }
}
