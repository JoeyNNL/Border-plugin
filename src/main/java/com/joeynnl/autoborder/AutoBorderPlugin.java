package com.joeynnl.autoborder;

import org.bukkit.Bukkit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import java.time.Duration;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

public class AutoBorderPlugin extends JavaPlugin implements Listener {
    private static final Pattern AMPERSAND_COLOR_PATTERN = Pattern.compile("(&[0-9a-fk-orA-FK-OR])|(&#[0-9a-fA-F]{6})");
    public int getBorderSize() {
        return borderSize;
    }
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (bypassPlayers.contains(player.getName())) return;
        WorldBorder border = player.getWorld().getWorldBorder();
        double x = event.getTo().getX();
        double z = event.getTo().getZ();
        double size = border.getSize() / 2.0;
        double cx = border.getCenter().getX();
        double cz = border.getCenter().getZ();
        if (x < cx - size || x > cx + size || z < cz - size || z > cz + size) {
            event.setTo(event.getFrom());
            // Send MiniMessage-based warning from messages.yml (with optional prefix)
            String mm = msgPrefix + msgNotAllowedOutside;
            player.sendMessage(parse(mm));
        }
    }
    private final Set<String> bypassPlayers = new HashSet<>();
    private boolean pregenChunks;
    private int pregenChunksPerTick;
    private int borderSize;
    private int centerX;
    private int centerZ;
    private int growAmount;
    private String growTime;
    private List<String> growDays;
    private boolean growAnimated;
    private int growAnimatedSeconds;
    private boolean broadcast;
    private boolean logToFile;
    private boolean broadcastTitleEnabled;
    private int broadcastTitleFadeIn;
    private int broadcastTitleStay;
    private int broadcastTitleFadeOut;
    private boolean soundEnabled;
    private String soundName;
    private float soundVolume;
    private float soundPitch;
    private BukkitTask borderGrowTask;
    // Shrink settings
    private double shrinkChancePercent;
    private int shrinkAmount;
    private boolean shrinkBroadcast;
    private boolean shrinkSoundEnabled;
    private String shrinkSoundName;
    private float shrinkSoundVolume;
    private float shrinkSoundPitch;
    // Limits
    private int minBorderSize;

    // Messages.yml handling
    private File messagesFile;
    private FileConfiguration messagesCfg;
    private String msgPrefix;
    private String msgGrowChat;
    private String msgGrowTitleMain;
    private String msgGrowTitleSub;
    private String msgShrinkChat;
    private String msgShrinkTitleMain;
    private String msgShrinkTitleSub;
    private String msgNotAllowedOutside;

    private void reloadSettings() {
        FileConfiguration config = getConfig();
        borderSize = config.getInt("border.size", 1000);
        centerX = config.getInt("border.center_x", 0);
        centerZ = config.getInt("border.center_z", 0);
        growAmount = config.getInt("border.grow_amount", 100);
        growTime = config.getString("border.grow_time", "20:00");
        growDays = config.getStringList("border.grow_days");
        growAnimated = config.getBoolean("border.grow_animated", true);
        growAnimatedSeconds = config.getInt("border.grow_animated_seconds", 10);
    broadcast = config.getBoolean("border.broadcast", true);
    logToFile = config.getBoolean("border.log_to_file", true);
        pregenChunks = config.getBoolean("pregen_chunks", false);
        pregenChunksPerTick = config.getInt("pregen_chunks_per_tick", 10);
        broadcastTitleEnabled = config.getBoolean("border.broadcast_title_enabled", false);
        broadcastTitleFadeIn = config.getInt("border.broadcast_title_fadein", 10);
        broadcastTitleStay = config.getInt("border.broadcast_title_stay", 60);
        broadcastTitleFadeOut = config.getInt("border.broadcast_title_fadeout", 10);
        soundEnabled = config.getBoolean("border.sound_enabled", false);
        soundName = config.getString("border.sound", "ENTITY_PLAYER_LEVELUP");
        soundVolume = (float) config.getDouble("border.sound_volume", 1.0);
        soundPitch = (float) config.getDouble("border.sound_pitch", 1.0);
        // Shrink
        shrinkChancePercent = config.getDouble("border.shrink_chance_percent", 0.0);
        shrinkAmount = config.getInt("border.shrink_amount", 0);
        shrinkBroadcast = config.getBoolean("border.shrink_broadcast", true);
        shrinkSoundEnabled = config.getBoolean("border.shrink_sound_enabled", false);
        shrinkSoundName = config.getString("border.shrink_sound", "ENTITY_ENDERMAN_TELEPORT");
        shrinkSoundVolume = (float) config.getDouble("border.shrink_sound_volume", 1.0);
        shrinkSoundPitch = (float) config.getDouble("border.shrink_sound_pitch", 1.0);
        // Limits
        minBorderSize = config.getInt("border.min_size", 16);
    }

    private void loadMessages() {
        // Ensure default file exists
        if (messagesFile == null) {
            messagesFile = new File(getDataFolder(), "messages.yml");
        }
        if (!messagesFile.exists()) {
            try { saveResource("messages.yml", false); } catch (IllegalArgumentException ignored) { }
        }
        messagesCfg = YamlConfiguration.loadConfiguration(messagesFile);
        // Defaults
        msgPrefix = messagesCfg.getString("messages.prefix", "<yellow>[</yellow><green>Border</green><yellow>]</yellow> ");
        msgGrowChat = messagesCfg.getString("messages.grow.chat", "<green>The world border has been increased to <red>%size%</red> blocks!</green>");
        msgGrowTitleMain = messagesCfg.getString("messages.grow.title_main", "<green>Border increased!</green>");
        msgGrowTitleSub = messagesCfg.getString("messages.grow.title_sub", "<yellow>New size:</yellow> <red>%size%</red> <yellow>blocks</yellow>");
        msgShrinkChat = messagesCfg.getString("messages.shrink.chat", "<red>The world border has been decreased to <yellow>%size%</yellow> blocks!</red>");
        msgShrinkTitleMain = messagesCfg.getString("messages.shrink.title_main", "<red>Border decreased!</red>");
        msgShrinkTitleSub = messagesCfg.getString("messages.shrink.title_sub", "<yellow>New size:</yellow> <red>%size%</red> <yellow>blocks</yellow>");
        msgNotAllowedOutside = messagesCfg.getString("messages.general.not_allowed_outside", "<red>You are not allowed outside the border!</red>");
    }

    private Component parse(String text) {
        if (text == null) return Component.empty();
        // Auto-detect ampersand/section color codes; otherwise treat as MiniMessage
        if (text.indexOf('§') >= 0 || AMPERSAND_COLOR_PATTERN.matcher(text).find()) {
            return LegacyComponentSerializer.legacyAmpersand().deserialize(text);
        }
        return MiniMessage.miniMessage().deserialize(text);
    }

    private void setWorldBorder() {
        World world = Bukkit.getWorlds().get(0);
        WorldBorder border = world.getWorldBorder();
        border.setCenter(centerX, centerZ);
        if (growAnimated) {
            border.setSize(borderSize, growAnimatedSeconds);
        } else {
            border.setSize(borderSize);
        }
    }

    private void cancelBorderGrowTask() {
        if (borderGrowTask != null) {
            borderGrowTask.cancel();
            borderGrowTask = null;
        }
    }

    private void scheduleBorderGrowth() {
        cancelBorderGrowTask();
        getLogger().info("[DEBUG] scheduleBorderGrowth() called at: " + java.time.LocalDateTime.now() + ", growTime=" + growTime);
        LocalTime now = LocalTime.now();
        LocalTime target = LocalTime.parse(growTime, DateTimeFormatter.ofPattern("HH:mm"));
        long delay = java.time.Duration.between(now, target).getSeconds();
        if (delay <= 0) delay += 24 * 60 * 60; 
        LocalDateTime nextGrow = LocalDateTime.now().plusSeconds(delay);
        getLogger().info("[DEBUG] Scheduling grow at: " + nextGrow + " (in " + delay + " seconds)");
        borderGrowTask = new BukkitRunnable() {
            @Override
            public void run() {
                DayOfWeek today = LocalDate.now().getDayOfWeek();
                getLogger().info("[DEBUG] runTaskLater triggered at: " + java.time.LocalDateTime.now() + ", day=" + today);
                if (growDays.isEmpty() || growDays.contains(today.toString())) {
                    // Decide shrink vs grow based on configured chance
                    double roll = ThreadLocalRandom.current().nextDouble(0.0, 100.0);
                    getLogger().info("[DEBUG] Daily roll=" + roll + ", shrinkChancePercent=" + shrinkChancePercent);
                    if (shrinkAmount > 0 && roll < shrinkChancePercent) {
                        shrinkBorder();
                    } else {
                        growBorder();
                    }
                } else {
                    getLogger().info("[DEBUG] Today is not a grow day, skipping border growth.");
                }
                scheduleBorderGrowth();
            }
        }.runTaskLater(this, delay * 20);
    }

    private void growBorder() {
        getLogger().info("[DEBUG] growBorder() called at: " + java.time.LocalDateTime.now() + ", borderSize=" + borderSize + ", growAmount=" + growAmount);
        borderSize += growAmount;
        getConfig().set("border.size", borderSize);
        saveConfig();
        setWorldBorder();
        if (broadcast) {
            String msg = (msgPrefix + msgGrowChat).replace("%size%", String.valueOf(borderSize));
            Component comp = parse(msg);
            Bukkit.getServer().sendMessage(comp);
        }
        if (broadcastTitleEnabled) {
            String main = msgGrowTitleMain.replace("%size%", String.valueOf(borderSize));
            String sub = msgGrowTitleSub.replace("%size%", String.valueOf(borderSize));
            Component mainComp = parse(main);
            Component subComp = parse(sub);
            Title title = Title.title(
                mainComp,
                subComp,
                Title.Times.times(
                    Duration.ofMillis(broadcastTitleFadeIn * 50L),
                    Duration.ofMillis(broadcastTitleStay * 50L),
                    Duration.ofMillis(broadcastTitleFadeOut * 50L)
                )
            );
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showTitle(title);
            }
        }
        if (soundEnabled) {
            try {
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), sound, soundVolume, soundPitch);
                }
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid sound in config: " + soundName);
            }
        }
        if (logToFile) {
            logToFile("Border increased to " + borderSize + " blocks on " + LocalDate.now() + ".");
        }
        getLogger().info("World border increased to " + borderSize + " blocks.");
    }

    private void shrinkBorder() {
        getLogger().info("[DEBUG] shrinkBorder() called at: " + java.time.LocalDateTime.now() + ", borderSize=" + borderSize + ", shrinkAmount=" + shrinkAmount);
        int newSize = borderSize - shrinkAmount;
        // Clamp to configured minimum size
        if (newSize < minBorderSize) {
            getLogger().warning("Shrink would reduce border below min_size (" + minBorderSize + "); clamping to min_size.");
            newSize = minBorderSize;
        }
        borderSize = newSize;
        getConfig().set("border.size", borderSize);
        saveConfig();
        setWorldBorder();
        if (shrinkBroadcast) {
            String msg = (msgPrefix + msgShrinkChat).replace("%size%", String.valueOf(borderSize));
            Component comp = parse(msg);
            Bukkit.getServer().sendMessage(comp);
        }
        if (broadcastTitleEnabled) {
            String main = msgShrinkTitleMain.replace("%size%", String.valueOf(borderSize));
            String sub = msgShrinkTitleSub.replace("%size%", String.valueOf(borderSize));
            Component mainComp = parse(main);
            Component subComp = parse(sub);
            Title title = Title.title(
                mainComp,
                subComp,
                Title.Times.times(
                    Duration.ofMillis(broadcastTitleFadeIn * 50L),
                    Duration.ofMillis(broadcastTitleStay * 50L),
                    Duration.ofMillis(broadcastTitleFadeOut * 50L)
                )
            );
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showTitle(title);
            }
        }
        if (shrinkSoundEnabled) {
            try {
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(shrinkSoundName);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.playSound(p.getLocation(), sound, shrinkSoundVolume, shrinkSoundPitch);
                }
            } catch (IllegalArgumentException e) {
                getLogger().warning("Invalid shrink sound in config: " + shrinkSoundName);
            }
        }
        if (logToFile) {
            logToFile("Border decreased to " + borderSize + " blocks on " + LocalDate.now() + ".");
        }
        getLogger().info("World border decreased to " + borderSize + " blocks.");
    }

    private void logToFile(String message) {
        try (FileWriter fw = new FileWriter(getDataFolder() + "/border.log", true)) {
            fw.write("[" + java.time.LocalDateTime.now() + "] " + message + "\n");
        } catch (IOException e) {
                getLogger().log(Level.WARNING, "Could not log to border.log", e);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("border")) return false;
        if (args.length == 0) {
            sender.sendMessage("§eUse /border help for options.");
            return true;
        }
        if (args[0].equalsIgnoreCase("log")) {
            if (!sender.hasPermission("border.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            java.io.File logFile = new java.io.File(getDataFolder(), "border.log");
            if (!logFile.exists()) {
                sender.sendMessage("§cNo border.log file found.");
                return true;
            }
            try {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(logFile.toPath());
                int start = Math.max(0, lines.size() - 10);
                sender.sendMessage("§eLast 10 lines of border.log:");
                for (int i = start; i < lines.size(); i++) {
                    sender.sendMessage("§7" + lines.get(i));
                }
            } catch (Exception e) {
                sender.sendMessage("§cError reading border.log: " + e.getMessage());
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("bypass")) {
            if (!sender.hasPermission("border.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            if (args.length == 1 && sender instanceof Player) {
                Player p = (Player) sender;
                if (bypassPlayers.contains(p.getName())) {
                    bypassPlayers.remove(p.getName());
                    sender.sendMessage("§aYou can no longer move outside the border.");
                } else {
                    bypassPlayers.add(p.getName());
                    sender.sendMessage("§aYou can now move outside the border.");
                }
                return true;
            } else if (args.length == 2) {
                String target = args[1];
                if (bypassPlayers.contains(target)) {
                    bypassPlayers.remove(target);
                    sender.sendMessage("§a" + target + " can no longer move outside the border.");
                } else {
                    bypassPlayers.add(target);
                    sender.sendMessage("§a" + target + " can now move outside the border.");
                }
                return true;
            } else {
                sender.sendMessage("§cUsage: /border bypass [player]");
                return true;
            }
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("border.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            reloadConfig();
            reloadSettings();
            loadMessages();
            setWorldBorder();
            cancelBorderGrowTask();
            scheduleBorderGrowth();
            sender.sendMessage("§aBorder config reloaded!");
            return true;
        }
        if (args[0].equalsIgnoreCase("set")) {
            if (!sender.hasPermission("border.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /border set <size>");
                return true;
            }
            try {
                int newSize = Integer.parseInt(args[1]);
                if (newSize < minBorderSize) {
                    newSize = minBorderSize;
                    sender.sendMessage("§eValue was below min_size; clamped to " + minBorderSize + ".");
                }
                borderSize = newSize;
                getConfig().set("border.size", borderSize);
                saveConfig();
                setWorldBorder();
                sender.sendMessage("§aBorder size set to " + borderSize);
                if (logToFile) logToFile(sender.getName() + " set border to " + borderSize);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cNot a valid number.");
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("add")) {
            if (!sender.hasPermission("border.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /border add <amount>");
                return true;
            }
            try {
                int add = Integer.parseInt(args[1]);
                borderSize += add;
                getConfig().set("border.size", borderSize);
                saveConfig();
                setWorldBorder();
                sender.sendMessage("§aBorder increased by " + add + " to " + borderSize);
                if (logToFile) logToFile(sender.getName() + " increased border by " + add);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cNot a valid number.");
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("remove")) {
            if (!sender.hasPermission("border.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /border remove <amount>");
                return true;
            }
            try {
                int remove = Integer.parseInt(args[1]);
                borderSize -= remove;
                if (borderSize < minBorderSize) {
                    borderSize = minBorderSize;
                    sender.sendMessage("§eResult was below min_size; clamped to " + minBorderSize + ".");
                }
                getConfig().set("border.size", borderSize);
                saveConfig();
                setWorldBorder();
                sender.sendMessage("§aBorder decreased by " + remove + " to " + borderSize);
                if (logToFile) logToFile(sender.getName() + " decreased border by " + remove);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cNot a valid number.");
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("center")) {
            if (!sender.hasPermission("border.admin")) {
                sender.sendMessage("§cNo permission.");
                return true;
            }
            if (args.length == 3) {
                try {
                    centerX = Integer.parseInt(args[1]);
                    centerZ = Integer.parseInt(args[2]);
                    getConfig().set("border.center_x", centerX);
                    getConfig().set("border.center_z", centerZ);
                    saveConfig();
                    setWorldBorder();
                    sender.sendMessage("§aBorder center set to " + centerX + ", " + centerZ);
                    if (logToFile) logToFile(sender.getName() + " set border center to " + centerX + ", " + centerZ);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cUsage: /border center <x> <z>");
                }
            } else if (args.length == 2 && args[1].equalsIgnoreCase("hier")) {
                if (sender instanceof Player) {
                    Player p = (Player) sender;
                    centerX = p.getLocation().getBlockX();
                    centerZ = p.getLocation().getBlockZ();
                    getConfig().set("border.center_x", centerX);
                    getConfig().set("border.center_z", centerZ);
                    saveConfig();
                    setWorldBorder();
                    sender.sendMessage("§aBorder center set to your location: " + centerX + ", " + centerZ);
                    if (logToFile) logToFile(sender.getName() + " set border center to own location: " + centerX + ", " + centerZ);
                } else {
                    sender.sendMessage("§cOnly players can use this.");
                }
            } else {
                sender.sendMessage("§cUsage: /border center <x> <z> or /border center hier");
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("info")) {
            sender.sendMessage("§eBorder info:");
            sender.sendMessage("§7Size: §f" + borderSize);
            sender.sendMessage("§7Center: §f" + centerX + ", " + centerZ);
            sender.sendMessage("§7Next growth: §f" + growAmount + " at " + growTime);
            sender.sendMessage("§7Animation: §f" + (growAnimated ? (growAnimatedSeconds + "s") : "off"));
            sender.sendMessage("§7Broadcast: §f" + (broadcast ? "on" : "off"));
            sender.sendMessage("§7Log to file: §f" + (logToFile ? "on" : "off"));
            return true;
        }
        if (args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§eBorder commands:");
            sender.sendMessage("§7/border reload - reload config");
            sender.sendMessage("§7/border set <size> - set border size");
            sender.sendMessage("§7/border add <amount> - increase border");
            sender.sendMessage("§7/border remove <amount> - decrease border");
            sender.sendMessage("§7/border center <x> <z> - set center");
            sender.sendMessage("§7/border center hier - set center to your location");
            sender.sendMessage("§7/border info - info about border");
            sender.sendMessage("§7/border log - view last 10 lines of border.log");
            sender.sendMessage("§7/border bypass [player] - admin can allow self or others to move outside the border");
            return true;
        }
        sender.sendMessage("§cUnknown command. Use /border help.");
        return true;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        // Export messages.yml to the plugin data folder (for future message customization)
        try { saveResource("messages.yml", false); } catch (IllegalArgumentException ignored) { }
        reloadSettings();
        loadMessages();
        setWorldBorder();
        cancelBorderGrowTask();
        scheduleBorderGrowth();
        getCommand("border").setExecutor(this);
        getCommand("border").setTabCompleter(this);
        Bukkit.getPluginManager().registerEvents(this, this);
        if (pregenChunks) {
            getLogger().info("Start met pre-genereren van chunks binnen de border...");
            pregenAllChunks();
        }
        // PlaceholderAPI support
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new AutoBorderPlaceholderExpansion(this);
            getLogger().info("PlaceholderAPI hook geregistreerd: %autoborder_size% beschikbaar.");
        }
    }

    @Override
    public void onDisable() {
        cancelBorderGrowTask();
    }

    private void pregenAllChunks() {
        World world = Bukkit.getWorlds().get(0);
        int minX = centerX - borderSize / 2;
        int maxX = centerX + borderSize / 2;
        int minZ = centerZ - borderSize / 2;
        int maxZ = centerZ + borderSize / 2;

        int chunkMinX = minX >> 4;
        int chunkMaxX = maxX >> 4;
        int chunkMinZ = minZ >> 4;
        int chunkMaxZ = maxZ >> 4;
        int totalChunks = (chunkMaxX - chunkMinX + 1) * (chunkMaxZ - chunkMinZ + 1);
            getLogger().info("Chunks to generate: " + totalChunks);
        new BukkitRunnable() {
            int cx = chunkMinX;
            int cz = chunkMinZ;
            int done = 0;
            @Override
            public void run() {
                int count = 0;
                while (count < pregenChunksPerTick && cx <= chunkMaxX) {
                    world.getChunkAt(cx, cz).load(true);
                    done++;
                    cz++;
                    if (cz > chunkMaxZ) {
                        cz = chunkMinZ;
                        cx++;
                    }
                    count++;
                }
                    if (done % 100 == 0 || cx > chunkMaxX) {
                        getLogger().info("Chunks pregenerated: " + done + "/" + totalChunks);
                    }
                    if (cx > chunkMaxX) {
                        getLogger().info("All chunks within the border have been pregenerated.");
                        cancel();
                    }
            }
        }.runTaskTimer(this, 1L, 1L);
    }

    // Tab completion for /border command
    public java.util.List<String> onTabComplete(org.bukkit.command.CommandSender sender, org.bukkit.command.Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("border")) return java.util.Collections.emptyList();
        java.util.List<String> completions = new java.util.ArrayList<>();
        if (args.length == 1) {
            java.util.List<String> sub = java.util.Arrays.asList("reload", "set", "add", "remove", "center", "info", "help", "bypass", "log");
            for (String s : sub) {
                if (s.startsWith(args[0].toLowerCase())) completions.add(s);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("bypass")) {
            for (org.bukkit.entity.Player p : getServer().getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[1].toLowerCase())) completions.add(p.getName());
            }
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("center")) {
            if (args.length == 2) completions.add("hier");
        }
        return completions;
    }
}
