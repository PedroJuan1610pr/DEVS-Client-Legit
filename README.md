# Devs Client Legit — Minecraft **26.1.2**

Puerto del cliente legit a Minecraft 26.1.2.

## Requisitos

- **Java 25** (obligatorio en 26.1+)
- Fabric Loader **0.19.5+**
- Fabric API para **26.1.2**

## Java 25 (Windows)

En `gradle.properties` ya va configurado:

```
org.gradle.java.home=C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot
```

Si Gradle dice que la ruta es invalida, ejecuta:
```
dir "C:\Program Files\Eclipse Adoptium"
```
y corrige el nombre de la carpeta en `gradle.properties`.

## Build

```bash
./gradlew build
```

Jar → `build/libs/devs-client-legit-2.0.0-26.1.2.jar`

## Notas del puerto

Minecraft **26.1** ya no está ofuscado. Fabric dejó de mantener Yarn oficial.

Este proyecto usa **Modern Yarn** (RelativityMC) para conservar nombres parecidos a Yarn y facilitar el puerto desde 1.21.11.

Es posible que al compilar salgan errores por cambios de API entre 1.21.11 y 26.1.2 (métodos renombrados, mixins, render, HUD). Si falla `./gradlew build`, el log de errores indica qué hay que ajustar.

### Controles
- **Right Shift** → ClickGUI  
- **H** → editor de HUD  

### Imágenes (Image Display)
Carpeta: `.minecraft/config/devs/images/`
