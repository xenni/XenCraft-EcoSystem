package me.xenni.plugins.xencraft.ecosystem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public final class EcoSystemPlayerPluginData implements Serializable
{
    public ArrayList<String> personalWallets;

    public EcoSystemPlayerPluginData()
    {
        personalWallets = new ArrayList<String>();
    }
}
