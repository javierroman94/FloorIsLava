package com.javierroman.floorislava.command;

import com.javierroman.floorislava.FloorIsLava;
import com.javierroman.floorislava.arena.Arena;
import com.javierroman.floorislava.game.Game;
import com.javierroman.floorislava.game.GameState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class FloorIsLavaCommand implements TabExecutor {

    private static final String ADMIN = "floorislava.admin";
    private static final String PLAY = "floorislava.play";
    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private static final List<String> SUBS = Arrays.asList(
            "help", "setup", "join", "leave", "start", "stop", "arena", "test", "restore", "reload");
    private static final List<String> ARENA_SUBS = Arrays.asList(
            "create", "delete", "list", "select", "info", "setcenter", "setradius", "setstarty");

    private final FloorIsLava plugin;

    public FloorIsLavaCommand(FloorIsLava plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            for (String line : plugin.messages().list("help")) sender.sendMessage(line);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "setup", "gui" -> setup(sender);
            case "start" -> start(sender, args);
            case "stop" -> stop(sender, args);
            case "join" -> join(sender, args);
            case "leave" -> leave(sender);
            case "arena" -> arena(sender, args);
            case "test" -> test(sender, args);
            case "restore" -> restore(sender, args);
            case "reload" -> reload(sender);
            default -> plugin.messages().send(sender, "unknown-subcommand");
        }
        return true;
    }

    // ------------------------------------------------------------------ helpers

    private boolean requireAdmin(CommandSender s) {
        if (s.hasPermission(ADMIN)) return true;
        plugin.messages().send(s, "no-permission");
        return false;
    }

    private Player requirePlayer(CommandSender s) {
        if (s instanceof Player p) return p;
        plugin.messages().send(s, "player-only");
        return null;
    }

    /** Arena indicada en args[index] o, si falta, la seleccionada por quien ejecuta. */
    private Arena resolveArena(CommandSender s, String[] args, int index) {
        if (args.length > index) {
            Arena a = plugin.arenas().get(args[index]);
            if (a == null) plugin.messages().send(s, "arena-not-found", "arena", args[index]);
            return a;
        }
        Arena a = plugin.arenas().selected(s);
        if (a == null) plugin.messages().send(s, "no-arena");
        return a;
    }

    private void sendKey(CommandSender s, String key, Object... kv) {
        plugin.messages().send(s, key, kv);
    }

    // ------------------------------------------------------------------ subcomandos

    private void setup(CommandSender s) {
        if (!requireAdmin(s)) return;
        Player p = requirePlayer(s);
        if (p != null) plugin.gui().open(p);
    }

    private void start(CommandSender s, String[] args) {
        if (!requireAdmin(s)) return;
        Arena a = resolveArena(s, args, 1);
        if (a == null) return;
        String err = plugin.games().startFor(s, a);
        if (err != null) sendKey(s, err);
        else sendKey(s, "started", "arena", a.getName());
    }

    private void stop(CommandSender s, String[] args) {
        if (!requireAdmin(s)) return;
        Arena a = resolveArena(s, args, 1);
        if (a == null) return;
        String err = plugin.games().get(a).stop();
        if (err != null) sendKey(s, err);
        else sendKey(s, "stopped", "arena", a.getName());
    }

    private void join(CommandSender s, String[] args) {
        if (!s.hasPermission(PLAY)) {
            sendKey(s, "no-permission");
            return;
        }
        Player p = requirePlayer(s);
        if (p == null) return;
        Arena a = resolveArena(s, args, 1);
        if (a == null) return;
        if (plugin.games().getGame(p.getUniqueId()) != null) {
            sendKey(s, "already-joined");
            return;
        }
        Game g = plugin.games().get(a);
        String err = g.join(p);
        if (err != null) sendKey(s, err);
        else sendKey(s, "joined", "arena", a.getName(), "players", g.participantCount());
    }

    private void leave(CommandSender s) {
        Player p = requirePlayer(s);
        if (p == null) return;
        Game g = plugin.games().getGame(p.getUniqueId());
        if (g == null) {
            sendKey(s, "not-joined");
            return;
        }
        String err = g.leave(p);
        sendKey(s, err != null ? err : "left");
    }

    private void test(CommandSender s, String[] args) {
        if (!requireAdmin(s)) return;
        Arena a = plugin.arenas().selected(s);
        if (a == null) {
            sendKey(s, "no-arena");
            return;
        }
        if (plugin.games().get(a).getState() != GameState.WAITING) {
            sendKey(s, "arena-busy");
            return;
        }
        boolean value;
        if (args.length > 1 && args[1].equalsIgnoreCase("on")) value = true;
        else if (args.length > 1 && args[1].equalsIgnoreCase("off")) value = false;
        else value = !a.isTestMode();
        a.setTestMode(value);
        plugin.arenas().save();
        sendKey(s, value ? "test-on" : "test-off", "arena", a.getName());
    }

    private void restore(CommandSender s, String[] args) {
        if (!requireAdmin(s)) return;
        Arena a = resolveArena(s, args, 1);
        if (a == null) return;
        String err = plugin.games().get(a).restoreNow();
        sendKey(s, err != null ? err : "restore-done");
    }

    private void reload(CommandSender s) {
        if (!requireAdmin(s)) return;
        plugin.reloadConfig();
        sendKey(s, "reloaded");
    }

    private void arena(CommandSender s, String[] args) {
        if (!requireAdmin(s)) return;
        if (args.length < 2) {
            for (String line : plugin.messages().list("help")) s.sendMessage(line);
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "create" -> arenaCreate(s, args);
            case "delete" -> arenaDelete(s, args);
            case "list" -> sendKey(s, "arena-list", "arenas", String.join(", ", plugin.arenas().names()));
            case "select" -> arenaSelect(s, args);
            case "info" -> arenaInfo(s, args);
            case "setcenter", "setradius", "setstarty" -> arenaEdit(s, sub, args);
            default -> sendKey(s, "unknown-subcommand");
        }
    }

    private void arenaCreate(CommandSender s, String[] args) {
        Player p = requirePlayer(s);
        if (p == null) return;
        if (args.length < 3 || !NAME.matcher(args[2]).matches()) {
            sendKey(s, "invalid-name");
            return;
        }
        if (plugin.arenas().get(args[2]) != null) {
            sendKey(s, "arena-exists", "arena", args[2]);
            return;
        }
        Arena a = plugin.arenas().create(args[2], p.getLocation());
        plugin.arenas().select(s, a);
        plugin.arenas().save();
        sendKey(s, "arena-created", "arena", a.getName(), "radius", a.getRadius());
    }

    private void arenaDelete(CommandSender s, String[] args) {
        Arena a = args.length > 2 ? plugin.arenas().get(args[2]) : null;
        if (a == null) {
            sendKey(s, "arena-not-found", "arena", args.length > 2 ? args[2] : "?");
            return;
        }
        if (plugin.games().get(a).getState() != GameState.WAITING) {
            sendKey(s, "arena-busy");
            return;
        }
        plugin.games().remove(a);
        plugin.arenas().delete(a.getName());
        plugin.arenas().save();
        sendKey(s, "arena-deleted", "arena", a.getName());
    }

    private void arenaSelect(CommandSender s, String[] args) {
        Arena a = args.length > 2 ? plugin.arenas().get(args[2]) : null;
        if (a == null) {
            sendKey(s, "arena-not-found", "arena", args.length > 2 ? args[2] : "?");
            return;
        }
        plugin.arenas().select(s, a);
        sendKey(s, "arena-selected", "arena", a.getName());
    }

    private void arenaInfo(CommandSender s, String[] args) {
        Arena a = resolveArena(s, args, 2);
        if (a == null) return;
        sendKey(s, "arena-info",
                "arena", a.getName(),
                "world", a.getWorldName(),
                "x", a.getCenterX(), "y", a.getCenterY(), "z", a.getCenterZ(),
                "radius", a.getRadius(),
                "starty", a.getStartY(),
                "speed", a.getSpeed().display(),
                "difficulty", a.getDifficulty().display(),
                "test", a.isTestMode());
    }

    private void arenaEdit(CommandSender s, String sub, String[] args) {
        Arena a = plugin.arenas().selected(s);
        if (a == null) {
            sendKey(s, "no-arena");
            return;
        }
        if (plugin.games().get(a).getState() != GameState.WAITING) {
            sendKey(s, "arena-busy");
            return;
        }
        switch (sub) {
            case "setcenter" -> {
                Player p = requirePlayer(s);
                if (p == null) return;
                a.setCenter(p.getLocation());
            }
            case "setradius", "setstarty" -> {
                if (args.length < 3) {
                    sendKey(s, "invalid-number");
                    return;
                }
                int value;
                try {
                    value = Integer.parseInt(args[2]);
                } catch (NumberFormatException ex) {
                    sendKey(s, "invalid-number");
                    return;
                }
                if (sub.equals("setradius")) a.setRadius(value);
                else a.setStartY(value);
            }
            default -> {
                return;
            }
        }
        plugin.arenas().save();
        sendKey(s, "arena-updated", "arena", a.getName());
    }

    // ------------------------------------------------------------------ tab

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            addMatches(out, SUBS, args[0]);
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "arena" -> addMatches(out, ARENA_SUBS, args[1]);
                case "join", "start", "stop", "restore" -> addMatches(out, plugin.arenas().names(), args[1]);
                case "test" -> addMatches(out, Arrays.asList("on", "off"), args[1]);
                default -> { }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("arena")) {
            switch (args[1].toLowerCase(Locale.ROOT)) {
                case "delete", "select", "info" -> addMatches(out, plugin.arenas().names(), args[2]);
                default -> { }
            }
        }
        return out;
    }

    private static void addMatches(List<String> out, List<String> options, String typed) {
        String prefix = typed.toLowerCase(Locale.ROOT);
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(prefix)) out.add(o);
        }
    }
}
