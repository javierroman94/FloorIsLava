package com.javierroman.floorislava.listener;

import com.javierroman.floorislava.FloorIsLava;
import com.javierroman.floorislava.game.Game;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

/**
 * Registra en el snapshot todo cambio de terreno hecho por jugadores o por la
 * física durante la partida, para poder restaurarlo después. En modo test no
 * se registran cambios de jugadores.
 */
public class TrackingListener implements Listener {

    private final FloorIsLava plugin;

    public TrackingListener(FloorIsLava plugin) {
        this.plugin = plugin;
    }

    private Game trackingGameAt(Block b) {
        Game g = plugin.games().activeGameAt(b.getWorld(), b.getX(), b.getZ());
        return (g == null || g.isTestMode()) ? null : g;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Game g = trackingGameAt(e.getBlock());
        if (g != null) g.recordAround(e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        Game g = trackingGameAt(b);
        if (g != null) g.record(b.getX(), b.getY(), b.getZ(), e.getBlockReplacedState().getBlockData());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) {
        Game g = trackingGameAt(e.getBlock());
        if (g != null) g.recordAround(e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        Game g = trackingGameAt(e.getBlock());
        if (g != null) g.record(e.getBlock());
    }

    /** Cubre hielo, nieve, concreto, y propagación de fuego/césped (BlockSpreadEvent es un BlockFormEvent). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onForm(BlockFormEvent e) {
        Game g = trackingGameAt(e.getBlock());
        if (g != null) g.record(e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityChange(EntityChangeBlockEvent e) {
        Game g = trackingGameAt(e.getBlock());
        if (g != null) g.record(e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        Block target = e.getBlockClicked().getRelative(e.getBlockFace());
        Game g = trackingGameAt(target);
        if (g != null) {
            g.record(target);
            g.record(e.getBlockClicked());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        Block target = e.getBlockClicked();
        Game g = trackingGameAt(target);
        if (g != null) g.record(target);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        Game g = trackingGameAt(e.getLocation().getBlock());
        if (g == null) return;
        for (Block b : e.blockList()) {
            g.recordAround(b);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        Game g = trackingGameAt(e.getBlock());
        if (g == null) return;
        for (Block b : e.blockList()) {
            g.recordAround(b);
        }
    }

    /**
     * La lava de la partida son bloques fuente sin física: se evita que fluya
     * fuera del área. El agua y otros flujos se registran antes de cambiar el bloque.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        Block from = e.getBlock();
        Game fromGame = plugin.games().activeGameAt(from.getWorld(), from.getX(), from.getZ());
        if (fromGame != null && from.getType() == Material.LAVA) {
            e.setCancelled(true);
            return;
        }
        Block to = e.getToBlock();
        Game g = trackingGameAt(to);
        if (g != null) g.record(to);
    }
}
