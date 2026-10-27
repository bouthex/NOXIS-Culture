# Pinturas y documentos de los Noxis (etapa 2)

Notas técnicas para mantener y ampliar la colección. No es lore.

## Pinturas

- **53 obras originales** (pixel art de 16×16: marco de 1 px y lienzo de 14×14), en
  `src/main/resources/assets/noxis_culture/textures/block/noxis_painting/art_NN.png`.
- **Identidad de la obra = su número** (`art`). Viaja:
  - en el objeto: `minecraft:custom_model_data` (float = número; elige el ícono) y
    `minecraft:custom_data` `{art, painter, day}`;
  - en el bloque colocado: propiedad de bloque `art` (0–52), más `facing` y `standing`
    (colgada en la pared o apoyada en un caballetito en el piso).
- Al romper el bloque se recupera **la misma obra** (sin el autor ni el día: el bloque no tiene
  entidad de bloque para no cargar el mundo).
- Los Noxis encuentran las obras por **puntos de interés** (`noxis_culture:painting`), sin revisar
  bloque por bloque.

### Familias de estilo

Cada artista tiene una familia preferida (`art_style`, guardada en la entidad) y pinta de ella el 80 %
de las veces; nunca repite la última obra.

| Familia | Tema | Obras |
|---|---|---|
| 0 | Recuerdos tiernos | 0 flores amarillas · 1 flores violetas · 2 una flor · 3 bichitos de luz · 4 hongo · 5 farol · 6 gato en la ventana · 7 mariposas · 48 retrato de un Noxis |
| 1 | Paisajes grises y gastados | 8 colinas grises · 9 luz tenue · 10 arco roto · 11 árbol seco · 12 llanura de polvo · 13 el último cristal · 14 lago gris · 15 escalera gastada |
| 2 | Paisajes y viajes | 16 colinas y sol · 17 montañas y lago · 18 bosque · 19 atardecer en el mar · 20 desierto · 21 Nether · 22 End · 23 isla · 49 noche con luna · 50 Noxis bajo las estrellas |
| 3 | Arquitectura y lugares | 24 torre · 25 puente · 26 casa iluminada · 27 pilares · 28 portón de piedra · 29 molino · 30 casa-pecera · 31 faro · 52 pecera bajo las estrellas |
| 4 | Formas inquietantes | 32 ojos en la oscuridad · 33 sin rostro · 34 grieta de luz · 35 espiral · 36 sombra larga · 37 pozo · 38 luna roja · 39 manos · 51 mirando desde la oscuridad |
| 5 | Ceremonial | 40 mandala · 41 gema en forma de lágrima · 42 rombos · 43 racimo de cristales · 44 concéntricos · 45 signo de estrella · 46 estudio de mineral · 47 luces gemelas |

Ninguna obra contiene escritura, letras ni glifos.

### Agregar obras

1. Agregar el diseño al final de `tools/paintings/gen.py` (nunca en el medio: los números existentes
   no se reordenan) y correrlo (`python3 tools/paintings/gen.py`, necesita Pillow).
2. Sumar su familia al final de `FAMILY_OF` en `NoxisPaintings.java`.
3. Subir `N` en `tools/paintings/assets.py` y correrlo: regenera modelos de pared/caballete, el
   blockstate y el modelo del ítem.
4. Nada más: la propiedad `ART` del bloque y la pestaña creativa toman el total de
   `NoxisPaintings.COUNT`.

## Documentos (infraestructura, sin idioma)

- Tres formatos: **libro** (tapas, lomo, páginas), **cuaderno** (tapa y anillado) y **hoja** suelta,
  con modelos 3D propios (`models/block/noxis_book.json`, `noxis_notebook.json`, `noxis_sheet.json`).
- Datos de cada documento (`minecraft:custom_data`):
  `{doc_id: UUID, kind, author, created, format: 1, language: "pending", pages: []}`.
  - `doc_id` lo identifica para siempre (aunque se lleve, se apoye o se recoja).
  - `pages` está **vacío a propósito**: el idioma de los Noxis todavía no existe. No se generan
    textos, palabras, alfabetos ni símbolos.
  - `format` permite migrar el contenido cuando se defina el idioma.
- Se pueden guardar (inventario privado del Noxis), llevar, y dejar en el mundo encima de un mueble
  de la etiqueta `noxis_culture:document_surfaces`. El bloque guarda **la pila exacta** en su entidad de
  bloque y la devuelve una sola vez al romperlo.

### Pendiente para la etapa del idioma

- Definir el idioma (alfabeto, palabras, reglas) — fuera de esta etapa.
- Llenar `pages` y mostrarlas (pantalla de lectura).
- Decidir qué escriben los Noxis según su personalidad y lo que descubrieron.
- Que los Noxis puedan volver a levantar documentos (hoy solo los recogen los jugadores al romper el
  bloque).
