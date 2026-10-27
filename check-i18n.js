// Comprueba que las cinco versiones de la presentación estén completas e iguales en estructura.
// Uso: node tools/check-i18n.js   (lo corre también el flujo de GitHub Pages antes de publicar)
'use strict';
const fs = require('fs');
const path = require('path');
const vm = require('vm');

const ROOT = path.join(__dirname, '..');
const SITE = path.join(ROOT, 'site');
const LANGS = ['en', 'es', 'ru', 'zh', 'pt'];
let errors = 0;
const fail = (m) => { console.error('✗ ' + m); errors++; };

function load(lang) {
  const file = path.join(SITE, 'i18n', lang + '.js');
  if (!fs.existsSync(file)) { fail('falta ' + path.relative(ROOT, file)); return null; }
  const ctx = { window: {} };
  vm.runInNewContext(fs.readFileSync(file, 'utf8'), ctx, { filename: file });
  const t = ctx.window.NOXIS_I18N && ctx.window.NOXIS_I18N[lang];
  if (!t) fail(lang + '.js no define window.NOXIS_I18N.' + lang);
  return t;
}

// Misma forma: mismas claves, mismos largos de listas, texto no vacío.
function shape(a, b, where, lang) {
  if (Array.isArray(a)) {
    if (!Array.isArray(b)) return fail(`${lang}: ${where} debería ser una lista`);
    if (a.length !== b.length) fail(`${lang}: ${where} tiene ${b.length} elementos (en: ${a.length})`);
    a.forEach((x, i) => b[i] !== undefined && shape(x, b[i], `${where}[${i}]`, lang));
  } else if (a && typeof a === 'object') {
    if (!b || typeof b !== 'object' || Array.isArray(b)) return fail(`${lang}: ${where} debería ser un objeto`);
    for (const k of Object.keys(a)) {
      if (!(k in b)) fail(`${lang}: falta ${where}.${k}`);
      else shape(a[k], b[k], `${where}.${k}`, lang);
    }
    for (const k of Object.keys(b)) if (!(k in a)) fail(`${lang}: sobra ${where}.${k} (no está en en.js)`);
  } else if (typeof b !== 'string' || !b.trim()) {
    fail(`${lang}: ${where} está vacío`);
  } else {
    // Los enlaces y el código tienen que seguir estando.
    for (const tok of (a.match(/\{[A-Z_]+\}|<code>[^<]*<\/code>/g) || [])) {
      if (!b.includes(tok)) fail(`${lang}: ${where} perdió ${tok}`);
    }
  }
}

const en = load('en');
for (const lang of LANGS.slice(1)) {
  const t = load(lang);
  if (en && t) shape(en, t, lang, lang);
  for (const img of ['banner', 'materials']) {
    const p = path.join(SITE, 'assets', 'i18n', `${img}-${lang}.png`);
    if (!fs.existsSync(p)) fail('falta ' + path.relative(ROOT, p));
  }
}
const app = fs.readFileSync(path.join(SITE, 'app.js'), 'utf8');
for (const lang of LANGS) if (!app.includes(`'${lang}'`)) fail(`app.js no conoce el idioma ${lang}`);
for (const f of ['banner.png', 'materials.png', 'eyes.png', 'hats.png', 'divider.png', 'friends.gif', 'crystal.gif', 'books.gif', 'bowl.gif', 'wake.gif']) {
  if (!fs.existsSync(path.join(ROOT, 'docs', 'readme', f))) fail('falta docs/readme/' + f);
}

if (errors) { console.error(`\n${errors} problema(s): las cinco versiones tienen que estar completas.`); process.exit(1); }
console.log('✓ Presentación completa en: ' + LANGS.join(', '));
