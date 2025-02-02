package xyz.skaerf.yesssirbox;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.skaerf.yesssirbox.cmds.ShopCommand;

import java.util.*;
import java.util.List;

public class Events implements Listener {

    private static final List<Material> helmetList = new ArrayList<>();
    private static final List<Material> chestplateList = new ArrayList<>();
    private static final List<Material> leggingsList = new ArrayList<>();
    private static final List<Material> bootsList = new ArrayList<>();

    private static HashMap<Player, Integer> spamCheck = new HashMap<>();
    private static HashMap<Player, String> lastMessage = new HashMap<>();
    public static HashMap<Player, Long> lastBlockBroken = new HashMap<>();

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player && event.getDamager() instanceof Player) {
            int countDamaged = 0;
            int countDefender = 0;
            ((Player) event.getEntity()).updateInventory();
            ((Player) event.getDamager()).updateInventory();
            for (ItemStack i : ((Player)event.getEntity()).getInventory()) {
                if (i != null) {
                    if (helmetList.contains(i.getType())) countDamaged++;
                    if (chestplateList.contains(i.getType())) countDamaged++;
                    if (leggingsList.contains(i.getType())) countDamaged++;
                    if (bootsList.contains(i.getType())) countDamaged++;
                }
            }
            for (ItemStack i : ((Player)event.getDamager()).getInventory()) {
                if (i != null) {
                    if (helmetList.contains(i.getType())) countDefender++;
                    if (chestplateList.contains(i.getType())) countDefender++;
                    if (leggingsList.contains(i.getType())) countDefender++;
                    if (bootsList.contains(i.getType())) countDefender++;
                }
            }
            if (countDefender < 4) {
                event.getDamager().sendMessage(ChatColor.RED + "You don't have any armor - you can't hit that person!");
                event.setCancelled(true);
            }
            if (countDamaged < 4) {
                event.getDamager().sendMessage(ChatColor.RED+"That person does not have a full set of armor - you cannot hit them!");
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        List<String> bounties = Yesssirbox.getPlugin(Yesssirbox.class).getConfig().getStringList("bounties");
        for (String line : bounties) {
            if (UUID.fromString(line.split(":")[0]).equals(event.getEntity().getUniqueId())) {
                double amount = Double.parseDouble(line.split(":")[1]);
                if (amount < 0) return;
                EconomyResponse res = Yesssirbox.econ.depositPlayer(event.getEntity().getKiller(), amount);
                if (res.transactionSuccess()) {
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        online.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lyesssirbox &8&l>> &aThe bounty on "+event.getPlayer().getName()+" for $"+amount+" has been claimed by "+event.getPlayer().getKiller().getName()+"!"));
                    }
                    bounties.remove(line);
                    Yesssirbox.getPlugin(Yesssirbox.class).getConfig().set("bounties", bounties);
                    Yesssirbox.getPlugin(Yesssirbox.class).saveConfig();
                    Yesssirbox.getPlugin(Yesssirbox.class).reloadConfig();
                }
                else {
                    Yesssirbox.getPlugin(Yesssirbox.class).getLogger().warning("Could not deposit bounty money - "+res.errorMessage);
                }
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        long time = System.currentTimeMillis();
        try {
            double value = Yesssirbox.blockValues.get(event.getBlock().getType());
            if (!player.getGameMode().equals(GameMode.SURVIVAL) && !player.getGameMode().equals(GameMode.ADVENTURE)) return;
            if ((time - lastBlockBroken.get(player)) <= 500) value = value/2;
            if (!event.isCancelled()) {
                EconomyResponse res = Yesssirbox.econ.depositPlayer(player, value);
                if (res.transactionSuccess()) {
                    Yesssirbox.updateActionBar(player, value);
                }
                else {
                    Yesssirbox.getPlugin(Yesssirbox.class).getLogger().warning("Could not deposit money into "+player.getName()+"'s account - "+res.errorMessage);
                }
            }
        }
        catch (NullPointerException ignored) {}
        lastBlockBroken.remove(player);
        lastBlockBroken.put(player, time);
    }

    @EventHandler
    public void onInvInteract(InventoryClickEvent event) {
        if (event.getCurrentItem() == null) {return;}
        Set<Material> shulkerBoxes = Tag.SHULKER_BOXES.getValues();
        if (event.getView().title().equals(ShopCommand.getShopInvName())) {
            ShopCommand.inventoryClick(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChat(AsyncChatEvent event) {
        /*
        Player player = event.getPlayer();
        String msg = ((TextComponent)event.message()).content().replace(" ", "").replaceAll("\\p{Punct}", "");
        checkSpam(player, msg);
        if (spamCheck.get(player) >= 3) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED+"Please do not spam. You have sent the same or very similar messages three times in a row - you will now be muted for one hour.");
            spamCheck.remove(player);
            spamCheck.put(player, 0);
            new BukkitRunnable() {
                @Override
                public void run() {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "tempmute "+player.getName()+" 1h Auto: Spam");
                }
            }.runTask(Yesssirbox.getPlugin(Yesssirbox.class));
        }
        for (String word : Yesssirbox.getBlockedWords()) {
            if (msg.toUpperCase().contains(word.toUpperCase())) {
                player.sendMessage(ChatColor.RED+"Please reconsider what you have typed - it contains blocked language. Be considerate to your fellow players and do not say anything that could offend someone or cause hurt.");
                event.setCancelled(true);
            }
        }*/
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        spamCheck.put(event.getPlayer(), 0);
    }

    private void checkSpam(Player player, String newMessage) {
        if (lastMessage.get(player) == null) {
            lastMessage.put(player, newMessage);
            return;
        }
        if (lastMessage.get(player).equals(newMessage)) {
            if (spamCheck.get(player) != null) {
                spamCheck.put(player, spamCheck.get(player)+1);
                return;
            }
            spamCheck.put(player, 1);
        }
        lastMessage.put(player, newMessage);
    }

    public static void fillArmorLists() {
        helmetList.add(Material.CHAINMAIL_HELMET);
        helmetList.add(Material.DIAMOND_HELMET);
        helmetList.add(Material.IRON_HELMET);
        helmetList.add(Material.GOLDEN_HELMET);
        helmetList.add(Material.LEATHER_HELMET);
        helmetList.add(Material.NETHERITE_HELMET);
        helmetList.add(Material.TURTLE_HELMET);

        chestplateList.add(Material.CHAINMAIL_CHESTPLATE);
        chestplateList.add(Material.DIAMOND_CHESTPLATE);
        chestplateList.add(Material.IRON_CHESTPLATE);
        chestplateList.add(Material.GOLDEN_CHESTPLATE);
        chestplateList.add(Material.LEATHER_CHESTPLATE);
        chestplateList.add(Material.NETHERITE_CHESTPLATE);
        chestplateList.add(Material.ELYTRA);

        leggingsList.add(Material.CHAINMAIL_LEGGINGS);
        leggingsList.add(Material.DIAMOND_LEGGINGS);
        leggingsList.add(Material.IRON_LEGGINGS);
        leggingsList.add(Material.GOLDEN_LEGGINGS);
        leggingsList.add(Material.LEATHER_LEGGINGS);
        leggingsList.add(Material.NETHERITE_LEGGINGS);

        bootsList.add(Material.CHAINMAIL_BOOTS);
        bootsList.add(Material.DIAMOND_BOOTS);
        bootsList.add(Material.IRON_BOOTS);
        bootsList.add(Material.GOLDEN_BOOTS);
        bootsList.add(Material.LEATHER_BOOTS);
        bootsList.add(Material.NETHERITE_BOOTS);
    }

}
