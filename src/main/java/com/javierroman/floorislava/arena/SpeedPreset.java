package com.javierroman.floorislava.arena;

public enum SpeedPreset {
    LENTA("Lenta", 20.0),
    NORMAL("Normal", 10.0),
    RAPIDA("Rápida", 5.0),
    CUSTOM("Personalizada", 10.0);

    private final String display;
    private final double defaultSeconds;

    SpeedPreset(String display, double defaultSeconds) {
        this.display = display;
        this.defaultSeconds = defaultSeconds;
    }

    public String display() {
        return display;
    }

    public double defaultSeconds() {
        return defaultSeconds;
    }

    public SpeedPreset next() {
        SpeedPreset[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public static SpeedPreset parse(String name, SpeedPreset fallback) {
        if (name == null) return fallback;
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
