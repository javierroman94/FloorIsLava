package com.javierroman.floorislava.api.event;

import com.javierroman.floorislava.arena.Arena;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Se dispara cuando una partida empieza (al comenzar la cuenta atrás de gracia). */
public class GameStartEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Arena arena;
    private final List<Player> players;

    public GameStartEvent(Arena arena, List<Player> players) {
        this.arena = arena;
        this.players = Collections.unmodifiableList(new ArrayList<>(players));
    }

    public Arena getArena() {
        return arena;
    }

    public List<Player> getPlayers() {
        return players;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
