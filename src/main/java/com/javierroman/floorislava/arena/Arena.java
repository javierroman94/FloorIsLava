package com.javierroman.floorislava.arena;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Configuración de una arena circular. El área de juego es un cilindro
 * centrado en (centerX, centerZ) con el radio indicado.
 */
public class Arena {

    private final String name;
    private String worldName = "world";
    private int centerX;
    private int centerY;
    private int centerZ;
    private int radius = 30;
    private int startY = 60;
    private SpeedPreset speed = SpeedPreset.NORMAL;
    private double customIntervalSeconds = 10.0;
    private Difficulty difficulty = Difficulty.NORMAL;
    private boolean testMode;

    public Arena(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public int getCenterX() {
        return centerX;
    }

    public int getCenterY() {
        return centerY;
    }

    public int getCenterZ() {
        return centerZ;
    }

    public void setCenter(String worldName, int x, int y, int z) {
        this.worldName = worldName;
        this.centerX = x;
        this.centerY = y;
        this.centerZ = z;
    }

    public void setCenter(Location loc) {
        setCenter(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = Math.max(5, Math.min(300, radius));
    }

    public int getStartY() {
        return startY;
    }

    public void setStartY(int startY) {
        World w = getWorld();
        if (w != null) {
            startY = Math.max(w.getMinHeight(), Math.min(w.getMaxHeight() - 1, startY));
        }
        this.startY = startY;
    }

    public SpeedPreset getSpeed() {
        return speed;
    }

    public void setSpeed(SpeedPreset speed) {
        this.speed = speed;
    }

    public double getCustomIntervalSeconds() {
        return customIntervalSeconds;
    }

    public void setCustomIntervalSeconds(double seconds) {
        this.customIntervalSeconds = Math.max(1.0, Math.min(600.0, seconds));
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public boolean isTestMode() {
        return testMode;
    }

    public void setTestMode(boolean testMode) {
        this.testMode = testMode;
    }

    /** Segundos entre cada subida de lava según el preset actual. */
    public double intervalSeconds(FileConfiguration cfg) {
        if (speed == SpeedPreset.CUSTOM) {
            return customIntervalSeconds;
        }
        return cfg.getDouble("speed." + speed.name(), speed.defaultSeconds());
    }

    public long intervalTicks(FileConfiguration cfg) {
        return Math.max(1L, Math.round(intervalSeconds(cfg) * 20.0));
    }

    /** ¿Está la posición (en coordenadas continuas) dentro del círculo, con un margen extra? */
    public boolean insideCircle(double x, double z, double margin) {
        double dx = x - (centerX + 0.5);
        double dz = z - (centerZ + 0.5);
        double limit = radius + 0.5 + margin;
        return dx * dx + dz * dz <= limit * limit;
    }

    /** ¿Está el bloque (x, z) dentro del círculo, con un margen extra en bloques? */
    public boolean containsBlock(int x, int z, int margin) {
        return insideCircle(x + 0.5, z + 0.5, margin);
    }
}
