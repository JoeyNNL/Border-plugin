
package com.example.borderplugin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Level;



public class BorderPlugin extends JavaPlugin implements Listener {
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
            player.sendMessage("§cJe mag niet buiten de border!");
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
    private String broadcastMessage;

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
        broadcastMessage = config.getString("border.broadcast_message", "§aDe wereldborder is vergroot naar %size% blokken!");
        pregenChunks = config.getBoolean("pregen_chunks", false);
        pregenChunksPerTick = config.getInt("pregen_chunks_per_tick", 10);
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

    private void scheduleBorderGrowth() {
        new BukkitRunnable() {
            @Override
            public void run() {
                LocalTime now = LocalTime.now();
                LocalTime target = LocalTime.parse(growTime, DateTimeFormatter.ofPattern("HH:mm"));
                long delay = java.time.Duration.between(now, target).getSeconds();
                if (delay < 0) delay += 24 * 60 * 60;
                Bukkit.getScheduler().runTaskLater(BorderPlugin.this, () -> {
                    DayOfWeek today = LocalDate.now().getDayOfWeek();
                    if (growDays.isEmpty() || growDays.contains(today.toString())) {
                        growBorder();
                    }
                }, delay * 20);
            }
        }.runTaskTimer(this, 0, 20 * 60 * 60); // Check elk uur
    }

    private void growBorder() {
        borderSize += growAmount;
        getConfig().set("border.size", borderSize);
        saveConfig();
        setWorldBorder();
        if (broadcast) {
            String msg = broadcastMessage.replace("%size%", String.valueOf(borderSize));
            Bukkit.broadcastMessage(msg);
        }
        if (logToFile) {
            logToFile("Border vergroot naar " + borderSize + " blokken op " + LocalDate.now() + ".");
        }
        getLogger().info("Wereldborder vergroot naar " + borderSize + " blokken.");
    }

