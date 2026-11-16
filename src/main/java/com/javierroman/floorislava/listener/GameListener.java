package com.javierroman.floorislava.listener;

import com.javierroman.floorislava.FloorIsLava;
import com.javierroman.floorislava.arena.Arena;
import com.javierroman.floorislava.game.Game;
import com.javierroman.floorislava.game.GameState;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/** Control del borde, eliminación de jugadores y salidas. */
public class GameListener implements Listener {

    private final FloorIsLava plugin;

    public GameListener(FloorIsLava plugin) {
        this.plugin = plugin;
    }

    /** Bloquea el movimiento fuera del área de juego. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;
        if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) return;

        Player p = e.getPlayer();
        Game g = plugin.games().getGame(p.getUniqueId());
        if (g == null || !g.isAlivePlayer(p.getUniqueId())) return;

        Arena arena = g.getArena();
        if (arena.insideCircle(to.getX(), to.getZ(), 0)) return;

        if (arena.insideCircle(from.getX(), from.getZ(), 0)) {
            Location back = new Location(from.getWorld(), from.getX(), to.getY(), from.getZ(), to.getYaw(), to.getPitch());
            e.setTo(back);
            plugin.effects().actionBar(p, plugin.messages().get("border-blocked"));
        } else {
            g.teleportInside(p);
        }
    }

    /** Impide salir del área con perlas de ender o frutas de chorus. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        PlayerTeleportEvent.TeleportCause cause = e.getCause();
        if (cause != PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                && cause != PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT) {
            return;
        }
        Player p = e.getPlayer();
        Game g = plugin.games().getGame(p.getUniqueId());
        if (g == null || !g.isAlivePlayer(p.getUniqueId())) return;
        Location to = e.getTo();
        if (to == null || to.getWorld() == null
                || !to.getWorld().getName().equals(g.getArena().getWorldName())
                || !g.getArena().insideCircle(to.getX(), to.getZ(), 0)) {
            e.setCancelled(true);
        }
    }

    /**
     * Si el daño sería mortal, se cancela y se elimina al jugador (pasa a espectador
     * sin pantalla de muerte). En el periodo de gracia no hay daño.
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Game g = plugin.games().getGame(p.getUniqueId());
        if (g == null || !g.isAlivePlayer(p.getUniqueId())) return;

        if (g.getState() == GameState.STARTING) {
            e.setCancelled(true);
            return;
        }

        if (g.isTestMode()) {
            switch (e.getCause()) {
                case LAVA, FIRE, FIRE_TICK, HOT_FLOOR -> e.setCancelled(true);
                default -> { }
            }
            return;
        }

        if (e.getFinalDamage() >= p.getHealth()) {
            e.setCancelled(true);
            g.eliminate(p, false);
        }
    }

    /** Respaldo por si un jugador llega a morir igualmente (comandos, plugins...). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        Game g = plugin.games().getGame(p.getUniqueId());
        if (g == null || !g.isAlivePlayer(p.getUniqueId()) || g.isTestMode()) return;

        e.setKeepInventory(true);
        e.setKeepLevel(true);
        e.getDrops().clear();
        e.setDroppedExp(0);
        e.setDeathMessage(null);
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (p.isDead()) p.spigot().respawn();
            g.eliminate(p, false);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        Game g = plugin.games().getGame(p.getUniqueId());
        if (g != null) g.leave(p);
    }
}
