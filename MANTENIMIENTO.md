# Mantenimiento de Noxis Culture

## Regla permanente: presentación en cinco idiomas

La presentación multilingüe es parte permanente de Noxis Culture, no una tarea de una sola vez.

Cada vez que se actualice el README, el lore, las características del mod, las imágenes, los GIFs o cualquier texto de la presentación, se actualizan también **las cinco versiones**: inglés, español, ruso, chino simplificado y portugués.

- Ningún idioma queda desactualizado respecto del contenido original (el README en inglés).
- Todos los idiomas conservan el mismo diseño cinematográfico, las mismas imágenes y la misma identidad visual.
- Se traducen solo características confirmadas: nunca se presentan funciones futuras como si ya existieran.
- Si se agrega o se quita una sección, el cambio se hace en las cinco versiones.
- La detección automática del idioma y el selector manual siguen funcionando después de cada actualización.

## Dónde está cada cosa

| Qué | Dónde |
| :-- | :-- |
| README original (inglés, en GitHub) | `README.md` + imágenes en `docs/readme/` |
| Página de la presentación | `site/index.html`, `site/style.css` |
| Detección de idioma, selector, imágenes y enlaces (una sola vez para todos) | `site/app.js` |
| Textos de cada idioma (mismas claves en todos) | `site/i18n/en.js` (fuente), `es.js`, `ru.js`, `zh.js`, `pt.js` |
| Imágenes con texto, por idioma (banner y materiales) | `site/assets/i18n/banner-xx.png`, `materials-xx.png` |
| Comprobación de las cinco versiones | `tools/check-i18n.js` |
| Publicación en GitHub Pages | `.github/workflows/pages.yml` |

Las imágenes sin texto (Noxis, GIFs, sombreros, divisor) se toman directamente de `docs/readme/`: no se duplican. El inglés usa el `banner.png` y el `materials.png` originales.

## Cómo actualizar la presentación

1. Cambiar el README (inglés).
2. Llevar el mismo cambio a `site/i18n/en.js`.
3. Hacer el mismo cambio en `es.js`, `ru.js`, `zh.js` y `pt.js`:
   - traducción natural, con el mismo tono misterioso y fantástico;
   - los nombres propios no se traducen (Noxis, Noxus, Noyux, Nixil, Noxite…);
   - en español se usan los nombres que muestra el juego (`lang/es_*.json`);
   - en ruso, chino y portugués el juego todavía muestra los nombres en inglés: se agregan entre paréntesis con `<span class="ig">(…)</span>`.
4. Si cambia una imagen con texto (banner o materiales), regenerar también las cuatro versiones de `site/assets/i18n/`.
5. Si se agrega una sección nueva: agregar sus textos en los cinco archivos y dibujarla en `render()` de `site/app.js`.
6. Comprobar: `node tools/check-i18n.js` (también lo hace GitHub antes de publicar: si falta algo en un idioma, no publica).
7. Probar cada idioma con `?lang=en`, `?lang=es`, `?lang=ru`, `?lang=zh`, `?lang=pt`.

## Cómo elige el idioma la página

1. `?lang=xx` en la dirección (solo para probar; no se guarda).
2. La elección manual del selector, guardada en ese navegador.
3. El idioma preferido del navegador (`navigator.languages`): el primero de la lista que exista. `es*` → español, `ru*` → ruso, `zh*` → chino simplificado, `pt*` → portugués, `en*` → inglés.
4. Cualquier otro idioma → inglés.

En el selector, «Automático» borra la elección guardada y vuelve a la detección.

## GitHub Pages

Activación (una sola vez): **Settings → Pages → Build and deployment → Source: GitHub Actions**. Después, cada cambio en `site/`, `docs/readme/` o `tools/check-i18n.js` se publica solo. La dirección es **https://bouthex.github.io/NOXIS-Culture/** (también aparece en **Settings → Pages**).
