package com.javierroman.floorislava.game;

import com.javierroman.floorislava.FloorIsLava;
import com.javierroman.floorislava.api.event.GameEndEvent;
import com.javierroman.floorislava.api.event.GameStartEvent;
import com.javierroman.floorislava.api.event.LavaRiseEvent;
import com.javierroman.floorislava.api.event.PlayerEliminatedEvent;
import com.javierroman.floorislava.arena.Arena;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** Una partida de Floor Is Lava en una arena concreta. */
public class Game {

    private static final BlockFace[] NEIGHBORS = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST
    };

    private static final class PlayerState {
        final Location location;
        final GameMode mode;

        PlayerState(Location location, GameMode mode) {
            this.location = location;
            this.mode = mode;
        }
    }

    private final FloorIsLava plugin;
    private final Arena arena;
    private final Random random = new Random();

    private GameState state = GameState.WAITING;
    private final Map<UUID, PlayerState> participants = new LinkedHashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    private final Set<UUID> alive = new LinkedHashSet<>();
    private final List<UUID> eliminationOrder = new ArrayList<>();
    private final List<ArmorStand> holograms = new ArrayList<>();

    private Snapshot snapshot;
    private LavaFiller filler;
    private BukkitTask countdownTask;
    private BukkitTask mainTask;
    private BukkitTask restoreTask;
    private BukkitTask finishTask;

    private int currentLavaY;
    private long ticksUntilRise;
    private long tickCounter;
    private int countdown;
    private boolean testMode;

    public Game(FloorIsLava plugin, Arena arena) {
        this.plugin = plugin;
        this.arena = arena;
    }

    // ------------------------------------------------------------------ consultas

    public Arena getArena() {
        return arena;
    }

    public GameState getState() {
        return state;
    }

    /** Jugadores en juego: cuenta atrás o lava subiendo. */
    public boolean isActive() {
        return state == GameState.STARTING || state == GameState.RUNNING;
    }

    /** Mientras haya un snapshot abierto (los cambios en el terreno se registran). */
    public boolean isRecording() {
        return state == GameState.STARTING || state == GameState.RUNNING || state == GameState.ENDING;
    }

    public boolean isTestMode() {
        return testMode;
    }

    public boolean isParticipant(UUID id) {
        return participants.containsKey(id);
    }

    public boolean isAlivePlayer(UUID id) {
        return isActive() && alive.contains(id);
    }

    public int aliveCount() {
        return alive.size();
    }

    public int participantCount() {
        return participants.size();
    }

    public int getCurrentLavaY() {
        return currentLavaY;
    }

    // ------------------------------------------------------------------ lobby

    /** @return null si todo fue bien, o la clave del mensaje de error. */
    public String join(Player p) {
        if (state != GameState.WAITING) return "game-running";
        UUID id = p.getUniqueId();
        if (participants.containsKey(id)) return "already-joined";
        participants.put(id, new PlayerState(p.getLocation().clone(), p.getGameMode()));
        names.put(id, p.getName());
        return null;
    }

    /** @return null si todo fue bien, o la clave del mensaje de error. */
    public String leave(Player p) {
        UUID id = p.getUniqueId();
        if (!participants.containsKey(id)) return "not-joined";
        if (isActive()) {
            if (alive.contains(id)) {
                eliminate(p, true);
            } else {
                // ya eliminado (espectador): simplemente sale
                restorePlayer(p);
                participants.remove(id);
            }
        } else if (state == GameState.WAITING) {
            participants.remove(id);
        } else {
            restorePlayer(p);
            participants.remove(id);
        }
        return null;
    }

    private List<Player> onlinePlayers() {
        List<Player> out = new ArrayList<>();
        for (UUID id : participants.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) out.add(p);
        }
        return out;
    }

    private void broadcast(String key, Object... kv) {
        String msg = plugin.messages().prefixed(key, kv);
        for (Player p : onlinePlayers()) {
            p.sendMessage(msg);
        }
    }

    // ------------------------------------------------------------------ inicio

    /** @return null si la partida arrancó, o la clave del mensaje de error. */
    public String start() {
        if (state != GameState.WAITING) return "game-already-running";
        World world = arena.getWorld();
        if (world == null) return "world-missing";

        participants.keySet().removeIf(id -> Bukkit.getPlayer(id) == null);
        List<Player> players = onlinePlayers();
        testMode = arena.isTestMode();
        if (players.isEmpty()) return "no-players";
        if (!testMode && players.size() < 2) return "not-enough-players";

        snapshot = new Snapshot(world.getName());
        filler = new LavaFiller(world, arena.getCenterX(), arena.getCenterZ(), arena.getRadius(), snapshot);
        alive.clear();
        eliminationOrder.clear();
        currentLavaY = arena.getStartY();
        state = GameState.STARTING;

        for (Player p : players) {
            alive.add(p.getUniqueId());
            names.put(p.getUniqueId(), p.getName());
            prepare(p, world);
        }
        Bukkit.getPluginManager().callEvent(new GameStartEvent(arena, players));

        int grace = Math.max(1, plugin.getConfig().getInt(
                "difficulty." + arena.getDifficulty().name() + ".grace-seconds",
                arena.getDifficulty().defaultGrace()));
        countdown = grace;
        broadcast("game-starting", "arena", arena.getName(), "seconds", grace);

        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != GameState.STARTING) {
                    cancel();
                    return;
                }
                if (countdown <= 0) {
                    cancel();
                    countdownTask = null;
                    beginRunning();
                    return;
                }
                if (countdown <= 5 || countdown % 10 == 0) {
                    List<Player> ps = onlinePlayers();
                    plugin.effects().title(ps, "countdown", "seconds", countdown);
                    plugin.effects().sound(ps, "countdown");
                }
                countdown--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
        return null;
    }

    private void prepare(Player p, World world) {
        p.setGameMode(GameMode.SURVIVAL);
        p.setHealth(Math.min(20.0, p.getMaxHealth()));
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFireTicks(0);
        p.setFallDistance(0f);
        p.teleport(randomSpot(world));
        applyBorder(p);
    }

    private Location randomSpot(World world) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double dist = Math.sqrt(random.nextDouble()) * Math.max(1.0, arena.getRadius() * 0.8);
        int x = arena.getCenterX() + (int) Math.round(Math.cos(angle) * dist);
        int z = arena.getCenterZ() + (int) Math.round(Math.sin(angle) * dist);
        int y = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES) + 1;
        return new Location(world, x + 0.5, y, z + 0.5, random.nextFloat() * 360f, 0f);
    }

    private Location spectatorSpawn() {
        World w = arena.getWorld();
        int highest = w.getHighestBlockYAt(arena.getCenterX(), arena.getCenterZ(), HeightMap.MOTION_BLOCKING_NO_LEAVES);
        double y = Math.max(currentLavaY + 6, highest + 3);
        y = Math.min(y, w.getMaxHeight() - 2);
        return new Location(w, arena.getCenterX() + 0.5, y, arena.getCenterZ() + 0.5);
    }

    /** Devuelve a un jugador vivo que quedó fuera del área a un punto seguro dentro. */
    public void teleportInside(Player p) {
        World w = arena.getWorld();
        if (w != null) p.teleport(randomSpot(w));
    }

    private void applyBorder(Player p) {
        if (!plugin.getConfig().getBoolean("border.visual", true)) return;
        try {
            WorldBorder border = Bukkit.createWorldBorder();
            border.setCenter(arena.getCenterX() + 0.5, arena.getCenterZ() + 0.5);
            border.setSize(arena.getRadius() * 2.0 + 1.0);
            border.setWarningDistance(2);
            p.setWorldBorder(border);
        } catch (Throwable ignored) {
            // API no disponible en esta versión del servidor
        }
    }

    // ------------------------------------------------------------------ lava

    private void beginRunning() {
        state = GameState.RUNNING;
        tickCounter = 0;
        currentLavaY = arena.getStartY();
        filler.queueLayer(currentLavaY);
        ticksUntilRise = arena.intervalTicks(plugin.getConfig());

        List<Player> ps = onlinePlayers();
        broadcast("lava-start");
        plugin.effects().title(ps, "start");
        plugin.effects().sound(ps, "start");

        mainTask = new BukkitRunnable() {
            @Override
            public void run() {
                tick();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void tick() {
        if (state != GameState.RUNNING) return;
        tickCounter++;
        double tps = currentTps();

        filler.process(budgetFor(tps));

        double pauseBelow = plugin.getConfig().getDouble("lava.tps-pause-below", 15.0);
        if (tps >= pauseBelow) {
            ticksUntilRise--;
            if (ticksUntilRise <= 0 && filler.isIdle()) {
                riseStep();
                long base = arena.intervalTicks(plugin.getConfig());
                ticksUntilRise = Math.max(1L, Math.round(base * slowFactor(tps)));
            }
        }

        if (tickCounter % 20 == 0) {
            String bar = plugin.messages().get("actionbar", "lava_y", currentLavaY, "alive", alive.size());
            for (Player p : onlinePlayers()) {
                plugin.effects().actionBar(p, bar);
            }
        }
        if (!testMode && tickCounter % 600 == 0) {
            snapshot.writeAsync(plugin, plugin.games().snapshotFile(arena.getName()));
        }
    }

    private void riseStep() {
        World w = arena.getWorld();
        if (w == null) return;
        int layers = Math.max(1, plugin.getConfig().getInt(
                "difficulty." + arena.getDifficulty().name() + ".layers-per-step",
                arena.getDifficulty().defaultLayers()));
        for (int i = 0; i < layers; i++) {
            int y = currentLavaY + 1;
            if (y >= w.getMaxHeight()) break;
            filler.queueLayer(y);
            currentLavaY = y;
        }
        Bukkit.getPluginManager().callEvent(new LavaRiseEvent(arena, currentLavaY));
        plugin.effects().sound(onlinePlayers(), "lava-rise");
    }

    private static java.lang.reflect.Method tpsMethod;
    private static Object tpsTarget;
    private static boolean tpsResolved;

    /** TPS del último minuto. getTPS() no está en la API de Spigot, así que se busca por reflexión. */
    private static double currentTps() {
        try {
            if (!tpsResolved) {
                tpsResolved = true;
                Object server = Bukkit.getServer();
                Object[] candidates = {server, Bukkit.getServer().spigot()};
                for (Object target : candidates) {
                    try {
                        java.lang.reflect.Method m = target.getClass().getMethod("getTPS");
                        m.setAccessible(true);
                        tpsMethod = m;
                        tpsTarget = target;
                        break;
                    } catch (ReflectiveOperationException | RuntimeException ignored) {
                        // probar el siguiente candidato
                    }
                }
            }
            if (tpsMethod != null) {
                double[] tps = (double[]) tpsMethod.invoke(tpsTarget);
                if (tps != null && tps.length > 0) return Math.min(20.0, tps[0]);
            }
        } catch (Throwable ignored) {
            // sin TPS disponible: se asume servidor sano
        }
        return 20.0;
    }

    /** Bloques de lava que se pueden colocar por tick según el TPS actual. */
    private static int budgetFor(double tps) {
        if (tps >= 19.0) return 3000;
        if (tps >= 17.0) return 1200;
        if (tps >= 15.0) return 400;
        return 120;
    }

    /** Multiplicador del intervalo: cuanto peor el TPS, más lenta sube la lava. */
    private static double slowFactor(double tps) {
        if (tps >= 19.0) return 1.0;
        if (tps >= 17.0) return 1.25;
        if (tps >= 15.0) return 1.75;
        return 2.5;
    }

    // ------------------------------------------------------------------ eliminación y final

    public void eliminate(Player p, boolean quit) {
        UUID id = p.getUniqueId();
        if (!alive.remove(id)) return;
        eliminationOrder.add(id);
        int placement = alive.size() + 1;
        Bukkit.getPluginManager().callEvent(new PlayerEliminatedEvent(p, arena, placement));

        if (quit) {
            broadcast("eliminated-quit", "player", p.getName(), "alive", alive.size());
            restorePlayer(p);
            participants.remove(id);
        } else {
            p.setGameMode(GameMode.SPECTATOR);
            p.setFireTicks(0);
            p.setHealth(Math.min(20.0, p.getMaxHealth()));
            p.setFallDistance(0f);
            p.teleport(spectatorSpawn());
            broadcast("eliminated-broadcast", "player", p.getName(), "alive", alive.size());
            plugin.effects().title(p, "elimination", "place", placement);
            plugin.effects().sound(p, "elimination");
        }
        checkWin();
    }

    private void checkWin() {
        if (testMode || alive.size() > 1) return;
        if (state == GameState.RUNNING) {
            endGame(true);
        } else if (state == GameState.STARTING) {
            endGame(false);
        }
    }

    /** Detiene la partida a mano. @return null si se detuvo, o la clave del mensaje de error. */
    public String stop() {
        if (!isActive()) return "game-not-running";
        endGame(false);
        return null;
    }

    private void endGame(boolean natural) {
        if (!isActive()) return;
        cancelTasks();
        state = GameState.ENDING;

        List<String> top = buildTop();
        plugin.setLastTop(top);
        String winner = null;
        boolean showResults = natural && !testMode;

        if (showResults) {
            List<Player> ps = onlinePlayers();
            if (alive.size() == 1) {
                UUID w = alive.iterator().next();
                winner = names.get(w);
                Player wp = Bukkit.getPlayer(w);
                if (wp != null) plugin.effects().title(wp, "win");
                plugin.effects().sound(ps, "win");
                broadcast("winner", "player", winner);
            } else {
                broadcast("no-winner");
            }
            broadcast("top-header");
            for (int i = 0; i < top.size(); i++) {
                broadcast("top-line", "place", i + 1, "player", top.get(i));
            }
            spawnHologram(top);
        }

        Bukkit.getPluginManager().callEvent(new GameEndEvent(arena, winner, top, natural));

        long delay = showResults
                ? Math.max(1L, plugin.getConfig().getLong("end.delay-seconds", 10L) * 20L)
                : 1L;
        finishTask = Bukkit.getScheduler().runTaskLater(plugin, this::finish, delay);
    }

    private List<String> buildTop() {
        List<String> top = new ArrayList<>();
        for (UUID id : alive) {
            top.add(names.getOrDefault(id, "?"));
        }
        for (int i = eliminationOrder.size() - 1; i >= 0 && top.size() < 3; i--) {
            top.add(names.getOrDefault(eliminationOrder.get(i), "?"));
        }
        return top.size() > 3 ? new ArrayList<>(top.subList(0, 3)) : top;
    }

    private void finish() {
        finishTask = null;
        removeHolograms();
        for (UUID id : new ArrayList<>(participants.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) restorePlayer(p);
        }
        participants.clear();
        alive.clear();
        eliminationOrder.clear();

        state = GameState.RESTORING;
        restoreArea(() -> state = GameState.WAITING);
    }

    private void restorePlayer(Player p) {
        try {
            p.setWorldBorder(null);
        } catch (Throwable ignored) {
            // API no disponible
        }
        PlayerState s = participants.get(p.getUniqueId());
        if (s != null) {
            p.setGameMode(s.mode);
            p.teleport(s.location);
        }
    }

    // ------------------------------------------------------------------ restauración del terreno

    private void restoreArea(Runnable done) {
        Snapshot s = snapshot;
        Runnable after = () -> {
            snapshot = null;
            restoreTask = null;
            deleteSnapshotFile();
            done.run();
        };
        if (s == null || s.isEmpty()) {
            after.run();
            return;
        }
        String mode = plugin.getConfig().getString("restore.mode", "gradual");
        if ("instant".equalsIgnoreCase(mode)) {
            s.restoreInstant();
            after.run();
        } else {
            int perTick = plugin.getConfig().getInt("restore.blocks-per-tick", 2500);
            restoreTask = s.restoreGradual(plugin, perTick, after);
        }
    }

    /**
     * Restaura el área al instante (botón de la GUI y /fil restore).
     *
     * @return null si se restauró, o la clave del mensaje de error.
     */
    public String restoreNow() {
        if (isRecording()) return "game-running";
        if (state == GameState.RESTORING) {
            if (restoreTask != null) {
                restoreTask.cancel();
                restoreTask = null;
            }
            if (snapshot != null) snapshot.restoreInstant();
            snapshot = null;
            deleteSnapshotFile();
            state = GameState.WAITING;
            return null;
        }
        if (snapshot != null && !snapshot.isEmpty()) {
            snapshot.restoreInstant();
            snapshot = null;
            deleteSnapshotFile();
            return null;
        }
        File file = plugin.games().snapshotFile(arena.getName());
        if (file.exists()) {
            try {
                Snapshot s = Snapshot.read(file);
                s.restoreInstant();
                deleteSnapshotFile();
                return null;
            } catch (IOException ex) {
                plugin.getLogger().warning("No se pudo leer el snapshot de " + arena.getName() + ": " + ex.getMessage());
            }
        }
        return "nothing-to-restore";
    }

    private void deleteSnapshotFile() {
        File f = plugin.games().snapshotFile(arena.getName());
        if (f.exists() && !f.delete()) {
            plugin.getLogger().warning("No se pudo borrar " + f.getName());
        }
    }

    // ------------------------------------------------------------------ registro de cambios en el terreno

    public void record(Block b) {
        if (snapshot != null) snapshot.record(b);
    }

    public void record(int x, int y, int z, BlockData original) {
        if (snapshot != null) snapshot.record(x, y, z, original);
    }

    /** Registra el bloque y sus vecinos (puertas, antorchas y bloques dependientes que caen). */
    public void recordAround(Block b) {
        if (snapshot == null) return;
        snapshot.record(b);
        for (BlockFace face : NEIGHBORS) {
            snapshot.record(b.getRelative(face));
        }
    }

    // ------------------------------------------------------------------ holograma

    private void spawnHologram(List<String> top) {
        World w = arena.getWorld();
        if (w == null) return;
        List<String> lines = new ArrayList<>();
        lines.add(plugin.messages().get("hologram-title"));
        for (int i = 0; i < top.size(); i++) {
            lines.add(plugin.messages().get("hologram-line", "place", i + 1, "player", top.get(i)));
        }
        Location base = spectatorSpawn().add(0, 2, 0);
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            Location at = base.clone().add(0, -0.3 * i, 0);
            ArmorStand stand = w.spawn(at, ArmorStand.class);
            stand.setVisible(false);
            stand.setGravity(false);
            stand.setMarker(true);
            stand.setInvulnerable(true);
            stand.setPersistent(false);
            stand.setCustomName(line);
            stand.setCustomNameVisible(true);
            holograms.add(stand);
        }
    }

    private void removeHolograms() {
        for (ArmorStand stand : holograms) {
            if (stand.isValid()) stand.remove();
        }
        holograms.clear();
    }

    // ------------------------------------------------------------------ cierre

    private void cancelTasks() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        if (mainTask != null) {
            mainTask.cancel();
            mainTask = null;
        }
    }

    /** Cierre limpio al desactivar el plugin: devuelve a los jugadores y restaura el terreno al instante. */
    public void shutdown() {
        cancelTasks();
        if (restoreTask != null) {
            restoreTask.cancel();
            restoreTask = null;
        }
        if (finishTask != null) {
            finishTask.cancel();
            finishTask = null;
        }
        removeHolograms();
        for (UUID id : new ArrayList<>(participants.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) restorePlayer(p);
        }
        participants.clear();
        alive.clear();
        eliminationOrder.clear();
        if (snapshot != null) {
            snapshot.restoreInstant();
            snapshot = null;
            deleteSnapshotFile();
        }
        state = GameState.WAITING;
    }

    /** Utilidad para listas inmutables en consultas externas. */
    public List<UUID> aliveIds() {
        return Collections.unmodifiableList(new ArrayList<>(alive));
    }
}
