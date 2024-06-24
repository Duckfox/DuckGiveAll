package com.duckfox.duckgiveall;

import com.duckfox.duckapi.managers.ConfigManager;
import com.duckfox.duckapi.managers.MessageManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class DuckGiveAll extends JavaPlugin {
    private static DuckGiveAll instance;
    private static MessageManager messageManager;
    private static ConfigManager configManager;

    public static MessageManager getMessageManager() {
        return messageManager;
    }

    public static ConfigManager getConfigManager() {
        return configManager;
    }

    public static DuckGiveAll getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginCommand("duckgiveall").setExecutor(new DGACommand());

        // Managers
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this, "messages.yml", configManager.getString("prefix"));

    }

    public void reload(){
        configManager = new ConfigManager(this);
        messageManager = new MessageManager(this, "messages.yml", configManager.getString("prefix"));
    }
    @Override
    public void onDisable() {
    }
}
