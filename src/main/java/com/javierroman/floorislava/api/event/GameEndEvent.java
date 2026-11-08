package com.javierroman.floorislava.api.event;

import com.javierroman.floorislava.arena.Arena;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Se dispara cuando una partida termina (con ganador o detenida a mano). */
public class GameEndEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Arena arena;
    private final String winner;
    private final List<String> top;
    private final boolean natural;

    /**
     * @param winner  nombre del ganador o null si no lo hay
     * @param top     nombres del TOP 3 (el primero es el ganador)
     * @param natural true si terminó por victoria, false si se detuvo manualmente
     */
    public GameEndEvent(Arena arena, String winner, List<String> top, boolean natural) {
        this.arena = arena;
        this.winner = winner;
        this.top = Collections.unmodifiableList(new ArrayList<>(top));
        this.natural = natural;
    }

    public Arena getArena() {
        return arena;
    }

    public String getWinner() {
        return winner;
    }

    public List<String> getTop() {
        return top;
    }

    public boolean isNatural() {
        return natural;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
