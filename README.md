# Noxis Culture

Mod de Minecraft (Fabric · Minecraft 26.2) sobre los Noxis: una pequeña raza alienígena
de sombrero de copa que comercia en aldeas de Ladrillo de Noxita.

## Contenido actual (Etapa 1)
- Aldeano Noxis (`/summon noxis_culture:noxis_villager`)
- Ladrillos de Noxita
- Pico del Cosmos (habilidad: Pulso Estelar)

## Compilar
GitHub Actions compila el mod automáticamente en cada cambio.
El `.jar` se descarga desde la pestaña **Actions** → última ejecución → **Artifacts**.

## Estructura
- `block/`, `item/`, `entity/`, `tag/` → registro modular, un sistema por clase
- `src/client/` → modelo y renderer (solo cliente)

Las texturas actuales son provisorias.
