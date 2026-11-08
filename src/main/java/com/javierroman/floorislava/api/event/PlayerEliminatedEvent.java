package com.javierroman.floorislava.api.event;

import com.javierroman.floorislava.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se dispara cuando un jugador es eliminado de la partida. */
public class PlayerEliminatedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Arena arena;
    private final int placement;

    public PlayerEliminatedEvent(Player player, Arena arena, int placement) {
        this.player = player;
        this.arena = arena;
        this.placement = placement;
    }

    public Player getPlayer() {
        return player;
    }

    public Arena getArena() {
        return arena;
    }

    /** Puesto final del jugador (1 = ganador, 2 = último eliminado, ...). */
    public int getPlacement() {
        return placement;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
