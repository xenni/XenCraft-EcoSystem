package me.xenni.plugins.xencraft.ecosystem.builtin;

import me.xenni.plugins.xencraft.XenCraftCorePlugin;
import me.xenni.plugins.xencraft.ecosystem.MoneySystem;
import me.xenni.plugins.xencraft.ecosystem.ValueStore;
import me.xenni.plugins.xencraft.ecosystem.XenCraftEcoSystemPlugin;
import me.xenni.plugins.xencraft.util.ItemStackUtil;
import me.xenni.plugins.xencraft.util.XenCraftLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

public final class ItemStackMoneySystem extends MoneySystem<Collection<ItemStack>>
{
    protected final static class ItemStackMoneySystemPlayerListener extends PlayerListener
    {
        private final XenCraftEcoSystemPlugin ecoSystemPlugin;
        private final ArrayList<ValueStore<?>> defaultWalletValues = new ArrayList<ValueStore<?>>();
        private final ArrayList<MoneySystem<?>> defaultWalletSystems = new ArrayList<MoneySystem<?>>();

        public ItemStackMoneySystemPlayerListener(XenCraftEcoSystemPlugin plugin, ValueStore<Collection<ItemStack>> defaultPlayerInventory)
        {
            ecoSystemPlugin = plugin;

            if (defaultPlayerInventory != null)
            {
                defaultWalletValues.add(defaultPlayerInventory);
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

    public static final class ItemStackValueStore extends ValueStore<Collection<ItemStack>>
    {
        private ArrayList<ItemStack> value = new ArrayList<ItemStack>();

        public ItemStackValueStore(ItemStackMoneySystem system, Serializable data)
        {
            super(system, data);
        }

        public Collection<ItemStack> getValue()
        {
            return value;
        }
        public boolean setValue(Collection<ItemStack> newval)
        {
            value = ItemStackUtil.coalesce(newval, ItemStackUtil.ItemStackCoalesceMode.ONE_STACK, true);
            return true;
        }
    }

    public static final class InventoryValueStore extends ValueStore<Collection<ItemStack>>
    {
        public Player player;
        public Inventory inventory;

        public InventoryValueStore(ItemStackMoneySystem system, Serializable data)
        {
            super(system, data);
        }

        public void loadData(Serializable data)
        {
            if (data != null)
            {
                player = Bukkit.getServer().getPlayer((String)data);
            }
        }
        public Serializable getData()
        {
            if (inventory instanceof PlayerInventory)
            {
                return inventory.getName();
            }
            else
            {
                return null;
            }
        }

        private void syncInventory()
        {
            if (player != null)
            {
                inventory = player.getInventory();
            }
        }

        public Collection<ItemStack> getValue()
        {
            syncInventory();

            ItemStack[] aitems = inventory.getContents();
            ArrayList<ItemStack> items = new ArrayList<ItemStack>(aitems.length);
            for (int i = 0; i < aitems.length; i++)
            {
                items.add(aitems[i]);
            }

            return ItemStackUtil.coalesce(items, ItemStackUtil.ItemStackCoalesceMode.ONE_STACK, false);
        }
        public boolean setValue(Collection<ItemStack> newval)
        {
            syncInventory();

            if (moneySystem.isValueDeficit(newval))
            {
                return false;
            }

            inventory.setContents((ItemStack[])(ItemStackUtil.coalesce(newval, ItemStackUtil.ItemStackCoalesceMode.STACKS_OF_MATERIAL_MAX_STACK_SIZE, false).toArray()));
            return true;
        }

        public boolean addValue(Collection<ItemStack> value)
        {
            syncInventory();

            ItemStack[] old = inventory.getContents();

            for (ItemStack stack : value)
            {
                if (inventory.addItem(stack).size() > 0)
                {
                    inventory.setContents(old);
                    return false;
                }
            }

            return true;
        }
        public boolean subtractValue(Collection<ItemStack> value)
        {
            syncInventory();

            ItemStack[] old = inventory.getContents();

            for (ItemStack stack : value)
            {
                if (!ItemStackUtil.inventoryContains(inventory, stack))
                {
                    return false;
                }
            }
            for (ItemStack stack : value)
            {
                if (!ItemStackUtil.removeFromInventory(inventory, stack))
                {
                    inventory.setContents(old);
                    return false;
                }
            }

            return true;
        }

        public static InventoryValueStore getForPlayer(ItemStackMoneySystem system, String playerName)
        {
            return new InventoryValueStore(system, playerName);
        }
        public static InventoryValueStore getForPlayer(ItemStackMoneySystem system, Player player)
        {
            return getForPlayer(system, player.getName());
        }
        public static InventoryValueStore getForGenericInventory(ItemStackMoneySystem system, Inventory inventory)
        {
            InventoryValueStore store = new InventoryValueStore(system, null);
            store.inventory = inventory;
            return store;
        }
    }

    public final ItemStackUtil.ItemStackCoalesceMode coalesceMode;
    private ItemStackMoneySystemPlayerListener playerListener;

    public ItemStackMoneySystem(String name, ItemStackUtil.ItemStackCoalesceMode mode, boolean uniqueRepresentation)
    {
        super(name, uniqueRepresentation);

        coalesceMode = mode;
    }

    public void initialize(ConfigurationNode node, XenCraftEcoSystemPlugin plugin)
    {
        if (node.getBoolean("includeplayerinventory", false))
        {
            ValueStore<Collection<ItemStack>> defaultPlayerInventory = null;
            String defaultPlayerInventoryRep = node.getString("defaultplayerinventory");
            if (defaultPlayerInventoryRep != null && !defaultPlayerInventoryRep.isEmpty())
            {
                Collection<ItemStack> defaultPlayerInventoryItems = parseRepresentation(defaultPlayerInventoryRep);
                if (defaultPlayerInventoryItems != null)
                {
                    if (!isValueNothing(defaultPlayerInventoryItems))
                    {
                        defaultPlayerInventory = createValueStore();
                        defaultPlayerInventory.setValue(defaultPlayerInventoryItems);
                    }
                }
                else
                {
                    plugin.getLogger().log("EcoSystem configuration node 'defaultplayerinventory' was unable to be parsed.", Level.WARNING);
                }
            }

            playerListener = new ItemStackMoneySystemPlayerListener(plugin, defaultPlayerInventory);
        }
    }

    public ValueStore<Collection<ItemStack>> createValueStore(Serializable data)
    {
        return new ItemStackValueStore(this, data);
    }

    public Collection<ItemStack> normalizeValue(Collection<ItemStack> value)
    {
        if (value == null)
        {
            return new ArrayList<ItemStack>(0);
        }

        return ItemStackUtil.coalesce(value, ItemStackUtil.ItemStackCoalesceMode.ONE_STACK, false);
    }

    public boolean isValueDeficit(Collection<ItemStack> value)
    {
        if (value != null)
        {
            for (ItemStack stack : ItemStackUtil.coalesce(value, ItemStackUtil.ItemStackCoalesceMode.ONE_STACK, true))
            {
                if (stack.getAmount() < 0)
                {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isValueNothing(Collection<ItemStack> value)
    {
        return (value == null || ItemStackUtil.coalesce(value, ItemStackUtil.ItemStackCoalesceMode.ONE_STACK, true).size() == 0);
    }

    public Collection<ItemStack> negate(Collection<ItemStack> value)
    {
        if (value == null)
        {
            return new ArrayList<ItemStack>(0);
        }

        value = ItemStackUtil.coalesce(value, ItemStackUtil.ItemStackCoalesceMode.ONE_STACK, true);
        ArrayList<ItemStack> result = new ArrayList<ItemStack>();

        for (ItemStack stack : value)
        {
            result.add(new ItemStack(stack.getType(), (-1 * stack.getAmount()), stack.getDurability(), (stack.getData() == null ? null : stack.getData().getData())));
        }

        return result;
    }

    public Collection<ItemStack> add(Collection<ItemStack> a, Collection<ItemStack> b)
    {
        ArrayList<ItemStack> acc = new ArrayList<ItemStack>(a);
        acc.addAll(b);

        return ItemStackUtil.coalesce(acc, ItemStackUtil.ItemStackCoalesceMode.ONE_STACK, true);
    }

    public boolean isSpecialWalletName(String name)
    {
        return (name.startsWith("player.") && Bukkit.getServer().matchPlayer(name.substring(7).toLowerCase()).size() == 1);
    }
    public Serializable getSpecialWalletData(String name, ValueStore<Collection<ItemStack>> def)
    {
        Player player = Bukkit.getServer().matchPlayer(name.substring(7).toLowerCase()).get(0);
        if (player == null)
        {
            return null;
        }

        if (def != null)
        {
            Configuration config = XenCraftCorePlugin.getInstance(Bukkit.getServer().getPluginManager()).getDataForPlayer(player).config;
            if (!config.getBoolean("PersonalInventoryValueStoreInitialized", false))
            {
                config.setProperty("PersonalInventoryValueStoreInitialized", true);

                Inventory inv = player.getInventory();
                for (ItemStack stack : def.getValue())
                {
                    inv.addItem(stack);
                }

                player.sendRawMessage("[EcoSystem] Welcome, " + player.getName() + "! You have been granted " + getLongRepresentation(def.getValue()) + ".");
            }
        }

        return player.getName();
    }
    public ValueStore<Collection<ItemStack>> getSpecialWallet(Serializable data)
    {
        return new InventoryValueStore(this, data);
    }

    public Collection<ItemStack> parseRepresentation(String rep)
    {
        ArrayList<ItemStack> results = new ArrayList<ItemStack>();

        if (rep.startsWith("["))
        {
            if (!rep.endsWith("]"))
            {
                return null;
            }

            for(String stack : rep.substring(1, rep.length() - 1).split(", ?"))
            {
                ItemStack parsed = ItemStackUtil.parse(stack);
                if (parsed == null)
                {
                    return null;
                }
                results.add(parsed);
            }
        }
        else
        {
            ItemStack parsed = ItemStackUtil.parse(rep);
            if (parsed == null)
            {
                return null;
            }
            results.add(parsed);
        }

        return results;
    }

    private String getRepresentation(Collection<ItemStack> value, boolean uselong)
    {
        if (value == null)
        {
            return "[]";
        }

        value = normalizeValue(value);
        switch (value.size())
        {
            case 0:
                return "[]";
            case 1:
                return ItemStackUtil.toString(value.iterator().next());
            default:
                StringBuilder sb = new StringBuilder("[");
                boolean usecomma = false;
                for (ItemStack stack : value)
                {
                    if (usecomma)
                    {
                        sb.append(uselong ? ", " : ",");
                    }
                    else
                    {
                        usecomma = true;
                    }

                    sb.append(ItemStackUtil.toString(stack));
                }
                sb.append("]");
                return sb.toString();
        }
    }

    public String getLongRepresentation(Collection<ItemStack> value)
    {
        return getRepresentation(value, true);
    }

    public String getShortRepresentation(Collection<ItemStack> value)
    {
        return getRepresentation(value, false);
    }
}
