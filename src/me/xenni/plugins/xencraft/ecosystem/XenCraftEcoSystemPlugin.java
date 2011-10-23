package me.xenni.plugins.xencraft.ecosystem;

import me.xenni.plugins.xencraft.ecosystem.arbiters.CurrencyExchanger;
import me.xenni.plugins.xencraft.ecosystem.arbiters.ItemAppraiser;
import me.xenni.plugins.xencraft.ecosystem.arbiters.MoneyConverter;
import me.xenni.plugins.xencraft.ecosystem.arbiters.factories.CurrencyExchangerFactory;
import me.xenni.plugins.xencraft.ecosystem.arbiters.factories.ItemAppraiserFactory;
import me.xenni.plugins.xencraft.ecosystem.arbiters.factories.MoneyConverterFactory;
import me.xenni.plugins.xencraft.ecosystem.builtin.ItemStackMoneySystem;
import me.xenni.plugins.xencraft.ecosystem.builtin.arbiters.MultiStepMoneyConverter;
import me.xenni.plugins.xencraft.ecosystem.factories.MoneySystemFactory;
import me.xenni.plugins.xencraft.plugin.GenericXenCraftPlugin;
import me.xenni.plugins.xencraft.util.CommandUtil;
import me.xenni.plugins.xencraft.util.ItemStackUtil;
import me.xenni.plugins.xencraft.util.ModuleUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

import java.io.*;
import java.util.*;
import java.util.logging.Level;

public final class XenCraftEcoSystemPlugin extends GenericXenCraftPlugin
{
    public final Hashtable<String, MoneySystem<?>> moneySystems = new Hashtable<String, MoneySystem<?>>();
    public CurrencySystem primaryCurrencySystem = null;
    public ItemAppraiser appraiser = null;
    public CurrencyExchanger exchanger = null;
    private int maxTradeRadius;

    public int getMaxTradeRadius()
    {
        return maxTradeRadius;
    }

    public Configuration config;

    public final Hashtable<
        MoneySystem<?>,
        Hashtable<
            MoneySystem<?>,
            MoneyConverter<?, ?>
        >
    > moneyConverters = new Hashtable<MoneySystem<?>, Hashtable<MoneySystem<?>, MoneyConverter<?, ?>>>();
    public final ArrayList<Wallet> wallets = new ArrayList<Wallet>();

    public void onPluginEnable()
    {
        log("Loading main configuration...", Level.FINE, 1);
        config = getConfiguration();
        maxTradeRadius = config.getInt("maxtraderadius", 0);
        log("Completed loading main configuration.", Level.FINE, 2);

        log("Loading MoneySystems...", 1);
        Map<MoneySystem<?>, ConfigurationNode> moneySystemInitEntries = loadMoneySystems(config.getNodeList("moneysystems", null));

        String primarycurrencysystem = config.getString("primarycurrencysystem");
        if (primarycurrencysystem != null)
        {
            MoneySystem<?> system = moneySystems.get(primarycurrencysystem);
            if (system == null || !(system instanceof CurrencySystem))
            {
                primaryCurrencySystem = null;
                log("Invalid primary currency system.", Level.SEVERE, 1);
                log("Must be null or a registered instance of 'me.xenni.plugins.xencraft.ecosystem.CurrencySystem'", 2);
            }
            else
            {
                primaryCurrencySystem = (CurrencySystem)system;
                log("Primary currency system is: '" + primarycurrencysystem + "'.", 1);
            }
        }
        else
        {
            log("No primary currency system configured.", Level.INFO, 1);
        }

        log("Loading currency exchanger...", 1);
        ConfigurationNode exnode = config.getNode("currencyexchanger");
        if (exnode != null)
        {
            String exArchive = exnode.getString("factoryarchive");
            String exClass = exnode.getString("factoryclass");

            CurrencyExchangerFactory factory;
            if (exArchive == null)
            {
                factory = ModuleUtil.loadClass(exClass);
            }
            else
            {
                factory = ModuleUtil.loadClassFromJar(exClass, exArchive);
            }

            if (factory == null)
            {
                log("Unable to load currency exchanger: Unable to load factory class '" + exClass + "'.", Level.SEVERE, 2);
            }
            else
            {
                exchanger = factory.getCurrencyExchanger(this, exnode.getNode("factoryconfig"));
                if (exchanger != null)
                {
                    log("Enabling currency exchanger of type '" + exClass + "'...", Level.FINE, 3);
                    exchanger.onEnable();
                    log("Completed loading currency exchanger.", 2);
                }
                else
                {
                    log("Unable to load currency exchanger: Factory instance returned a null CurrencyExchanger instance.", Level.SEVERE, 2);
                }
            }
        }
        else
        {
            log("No currency exchanger configured.", 2);
        }

        log("Loading item appraiser...", 1);
        ConfigurationNode ianode = config.getNode("itemappraiser");
        if (exnode != null)
        {
            String iaArchive = ianode.getString("factoryarchive");
            String iaClass = ianode.getString("factoryclass");

            ItemAppraiserFactory factory;
            if (iaArchive == null)
            {
                factory = ModuleUtil.loadClass(iaClass);
            }
            else
            {
                factory = ModuleUtil.loadClassFromJar(iaClass, iaArchive);
            }

            if (factory == null)
            {
                log("Unable to load item appraiser: Unable to load factory class '" + iaClass + "'.", Level.SEVERE, 2);
            }
            else
            {
                appraiser = factory.getItemAppraiser(this, ianode.getNode("factoryconfig"));
                if (appraiser != null)
                {
                    log("Enabling item appraiser of type '" + iaClass + "'...", Level.FINE, 3);
                    appraiser.onEnable();
                    log("Completed loading item appraiser.", 2);
                }
                else
                {
                    log("Unable to load item appraiser: Factory instance returned a null ItemAppraiser instance.", Level.SEVERE, 2);
                }
            }
        }
        else
        {
            log("No item appraiser configured.", 2);
        }

        log("Loading MoneyConverters...", 1);
        loadMoneyConverters(config.getNodeList("moneyconverters", null));

        log("Initializing MoneySystems...", Level.FINE, 1);
        for(Map.Entry<MoneySystem<?>, ConfigurationNode> moneySystemInitEntry : moneySystemInitEntries.entrySet())
        {
            moneySystemInitEntry.getKey().initialize(moneySystemInitEntry.getValue(), this);
        }
    }

