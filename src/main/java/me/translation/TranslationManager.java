package me.translation;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class TranslationManager {
    private final JavaPlugin plugin;
    private final Map<String, String> translationMap = new HashMap<>();

    public TranslationManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadTranslations();
    }

    public void loadTranslations() {
        translationMap.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("translations");
        if (section != null) {
            Set<String> keys = section.getKeys(false);
            for (String key : keys) {
                String value = section.getString(key);
                translationMap.put(key, colorize(value));
            }
        }
    }

    public String translate(String original) {
        // Exact match
        if (translationMap.containsKey(original)) {
            return translationMap.get(original);
        }

        // Partial match (Check if original contains any of our keys)
        for (Map.Entry<String, String> entry : translationMap.entrySet()) {
            if (original.contains(entry.getKey())) {
                return original.replace(entry.getKey(), entry.getValue());
            }
        }

        return original;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}