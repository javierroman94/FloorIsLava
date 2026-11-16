package com.javierroman.floorislava;

import com.javierroman.floorislava.arena.ArenaManager;
import com.javierroman.floorislava.command.FloorIsLavaCommand;
import com.javierroman.floorislava.game.GameManager;
import com.javierroman.floorislava.gui.SetupGUI;
import com.javierroman.floorislava.hook.FloorIsLavaExpansion;
import com.javierroman.floorislava.listener.GameListener;
import com.javierroman.floorislava.listener.TrackingListener;
import com.javierroman.floorislava.util.Effects;
import com.javierroman.floorislava.util.Messages;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FloorIsLava extends JavaPlugin {

    private static FloorIsLava instance;

    private Messages messages;
    private Effects effects;
    private ArenaManager arenas;
    private GameManager games;
    private SetupGUI gui;
    private volatile List<String> lastTop = Collections.emptyList();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        messages = new Messages(this);
        effects = new Effects(this);
        arenas = new ArenaManager(this);
        arenas.load();
        games = new GameManager(this);
        gui = new SetupGUI(this);

        getServer().getPluginManager().registerEvents(gui, this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);
        getServer().getPluginManager().registerEvents(new TrackingListener(this), this);

        PluginCommand command = getCommand("floorislava");
        if (command != null) {
            FloorIsLavaCommand handler = new FloorIsLavaCommand(this);
            command.setExecutor(handler);
            command.setTabCompleter(handler);
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new FloorIsLavaExpansion(this).register();
            getLogger().info("PlaceholderAPI detectado: placeholders %floorislava_*% registrados.");
        }

        // Los mundos ya están cargados en este punto: crear la arena por defecto
        // y recuperar snapshots que quedaron pendientes tras un cierre brusco.
        getServer().getScheduler().runTask(this, () -> {
            arenas.ensureDefault();
            games.recoverSnapshots();
        });
    }

    @Override
    public void onDisable() {
        if (games != null) games.shutdownAll();
        if (arenas != null) arenas.save();
    }

    public static FloorIsLava getInstance() {
        return instance;
    }

    public Messages messages() {
        return messages;
    }

    public Effects effects() {
        return effects;
    }

    public ArenaManager arenas() {
        return arenas;
    }

    public GameManager games() {
        return games;
    }

    public SetupGUI gui() {
        return gui;
    }

    /** Nombres del TOP 3 de la última partida terminada. */
    public List<String> getLastTop() {
        return lastTop;
    }

    public void setLastTop(List<String> top) {
        this.lastTop = Collections.unmodifiableList(new ArrayList<>(top));
    }
}
