    // ...existing code...
package com.example.borderplugin;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class BorderPlaceholderExpansion extends PlaceholderExpansion {
    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }
    private final BorderPlugin plugin;

    public BorderPlaceholderExpansion(BorderPlugin plugin) {
        this.plugin = plugin;
        register();
    }

    @Override
    public @NotNull String getIdentifier() {
        return "borderplugin";
    }

    @Override
    public @NotNull String getAuthor() {
        return "JoeyNNL";
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
