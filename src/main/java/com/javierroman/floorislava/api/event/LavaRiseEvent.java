package com.javierroman.floorislava.api.event;

import com.javierroman.floorislava.arena.Arena;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/** Se dispara cada vez que la lava sube de nivel. */
public class LavaRiseEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Arena arena;
    private final int lavaY;

    public LavaRiseEvent(Arena arena, int lavaY) {
        this.arena = arena;
        this.lavaY = lavaY;
    }

    public Arena getArena() {
        return arena;
    }

    /** Altura (Y) de la capa de lava más alta encolada. */
    public int getLavaY() {
        return lavaY;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
