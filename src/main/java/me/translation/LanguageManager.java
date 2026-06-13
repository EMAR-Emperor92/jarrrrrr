package me.translation;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class LanguageManager {
    private final JavaPlugin plugin;
    private final Map<String, Map<String, Map<String, String>>> languageData = new HashMap<>();

    public LanguageManager(JavaPlugin plugin) {
        this.plugin = plugin;
        setupPluginFolders();
        loadAllLanguages();
    }

    private void setupPluginFolders() {
        String[] langs = {"persian", "english"};
        
        // Get all installed plugins
        Plugin[] allPlugins = Bukkit.getPluginManager().getPlugins();

        for (String lang : langs) {
            File langDir = new File(plugin.getDataFolder(), "languages/" + lang);
            if (!langDir.exists()) langDir.mkdirs();

            for (Plugin p : allPlugins) {
                // Create a dedicated YML file for each plugin
                File pluginFile = new File(langDir, p.getName() + ".yml");
                if (!pluginFile.exists()) {
                    try {
                        pluginFile.createNewFile();
                        // Initialize with a comment
                        FileConfiguration config = YamlConfiguration.loadConfiguration(pluginFile);
                        config.set("info", "Translations for " + p.getName());
                        config.save(pluginFile);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        
        // Create a global file for unidentified texts
        File unidentified = new File(plugin.getDataFolder(), "unidentified_texts.yml");
        if (!unidentified.exists()) {
            try {
                unidentified.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadAllLanguages() {
        languageData.clear();
        File langRoot = new File(plugin.getDataFolder(), "languages");
        if (!langRoot.exists()) return;

        for (File langFolder : langRoot.listFiles(File::isDirectory)) {
            String langName = langFolder.getName();
            Map<String, Map<String, String>> pluginMaps = new HashMap<>();

            File[] pluginFiles = langFolder.listFiles((dir, name) -> name.endsWith(".yml"));
            if (pluginFiles != null) {
                for (File pluginFile : pluginFiles) {
                    String pluginName = pluginFile.getName().replace(".yml", "");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(pluginFile);
                    Map<String, String> translations = new HashMap<>();
                    
                    for (String key : config.getKeys(false)) {
                        if (!key.equals("info")) {
                            translations.put(key, colorize(config.getString(key)));
                        }
                    }
                    pluginMaps.put(pluginName, translations);
                }
            }
            languageData.put(langName, pluginMaps);
        }
    }

    public String translate(String lang, String originalText) {
        if (!languageData.containsKey(lang)) return originalText;

        Map<String, Map<String, String>> pluginMaps = languageData.get(lang);
        for (Map<String, String> translations : pluginMaps.values()) {
            if (translations.containsKey(originalText)) {
                return translations.get(originalText);
            }
            for (Map.Entry<String, String> entry : translations.entrySet()) {
                if (originalText.contains(entry.getKey())) {
                    return originalText.replace(entry.getKey(), entry.getValue());
                }
            }
        }
        return originalText;
    }

    public boolean isTextRegistered(String text) {
        for (Map<String, Map<String, String>> pluginMaps : languageData.values()) {
            for (Map<String, String> translations : pluginMaps.values()) {
                if (translations.containsKey(text)) return true;
            }
        }
        return false;
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}