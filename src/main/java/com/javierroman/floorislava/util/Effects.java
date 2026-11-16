package com.javierroman.floorislava.util;

import com.javierroman.floorislava.FloorIsLava;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

/** Sonidos, títulos y action bar configurables. */
public final class Effects {

    private final FloorIsLava plugin;

    public Effects(FloorIsLava plugin) {
        this.plugin = plugin;
    }

    public void sound(Player player, String event) {
        sound(Collections.singletonList(player), event);
    }

    public void sound(Collection<? extends Player> players, String event) {
        FileConfiguration cfg = plugin.getConfig();
        if (!cfg.getBoolean("sounds.enabled", true)) return;
        ConfigurationSection s = cfg.getConfigurationSection("sounds.events." + event);
        if (s == null || !s.getBoolean("enabled", true)) return;
        String name = s.getString("sound", "");
        float volume = (float) s.getDouble("volume", 1.0);
        float pitch = (float) s.getDouble("pitch", 1.0);
        for (Player p : players) {
            playByName(p, name, volume, pitch);
        }
    }

    /** Reproduce un sonido por nombre (p. ej. UI_BUTTON_CLICK); ignora nombres no válidos. */
    public void playByName(Player p, String name, float volume, float pitch) {
        if (name == null || name.isEmpty()) return;
        // Clave con namespace (p. ej. "entity.ender_dragon.growl"): funciona en cualquier versión.
        if (name.indexOf('.') >= 0 || name.indexOf(':') >= 0) {
            p.playSound(p.getLocation(), name, volume, pitch);
            return;
        }
        try {
            Sound sound = Sound.valueOf(name.toUpperCase(Locale.ROOT));
            p.playSound(p.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException | LinkageError ignored) {
            // sonido inexistente en esta versión
        }
    }

    public void title(Player player, String event, Object... kv) {
        title(Collections.singletonList(player), event, kv);
    }

    public void title(Collection<? extends Player> players, String event, Object... kv) {
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("titles." + event);
        if (s == null || !s.getBoolean("enabled", true)) return;
        String title = Messages.color(Messages.apply(s.getString("title", ""), kv));
        String subtitle = Messages.color(Messages.apply(s.getString("subtitle", ""), kv));
        int in = s.getInt("fade-in", 5);
        int stay = s.getInt("stay", 40);
        int out = s.getInt("fade-out", 10);
        for (Player p : players) {
            p.sendTitle(title, subtitle, in, stay, out);
        }
    }

    public void actionBar(Player player, String text) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(text));
    }
}
