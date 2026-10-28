/*
 * Noxis Culture — presentación multilingüe.
 *
 * Idiomas: en (predeterminado), es, ru, zh (chino simplificado), pt.
 * Orden para elegir el idioma:
 *   1. ?lang=xx en la dirección (para probar; no se guarda),
 *   2. la elección manual guardada en este navegador (localStorage),
 *   3. el idioma preferido del navegador (navigator.languages): el primero de la lista que tengamos,
 *   4. inglés.
 * Solo se descarga el archivo del idioma elegido (i18n/xx.js): nunca se muestran varios a la vez.
 *
 * Los textos viven en i18n/*.js (un archivo por idioma, todos con las mismas claves).
 * Las imágenes y los enlaces viven acá, una sola vez para todos los idiomas.
 */
(function () {
  'use strict';

  var SUPPORTED = ['en', 'es', 'ru', 'zh', 'pt'];
  var NATIVE = { en: 'English', es: 'Español', ru: 'Русский', zh: '简体中文', pt: 'Português' };
  var STORAGE_KEY = 'noxis-culture.lang';

  // Imágenes compartidas (las mismas del README, en assets/). Banner y materiales tienen texto: van por idioma.
  var IMG = {
    divider: 'assets/divider.png',
    eyes: 'assets/eyes.png',
    hats: 'assets/hats.png',
    wake: 'assets/wake.gif',
    cards: ['assets/friends.gif', 'assets/crystal.gif', 'assets/books.gif', 'assets/bowl.gif'],
    banner: function (lang) { return lang === 'en' ? 'assets/banner.png' : 'assets/i18n/banner-' + lang + '.png'; },
    materials: function (lang) { return lang === 'en' ? 'assets/materials.png' : 'assets/i18n/materials-' + lang + '.png'; }
  };

  // Repositorio en GitHub: se deduce de la dirección de GitHub Pages (usuario.github.io/repositorio/).
  function repoUrl() {
    var host = location.hostname.toLowerCase();
    var m = host.match(/^([a-z0-9-]+)\.github\.io$/);
    if (!m) return 'https://github.com/bouthex/NOXIS-Culture';   // vista previa local
    var seg = location.pathname.split('/').filter(Boolean)[0];
    return 'https://github.com/' + m[1] + '/' + (seg || (m[1] + '.github.io'));
  }
  var REPO = repoUrl();
  var LINKS = {
    FABRIC_INSTALLER: 'https://fabricmc.net/use/installer/',
    MODRINTH: 'https://modrinth.com/mod/fabric-api',
    CURSEFORGE: 'https://www.curseforge.com/minecraft/mc-mods/fabric-api',
    ACTIONS: REPO ? REPO + '/actions' : null,
    README: REPO ? REPO + '#readme' : null
  };

  // ------------------------------------------------------------------ idioma

  function primary(tag) { return String(tag || '').toLowerCase().split(/[-_]/)[0]; }

  function detect() {
    var list = (navigator.languages && navigator.languages.length) ? navigator.languages : [navigator.language || navigator.userLanguage || 'en'];
    for (var i = 0; i < list.length; i++) {
      var p = primary(list[i]);
      if (SUPPORTED.indexOf(p) >= 0) return p;   // zh-CN, zh-TW, zh-Hans... -> zh (chino simplificado)
    }
    return 'en';                                  // cualquier otro idioma
  }

  function stored() {
    try { var v = localStorage.getItem(STORAGE_KEY); return SUPPORTED.indexOf(v) >= 0 ? v : null; } catch (e) { return null; }
  }
  function store(v) {
    try { if (v) localStorage.setItem(STORAGE_KEY, v); else localStorage.removeItem(STORAGE_KEY); } catch (e) { /* sin almacenamiento: solo esta visita */ }
  }
  function fromUrl() {
    try { var v = new URLSearchParams(location.search).get('lang'); return SUPPORTED.indexOf(v) >= 0 ? v : null; } catch (e) { return null; }
  }
  function choose() { return fromUrl() || stored() || detect(); }

  window.NOXIS_I18N = window.NOXIS_I18N || {};
  function load(lang) {
    return new Promise(function (resolve, reject) {
      if (window.NOXIS_I18N[lang]) return resolve(window.NOXIS_I18N[lang]);
      var s = document.createElement('script');
      s.src = 'i18n/' + lang + '.js';
      s.onload = function () { window.NOXIS_I18N[lang] ? resolve(window.NOXIS_I18N[lang]) : reject(new Error('empty ' + lang)); };
      s.onerror = function () { reject(new Error('missing ' + lang)); };
      document.head.appendChild(s);
    });
  }

  // ------------------------------------------------------------------ dibujar

  function links(html) {
    return String(html).replace(/\{(\w+)\}/g, function (all, key) {
      if (!(key in LINKS)) return all;
      return LINKS[key] || '#install';
    });
  }
  function divider() { return '<img class="divider" src="' + IMG.divider + '" alt="" aria-hidden="true">'; }
  function esc(s) { return String(s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;'); }
  function img(src, alt, cls, lazy) {
    return '<img class="' + (cls || '') + '" src="' + src + '" alt="' + esc(alt) + '"' + (lazy ? ' loading="lazy" decoding="async"' : '') + '>';
  }

  function render(t, lang) {
    var h = [];
    // Portada
    h.push('<header class="hero reveal">' + img(IMG.banner(lang), t.hero.alt, 'banner') +
      '<p class="intro">' + t.hero.intro.join('<br>') + '</p>' +
      '<nav class="nav" aria-label="' + esc(t.ui.sections) + '">' +
      '<a href="#universe">' + t.nav.universe + '</a><span aria-hidden="true">·</span>' +
      '<a href="#discover">' + t.nav.discover + '</a><span aria-hidden="true">·</span>' +
      '<a href="#secrets">' + t.nav.secrets + '</a><span aria-hidden="true">·</span>' +
      '<a href="#install">' + t.nav.install + '</a></nav></header>');
    h.push(divider());

    // El universo
    h.push('<section id="universe" class="reveal"><h2>' + t.universe.title + '</h2>' +
      img(IMG.eyes, t.universe.alt, 'wide', true) +
      '<blockquote class="lore">' + t.universe.lore.map(function (st) { return '<p>' + st.join('<br>') + '</p>'; }).join('') + '</blockquote></section>');
    h.push(divider());

    // Descubrí su mundo
    var cards = t.discover.cards.map(function (c, i) {
      return '<figure class="card">' + img(IMG.cards[i], c.alt, 'gif', true) +
        '<figcaption><b>' + c.title + '</b><span>' + c.text + '</span></figcaption></figure>';
    }).join('');
    h.push('<section id="discover" class="reveal"><h2>' + t.discover.title + '</h2>' +
      '<p class="note">' + t.discover.note + '</p><div class="cards">' + cards + '</div>' +
      '<h3>' + t.discover.relics.title + '</h3>' + img(IMG.materials(lang), t.discover.relics.alt, 'wide', true) +
      '<ul class="relics">' + t.discover.relics.items.map(function (x) { return '<li>' + x + '</li>'; }).join('') + '</ul>' +
      '<h3>' + t.discover.hats.title + '</h3>' + img(IMG.hats, t.discover.hats.alt, 'wide', true) +
      '<p class="caption">' + t.discover.hats.text + '</p></section>');
    h.push(divider());

    // Pequeñas vidas, grandes secretos
    h.push('<section id="secrets" class="reveal"><h2>' + t.secrets.title + '</h2><div class="split">' +
      img(IMG.wake, t.secrets.alt, 'gif', true) + '<div class="prose">' +
      t.secrets.paras.map(function (p) { return '<p>' + p + '</p>'; }).join('') +
      '<p class="whisper">' + t.secrets.whisper + '</p></div></div>' +
      '<p class="caption">' + t.secrets.tagline + '</p></section>');
    h.push(divider());

    // Instalación
    var rows = t.install.reqs.map(function (r) { return '<tr><td>' + r[0] + '</td><td>' + r[1] + '</td></tr>'; }).join('');
    var steps = t.install.steps.map(function (s) {
      if (typeof s === 'string') return '<li>' + links(s) + '</li>';
      return '<li>' + links(s.text) + '<ol>' + s.sub.map(function (x) { return '<li>' + links(x) + '</li>'; }).join('') + '</ol></li>';
    }).join('');
    h.push('<section id="install" class="reveal"><h2>' + t.install.title + '</h2>' +
      '<div class="table"><table><thead><tr><th>' + t.install.head[0] + '</th><th>' + t.install.head[1] + '</th></tr></thead><tbody>' + rows + '</tbody></table></div>' +
      '<ol class="steps">' + steps + '</ol><p>' + links(t.install.meet) + '</p>' +
      '<details><summary>' + t.install.build.summary + '</summary><p>' + t.install.build.text + '</p>' +
      '<pre><code>./gradlew build</code></pre><p>' + t.install.build.after + '</p></details></section>');
    h.push(divider());

    // Cierre
    h.push('<footer class="outro reveal"><p class="last">' + t.outro.line + '</p><p class="fine">' + t.outro.notes.join('<br>') + '</p>' +
      (LINKS.README ? '<p class="fine"><a href="' + LINKS.README + '">' + t.ui.readme + '</a></p>' : '') + '</footer>');

    var app = document.getElementById('app');
    app.innerHTML = h.join('');

    document.documentElement.lang = t.htmlLang;
    document.title = t.title;
    var md = document.querySelector('meta[name="description"]');
    if (md) md.setAttribute('content', t.description);
    reveal();
  }

  // Aparición suave de cada sección al llegar a ella (nada si el sistema pide menos movimiento).
  function reveal() {
    var els = document.querySelectorAll('.reveal');
    var reduce = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    if (reduce || !('IntersectionObserver' in window)) { els.forEach(function (e) { e.classList.add('in'); }); return; }
    var io = new IntersectionObserver(function (entries) {
      entries.forEach(function (en) { if (en.isIntersecting) { en.target.classList.add('in'); io.unobserve(en.target); } });
    }, { rootMargin: '0px 0px -8% 0px' });
    els.forEach(function (e) { io.observe(e); });
  }

  // ------------------------------------------------------------------ selector

  function fillSelector(t, current) {
    var sel = document.getElementById('lang-select');
    var detected = detect();
    var saved = stored();
    var forced = fromUrl();
    var opts = '<option value="auto">' + t.ui.auto + ' · ' + NATIVE[detected] + '</option>';
    SUPPORTED.forEach(function (l) { opts += '<option value="' + l + '" lang="' + l + '">' + NATIVE[l] + '</option>'; });
    sel.innerHTML = opts;
    sel.value = forced || saved || 'auto';
    document.getElementById('lang-label').textContent = t.ui.language;
    sel.setAttribute('aria-label', t.ui.language);
    sel.title = t.ui.language;
  }

  function show(lang) {
    document.body.classList.add('is-loading');
    return load(lang).catch(function () { lang = 'en'; return load('en'); }).then(function (t) {
      render(t, lang);
      fillSelector(t, lang);
      document.body.classList.remove('is-loading');
    });
  }

  document.getElementById('lang-select').addEventListener('change', function (e) {
    var v = e.target.value;
    store(v === 'auto' ? null : v);                 // "Automático" vuelve a la detección
    // Una elección manual reemplaza el ?lang= de prueba.
    if (fromUrl() && history.replaceState) {
      var u = new URL(location.href); u.searchParams.delete('lang'); history.replaceState(null, '', u.toString());
    }
    var y = window.scrollY;
    show(v === 'auto' ? detect() : v).then(function () { window.scrollTo(0, y); });
  });

  // Exponer lo justo para comprobar la detección desde la consola.
  window.NoxisLang = { detect: detect, stored: stored, current: function () { return document.documentElement.lang; }, supported: SUPPORTED.slice() };

  show(choose());
})();