    private boolean cmdexecEcoInfo(CommandSender sender, String[] args)
    {
        if (args.length != 0)
        {
            return false;
        }

        sender.sendMessage("[EcoSystem] Available Money Systems:");
        for (MoneySystem<?> moneySystem : moneySystems.values())
        {
            String message = ("   " + moneySystem.name + " (");
            if (moneySystem instanceof CurrencySystem)
            {
                CurrencySystem currencySystem = (CurrencySystem)moneySystem;
                message = (
                    message + "Currency: " + currencySystem.fractionalDigits + " fractional digits, '" +
                    (currencySystem.symbolIsPostfix ? ("X" + currencySystem.symbol) : (currencySystem.symbol + "X")) +
                    "'"
                );
                if (primaryCurrencySystem != null)
                {
                    message = (message + ", " + (currencySystem == primaryCurrencySystem ? "Primary" : "Secondary"));
                }
            }
            else if (moneySystem instanceof ItemStackMoneySystem)
            {
                message = (message + "Items");
            }
            else
            {
                message = (message + "Unknown: " + moneySystem.getClass().getSimpleName());
            }
            message = (message + ")");

            sender.sendMessage(message);
        }
        sender.sendMessage(" (" + moneySystems.size() + " Total)");
        if (primaryCurrencySystem == null)
        {
            sender.sendMessage("[EcoSystem] There is no primary currency system.");
        }

        return true;
    }
    private boolean cmdexecWallet(CommandSender sender, String[] args)
    {
        if (args.length > 1)
        {
            return false;
        }
        if (!(sender instanceof Player))
        {
            sender.sendMessage("[EcoSystem] This command requires a player context.");
            return true;
        }
        Player player = (Player)sender;

        if (sender.hasPermission("xencraft.eco.checkwallet"))
        {
            Wallet wallet;
            try
            {
                wallet = getWallet(("player." + player.getName()), null, null);
            }
            catch (IOException ex)
            {
                sender.sendMessage("[EcoSystem] Unable to perform operation: " + ex.getMessage());
                return true;
            }
            if (wallet == null)
            {
                sender.sendMessage("[EcoSystem] You have no wallet.");
                return true;
            }

            if (args.length == 0)
            {
                sender.sendMessage("[EcoSystem] Available Balances: ");
                for (MoneySystem<?> system : wallet.availableMoneySystems())
                {
                    ValueStore<?> value = wallet.getValueStoreForMoneySystem(system);
                    if (value == null || value instanceof ItemStackMoneySystem.InventoryValueStore)
                    {
                        continue;
                    }

                    String message = ("    " + system.name + ": " + (value == null ? "<Invalid>" : (value.getIsNothing() ? "(nothing)" : value.toString(!(system instanceof CurrencySystem)))));
                    if (primaryCurrencySystem != null && system != primaryCurrencySystem && !value.getIsNothing())
                    {
                        ValueStore<?> convertedValue = convertToValueStore(value, primaryCurrencySystem);
                        if (
                            convertedValue != null && !convertedValue.getIsNothing() &&
                            (!(system instanceof ItemStackMoneySystem) || sender.hasPermission("xencraft.eco.appraise")))
                        {
                            message = (message + " (worth " + convertedValue.toString(false) + ")");
                        }
                    }
                    sender.sendMessage(message);
                }
            }
            else
            {
                MoneySystem<?> system = moneySystems.get(args[0]);
                if (system == null)
                {
                    sender.sendMessage("[EcoSystem] Money system '" + args[0] + "' could not be found.");
                    return true;
                }
                ValueStore<?> value = wallet.getValueStoreForMoneySystem(system);
                if (value == null)
                {
                    sender.sendMessage("[EcoSystem] Value for money system '" + system.name + "' not available.");
                    return true;
                }

                String message;
                if (system instanceof CurrencySystem)
                {
                    if (((CurrencySystem)system).isRepresentationUnique)
                    {
                        message = ("[EcoSystem] Balance: " + value.toString(false));
                    }
                    else
                    {
                        message = ("[EcoSystem] " + system.name + ": " + value.toString(false));
                    }
                }
                else
                {
                    message = ("[EcoSystem] " + system.name + ": " + (value.getIsNothing() ? "(nothing)" : value.toString(true)));
                }

                if (primaryCurrencySystem != null && system != primaryCurrencySystem && !value.getIsNothing())
                {
                    ValueStore<?> convertedValue = convertToValueStore(value, primaryCurrencySystem);
                    if (
                        convertedValue != null && !convertedValue.getIsNothing() &&
                        (!(system instanceof ItemStackMoneySystem) || sender.hasPermission("xencraft.eco.appraise")))
                    {
                        message = (message + " (worth " + convertedValue.toString(false) + ")");
                    }
                }
                sender.sendMessage(message);
            }
        }
        else
        {
            sender.sendMessage("[EcoSystem] You do not have permission to do that.");
        }

        return true;
    }
    private boolean cmdexecPay(CommandSender sender, String[] args)
    {
        if (args.length < 2 || args.length > 3)
        {
            return false;
        }
        if (!(sender instanceof Player))
        {
            sender.sendMessage("[EcoSystem] This command requires a player context.");
            return true;
        }

        Player source = (Player)sender;

        if (sender.hasPermission("xencraft.eco.tradewallet"))
        {
            Player target = getServer().getPlayer(args[0]);
            if (target == null)
            {
                sender.sendMessage("[EcoSystem] Could not find player '" + args[0] + "'.");
                return true;
            }
            if (source == target)
            {
                sender.sendMessage("[EcoSystem] You cannot trade with yourself.");
                return true;
            }

            if (!target.hasPermission("xencraft.eco.tradewallet"))
            {
                sender.sendMessage("[EcoSystem] '" + target.getDisplayName() + "' is not permitted to trade.");
                return true;
            }

            Wallet srcwallet;
            try
            {
                srcwallet = getWallet("player." + source.getName(), null, null);
            }
            catch (IOException ex)
            {
                sender.sendMessage("[EcoSystem] Unable to complete operation: " + ex.getMessage());
                return true;
            }
            if (srcwallet == null)
            {
                sender.sendMessage("[EcoSystem] You have no wallet.");
                return true;
            }

            Wallet dstwallet;
            try
            {
                dstwallet = getWallet("player." + target.getName(), null, null);
            }
            catch (IOException ex)
            {
                sender.sendMessage("[EcoSystem] Unable to complete operation: " + ex.getMessage());
                return true;
            }
            if (dstwallet == null)
            {
                sender.sendMessage("[EcoSystem] '" + target.getDisplayName() + "' has no wallet.");
                return true;
            }

            if (
                maxTradeRadius != 0 &&
                !sender.hasPermission("xencraft.eco.tradeanywhere") &&
                !target.hasPermission("xencraft.eco.tradeanywhere") &&
                (
                    source.getWorld() != target.getWorld() ||
                    source.getLocation().distance(target.getLocation()) > maxTradeRadius
                )
            )
            {
                sender.sendMessage("[EcoSystem] You are too far away to trade with '" + target.getDisplayName() + "'.");
                return true;
            }

            MoneySystem<?> system = null;
            ValueStore<?> amount;
            if (args.length == 3)
            {
                system = moneySystems.get(args[2]);
                if (system == null)
                {
                    sender.sendMessage("[EcoSystem] Money system '" + args[2] + "' could not be found.");
                    return true;
                }

                amount = parseRepresentation(args[1], system);

            }
            else
            {
                amount = parseRepresentation(args[1]);
            }
            if (amount == null || amount.getValue() == null)
            {
                sender.sendMessage("[EcoSystem] '" + args[1] + "' is not a recognized value.");
                return true;
            }
            if (system == null)
            {
                system = amount.moneySystem;
            }

            if (amount.getIsNothing())
            {
                sender.sendMessage("[EcoSystem] Operation ignored: That value is nothing.");
                return true;
            }
            if (amount.getIsDeficit())
            {
                sender.sendMessage("[EcoSystem] Operation ignored: That value represents a deficit.");
                return true;
            }

            ValueStore<?> balance = srcwallet.getValueStoreForMoneySystem(system);
            if (balance == null)
            {
                sender.sendMessage("[EcoSystem] Your value for money system '" + system.name + "' is not available.");
                return true;
            }

            ValueStore<?> dstbalance = dstwallet.getValueStoreForMoneySystem(system);
            if (dstbalance == null)
            {
                sender.sendMessage("[EcoSystem] '" + target.getDisplayName() + "'s value for money system '" + system.name + "' is not available.");
                return true;
            }

            Integer result = balance.compareAny(amount);
            if (result == null)
            {
                sender.sendMessage("[EcoSystem] Unable to complete operation: Amount is incompatible with your wallet.");
                return true;
            }
            if (result < 0 || !balance.subtractAny(amount.getValue()))
            {
                sender.sendMessage("[EcoSystem] You cannot remove that amount from your wallet.");
                return true;
            }
            if (!dstbalance.addAny(amount.getValue()))
            {
                sender.sendMessage("[EcoSystem] That amount could not be added to '" + target.getDisplayName() + "'s wallet.");
                if (!balance.addAny(amount.getValue()))
                {
                    log("Could not refund '" + source.getName() + "' amount '" + amount.toString() + "' after an aborted transaction.", Level.WARNING);
                    sender.sendMessage("[EcoSystem] WARNING: Could not refund your balance.");
                    if (!sender.isOp())
                    {
                        sender.sendMessage("           You should report this to an administrator.");
                    }
                }

                return true;
            }
            target.sendMessage("[EcoSystem] You received '" + amount.toString(false) + "' from '" + source.getDisplayName() + "'.");
            sender.sendMessage("[EcoSystem] Transaction successful.");
        }
        else
        {
            sender.sendMessage("[EcoSystem] You do not have permission to do that.");
        }

        return true;
    }
    private boolean cmdexecValue(CommandSender sender, String[] args)
    {
        if (primaryCurrencySystem == null)
        {
            sender.sendMessage("[EcoSystem] No primary currency system is defined.");
            return true;
        }
        if (appraiser == null)
        {
            sender.sendMessage("[EcoSystem] Item appraisal is not currently available.");
            return true;
        }

        if (sender.hasPermission("xencraft.eco.appraise"))
        {
            ArrayList<ItemStack> items = new ArrayList<ItemStack>();

            if (args.length == 0)
            {
                if (!(sender instanceof Player))
                {
                    sender.sendMessage("[EcoSystem] This command requires a player context.");
                    sender.sendMessage("            Try providing a list of items to appraise instead.");
                    return true;
                }

                ItemStack item = ((Player)sender).getItemInHand();
                if (item == null || item.getAmount() == 0)
                {
                    sender.sendMessage("[EcoSystem] You have nothing in your hand.");
                }
                items.add(item);
            }
            else
            {
                StringBuilder sbarg = new StringBuilder(args[0]);
                for (int i = 1; i < args.length; i++)
                {
                    sbarg.append(" ");
                    sbarg.append(args[i]);
                }
                String arg = sbarg.toString();

                if (arg.startsWith("["))
                {
                    if (!arg.endsWith("]"))
                    {
                        sender.sendMessage("[EcoSystem] Unable to parse item list.");
                        return true;
                    }

                    String[] blocks = arg.substring(1, arg.length() - 1).split(", ?");
                    for(int i = 0; i < blocks.length; i++)
                    {
                        ItemStack item = ItemStackUtil.parse(blocks[i]);
                        if (item == null)
                        {
                            sender.sendMessage("[EcoSystem] Could not parse item '" + blocks[i] + "'.");
                        }
                        else
                        {
                            items.add(item);
                        }
                    }
                }
                else
                {
                    ItemStack item = ItemStackUtil.parse(arg);
                    if (item == null)
                    {
                        sender.sendMessage("[EcoSystem] Could not parse item '" + arg + "'.");
                        return true;
                    }
                    items.add(item);
                }
            }

            if (items.size() == 0)
            {
                return false;
            }

            float total = 0;
            int count = 0;

            for (ItemStack item : items)
            {
                Float value;

                if (sender instanceof Player)
                {
                    value = appraiser.appraise(item, (Player)sender);
                }
                else
                {
                    value = appraiser.appraise(item);
                }
                if (value == null)
                {
                    sender.sendMessage("[EcoSystem] The value of '" + ItemStackUtil.toString(item) + "' could not be appraised.");
                    return true;
                }

                count++;
                total += value;

                ValueStore<Float> valueStore = primaryCurrencySystem.createValueStore();
                valueStore.setValue(value);

                sender.sendMessage("[EcoSystem] The value of '" + ItemStackUtil.toString(item) + "' is " + valueStore.toString(false) + ".");
            }

            if (count > 1)
            {
                ValueStore<Float> valueStore = primaryCurrencySystem.createValueStore();
                valueStore.setValue(total);

                sender.sendMessage("[EcoSystem] (" + count + " items; total value: " + valueStore.toString(false) + ")");
            }
        }
        else
        {
            sender.sendMessage("[EcoSystem] You do not have permission to do that.");
        }

        return true;
    }
    private boolean cmdexecBaseValue(CommandSender sender, String[] args)
    {
        if (args.length != 0)
        {
            return false;
        }

        if (primaryCurrencySystem == null)
        {
            sender.sendMessage("[EcoSystem] No primary currency system is defined.");
            return true;
        }
        if (appraiser == null)
        {
            sender.sendMessage("[EcoSystem] Item appraisal is not currently available.");
            return true;
        }

        if (sender.hasPermission("xencraft.eco.appraise"))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage("[EcoSystem] This command requires a player context.");
                return true;
            }