    private void logToFile(String message) {
        try (FileWriter fw = new FileWriter(getDataFolder() + "/border.log", true)) {
            fw.write("[" + java.time.LocalDateTime.now() + "] " + message + "\n");
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Kon niet loggen naar border.log", e);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("border")) return false;
        if (args.length == 0) {
            sender.sendMessage("§eGebruik /border help voor opties.");
            return true;
        }
        if (args[0].equalsIgnoreCase("log")) {
            if (!sender.hasPermission("border.admin")) {
                sender.sendMessage("§cGeen permissie.");
                return true;
            }
            java.io.File logFile = new java.io.File(getDataFolder(), "border.log");
            if (!logFile.exists()) {
                sender.sendMessage("§cGeen border.log bestand gevonden.");
                return true;
            }
            try {
                java.util.List<String> lines = java.nio.file.Files.readAllLines(logFile.toPath());
                int start = Math.max(0, lines.size() - 10);
                sender.sendMessage("§eLaatste 10 regels van border.log:");
                for (int i = start; i < lines.size(); i++) {
                    sender.sendMessage("§7" + lines.get(i));
                }
            } catch (Exception e) {
                sender.sendMessage("§cFout bij lezen van border.log: " + e.getMessage());
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("bypass")) {
            if (!sender.hasPermission("border.admin")) {
                sender.sendMessage("§cGeen permissie.");
                return true;
            }
            if (args.length == 1 && sender instanceof Player) {
                Player p = (Player) sender;
                if (bypassPlayers.contains(p.getName())) {
                    bypassPlayers.remove(p.getName());
                    sender.sendMessage("§aJe kunt niet langer buiten de border.");
                } else {
                    bypassPlayers.add(p.getName());
                    sender.sendMessage("§aJe kunt nu buiten de border.");
                }
                return true;
            } else if (args.length == 2) {
                String target = args[1];
                if (bypassPlayers.contains(target)) {
                    bypassPlayers.remove(target);
                    sender.sendMessage("§a" + target + " kan niet langer buiten de border.");
                } else {
                    bypassPlayers.add(target);
                    sender.sendMessage("§a" + target + " kan nu buiten de border.");
                }
                return true;
            } else {
                sender.sendMessage("§cGebruik: /border bypass [speler]");
                return true;
            }
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("border.admin")) {
                sender.sendMessage("§cGeen permissie.");
                return true;
            }
            reloadConfig();
            reloadSettings();
            setWorldBorder();
            sender.sendMessage("§aBorder config herladen!");
            return true;
        }
        if (args[0].equalsIgnoreCase("set")) {
            if (!sender.hasPermission("border.admin")) {
                sender.sendMessage("§cGeen permissie.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cGebruik: /border set <grootte>");
                return true;
            }
            try {
                int newSize = Integer.parseInt(args[1]);
                borderSize = newSize;
                getConfig().set("border.size", borderSize);
                saveConfig();
                setWorldBorder();
                sender.sendMessage("§aBorder grootte gezet op " + borderSize);
                if (logToFile) logToFile(sender.getName() + " zette border op " + borderSize);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cGeen geldig getal.");
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("add")) {
            if (!sender.hasPermission("border.admin")) {
                sender.sendMessage("§cGeen permissie.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cGebruik: /border add <aantal>");
                return true;
            }
            try {
                int add = Integer.parseInt(args[1]);
                borderSize += add;
                getConfig().set("border.size", borderSize);
                saveConfig();
                setWorldBorder();
                sender.sendMessage("§aBorder vergroot met " + add + " naar " + borderSize);
                if (logToFile) logToFile(sender.getName() + " vergrootte border met " + add);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cGeen geldig getal.");
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("remove")) {
            if (!sender.hasPermission("border.admin")) {
                sender.sendMessage("§cGeen permissie.");
                return true;
            }
            if (args.length < 2) {
                sender.sendMessage("§cGebruik: /border remove <aantal>");
                return true;
            }
            try {
                int remove = Integer.parseInt(args[1]);
                borderSize -= remove;
                getConfig().set("border.size", borderSize);
                saveConfig();
                setWorldBorder();
                sender.sendMessage("§aBorder verkleind met " + remove + " naar " + borderSize);
                if (logToFile) logToFile(sender.getName() + " verkleinde border met " + remove);
            } catch (NumberFormatException e) {
                sender.sendMessage("§cGeen geldig getal.");
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("center")) {
            if (!sender.hasPermission("border.admin")) {
                sender.sendMessage("§cGeen permissie.");
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
                    sender.sendMessage("§aBorder center gezet op " + centerX + ", " + centerZ);
                    if (logToFile) logToFile(sender.getName() + " zette border center op " + centerX + ", " + centerZ);
                } catch (NumberFormatException e) {
                    sender.sendMessage("§cGebruik: /border center <x> <z>");
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
                    sender.sendMessage("§aBorder center gezet op jouw locatie: " + centerX + ", " + centerZ);
                    if (logToFile) logToFile(sender.getName() + " zette border center op eigen locatie: " + centerX + ", " + centerZ);
                } else {
                    sender.sendMessage("§cAlleen spelers kunnen dit gebruiken.");
                }
            } else {
                sender.sendMessage("§cGebruik: /border center <x> <z> of /border center hier");
            }
            return true;
        }
        if (args[0].equalsIgnoreCase("info")) {
            sender.sendMessage("§eBorder info:");
            sender.sendMessage("§7Grootte: §f" + borderSize);
            sender.sendMessage("§7Center: §f" + centerX + ", " + centerZ);
            sender.sendMessage("§7Volgende groei: §f" + growAmount + " om " + growTime);
            sender.sendMessage("§7Animatie: §f" + (growAnimated ? (growAnimatedSeconds + "s") : "uit"));
            sender.sendMessage("§7Broadcast: §f" + (broadcast ? "aan" : "uit"));
            sender.sendMessage("§7Log naar bestand: §f" + (logToFile ? "aan" : "uit"));
            return true;
        }
        if (args[0].equalsIgnoreCase("help")) {
            sender.sendMessage("§eBorder commando's:");
            sender.sendMessage("§7/border reload - herlaad config");
            sender.sendMessage("§7/border set <grootte> - zet border grootte");
            sender.sendMessage("§7/border add <aantal> - vergroot border");
            sender.sendMessage("§7/border remove <aantal> - verklein border");
            sender.sendMessage("§7/border center <x> <z> - zet center");
            sender.sendMessage("§7/border center hier - zet center op jouw locatie");
            sender.sendMessage("§7/border info - info over border");
            sender.sendMessage("§7/border log - bekijk de laatste 10 regels van border.log");
            sender.sendMessage("§7/border bypass [speler] - admin kan zichzelf of anderen buiten de border laten bewegen");
            return true;
        }
        sender.sendMessage("§cOnbekend commando. Gebruik /border help.");
        return true;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadSettings();
        setWorldBorder();
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
            new BorderPlaceholderExpansion(this);
            getLogger().info("PlaceholderAPI hook geregistreerd: %borderplugin_size% beschikbaar.");
        }
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
        getLogger().info("Chunks te genereren: " + totalChunks);
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
                    getLogger().info("Chunks gepregeneerd: " + done + "/" + totalChunks);
                }
                if (cx > chunkMaxX) {
                    getLogger().info("Alle chunks binnen de border zijn gepregeneerd.");
                    cancel();
                }
            }
        }.runTaskTimer(this, 1L, 1L);
    }

    // Tab completion voor /border commando
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

    // Plaats hier de overige methodes (setWorldBorder, scheduleBorderGrowth, growBorder, logToFile, onCommand, etc.)
    // Zorg dat alle code binnen deze class staat en dat er geen code buiten de class-definitie staat.
}
