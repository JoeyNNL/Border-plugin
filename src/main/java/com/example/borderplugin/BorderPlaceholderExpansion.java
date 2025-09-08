package com.example.borderplugin;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class BorderPlaceholderExpansion extends PlaceholderExpansion {
    private final BorderPlugin plugin;

    public BorderPlaceholderExpansion(BorderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "borderplugin";
    }

    @Override
    public @NotNull String getAuthor() {
        return "jouwnaam";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (identifier.equalsIgnoreCase("size")) {
            return String.valueOf(plugin.getBorderSize());
        }
        return null;
    }
}
