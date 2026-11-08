package com.javierroman.floorislava.gui;

import com.javierroman.floorislava.FloorIsLava;
import com.javierroman.floorislava.arena.Arena;
import com.javierroman.floorislava.arena.SpeedPreset;
import com.javierroman.floorislava.game.Game;
import com.javierroman.floorislava.game.GameState;
import com.javierroman.floorislava.util.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** GUI de configuración de la arena (comando /fil setup). */
public class SetupGUI implements Listener {

    private static final int SLOT_ARENA = 4;
    private static final int SLOT_RADIUS = 10;
    private static final int SLOT_CENTER = 11;
    private static final int SLOT_START_Y = 12;
    private static final int SLOT_INTERVAL = 13;
    private static final int SLOT_DIFFICULTY = 19;
    private static final int SLOT_SPEED = 20;
    private static final int SLOT_TEST = 21;
    private static final int SLOT_RESTORE = 23;
    private static final int SLOT_START = 48;
    private static final int SLOT_STOP = 50;
    private static final int SLOT_CLOSE = 53;

    private static final class Holder implements InventoryHolder {
        private Inventory inventory;
        private String arenaName;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private final FloorIsLava plugin;

    public SetupGUI(FloorIsLava plugin) {
        this.plugin = plugin;
    }

    public void open(Player p) {
        Arena arena = plugin.arenas().selected(p);
        if (arena == null) {
            plugin.messages().send(p, "no-arena");
            return;
        }
        Holder holder = new Holder();
        holder.arenaName = arena.getName();
        String title = Messages.color(plugin.getConfig().getString("gui.title", "&6&lFloor Is Lava"));
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.inventory = inv;
        render(inv, arena);
        p.openInventory(inv);
    }

    // ------------------------------------------------------------------ render

    private void render(Inventory inv, Arena a) {
        ItemStack pane = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, pane);
        }
        Game g = plugin.games().get(a);
        String status = g.getState() == GameState.WAITING ? "&aLibre" : "&c" + g.getState().name();

        inv.setItem(SLOT_ARENA, item(Material.MAP, "&6Arena: &e" + a.getName(),
                "&7Estado: " + status,
                "&7Mundo: &f" + a.getWorldName(),
                "",
                "&eClic &7para cambiar de arena"));

        inv.setItem(SLOT_RADIUS, item(Material.ENDER_PEARL, "&6Radio del área: &e" + a.getRadius(),
                "",
                "&eClic izq. &7+5   &eClic der. &7-5",
                "&eShift + clic &7±1"));

        inv.setItem(SLOT_CENTER, item(Material.COMPASS, "&6Centro de la arena",
                "&7Actual: &f" + a.getCenterX() + " " + a.getCenterY() + " " + a.getCenterZ(),
                "",
                "&eClic &7para fijar tu posición como centro"));

        inv.setItem(SLOT_START_Y, item(Material.LADDER, "&6Altura inicial de la lava: &eY=" + a.getStartY(),
                "",
                "&eClic izq. &7+5   &eClic der. &7-5",
                "&eShift + clic &7±1"));

        inv.setItem(SLOT_INTERVAL, item(Material.CLOCK,
                "&6Intervalo de subida: &e" + trim(a.intervalSeconds(plugin.getConfig())) + "s",
                "&7Preset: &f" + a.getSpeed().display(),
                "",
                "&eClic izq. &7+1s   &eClic der. &7-1s",
                "&eShift + clic &7±5s &8(pasa a Personalizada)"));

        inv.setItem(SLOT_DIFFICULTY, item(Material.IRON_SWORD, "&6Dificultad: &e" + a.getDifficulty().display(),
                "&7Segundos de gracia: &f" + plugin.getConfig().getInt(
                        "difficulty." + a.getDifficulty().name() + ".grace-seconds", a.getDifficulty().defaultGrace()),
                "&7Capas por subida: &f" + plugin.getConfig().getInt(
                        "difficulty." + a.getDifficulty().name() + ".layers-per-step", a.getDifficulty().defaultLayers()),
                "",
                "&eClic &7para cambiar"));

        inv.setItem(SLOT_SPEED, item(Material.FEATHER, "&6Velocidad: &e" + a.getSpeed().display(),
                "&7Lenta / Normal / Rápida / Personalizada",
                "",
                "&eClic &7para cambiar de preset"));

        inv.setItem(SLOT_TEST, item(a.isTestMode() ? Material.LIME_DYE : Material.GRAY_DYE,
                "&6Modo test: " + (a.isTestMode() ? "&aACTIVADO" : "&cdesactivado"),
                "&7Lava sin daño y sin registrar",
                "&7cambios de jugadores.",
                "",
                "&eClic &7para alternar"));

        inv.setItem(SLOT_RESTORE, item(Material.TNT, "&6Restaurar área",
                "&7Restaura al instante el terreno",
                "&7guardado en el snapshot.",
                "",
                "&eClic &7para restaurar"));

