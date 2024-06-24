package com.duckfox.duckgiveall;

import com.duckfox.duckapi.utils.ComponentUtil;
import com.duckfox.duckapi.utils.NMSUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DGACommand implements CommandExecutor {
    public static final Pattern MATERIAL_PATTERN = Pattern.compile("^(-material|-m):(?<material>.*)");
    public static final Pattern AMOUNT_PATTERN = Pattern.compile("^(-amount|-a):(?<amount>.*)");
    public static final Pattern DATA_PATTERN = Pattern.compile("^(-data|-d):(?<data>.*)");
    public static final Pattern NBT_PATTERN = Pattern.compile("^(-nbt):(?<nbt>.*)", Pattern.DOTALL);

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] args) {
        if (commandSender.isOp()) {
            boolean notice = !Arrays.asList(args).contains("-f");
            if (args.length >= 1 && args[0].equalsIgnoreCase("reload")) {
                DuckGiveAll.getInstance().reload();
                DuckGiveAll.getMessageManager().sendMessage(commandSender, "reload");
                return false;
            }
            if (args.length >= 1 && args[0].equalsIgnoreCase("help")) {
                DuckGiveAll.getMessageManager().sendMessages(commandSender, "help");
                return false;
            }
            if (args.length >= 1 && args[0].equalsIgnoreCase("giveall")) {
                giveAll(commandSender, args, notice);
                return false;
            }
            //TODO: 单个玩家给予
            if (args.length >= 1 && args[0].equalsIgnoreCase("give")) {
            }
            DuckGiveAll.getMessageManager().sendMessage(commandSender,"unknownCommand","%command%",args.length >= 1 ? args[0] : "");
        }
        return false;
    }


    private List<ItemStack> getItems(CommandSender sender, String[] args) {
        if (sender instanceof Player && (args.length == 1 || (args.length >= 2 && args[1].matches("(?i)%main_hand%|%off_hand%|%both_hands%")))) {
            Player player = (Player) sender;
            ArrayList<ItemStack> itemStacks = new ArrayList<>(2);
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (args.length == 1) {
                if (mainHand != null && mainHand.getType() != Material.AIR) {
                    itemStacks.add(mainHand.clone());
                } else {
                    DuckGiveAll.getMessageManager().sendMessage(player, "nothing_in_main_hand");
                    return null;
                }
            } else {
                switch (args[1].toLowerCase()) {
                    case "%main_hand%":
                        if (mainHand != null && mainHand.getType() != Material.AIR) {
                            itemStacks.add(mainHand.clone());
                        } else {
                            DuckGiveAll.getMessageManager().sendMessage(player, "nothing_in_main_hand");
                            return null;
                        }
                        break;
                    case "%off_hand%":
                        if (offHand != null && offHand.getType() != Material.AIR) {
                            itemStacks.add(offHand.clone());
                        } else {
                            DuckGiveAll.getMessageManager().sendMessage(player, "nothing_in_off_hand");
                            return null;
                        }
                        break;
                    case "%both_hands%":
                        if (mainHand != null&& mainHand.getType() != Material.AIR) {
                            itemStacks.add(mainHand.clone());
                        } else {
                            DuckGiveAll.getMessageManager().sendMessage(player, "nothing_in_main_hand");
                            return null;
                        }
                        if (offHand != null && offHand.getType() != Material.AIR) {
                            itemStacks.add(offHand.clone());
                        } else {
                            DuckGiveAll.getMessageManager().sendMessage(player, "nothing_in_off_hand");
                            return null;
                        }
                        break;
                }
            }
            return itemStacks;
        } else {
            ItemStack itemStack = parseItem(sender, args);
            if (itemStack == null || itemStack.getType() == Material.AIR){
                DuckGiveAll.getMessageManager().sendMessage(sender,"no_item_data");
                return null;
            }
            return Collections.singletonList(itemStack);
        }
    }

    private void giveAll(CommandSender sender, String[] args, boolean notice) {
        List<ItemStack> itemStacks = getItems(sender, args);
        if (itemStacks == null) {
            return;
        }
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    giveItem(sender, onlinePlayer, itemStacks, notice);
                }
                sendMessageWithItem(sender, "give_item_to_all", itemStacks);
            }
        }.runTaskLaterAsynchronously(DuckGiveAll.getInstance(), 0);
    }

    private ItemStack parseItem(CommandSender sender, String[] args) {
        Material material = Material.AIR;
        int amount = 1;
        byte data = 0;
        NBTTagCompound nbt = null;
        for (String s : args) {
            if (s.matches(MATERIAL_PATTERN.pattern())) {
                Matcher matcher = MATERIAL_PATTERN.matcher(s);
                if (matcher.find()) {
                    String group = matcher.group("material");
                    if (group != null && !group.isEmpty()) {
                        Material matched = Material.matchMaterial(group);
                        if (matched != null) {
                            material = matched;
                        } else {
                            DuckGiveAll.getMessageManager().sendMessage(sender, "material_invalid", "%material%", group);
                            return null;
                        }
                    } else {
                        DuckGiveAll.getMessageManager().sendMessage(sender, "material_invalid", "%material%", group);
                        return null;
                    }
                }
            } else if (s.matches(AMOUNT_PATTERN.pattern())) {
                Matcher matcher = AMOUNT_PATTERN.matcher(s);
                if (matcher.find()) {
                    String group = matcher.group("amount");
                    if (group != null && !group.isEmpty()) {
                        if (NumberUtils.isNumber(group) && Integer.parseInt(group) > 0) {
                            amount = Integer.parseInt(group);
                        } else {
                            DuckGiveAll.getMessageManager().sendMessage(sender, "amount_invalid", "%amount%", group);
                            return null;
                        }
                    }
                }
            } else if (s.matches(DATA_PATTERN.pattern())) {
                Matcher matcher = DATA_PATTERN.matcher(s);
                if (matcher.find()) {
                    String group = matcher.group("data");
                    if (group != null && !group.isEmpty()) {
                        if (NumberUtils.isNumber(group) && Integer.parseInt(group) > 0) {
                            data = Byte.parseByte(group);
                        } else {
                            DuckGiveAll.getMessageManager().sendMessage(sender, "data_invalid", "%data%", group);
                            return null;
                        }
                    }
                }
            } else if (s.matches(NBT_PATTERN.pattern())) {

                Matcher matcher = NBT_PATTERN.matcher(s);
                if (matcher.find()) {
                    String group = matcher.group("nbt");
                    if (group != null && !group.isEmpty()) {
                        try {
                            nbt = JsonToNBT.func_180713_a(group);
                        } catch (NBTException e) {
                            DuckGiveAll.getMessageManager().sendMessage(sender, "nbt_invalid", "%nbt%", group);
                            return null;
                        }
                    }
                }
            }
        }
        ItemStack itemStack = new ItemStack(material, amount, (short) 0, data);
        if (nbt != null) {
            net.minecraft.item.ItemStack nmsItemStack = NMSUtil.BKTToNMSItemStack(itemStack);
            nmsItemStack.func_77982_d(nbt);
            itemStack = NMSUtil.NMSToBKTItemStack(nmsItemStack);
        }
        return itemStack;
    }

    private void giveItem(CommandSender giver, Player player, List<ItemStack> itemStacks, boolean notice) {
        player.getInventory().addItem(itemStacks.toArray(new ItemStack[0]));
        if (notice) {
            sendMessageWithItem(player, "claim_item", itemStacks, "%giver%", giver.getName());
        }
    }

    private void sendMessageWithItem(CommandSender commandSender, String key, List<ItemStack> itemStacks, String... args) {
        BaseComponent itemInfo = getItemInfo(itemStacks);
        BaseComponent component = ComponentUtil.toComponent(DuckGiveAll.getMessageManager().getString(key, false),
                new ComponentUtil.ComponentNode("item", itemInfo));
        if (commandSender instanceof Player) {
            DuckGiveAll.getMessageManager().sendMessage((Player) commandSender, component, args);
        } else {
            DuckGiveAll.getMessageManager().sendMessage(commandSender, component.toLegacyText(), args);
        }
    }

    private BaseComponent getItemInfo(List<ItemStack> itemStacks) {
        BaseComponent base = new TextComponent("");
        for (ItemStack stack : itemStacks) {
            String itemName = (stack.getItemMeta().hasDisplayName() ? stack.getItemMeta().getDisplayName(): stack.getType().name());
            TextComponent component = new TextComponent(DuckGiveAll.getMessageManager().getString("item_style", false, "%item%", itemName,"%amount%", String.valueOf(stack.getAmount())));
            String itemNBT = getItemNBT(stack);

            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new BaseComponent[]{new TextComponent(itemNBT)}));
            base.addExtra(component);
        }
        return base;
    }

    private String getItemNBT(ItemStack itemStack) {
        if (itemStack != null && itemStack.getType() != Material.AIR) {
            return NMSUtil.BKTToNMSItemStack(itemStack).func_77955_b(new NBTTagCompound()).toString();
        }
        return "{id:\"minecraft:air\",Count:1b,Damage:0s}";
    }
}
