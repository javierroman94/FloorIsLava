package com.javierroman.floorislava.arena;

public enum Difficulty {
    FACIL("Fácil", 30, 1),
    NORMAL("Normal", 15, 1),
    DIFICIL("Difícil", 5, 2);

    private final String display;
    private final int defaultGrace;
    private final int defaultLayers;

    Difficulty(String display, int defaultGrace, int defaultLayers) {
        this.display = display;
        this.defaultGrace = defaultGrace;
        this.defaultLayers = defaultLayers;
    }

    public String display() {
        return display;
    }

    public int defaultGrace() {
        return defaultGrace;
    }

    public int defaultLayers() {
        return defaultLayers;
    }

    public Difficulty next() {
        Difficulty[] all = values();
        return all[(ordinal() + 1) % all.length];
    }

    public static Difficulty parse(String name, Difficulty fallback) {
        if (name == null) return fallback;
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
