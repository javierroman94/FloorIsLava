# 🔥 Floor Is Lava

**Floor Is Lava** es un plugin de minijuego para **Spigot/Paper** en el que la lava asciende progresivamente y los jugadores deben sobrevivir hasta que solo quede uno en pie.

Está diseñado para **servidores medianos y grandes**, con un fuerte enfoque en **rendimiento**, **configuración mediante GUI**, **soporte multi-arenas real** y **restauración completa del terreno sin necesidad de WorldEdit**.

---

## ✨ Características principales

### 🌋 Lava progresiva optimizada
- Subida por capas
- Protección automática por TPS
- Velocidad dinámica
- Presets de velocidad: **LENTA / NORMAL / RÁPIDA / CUSTOM**

### 🧠 Configuración mediante GUI
- Radio del área
- Centro de la arena
- Altura inicial de la lava
- Intervalo de subida
- Dificultad
- Presets de velocidad
- Modo Test
- Restaurar área instantáneamente

### 🗺️ Multi-arenas real
- Múltiples arenas registradas
- Selección de arena desde la GUI
- Snapshots independientes por arena

### 💾 Sistema de Snapshot ligero (sin WorldEdit)
- Guarda únicamente los bloques modificados
- Restauración instantánea o gradual (anti-lag extremo)
- No deja rastros de lava

### 🧪 Modo Test
- Lava sin daño
- No guarda snapshot en disco ni registra cambios de jugadores
- Ideal para pruebas y configuración de eventos

### 🏆 Sistema TOP 3
- Anuncio automático en el chat
- Holograma final
- Compatible con animaciones

### 🔒 Control total del borde
- Bloquea completamente el movimiento fuera del área
- Compatible con WorldBorder visual

### 👻 Sistema de eliminación
- Cambio automático a modo espectador al morir
- Teletransporte seguro
- Mensajes de eliminación configurables

### 🔊 Sonidos y títulos configurables
- Interruptor global de sonidos
- Activación/desactivación individual por evento

### 📡 API pública
- Eventos disponibles para otros plugins
- Integración con **PlaceholderAPI** y **TAB**

---

## 📦 Requisitos

- **Java:** 17 o superior
- **Servidor:** Spigot o Paper **1.20+**

### Opcional
- PlaceholderAPI
- TAB
- Plugin de hologramas (para animaciones avanzadas)

---

## 🛠️ Compilación e instalación

```bash
mvn clean package
```

El `.jar` queda en `target/FloorIsLava-1.0.0.jar`. Cópialo a la carpeta `plugins/` del servidor y reinícialo.

Al arrancar por primera vez se genera `config.yml` y, si no hay arenas, se crea una arena `default` en el spawn del primer mundo (radio 30, lava 10 bloques por debajo del spawn).

---

## ⚙️ Comandos

Alias: `/fil`, `/floorlava`

| Comando | Descripción | Permiso |
|---|---|---|
| `/floorislava setup` | Abre la GUI de configuración | `floorislava.admin` |
| `/floorislava start [arena]` | Inicia la partida | `floorislava.admin` |
| `/floorislava stop [arena]` | Detiene la partida | `floorislava.admin` |
| `/floorislava join [arena]` | Apuntarse a una arena | `floorislava.play` |
| `/floorislava leave` | Salir de la partida | `floorislava.play` |
| `/floorislava arena create <nombre>` | Crea una arena con centro en tu posición | `floorislava.admin` |
| `/floorislava arena delete <nombre>` | Elimina una arena | `floorislava.admin` |
| `/floorislava arena list` | Lista las arenas | `floorislava.admin` |
| `/floorislava arena select <nombre>` | Selecciona la arena con la que trabajas | `floorislava.admin` |
| `/floorislava arena info [nombre]` | Muestra la configuración de la arena | `floorislava.admin` |
| `/floorislava arena setcenter` | Fija tu posición como centro | `floorislava.admin` |
| `/floorislava arena setradius <n>` | Cambia el radio (5–300) | `floorislava.admin` |
| `/floorislava arena setstarty <y>` | Cambia la altura inicial de la lava | `floorislava.admin` |
| `/floorislava test [on\|off]` | Activa/desactiva el modo test de la arena seleccionada | `floorislava.admin` |
| `/floorislava restore [arena]` | Restaura el área al instante | `floorislava.admin` |
| `/floorislava reload` | Recarga `config.yml` | `floorislava.admin` |

### Inicio rápido

