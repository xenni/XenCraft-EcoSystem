package me.xenni.plugins.xencraft.ecosystem.builtin.arbiters;

import me.xenni.plugins.xencraft.ecosystem.arbiters.ItemAppraiser;
import me.xenni.plugins.xencraft.ecosystem.XenCraftEcoSystemPlugin;
import me.xenni.plugins.xencraft.ecosystem.util.AppraisalEntry;
import me.xenni.plugins.xencraft.util.ItemStackUtil;
import me.xenni.plugins.xencraft.util.XenCraftLogger;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.config.ConfigurationNode;

import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;

public final class BaseValueAppraiser extends ItemAppraiser
{
    private final Hashtable<Integer, AppraisalEntry> appraisalEntries = new Hashtable<Integer, AppraisalEntry>();
    private final float currencyScale;

    public BaseValueAppraiser(XenCraftEcoSystemPlugin plugin, float scale)
    {
        super(plugin);
        currencyScale = scale;
    }

    public Float appraise(ItemStack stack)
    {
        if (stack.getAmount() == 0)
        {
            return 0f;
        }

        AppraisalEntry appraisalEntry = appraisalEntries.get(stack.getTypeId());
        if (appraisalEntry == null)
        {
            return null;
        }

        float factor = appraisalEntry.base;
        if (stack.getData() != null)
        {
            Float databonus = appraisalEntry.dataBonuses.get(stack.getData().getData());
            if (databonus != null)
            {
                factor += databonus;
            }
        }
        if (appraisalEntry.durabilityScale)
        {
            factor *= (((double)(stack.getType().getMaxDurability() - stack.getDurability())) / ((double)stack.getType().getMaxDurability()));
        }

        return ((float)stack.getAmount() * factor * currencyScale);
    }

    public void onEnable()
    {
        XenCraftLogger logger = ecoSystemPlugin.getLogger();

        logger.log("Loading appraisal entries...", 2);
        loadAppraisalEntries(ecoSystemPlugin.tryGetConfiguration("appraisal.yml").getNodeList("appraisal", null), logger);
    }

    public void onDisable()
    {
        appraisalEntries.clear();
    }

    private void loadAppraisalEntries(List<ConfigurationNode> appraisalentries, XenCraftLogger logger)
    {
        for(ConfigurationNode appraisalentry : appraisalentries)
        {
            int itemid;
            itemid = appraisalentry.getInt("itemid", -1);
            if (itemid == -1)
            {
                String itemname = appraisalentry.getString("itemname");

                Material material;
                if (itemname == null)
                {
                    material = null;
                }
                else
                {
                    material = ItemStackUtil.matchMaterial(itemname);
                }
                if (material == null)
                {
                    logger.log("Invalid entry for item '" + itemname + "': Item not recognized.", Level.WARNING, 2);
                    continue;
                }
                else
                {
                    //log("Item name '" + itemname + "' recognized.", Level.FINER, 2);
                }
                itemid = material.getId();
            }

            logger.log("Entry for item #" + itemid + ":", Level.FINE, 3);

            AppraisalEntry entry = new AppraisalEntry(
                (float)appraisalentry.getDouble("base", -1),
                appraisalentry.getBoolean("durabilityscale", false)
            );
            if (entry.base < 0)
            {
                logger.log("Invalid entry for item " + itemid + ": 'base' must be present and non-negative.", Level.WARNING, 3);
                continue;
            }

            logger.log("Loading entry data bonuses:", Level.FINE, 3);
            for(ConfigurationNode databonus : appraisalentry.getNodeList("databonuses", null))
            {
                int data = databonus.getInt("datanum", -1);
                if (data > Byte.MAX_VALUE)
                {
                    logger.log("Invalid data bonus entry for item #" + itemid + ": 'datanum' must be non-negative and no greater than the maximum value of a byte.", Level.WARNING, 3);
                    continue;
                }
                if (data < 0)
                {
                    String dataname = databonus.getString("dataname");
                    Byte bdata = ItemStackUtil.parseDataName(itemid, dataname);
                    if (bdata == null)
                    {
                        logger.log("Invalid entry for item #" + itemid + " 'dataname': Data name '" + dataname + "' not recognized.", Level.WARNING, 3);
                        continue;
                    }
                    else
                    {
                        data = bdata.intValue();
                        logger.log("Data name '" + dataname + "' recognized.", Level.FINER, 4);
                    }
                }

                float bonus = (float)databonus.getDouble("bonus", 0);

                entry.dataBonuses.put(((Integer)data).byteValue(), bonus);

                logger.log(("Finished loading " + entry.dataBonuses.size() + " bonus entries."), Level.FINE, 4);
            }

            appraisalEntries.put(itemid, entry);
            logger.log("Entry for item #" + itemid + " completed loading.", Level.FINE, 4);
        }
        logger.log("Completed loading " + appraisalentries.size() + " appraisal entries.", 3);
    }
}
