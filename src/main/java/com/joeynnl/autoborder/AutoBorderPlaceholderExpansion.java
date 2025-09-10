package com.joeynnl.autoborder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class AutoBorderPlaceholderExpansion extends PlaceholderExpansion {
    private final AutoBorderPlugin plugin;

    public AutoBorderPlaceholderExpansion(AutoBorderPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "autoborder";
    }

    @Override
    public @NotNull String getAuthor() {
        return "JoeyNNL";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.1";
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (identifier.equalsIgnoreCase("size")) {
            return String.valueOf(plugin.getBorderSize());
        }
        return null;
    }
}