            ItemStack item = ((Player)sender).getItemInHand();
            if (item == null || item.getAmount() == 0)
            {
                sender.sendMessage("[EcoSystem] You have nothing in your hand.");
                return true;
            }

            MaterialData data = item.getData();
            item = new ItemStack(item.getTypeId(), 1, (short)0, (data == null ? 0 : data.getData()));

            Float value = appraiser.appraise(item);
            if (value == null)
            {
                sender.sendMessage("[EcoSystem] The base value of '" + ItemStackUtil.toString(item) + "' could not be appraised.");
                return true;
            }

            ValueStore<Float> valueStore = primaryCurrencySystem.createValueStore();
            valueStore.setValue(value);

            sender.sendMessage("[EcoSystem] The base value of '" + ItemStackUtil.toString(item) + "' is " + valueStore.toString(false) + ".");
        }
        else
        {
            sender.sendMessage("[EcoSystem] You do not have permission to do that.");
        }

        return true;
    }
    private boolean cmdexecGrant(CommandSender sender, String[] args)
    {
        if (args.length < 2 || args.length > 3)
        {
            return false;
        }

        if (sender.hasPermission("xencraft.eco.admin.managewallets"))
        {
            MoneySystem<?> system = null;
            ValueStore<?> amount;
            if (args.length == 3)
            {
                system = moneySystems.get(args[2]);
                if (system == null)
                {
                    sender.sendMessage("[EcoSystem] Money system '" + args[2] + "' could not be found.");
                    return true;
                }

                amount = parseRepresentation(args[1], system);
            }
            else
            {
                amount = parseRepresentation(args[1]);
            }
            if (amount == null || amount.getValue() == null)
            {
                sender.sendMessage("[EcoSystem] '" + args[1] + "' is not a recognized value.");
                return true;
            }
            if (amount.getIsNothing())
            {
                sender.sendMessage("[EcoSystem] Operation ignored: That value is nothing.");
                return true;
            }
            if (system == null)
            {
                system = amount.moneySystem;
            }

            Player player = null;
            boolean isDeficit = amount.getIsDeficit();
            if (args[0].startsWith("player."))
            {
                player = getServer().getPlayer(args[0].substring(7));
                if (
                    player != null && player != sender && isDeficit &&
                    !sender.hasPermission("xencraft.eco.admin.bypassblockmanagewallet") &&
                    player.hasPermission("xencraft.eco.admin.blockmanagewallet")
                )
                {
                    sender.sendMessage("[EcoSystem] You do not have permission to do that to '" + player.getDisplayName() + "'.");
                    return true;
                }
            }

            Wallet wallet;
            try
            {
                wallet = getWallet(args[0], null, null);
            }
            catch (IOException ex)
            {
                sender.sendMessage("[EcoSystem] Unable to complete operation: " + ex.getMessage());
                return true;
            }
            if (wallet == null)
            {
                sender.sendMessage("[EcoSystem] Could not find wallet '" + args[0] + "'.");
                return true;
            }

            ValueStore<?> walletContents = wallet.getValueStoreForMoneySystem(system);
            if (walletContents == null)
            {
                sender.sendMessage("[EcoSystem] Value of money system '" + system.name + "' for wallet '" + args[0] + "' is not available.");
                return true;
            }

            if (walletContents.addAny(amount.getValue()))
            {
                if (player != null)
                {
                    if (isDeficit)
                    {
                        player.sendMessage("[EcoSystem] You have been granted '" + amount.toString(false) + "'. (LOSS)");
                    }
                    else
                    {
                        player.sendMessage("[EcoSystem] You have been granted '" + amount.toString(false) + "'.");
                    }
                }

                if (sender != player)
                {
                    sender.sendMessage("[EcoSystem] Transaction successful.");
                }
            }
            else
            {
                sender.sendMessage("[EcoSystem] That value could not be added to wallet '" + args[0] + "'.");
            }
        }
        else
        {
            sender.sendMessage("[EcoSystem] You do not have permission to do that.");
        }

        return true;
    }
    private boolean cmdexecSetWalletContents(CommandSender sender, String[] args)
    {
        if (args.length < 2 || args.length > 3)
        {
            return false;
        }

        if (sender.hasPermission("xencraft.eco.admin.managewallets"))
        {
            MoneySystem<?> system = null;
            ValueStore<?> amount;
            if (args.length == 3)
            {
                system = moneySystems.get(args[2]);
                if (system == null)
                {
                    sender.sendMessage("[EcoSystem] Money system '" + args[2] + "' could not be found.");
                    return true;
                }

                amount = parseRepresentation(args[1], system);
            }
            else
            {
                amount = parseRepresentation(args[1]);
            }
            if (amount == null || amount.getValue() == null)
            {
                sender.sendMessage("[EcoSystem] '" + args[1] + "' is not a recognized value.");
                return true;
            }
            if (amount.getIsNothing())
            {
                sender.sendMessage("[EcoSystem] Operation ignored: That value is nothing.");
                return true;
            }
            if (system == null)
            {
                system = amount.moneySystem;
            }

            Player player = null;
            if (args[0].startsWith("player."))
            {
                player = getServer().getPlayer(args[0].substring(7));
                if (
                    player != null && player != sender &&
                    !sender.hasPermission("xencraft.eco.admin.bypassblockmanagewallet") &&
                    player.hasPermission("xencraft.eco.admin.blockmanagewallet")
                )
                {
                    sender.sendMessage("[EcoSystem] You do not have permission to do that to '" + player.getDisplayName() + "'.");
                    return true;
                }
            }

            Wallet wallet;
            try
            {
                wallet = getWallet(args[0], null, null);
            }
            catch (IOException ex)
            {
                sender.sendMessage("[EcoSystem] Unable to complete operation: " + ex.getMessage());
                return true;
            }
            if (wallet == null)
            {
                sender.sendMessage("[EcoSystem] Could not find wallet '" + args[0] + "'.");
                return true;
            }

            ValueStore<?> walletContents = wallet.getValueStoreForMoneySystem(system);
            if (walletContents == null)
            {
                sender.sendMessage("[EcoSystem] Value of money system '" + system.name + "' for wallet '" + args[0] + "' is not available.");
                return true;
            }

            if (walletContents.setAnyValue(amount.getValue()))
            {
                if (player != null)
                {
                    player.sendMessage("[EcoSystem]  Your wallet now contains '" + amount.toString(false) + "' (" + system.name  + ").");
                }

                if (sender != player)
                {
                    sender.sendMessage("[EcoSystem] Contents set successfully.");
                }
            }
            else
            {
                sender.sendMessage("[EcoSystem] The wallet's contents could not be set to that value.");
            }
        }
        else
        {
            sender.sendMessage("[EcoSystem] You do not have permission to do that.");
        }

        return true;
    }
    @SuppressWarnings("unchecked")
    private boolean cmdexecWalletInfo(CommandSender sender, String[] args)
    {
        if (args.length > 2)
        {
            return false;
        }

        if (sender.hasPermission("xencraft.eco.admin.managewallets"))
        {
            if (args.length == 0)
            {
                sender.sendMessage("[EcoSystem] Available Wallets: ");
                for (Wallet wallet : wallets)
                {
                    sender.sendMessage("   " + wallet.name);
                }
                sender.sendMessage("  (" + wallets.size() + " Wallets Total)");
            }
            else
            {
                Wallet wallet;
                try
                {
                    wallet = getWallet(args[0], null, null);
                }
                catch (IOException ex)
                {
                    sender.sendMessage("[EcoSystem] Unable to complete operation: " + ex.getMessage());
                    return true;
                }
                if (wallet == null)
                {
                    sender.sendMessage("[EcoSystem] Could not find wallet '" + args[0] + "'.");
                    return true;
                }

                if (args.length == 1)
                {
                    sender.sendMessage("[EcoSystem] Contents of wallet '" + wallet.name + "':");
                    for (MoneySystem<?> system : wallet.availableMoneySystems())
                    {
                        ValueStore<?> value = wallet.getValueStoreForMoneySystem(system);
                        if (value == null || value instanceof ItemStackMoneySystem.InventoryValueStore)
                        {
                            continue;
                        }

                        sender.sendMessage(
                            "    " + system.name + ": " +
                           (value == null ? "<Invalid>" : (value.getIsNothing() ? "(nothing)" : value.toString(!(system instanceof CurrencySystem))))
                        );
                    }
                }
                else
                {
                    MoneySystem system = moneySystems.get(args[1]);
                    if (system == null)
                    {
                        sender.sendMessage("[EcoSystem] Money system '" + args[1] + "' could not be found.");
                        return true;
                    }

                    ValueStore<?> value = wallet.getValueStoreForMoneySystem(system);
                    if (value == null || value.getValue() == null)
                    {
                        sender.sendMessage("[EcoSystem] Value of money system '" + system.name + "' for wallet '" + wallet.name + "' is not available.");
                        return true;
                    }

                    String message;
                    if (system instanceof CurrencySystem)
                    {
                        if (((CurrencySystem)system).isRepresentationUnique)
                        {
                            message = ("[EcoSystem] Balance: " + value.toString(false));
                        }
                        else
                        {
                            message = ("[EcoSystem] " + system.name + ": " + value.toString(false));
                        }
                    }
                    else
                    {
                        message = ("[EcoSystem] " + system.name + ": " + (value.getIsNothing() ? "(nothing)" : value.toString(true)));
                    }
                    sender.sendMessage(message);
                }
            }
        }
        else
        {
            sender.sendMessage("[EcoSystem] You do not have permission to do that.");
        }

        return true;
    }
    private boolean cmdexecNewWallet(CommandSender sender, String[] args)
    {
        if (args.length == 0)
        {
            return false;
        }

        if (sender.hasPermission("xencraft.eco.admin.managewallets"))
        {
            StringBuilder sbName = new StringBuilder(args[0]);
            for(int i = 1; i < args.length; ++i)
            {
                sbName.append("_");
                sbName.append(args[i]);
            }

            String name = sbName.toString();

            try
            {
                getWallet(name, null, null);
            }
            catch (IOException ex)
            {
                sender.sendMessage("[EcoSystem] Unable to complete operation: " + ex.getMessage());
                return true;
            }

            sender.sendMessage("[EcoSystem] Wallet '" + name + "' created.");
        }
        else
        {
            sender.sendMessage("[EcoSystem] You do not have permission to do that.");
        }

        return true;
    }
    private boolean cmdexecDelWallet(CommandSender sender, String[] args)
    {
        if (args.length == 0)
        {
            return false;
        }

        if (sender.hasPermission("xencraft.eco.admin.managewallets"))
        {
            StringBuilder sbName = new StringBuilder(args[0]);
            for(int i = 1; i < args.length; ++i)
            {
                sbName.append("_");
                sbName.append(args[i]);
            }

            String name = sbName.toString();
            Wallet wallet = null;
            for (Wallet candidate : wallets)
            {
                if (candidate.name.equalsIgnoreCase(name))
                {
                    wallet = candidate;
                    break;
                }
            }
            if (wallet == null)
            {
                sender.sendMessage("[EcoSystem] Wallet '" + name + "' could not be found.");
                return true;
            }

            wallets.remove(wallet);

            try
            {
                File fileWallet = new File(getDataFolder().getCanonicalPath() + "/Wallets/" + name + ".eswd");
                if (!fileWallet.delete())
                {
                    throw new IOException("Unable to delete file.");
                }
            }
            catch (IOException ex)
            {
                sender.sendMessage("[EcoSystem] Wallet '" + name + "' was disabled, but could not be deleted. Error: " + ex.getMessage());
                return true;
            }

            sender.sendMessage("[EcoSystem] Wallet '" + name + "' deleted.");
        }
        else
        {
            sender.sendMessage("[EcoSystem] You do not have permission to do that.");
        }

        return true;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (label.equals("ecoinfo"))
        {
            return cmdexecEcoInfo(sender, CommandUtil.groupArgs(args));
        }
        else if (label.equals("wallet"))
        {
            return cmdexecWallet(sender, CommandUtil.groupArgs(args));
        }
        else if (label.equals("pay"))
        {
            return cmdexecPay(sender, CommandUtil.groupArgs(args));
        }
        else if (label.equals("value"))
        {
            return cmdexecValue(sender, CommandUtil.groupArgs(args));
        }
        else if (label.equals("basevalue"))
        {
            return cmdexecBaseValue(sender, CommandUtil.groupArgs(args));
        }
        else if (label.equals("grant"))
        {
            return cmdexecGrant(sender, CommandUtil.groupArgs(args));
        }
        else if (label.equals("setwalletcontents"))
        {
            return cmdexecSetWalletContents(sender, CommandUtil.groupArgs(args));
        }
        else if (label.equals("walletinfo"))
        {
            return cmdexecWalletInfo(sender, CommandUtil.groupArgs(args));
        }
        else if (label.equals("newwallet"))
        {
            return cmdexecNewWallet(sender, args);
        }
        else if (label.equals("delwallet"))
        {
            return cmdexecDelWallet(sender, args);
        }

        return false;
    }

    public void onPluginDisable()
    {
        if (appraiser != null)
        {
            log("Disabling appraiser...", Level.FINE, 2);
            appraiser.onDisable();
        }

        if (exchanger != null)
        {
            log("Disabling exchanger...", Level.FINE, 2);
            exchanger.onDisable();
        }

        log("Saving Wallets...", Level.FINE, 2);
        try
        {
            saveWallets();
        }
        catch (IOException ex)
        {
            log("Unable to save wallets: " + ex.getMessage(), Level.SEVERE, 3);
        }

        log("Clearing caches...", Level.FINE, 2);
        wallets.clear();
        moneyConverters.clear();
        moneySystems.clear();
    }

    public Collection<CurrencySystem> getCurrencySystems()
    {
        ArrayList<CurrencySystem> systems = new ArrayList<CurrencySystem>();

        for (MoneySystem<?> system : moneySystems.values())
        {
            if (system instanceof CurrencySystem)
            {
                systems.add((CurrencySystem)system);
            }
        }

        return systems;
    }

    private boolean completeConversionPath(List<MoneyConverter<?, ?>> path, List<MoneySystem<?>> steps, MoneySystem<?> goal, int maxSteps)
    {
        if (maxSteps == 1)
        {
            return false;
        }

        MoneySystem<?> base = steps.get(steps.size() - 1);

        MoneyConverter<?, ?> candidate = getMoneyConverter(base, goal);
        if (candidate != null)
        {
            if (candidate instanceof MultiStepMoneyConverter<?,?>)
            {
                //This path is optimal.
                MultiStepMoneyConverter<?,?> route = (MultiStepMoneyConverter<?,?>)candidate;
                path.addAll(route.converters);
                if (route.converters.size() >= maxSteps)
                {
                    return false;
                }
            }
            else
            {
                path.add(candidate);
            }

            steps.add(goal);
            return true;
        }

        int bestLength = maxSteps;
        List<MoneyConverter<?, ?>> bestPath = null;
        List<MoneySystem<?>> bestSteps = null;

        for(Map.Entry<MoneySystem<?>, MoneyConverter<?, ?>> converter : moneyConverters.get(base).entrySet())
        {
            if (converter.getValue() instanceof MultiStepMoneyConverter<?,?>)
            {
                continue;
            }

            boolean visited = false;
            for (MoneySystem<?> step : steps)
            {
                if (converter == step)
                {
                    visited = true;
                    break;
                }
            }
            if (visited)
            {
                continue;
            }

            ArrayList<MoneyConverter<?, ?>> candidatePath = new ArrayList<MoneyConverter<?, ?>>();
            ArrayList<MoneySystem<?>> candidateSteps = new ArrayList<MoneySystem<?>>();
            candidatePath.add(converter.getValue());
            candidateSteps.add(converter.getKey());

            if (completeConversionPath(candidatePath, candidateSteps, goal, bestLength - 1))
            {
                bestLength = candidatePath.size();
                bestPath = candidatePath;
                bestSteps = candidateSteps;
            }
        }

        if (bestPath == null)
        {
            return false;
        }

        path.addAll(bestPath);
        steps.addAll(bestSteps);
        return true;
    }

    @SuppressWarnings("unchecked")
    private <A, B> MoneyConverter<A, B> getMoneyConverter(MoneySystem<A> systema, MoneySystem<B> systemb)
    {
        Hashtable<MoneySystem<?>, MoneyConverter<?, ?>> aConverter = moneyConverters.get(systema);
        if (aConverter == null)
        {
            return null;
        }
        MoneyConverter<?, ?> converter = aConverter.get(systemb);
        if (converter == null)
        {
            return null;
        }

        //Because java does type erasure, we cannot ever verify that the converter actually converts A to B, but it has been organized such that it should.
        return (MoneyConverter<A, B>)converter;
    }

    @SuppressWarnings("unchecked")      //Type erasure strikes again! (see getMoneyConverter)
    public <A, B> MoneyConverter<A, B> findMoneyConverter(MoneySystem<A> systema, MoneySystem<B> systemb)
    {
        MoneyConverter<A, B> converter = getMoneyConverter(systema, systemb);
        if (converter != null)
        {
            return converter;
        }

        ArrayList<MoneyConverter<?, ?>> path = new ArrayList<MoneyConverter<?, ?>>();
        ArrayList<MoneySystem<?>> steps = new ArrayList<MoneySystem<?>>();
        steps.add(systema);

        if (!completeConversionPath(path, steps, systemb, -1))
        {
            return null;
        }

        converter = new MultiStepMoneyConverter<A, B>(path);
        moneyConverters.get(systema).put(systemb, converter);
        return converter;
    }

    public <A, B> B convert(A a, MoneySystem<A> systema, MoneySystem<B> systemb)
    {
        MoneyConverter<A, B> converter = findMoneyConverter(systema, systemb);
        if (converter == null)
        {
            return null;
        }

        return converter.convert(a);
    }
    public <A, B> B convert(ValueStore<A> value, MoneySystem<B> system)
    {
        return convert(value.getValue(), value.moneySystem, system);
    }

    public <A, B> ValueStore<B> convertToValueStore(A a, MoneySystem<A> systema, MoneySystem<B> systemb)
    {
        ValueStore<B> store = systemb.createValueStore();
        store.setValue(convert(a, systema, systemb));
        return store;
    }
    public <A, B> ValueStore<B> convertToValueStore(ValueStore<A> value, MoneySystem<B> system)
    {
        ValueStore<B> store = system.createValueStore();
        store.setValue(convert(value, system));
        return store;
    }

    public MoneySystem<?> findRepresentedMoneySystem(String rep)
    {
        for(MoneySystem<?> system : moneySystems.values())
        {
            if (system.isRepresentationUnique && system.isValidRepresentation(rep))
            {
                return system;
            }
        }

        MoneySystem<?> candidate = null;

        for (MoneySystem<?> system : moneySystems.values())
        {
            if (!system.isRepresentationUnique && system.isValidRepresentation(rep))
            {
                if (candidate != null)
                {
                    return null;    //Ambiguous representation.
                }

                candidate = system;
            }
        }

        return candidate;
    }

    public <A> ValueStore<A> parseRepresentation(String rep, MoneySystem<A> system)
    {
        if (system == null)
        {
            return null;
        }

        ValueStore<A> store = system.createValueStore();
        return (store.setValue(system.parseRepresentation(rep)) ? store : null);
    }

    public ValueStore<?> parseRepresentation(String rep)
    {
        return parseRepresentation(rep, findRepresentedMoneySystem(rep));
    }

    public <A,B> boolean addValueTo(ValueStore<A> a, ValueStore<B> b)
    {
        A aval = convert(b, a.moneySystem);

        return (aval != null && a.addValue(aval));

    }
    public <A,B> boolean subtractValueFrom(ValueStore<A> a, ValueStore<B> b)
    {
        A aval = convert(b, a.moneySystem);
        return (aval != null && a.subtractValue(aval));
    }

    public Wallet getWallet(String name, Collection<ValueStore<?>> defValues, Collection<MoneySystem<?>> defSystems) throws IOException
    {
        Wallet wallet = null;

        for (Wallet candidate : wallets)
        {
            if (candidate.name.equalsIgnoreCase(name))
            {
                wallet = candidate;
                break;
            }
        }

        if (wallet == null)
        {
            File fileWallet = new File(getDataFolder().getCanonicalPath() + "/Wallets/" + name + ".eswd");
            if (fileWallet.exists() && fileWallet.isFile())
            {
                ObjectInputStream datain = new ObjectInputStream(new FileInputStream(fileWallet));
                try
                {
                    Serializable walletdata = (Serializable)datain.readObject();
                    wallet = new Wallet(name, walletdata, this);
                }
                catch (ClassNotFoundException ex)
                {
                    log("Unable to load data for wallet '" + name + "'.", Level.WARNING);
                    log("Message: " + ex.getMessage(), Level.FINE, 1);
                    log("A a module may be out ouf date.", Level.INFO, 1);
                    return null;
                }
                finally
                {
                    datain.close();
                }
            }
            else
            {
                wallet = new Wallet(name);
            }

            wallets.add(wallet);
        }

        if (defSystems != null)
        {
            for (MoneySystem<?> defSystem : defSystems)
            {
                wallet.initForMoneySystem(defSystem);
            }
        }
        if (defValues != null)
        {
            for(ValueStore<?> defValue : defValues)
            {
                wallet.initDefault(defValue);
            }
        }

        return wallet;
    }

    private Map<MoneySystem<?>, ConfigurationNode> loadMoneySystems(List<ConfigurationNode> moneysystems)
    {
        Hashtable<MoneySystem<?>, ConfigurationNode> initEntries = new Hashtable<MoneySystem<?>, ConfigurationNode>();

        for (ConfigurationNode moneysystem : moneysystems)
        {
            MoneySystemFactory moneySystemFactory;
            String factoryArchive = moneysystem.getString("factoryarchive");
            String factoryClass = moneysystem.getString("factoryclass");
            log("FactoryClass: '" + factoryClass + " (" + (factoryArchive == null ? "<Runtime>" : ("'" + factoryArchive + "'")) + ")", Level.FINER, 2);
            try
            {
                if (factoryArchive == null)
                {
                    moneySystemFactory = ModuleUtil.loadClass(factoryClass);
                }
                else
                {
                    moneySystemFactory = ModuleUtil.loadClassFromJar(factoryClass, factoryArchive);
                }
                if (moneySystemFactory == null)
                {
                    throw new Exception("MoneySystemFactory instance was null.");
                }
            }
            catch (Throwable ex)
            {
                log("Unable to load MoneySystem: " + ex.getMessage(), Level.SEVERE, 2);
                continue;
            }

            log("Instantiating MoneySystem with MoneySystemFactory...", Level.FINE, 2);
            MoneySystem<?> moneySystem = moneySystemFactory.getMoneySystem(this, moneysystem.getNode("factoryconfig"));
            if (moneySystem == null)
            {
                log("Unable to load MoneySystem: MoneySystem instance was null.", Level.SEVERE, 2);
                continue;
            }

            log("Registering MoneySystem...", Level.FINE, 2);
            moneySystems.put(moneySystem.name, moneySystem);

            log("Registering self converter...", Level.FINE, 2);
            moneyConverters.put(moneySystem, new Hashtable<MoneySystem<?>, MoneyConverter<?, ?>>());
            moneyConverters.get(moneySystem).put(moneySystem, moneySystem.getSelfConverter());

            log("Adding init entry...", Level.FINEST, 2);
            initEntries.put(moneySystem, moneysystem.getNode("init"));

            log("Completed loading MoneySystem: '" + moneySystem.name + "'", 2);
        }
        log("Completed loading " + moneySystems.size() + " MoneySystems.", 2);

        return initEntries;
    }

    private void loadMoneyConverters(List<ConfigurationNode> moneyconverters)
    {
        for (ConfigurationNode moneyconverter : moneyconverters)
        {
            MoneyConverterFactory moneyConverterFactory;
            String factoryArchive = moneyconverter.getString("factoryarchive");
            String factoryClass = moneyconverter.getString("factoryclass");
            log("FactoryClass: '" + factoryClass + " (" + (factoryArchive == null ? "<Runtime>" : ("'" + factoryArchive + "'")) + ")", Level.FINE, 2);
            try
            {
                if (factoryArchive == null)
                {
                    moneyConverterFactory = ModuleUtil.loadClass(factoryClass);
                }
                else
                {
                    moneyConverterFactory = ModuleUtil.loadClassFromJar(factoryClass, factoryArchive);
                }
                if (moneyConverterFactory == null)
                {
                    throw new Exception("MoneyConverterFactory instance was null.");
                }
            }
            catch (Throwable ex)
            {
                log("Unable to load MoneyConverters from MoneyConverterFactory: " + ex.getMessage(), Level.SEVERE, 2);
                continue;
            }

            log("Loading MoneyConverters from MoneyConverterFactory...", Level.FINER, 2);
            for(ConfigurationNode convert : moneyconverter.getNodeList("converts", null))
            {
                String fromname = convert.getString("from");
                String toname = convert.getString("to");

                if (!moneySystems.containsKey(fromname))
                {
                    log("Unable to load converter from '" + fromname + "' to '" + toname + "': 'from' MoneySystem was not found.", Level.SEVERE, 2);
                    continue;
                }
                if (!moneySystems.containsKey(toname))
                {
                    log("Unable to load converter from '" + fromname + "' to '" + toname + "': 'to' MoneySystem was not found.", Level.SEVERE, 2);
                    continue;
                }

                MoneySystem<?> from = moneySystems.get(fromname);
                MoneySystem<?> to = moneySystems.get(toname);

                MoneyConverter<?, ?> converter = moneyConverterFactory.getMoneyConverter(this, convert.getNode("factoryconfig"), from, to);
                if (converter == null)
                {
                    log("Unable to load converter from '" + fromname + "' to '" + toname + "': MoneyConverterFactory offered no converter.", Level.WARNING, 2);
                    continue;
                }

                log("Registering MoneyConverter...", Level.FINE, 2);
                if (!moneyConverters.containsKey(from))
                {
                    moneyConverters.put(from, new Hashtable<MoneySystem<?>, MoneyConverter<?, ?>>());
                }
                moneyConverters.get(from).put(to, converter);

                log("Completed loading MoneyConverter from '" + fromname + "' to '" + toname + "'.", 2);
            }
            log("Completed loading MoneyConverters from MoneyConverterFactory.", Level.FINER, 2);
        }
        log("Completed loading all MoneyConverters.", 2);
    }

    private void saveWallets() throws IOException
    {
        String walletsStorePath = (this.getDataFolder().getCanonicalPath() + "/Wallets/");
        File walletsStore = new File(walletsStorePath);
        if (!walletsStore.exists() && !walletsStore.mkdirs())
        {
            log("Unable to create wallets store directory '" + walletsStorePath + "'.", Level.SEVERE, 3);
            return;
        }

        for(Wallet wallet : wallets)
        {
            log("Saving wallet '" + wallet.name + "'...", Level.FINER, 3);
            String walletStorePath = (walletsStorePath + wallet.name + ".eswd");
            File walletStore = new File(walletStorePath);
            if (!walletStore.exists() && !walletStore.createNewFile())
            {
                log("Unable to create wallet store '" + walletStorePath + "' for wallet '" + wallet.name + "'.", Level.SEVERE, 4);
                continue;
            }

            ObjectOutputStream objout = new ObjectOutputStream(new FileOutputStream(walletStore, false));
            objout.writeObject(wallet.getData());
            objout.close();
        }

        log("Finished saving " + wallets.size() + " wallets.", Level.FINE, 3);
    }
}