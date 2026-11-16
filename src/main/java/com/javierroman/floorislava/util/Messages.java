package com.javierroman.floorislava.util;

import com.javierroman.floorislava.FloorIsLava;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/** Mensajes configurables (config.yml -> messages.*). Los marcadores se escriben como {clave}. */
public final class Messages {

    private final FloorIsLava plugin;

    public Messages(FloorIsLava plugin) {
        this.plugin = plugin;
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    /** Sustituye pares clave/valor: apply("Hola {x}", "x", 5) -> "Hola 5". */
    public static String apply(String s, Object... kv) {
        for (int i = 0; i + 1 < kv.length; i += 2) {
            s = s.replace("{" + kv[i] + "}", String.valueOf(kv[i + 1]));
        }
        return s;
    }

    public String get(String key, Object... kv) {
        String s = plugin.getConfig().getString("messages." + key);
        if (s == null) s = "&c[mensaje no encontrado: " + key + "]";
        return color(apply(s, kv));
    }

    public String prefixed(String key, Object... kv) {
        return get("prefix") + get(key, kv);
    }

    public void send(CommandSender to, String key, Object... kv) {
        to.sendMessage(prefixed(key, kv));
    }

    public List<String> list(String key) {
        List<String> out = new ArrayList<>();
        for (String s : plugin.getConfig().getStringList("messages." + key)) {
            out.add(color(s));
        }
        return out;
    }
}
