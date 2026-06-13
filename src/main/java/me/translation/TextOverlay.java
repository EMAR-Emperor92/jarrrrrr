package me.translation;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class TextOverlay extends JavaPlugin implements Listener, CommandExecutor {
    private LanguageManager languageManager;
    private final Map<UUID, String> playerLanguages = new HashMap<>();
    private final Set<String> detectedMessages = new HashSet<>();

    @Override
    public void onEnable() {
        this.languageManager = new LanguageManager(this);
        
        getCommand("language").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        
        setupPacketListener();
        
        getLogger().info("TextOverlay v1.1 enabled! Discovery mode active.");
    }

    private void setupPacketListener() {
        ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(this, PacketType.Play.Server.SYSTEM_CHAT, PacketType.Play.Server.CHAT) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                String originalText = getPacketText(event);
                if (originalText == null) return;

                // 1. Discovery Mode: Log unknown texts
                if (!languageManager.isTextRegistered(originalText)) {
                    logDetectedText(originalText);
                }

                // 2. Translation Layer
                String lang = playerLanguages.getOrDefault(player.getUniqueId(), "english");
                String translatedText = languageManager.translate(lang, originalText);

                if (!originalText.equals(translatedText)) {
                    setPacketText(event, translatedText);
                }
            }
        });
    }

    private void logDetectedText(String text) {
        if (detectedMessages.contains(text)) return;
        detectedMessages.add(text);
        
        try (PrintWriter out = new PrintWriter(new FileWriter(getDataFolder() + "/detected_texts.txt", true))) {
            out.println(text);
            out.println("----------------------------------");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) return true;
        Player player = (Player) sender;

        if (args.length >= 2 && args[0].equalsIgnoreCase("select")) {
            String lang = args[1].toLowerCase();
            if (lang.equals("persian") || lang.equals("english")) {
                playerLanguages.put(player.getUniqueId(), lang);
                player.sendMessage(ChatColor.GREEN + "زبان شما به " + (lang.equals("persian") ? "فارسی" : "انگلیسی") + " تغییر یافت.");
                return true;
            }
        }
        player.sendMessage(ChatColor.RED + "Usage: /language select <persian|english>");
        return true;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;

        if (item.getItemMeta().getDisplayName().contains("Language Switcher")) {
            Player player = event.getPlayer();
            if (event.getAction() == Action.LEFT_CLICK) {
                playerLanguages.put(player.getUniqueId(), "persian");
                player.sendMessage(ChatColor.GOLD + "زبان: فارسی");
                event.setCancelled(true);
            } else if (event.getAction() == Action.RIGHT_CLICK) {
                playerLanguages.put(player.getUniqueId(), "english");
                player.sendMessage(ChatColor.GOLD + "Language: English");
                event.setCancelled(true);
            }
        }
    }

    // Helper method to give the language switcher item
    public void giveLanguageItem(Player player) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Language Switcher");
        meta.setLore(Collections.singletonList(ChatColor.GRAY + "Left Click: Persian | Right Click: English"));
        item.setItemMeta(meta);
        player.getInventory().addItem(item);
    }

    private String getPacketText(PacketEvent event) {
        try {
            Object content = event.getPacket().getFields().read(0);
            return content != null ? content.toString() : null;
        } catch (Exception e) { return null; }
    }

    private void setPacketText(PacketEvent event, String newText) {
        try {
            event.getPacket().getFields().write(0, newText);
        } catch (Exception e) { e.printStackTrace(); }
    }
}