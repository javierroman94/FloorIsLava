package com.javierroman.floorislava.game;

import com.javierroman.floorislava.FloorIsLava;
import com.javierroman.floorislava.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Una partida independiente por arena. */
public class GameManager {

    private final FloorIsLava plugin;
    private final Map<String, Game> games = new HashMap<>();

    public GameManager(FloorIsLava plugin) {
        this.plugin = plugin;
    }

    public Game get(Arena arena) {
        return games.computeIfAbsent(arena.getName().toLowerCase(Locale.ROOT), k -> new Game(plugin, arena));
    }

    public Collection<Game> all() {
        return new ArrayList<>(games.values());
    }

    /** Partida en la que participa el jugador, o null. */
    public Game getGame(UUID playerId) {
        for (Game g : games.values()) {
            if (g.isParticipant(playerId)) return g;
        }
        return null;
    }

    /** Partida con terreno en juego (registrando cambios) que cubre el bloque dado, o null. */
    public Game activeGameAt(World world, int x, int z) {
        if (world == null) return null;
        for (Game g : games.values()) {
            if (!g.isRecording()) continue;
            Arena a = g.getArena();
            if (a.getWorldName().equals(world.getName()) && a.containsBlock(x, z, 2)) {
                return g;
            }
        }
        return null;
    }

    /** Primera partida en marcha, o null (para placeholders sin jugador). */
    public Game firstInProgress() {
        for (Game g : games.values()) {
            if (g.getState() != GameState.WAITING) return g;
        }
        return null;
    }

    public void remove(Arena arena) {
        Game g = games.remove(arena.getName().toLowerCase(Locale.ROOT));
        if (g != null) g.shutdown();
    }

    /**
     * Inicia la partida de una arena. En modo test, si el lobby está vacío y quien ejecuta
     * es un jugador, se apunta automáticamente.
     *
     * @return null si arrancó, o la clave del mensaje de error.
     */
    public String startFor(CommandSender sender, Arena arena) {
        Game g = get(arena);
        if (arena.isTestMode()
                && sender instanceof Player p
                && g.getState() == GameState.WAITING
                && g.participantCount() == 0
                && getGame(p.getUniqueId()) == null) {
            g.join(p);
        }
        return g.start();
    }

    public File snapshotFile(String arenaName) {
        return new File(plugin.getDataFolder(), "snapshots/" + arenaName.toLowerCase(Locale.ROOT) + ".snap.gz");
    }

    /** Restaura snapshots que quedaron en disco por un cierre brusco del servidor. */
    public void recoverSnapshots() {
        for (Arena a : plugin.arenas().all()) {
            File file = snapshotFile(a.getName());
            if (!file.exists()) continue;
            try {
                Snapshot s = Snapshot.read(file);
                if (Bukkit.getWorld(s.getWorldName()) == null) {
                    plugin.getLogger().warning("Snapshot de " + a.getName() + ": el mundo "
                            + s.getWorldName() + " no está cargado, se conserva para más tarde.");
                    continue;
                }
                s.restoreInstant();
                if (!file.delete()) {
                    plugin.getLogger().warning("No se pudo borrar " + file.getName());
                }
                plugin.getLogger().info("Arena " + a.getName() + " restaurada desde un snapshot pendiente.");
            } catch (IOException ex) {
                plugin.getLogger().warning("No se pudo leer el snapshot de " + a.getName() + ": " + ex.getMessage());
            }
        }
    }

    public void shutdownAll() {
        for (Game g : new ArrayList<>(games.values())) {
            g.shutdown();
        }
    }
}