        inv.setItem(SLOT_START, item(Material.LIME_CONCRETE, "&a&lINICIAR PARTIDA",
                "&7Jugadores apuntados: &f" + g.participantCount()));
        inv.setItem(SLOT_STOP, item(Material.RED_CONCRETE, "&c&lDETENER PARTIDA"));
        inv.setItem(SLOT_CLOSE, item(Material.BARRIER, "&cCerrar"));
    }

    private static String trim(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.valueOf(d);
    }

    private static ItemStack item(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(Messages.color(name));
            if (lore.length > 0) {
                List<String> lines = new ArrayList<>();
                for (String l : lore) lines.add(Messages.color(l));
                meta.setLore(lines);
            }
            stack.setItemMeta(meta);
        }
        return stack;
    }

    // ------------------------------------------------------------------ eventos

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof Holder holder)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != top) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!p.hasPermission("floorislava.admin")) {
            p.closeInventory();
            return;
        }
        Arena arena = plugin.arenas().get(holder.arenaName);
        if (arena == null) {
            p.closeInventory();
            return;
        }
        handle(p, holder, arena, e.getSlot(), e.getClick());
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Holder) {
            e.setCancelled(true);
        }
    }

    private void handle(Player p, Holder holder, Arena a, int slot, ClickType click) {
        Game g = plugin.games().get(a);
        boolean busy = g.getState() != GameState.WAITING;
        boolean left = click.isLeftClick();
        boolean shift = click.isShiftClick();
        boolean changed = false;

        switch (slot) {
            case SLOT_ARENA -> {
                List<Arena> all = plugin.arenas().all();
                if (all.size() > 1) {
                    int idx = 0;
                    for (int i = 0; i < all.size(); i++) {
                        if (all.get(i).getName().equalsIgnoreCase(a.getName())) idx = i;
                    }
                    Arena next = all.get((idx + 1) % all.size());
                    plugin.arenas().select(p, next);
                    holder.arenaName = next.getName();
                    click(p);
                    render(holder.getInventory(), next);
                }
                return;
            }
            case SLOT_RADIUS -> {
                if (busy) { deny(p); return; }
                int step = shift ? 1 : 5;
                a.setRadius(a.getRadius() + (left ? step : -step));
                changed = true;
            }
            case SLOT_CENTER -> {
                if (busy) { deny(p); return; }
                a.setCenter(p.getLocation());
                changed = true;
            }
            case SLOT_START_Y -> {
                if (busy) { deny(p); return; }
                int step = shift ? 1 : 5;
                a.setStartY(a.getStartY() + (left ? step : -step));
                changed = true;
            }
            case SLOT_INTERVAL -> {
                if (busy) { deny(p); return; }
                double step = shift ? 5.0 : 1.0;
                double current = a.intervalSeconds(plugin.getConfig());
                a.setCustomIntervalSeconds(current + (left ? step : -step));
                a.setSpeed(SpeedPreset.CUSTOM);
                changed = true;
            }
            case SLOT_DIFFICULTY -> {
                if (busy) { deny(p); return; }
                a.setDifficulty(a.getDifficulty().next());
                changed = true;
            }
            case SLOT_SPEED -> {
                if (busy) { deny(p); return; }
                SpeedPreset next = a.getSpeed().next();
                if (next == SpeedPreset.CUSTOM && a.getSpeed() != SpeedPreset.CUSTOM) {
                    a.setCustomIntervalSeconds(a.intervalSeconds(plugin.getConfig()));
                }
                a.setSpeed(next);
                changed = true;
            }
            case SLOT_TEST -> {
                if (busy) { deny(p); return; }
                a.setTestMode(!a.isTestMode());
                changed = true;
            }
            case SLOT_RESTORE -> {
                String err = g.restoreNow();
                p.sendMessage(plugin.messages().prefixed(err == null ? "restore-done" : err));
            }
            case SLOT_START -> {
                String err = plugin.games().startFor(p, a);
                if (err == null) {
                    p.closeInventory();
                    plugin.messages().send(p, "started", "arena", a.getName());
                    return;
                }
                plugin.messages().send(p, err);
            }
            case SLOT_STOP -> {
                String err = g.stop();
                p.sendMessage(plugin.messages().prefixed(err == null ? "stopped" : err, "arena", a.getName()));
            }
            case SLOT_CLOSE -> {
                p.closeInventory();
                return;
            }
            default -> {
                return;
            }
        }

        click(p);
        if (changed) plugin.arenas().save();
        render(holder.getInventory(), a);
    }

    private void deny(Player p) {
        plugin.messages().send(p, "arena-busy");
    }

    private void click(Player p) {
        plugin.effects().playByName(p, "UI_BUTTON_CLICK", 0.6f, 1.0f);
    }
}