1. `/fil arena create mi_arena` en el centro de la zona de juego.
2. `/fil setup` para ajustar radio, altura de la lava, velocidad y dificultad.
3. Cada jugador hace `/fil join mi_arena`.
4. `/fil start` — empieza el periodo de gracia y después la lava sube.

Para probar sin otros jugadores, activa el **modo test**: `/fil test on` y `/fil start` (te apunta automáticamente).

---

## 🧭 GUI de configuración

| Botón | Clic izq. | Clic der. | Shift + clic |
|---|---|---|---|
| Radio del área | +5 | −5 | ±1 |
| Centro de la arena | Fija tu posición | — | — |
| Altura inicial de la lava | +5 | −5 | ±1 |
| Intervalo de subida | +1 s | −1 s | ±5 s (pasa a CUSTOM) |
| Dificultad | Cambia | — | — |
| Velocidad (preset) | Cambia | — | — |
| Modo test | Alterna | — | — |
| Restaurar área | Restaura | — | — |
| Iniciar / Detener | Inicia / detiene la partida | — | — |

Mientras una arena tiene una partida en curso solo se pueden usar los botones de iniciar, detener y restaurar.

---

## 🔧 Configuración (`config.yml`)

- `speed.*` — segundos entre subidas para LENTA / NORMAL / RAPIDA (CUSTOM se define por arena).
- `difficulty.*` — segundos de gracia y capas por subida para FACIL / NORMAL / DIFICIL.
- `lava.tps-pause-below` — si el TPS baja de este valor, la lava deja de añadir capas hasta recuperarse. Por encima, la velocidad se ajusta dinámicamente y el número de bloques por tick se reduce según el TPS.
- `restore.mode` — `instant` o `gradual`; `restore.blocks-per-tick` para el modo gradual.
- `end.delay-seconds` — tiempo que se muestra el TOP 3 antes de devolver a los jugadores.
- `border.visual` — WorldBorder visual para los jugadores de la partida.
- `sounds.*` y `titles.*` — interruptor global y configuración individual por evento (`countdown`, `start`, `lava-rise`, `elimination`, `win`).
- `messages.*` — todos los textos, con códigos de color `&`.

Las arenas se guardan en `arenas.yml`.

---

## 💾 Snapshots y restauración

- Solo se guarda el estado **original** de los bloques que cambian (lava, bloques rotos o colocados, explosiones, fuego, flujos de agua, cubos…).
- Al terminar la partida el terreno se restaura según `restore.mode`.
- Mientras la partida está en marcha, el snapshot se guarda en disco cada 30 s (`plugins/FloorIsLava/snapshots/`). Si el servidor se cierra de golpe, se restaura al arrancar.
- **Modo test:** la lava sí se registra en memoria (para poder limpiarla al terminar), pero no se guarda nada en disco ni se registran cambios de jugadores.

Limitaciones: se restaura el tipo y estado de cada bloque, pero **no el contenido de cofres u otros contenedores**, ni entidades.

---

## 📡 API pública

Eventos de Bukkit disponibles para otros plugins (paquete `com.javierroman.floorislava.api.event`):

| Evento | Cuándo se dispara |
|---|---|
| `GameStartEvent` | Al comenzar una partida (arena, jugadores) |
| `LavaRiseEvent` | Cada vez que la lava sube (arena, nueva Y) |
| `PlayerEliminatedEvent` | Al eliminar a un jugador (jugador, arena, puesto) |
| `GameEndEvent` | Al terminar una partida (ganador, TOP 3, si fue natural) |

```java
@EventHandler
public void onEnd(GameEndEvent e) {
    Bukkit.broadcastMessage("Ganó " + e.getWinner() + " en " + e.getArena().getName());
}
```

Para animaciones con plugins de hologramas, escucha `GameEndEvent` y usa `getTop()`.

### PlaceholderAPI / TAB

| Placeholder | Valor |
|---|---|
| `%floorislava_state%` | `WAITING`, `STARTING`, `RUNNING`, `ENDING` o `RESTORING` |
| `%floorislava_arena%` | Nombre de la arena |
| `%floorislava_alive%` | Jugadores vivos |
| `%floorislava_players%` | Jugadores apuntados |
| `%floorislava_lava_y%` | Altura actual de la lava |
| `%floorislava_radius%` | Radio de la arena |
| `%floorislava_test%` | `true` / `false` |
| `%floorislava_top1%` … `%floorislava_top3%` | TOP 3 de la última partida |

Estos placeholders se pueden usar directamente en la configuración de **TAB**.

---

## 📄 Licencia

[MIT](LICENSE) © 2026 Javier Roman
