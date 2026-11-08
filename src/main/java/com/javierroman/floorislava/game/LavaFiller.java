package com.javierroman.floorislava.game;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayDeque;

/**
 * Rellena de lava, capa a capa y con un presupuesto de bloques por tick,
 * el disco definido por el centro y el radio de la arena.
 * Cada bloque modificado se registra antes en el snapshot.
 */
final class LavaFiller {

    private final World world;
    private final int cx;
    private final int cz;
    private final int radius;
    private final Snapshot snapshot;
    private final ArrayDeque<Integer> layers = new ArrayDeque<>();

    private int curY = Integer.MIN_VALUE;
    private int x;
    private int z;

    LavaFiller(World world, int cx, int cz, int radius, Snapshot snapshot) {
        this.world = world;
        this.cx = cx;
        this.cz = cz;
        this.radius = radius;
        this.snapshot = snapshot;
    }

    void queueLayer(int y) {
        if (y >= world.getMinHeight() && y < world.getMaxHeight()) {
            layers.add(y);
        }
    }

    boolean isIdle() {
        return curY == Integer.MIN_VALUE && layers.isEmpty();
    }

    /** Coloca como máximo {@code budget} bloques de lava y devuelve cuántos colocó. */
    int process(int budget) {
        int done = 0;
        final int r2 = radius * radius;
        while (done < budget) {
            if (curY == Integer.MIN_VALUE) {
                Integer next = layers.poll();
                if (next == null) break;
                curY = next;
                x = -radius;
                z = -radius;
            }
            while (x <= radius && done < budget) {
                if (x * x + z * z <= r2) {
                    placeAt(cx + x, curY, cz + z);
                    done++;
                }
                z++;
                if (z > radius) {
                    z = -radius;
                    x++;
                }
            }
            if (x > radius) {
                curY = Integer.MIN_VALUE;
            }
        }
        return done;
    }

    private void placeAt(int px, int py, int pz) {
        Block block = world.getBlockAt(px, py, pz);
        Material type = block.getType();
        if (type == Material.LAVA) return;
        if (type.isAir() || type == Material.WATER || block.isPassable()) {
            snapshot.record(block);
            block.setType(Material.LAVA, false);
        }
    }
}
