package com.javierroman.floorislava.arena;

import com.javierroman.floorislava.FloorIsLava;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Registro de arenas (arenas.yml) y selección de arena por usuario. */
public class ArenaManager {

    private final FloorIsLava plugin;
    private final File file;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private final Map<UUID, String> selection = new HashMap<>();
    private String consoleSelection;

    public ArenaManager(FloorIsLava plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
    }

    public void load() {
        arenas.clear();
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("arenas");
        if (root == null) return;
        for (String name : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(name);
            if (s == null) continue;
            Arena a = new Arena(name);
            a.setCenter(
                    s.getString("world", "world"),
                    s.getInt("center.x"),
                    s.getInt("center.y"),
                    s.getInt("center.z"));
            a.setRadius(s.getInt("radius", 30));
            a.setStartY(s.getInt("start-y", 60));
            a.setSpeed(SpeedPreset.parse(s.getString("speed"), SpeedPreset.NORMAL));
            a.setCustomIntervalSeconds(s.getDouble("custom-interval", 10.0));
            a.setDifficulty(Difficulty.parse(s.getString("difficulty"), Difficulty.NORMAL));
            a.setTestMode(s.getBoolean("test-mode", false));
            arenas.put(name.toLowerCase(Locale.ROOT), a);
        }
    }

    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Arena a : arenas.values()) {
            String p = "arenas." + a.getName() + ".";
            yaml.set(p + "world", a.getWorldName());
            yaml.set(p + "center.x", a.getCenterX());
            yaml.set(p + "center.y", a.getCenterY());
            yaml.set(p + "center.z", a.getCenterZ());
            yaml.set(p + "radius", a.getRadius());
            yaml.set(p + "start-y", a.getStartY());
            yaml.set(p + "speed", a.getSpeed().name());
            yaml.set(p + "custom-interval", a.getCustomIntervalSeconds());
            yaml.set(p + "difficulty", a.getDifficulty().name());
            yaml.set(p + "test-mode", a.isTestMode());
        }
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo guardar arenas.yml: " + ex.getMessage());
        }
    }

    /** Crea una arena "default" en el spawn del primer mundo si todavía no hay ninguna. */
    public void ensureDefault() {
        if (!arenas.isEmpty() || Bukkit.getWorlds().isEmpty()) return;
        World w = Bukkit.getWorlds().get(0);
        Location spawn = w.getSpawnLocation();
        Arena a = create("default", spawn);
        a.setStartY(spawn.getBlockY() - 10);
        save();
    }

    public Arena create(String name, Location center) {
        Arena a = new Arena(name);
        a.setCenter(center);
        a.setRadius(30);
        a.setStartY(center.getBlockY() - 10);
        arenas.put(name.toLowerCase(Locale.ROOT), a);
        return a;
    }

    public boolean delete(String name) {
        return arenas.remove(name.toLowerCase(Locale.ROOT)) != null;
    }

    public Arena get(String name) {
        return name == null ? null : arenas.get(name.toLowerCase(Locale.ROOT));
    }

    public List<Arena> all() {
        return new ArrayList<>(arenas.values());
    }

    public List<String> names() {
        List<String> out = new ArrayList<>();
        for (Arena a : arenas.values()) out.add(a.getName());
        return out;
    }

    public Arena first() {
        return arenas.isEmpty() ? null : arenas.values().iterator().next();
    }

    public void select(CommandSender sender, Arena arena) {
        if (sender instanceof Player p) {
            selection.put(p.getUniqueId(), arena.getName());
        } else {
            consoleSelection = arena.getName();
        }
    }

    /** Arena seleccionada por el usuario, o la primera registrada si no ha elegido ninguna. */
    public Arena selected(CommandSender sender) {
        String name = sender instanceof Player p ? selection.get(p.getUniqueId()) : consoleSelection;
        Arena a = get(name);
        return a != null ? a : first();
    }
}
