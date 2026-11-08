package com.javierroman.floorislava.game;

import com.javierroman.floorislava.FloorIsLava;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Snapshot ligero sin WorldEdit: solo guarda el estado ORIGINAL de los bloques
 * que cambian durante la partida (la primera vez que cambian).
 */
public final class Snapshot {

    private final String worldName;
    private final LinkedHashMap<Long, BlockData> blocks = new LinkedHashMap<>();

    public Snapshot(String worldName) {
        this.worldName = worldName;
    }

    public String getWorldName() {
        return worldName;
    }

    public int size() {
        return blocks.size();
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    // x: 26 bits (38..63), z: 26 bits (12..37), y: 12 bits (0..11, con offset 2048)
    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (long) ((y + 2048) & 0xFFF);
    }

    private static int keyX(long k) {
        return (int) (k >> 38);
    }

    private static int keyZ(long k) {
        return (int) ((k << 26) >> 38);
    }

    private static int keyY(long k) {
        return (int) (k & 0xFFF) - 2048;
    }

    /** Guarda el estado actual del bloque si todavía no estaba registrado. */
    public void record(Block block) {
        long k = key(block.getX(), block.getY(), block.getZ());
        if (!blocks.containsKey(k)) {
            blocks.put(k, block.getBlockData());
        }
    }

    /** Guarda un estado original conocido (p. ej. el bloque reemplazado al colocar). */
    public void record(int x, int y, int z, BlockData original) {
        blocks.putIfAbsent(key(x, y, z), original);
    }

    private static void apply(World world, long k, BlockData data) {
        world.getBlockAt(keyX(k), keyY(k), keyZ(k)).setBlockData(data, false);
    }

    /** Restaura todo de golpe y vacía el snapshot. */
    public void restoreInstant() {
        World world = Bukkit.getWorld(worldName);
        if (world != null) {
            for (Map.Entry<Long, BlockData> e : blocks.entrySet()) {
                apply(world, e.getKey(), e.getValue());
            }
        }
        blocks.clear();
    }

    /**
     * Restaura por tandas para no provocar lag. Cada bloque restaurado se elimina
     * del snapshot, así que si hay que cortar a medias basta con llamar a restoreInstant().
     */
    public BukkitTask restoreGradual(FloorIsLava plugin, int perTick, Runnable onDone) {
        final World world = Bukkit.getWorld(worldName);
        if (world == null || blocks.isEmpty()) {
            blocks.clear();
            onDone.run();
            return null;
        }
        final Iterator<Map.Entry<Long, BlockData>> it = blocks.entrySet().iterator();
        final int budget = Math.max(1, perTick);
        return new BukkitRunnable() {
            @Override
            public void run() {
                int n = 0;
                while (it.hasNext() && n++ < budget) {
                    Map.Entry<Long, BlockData> e = it.next();
                    apply(world, e.getKey(), e.getValue());
                    it.remove();
                }
                if (!it.hasNext()) {
                    cancel();
                    onDone.run();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    /** Guarda el snapshot en disco (gzip) para poder recuperarlo tras un cierre brusco. */
    public void writeAsync(FloorIsLava plugin, File file) {
        final List<String> lines = new ArrayList<>(blocks.size() + 1);
        lines.add(worldName);
        for (Map.Entry<Long, BlockData> e : blocks.entrySet()) {
            lines.add(e.getKey() + ";" + e.getValue().getAsString());
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                writeLines(file, lines);
            } catch (IOException ex) {
                plugin.getLogger().warning("No se pudo guardar el snapshot: " + ex.getMessage());
            }
        });
    }

    private static void writeLines(File file, List<String> lines) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        File tmp = new File(file.getPath() + ".tmp");
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                new GZIPOutputStream(new FileOutputStream(tmp)), StandardCharsets.UTF_8))) {
            for (String line : lines) {
                w.write(line);
                w.newLine();
            }
        }
        Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    public static Snapshot read(File file) throws IOException {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(new FileInputStream(file)), StandardCharsets.UTF_8))) {
            String world = r.readLine();
            if (world == null) throw new IOException("snapshot vacío");
            Snapshot s = new Snapshot(world);
            String line;
            while ((line = r.readLine()) != null) {
                int i = line.indexOf(';');
                if (i < 0) continue;
                try {
                    long k = Long.parseLong(line.substring(0, i));
                    s.blocks.put(k, Bukkit.createBlockData(line.substring(i + 1)));
                } catch (IllegalArgumentException ignored) {
                    // línea corrupta o bloque que ya no existe: se omite
                }
            }
            return s;
        }
    }
}
