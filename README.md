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
- No guarda snapshot
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

## ⚙️ Comandos

```text
/floorislava start
/floorislava stop
/floorislava setup

