package com.javierroman.floorislava.game;

public enum GameState {
    /** Sin partida; se pueden apuntar jugadores. */
    WAITING,
    /** Cuenta atrás de gracia, la lava aún no sube. */
    STARTING,
    /** La lava está subiendo. */
    RUNNING,
    /** Partida terminada, mostrando resultados. */
    ENDING,
    /** Restaurando el terreno. */
    RESTORING
}
