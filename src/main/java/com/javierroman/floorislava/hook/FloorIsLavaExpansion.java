package com.javierroman.floorislava.hook;

import com.javierroman.floorislava.FloorIsLava;
import com.javierroman.floorislava.arena.Arena;
import com.javierroman.floorislava.game.Game;
import com.javierroman.floorislava.game.GameState;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Locale;

/**
 * Placeholders de PlaceholderAPI (también usables desde TAB):
 * %floorislava_state%, _arena, _alive, _players, _lava_y, _radius, _test, _top1, _top2, _top3
 */
public class FloorIsLavaExpansion extends PlaceholderExpansion {

    private final FloorIsLava plugin;

    public FloorIsLavaExpansion(FloorIsLava plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "floorislava";
    }

    @Override
    public String getAuthor() {
        return "Javier Roman";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        String p = params.toLowerCase(Locale.ROOT);

        if (p.length() == 4 && p.startsWith("top")) {
            int index = Character.digit(p.charAt(3), 10) - 1;
            List<String> top = plugin.getLastTop();
            return index >= 0 && index < top.size() ? top.get(index) : "-";
        }

        Game g = player != null ? plugin.games().getGame(player.getUniqueId()) : null;
        if (g == null) g = plugin.games().firstInProgress();
        Arena a = g != null ? g.getArena() : plugin.arenas().first();

        switch (p) {
            case "state":
                return g == null ? GameState.WAITING.name() : g.getState().name();
            case "arena":
                return a == null ? "" : a.getName();
            case "alive":
                return String.valueOf(g == null ? 0 : g.aliveCount());
            case "players":
                return String.valueOf(g == null ? 0 : g.participantCount());
            case "lava_y":
                if (g != null && g.getState() != GameState.WAITING) return String.valueOf(g.getCurrentLavaY());
                return a == null ? "0" : String.valueOf(a.getStartY());
            case "radius":
                return a == null ? "0" : String.valueOf(a.getRadius());
            case "test":
                return String.valueOf(a != null && a.isTestMode());
            default:
                return null;
        }
    }
}
