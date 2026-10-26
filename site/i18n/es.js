/* Noxis Culture — Español. Traducción de README.md (fuente: en.js).
 * Mismas claves que en.js. Los nombres de bloques e ítems son los del propio juego en español. */
window.NOXIS_I18N = window.NOXIS_I18N || {};
window.NOXIS_I18N.es = {
  htmlLang: "es",
  title: "Noxis Culture — pequeñas luces, viejos secretos",
  description: "Noxis Culture es un mod de Fabric para Minecraft Java 26.2. Trae a los Noxis a tu mundo: pequeñas criaturas oscuras de ojos brillantes y sombreros altos.",
  ui: { language: "Idioma", auto: "Automático", sections: "Secciones", readme: "README en GitHub" },
  hero: {
    alt: "Noxis Culture — pequeñas luces, viejos secretos",
    intro: [
      "<b>Noxis Culture</b> es un mod de Fabric para Minecraft Java&nbsp;26.2.",
      "Trae a los Noxis a tu mundo: pequeñas criaturas oscuras de ojos brillantes y sombreros altos.",
      "Cada uno tiene su propia personalidad."
    ]
  },
  nav: { universe: "El universo", discover: "Descubre", secrets: "Secretos", install: "Instalación" },
  universe: {
    title: "El universo Noxis",
    alt: "Un Noxis asomándose en la oscuridad, con los ojos encendidos",
    lore: [
      [
        "Muy por debajo de la pizarra profunda, donde la piedra olvida el sol, hay un cristal violeta llamado <b>Noxus</b>.",
        "Nadie sabe quién lo enterró allí.",
        "Los Noxis actúan como si lo supieran."
      ],
      [
        "Son pequeños y suaves, oscuros como la hora antes del alba, con ojos como dos faroles tibios.",
        "Comercian. Coleccionan lo que brilla. Hacen amigos despacio y los mantienen cerca.",
        "Y nunca terminan de contarte su historia."
      ]
    ]
  },
  discover: {
    title: "Descubre su mundo",
    note: "Ilustraciones creadas a partir de los modelos del propio mod. No son imágenes de juego.",
    cards: [
      { title: "Vínculos", alt: "Dos Noxis tomados de la mano",
        text: "Algunos Noxis eligen a un amigo y se quedan a su lado para siempre.<br>Otros prefieren andar solos." },
      { title: "Cristales", alt: "Un Noxis fascinado por un cristal violeta",
        text: "La amatista y el Noxus los dejan inmóviles.<br>Mira sus ojos cuando hay uno cerca." },
      { title: "Lectores y escritores", alt: "Un Noxis leyendo y otro escribiendo",
        text: "Unos pocos llevan un libro a todas partes.<br>Lo que escriben no es para que tú lo leas." },
      { title: "Peceras", alt: "Un Noxis dormido dentro de una pecera de vidrio, con el sombrero afuera",
        text: "Una pequeña habitación de cristal para soñar.<br>El sombrero espera afuera hasta la mañana." }
    ],
    relics: {
      title: "Reliquias y materiales",
      alt: "Ladrillos de Noxita, Noxus, Nixil, el Pico de Noxus, Noyux, la Pecera Noxis y el huevo del Aldeano Noxis",
      items: [
        "El <b>Noxus</b> se forma en las profundidades. Refínalo en lingotes o colócalo como un bloque que desprende un tenue resplandor violeta.",
        "El <b>Noyux</b> es un líquido violeta, lento y corrosivo, que carcome la piedra. En un caldero, puede convertir el Noxus en bruto en lingotes. Enciende la piedra con un fuego violeta lento que la lluvia no apaga, y al tocar el agua se endurece en <b>Niris</b>, una piedra negra con reflejos azules y violetas.",
        "Los <b>Ladrillos de Noxita</b> son la piedra oscura de los Noxis. Su variante resplandeciente se funde en <b>Nixil</b>.",
        "El <b>Pico de Noxus</b> lleva el <i>Pulso Estelar</i>, que percibe las vetas de mineral ocultas a tu alrededor.",
        "Los <b>comerciantes Noxis</b> valoran los cristales, los ecos y el sculk. Sus clientes más fieles pueden irse con algo cósmico."
      ]
    },
    hats: {
      title: "Un sombrero para cada Noxis",
      alt: "Sombreros Noxis teñidos de distintos colores",
      text: "Tiñe la copa y la cinta por separado, con miles de tonos posibles.<br>Coloca un sombrero y conservará cada color exactamente como estaba."
    }
  },
  secrets: {
    title: "Pequeñas vidas, grandes secretos",
    alt: "Un Noxis despertando y estirándose",
    paras: [
      "Cada Noxis nace con uno de cinco temperamentos. Define cómo juega, cómo quiere y cuán a menudo se mete en problemas.",
      "Sus orejas dicen lo que sus caras callan. Algunos llevan capa. Otros llevan una luz hacia la oscuridad. Cuando llega la lluvia, tienen un plan.",
      "Se acomodan antes de dormir y se estiran al despertar. Guardan pequeños tesoros, devuelven las cosas a su lugar y dejan regalos a quienes quieren. De vez en cuando se detienen a mirar a un animal, en silencio y a una distancia respetuosa."
    ],
    whisper: "Algunas cosas solo pasan cuando no estás mirando. Otras, solo cuando sí.",
    tagline: "Dicen que el mundo se ve distinto a través de sus ojos."
  },
  install: {
    title: "Instalación",
    head: ["Requisito", "Versión"],
    reqs: [
      ["Minecraft: Java Edition", "<b>26.2</b>"],
      ["Fabric Loader", "<b>0.19.5</b> o superior"],
      ["Fabric API", "<b>0.161.0+26.2</b> o superior"],
      ["Java", "<b>25</b> (lo incluye el launcher oficial)"]
    ],
    steps: [
      "Instala <b>Fabric Loader</b> para Minecraft 26.2 con el <a href=\"{FABRIC_INSTALLER}\">instalador oficial de Fabric</a>.",
      "Descarga <b>Fabric API</b> para 26.2 desde <a href=\"{MODRINTH}\">Modrinth</a> o <a href=\"{CURSEFORGE}\">CurseForge</a>.",
      { text: "Descarga <b>Noxis Culture</b>:",
        sub: [
          "Abre la pestaña <a href=\"{ACTIONS}\"><b>Actions</b></a> y elige la ejecución <b>build</b> exitosa más reciente.",
          "En <b>Artifacts</b>, descarga <b><code>noxis-culture-jar</code></b>. Necesitas haber iniciado sesión en GitHub.",
          "Descomprímelo y quédate con <b><code>noxis-culture-1.0.0.jar</code></b>. Ignora el archivo que termina en <code>-sources.jar</code>."
        ] },
      "Coloca los dos archivos <code>.jar</code> en tu carpeta <code>.minecraft/mods</code>.",
      "Inicia el juego con el perfil de <b>Fabric</b>."
    ],
    meet: "Para conocer a tu primer Noxis, abre la pestaña creativa <b>Noxis Culture</b> y usa el <b>Huevo de Aldeano Noxis</b>.",
    build: {
      summary: "Compilar desde el código fuente",
      text: "Cada push se compila automáticamente con GitHub Actions. Para compilarlo tú mismo en un equipo con Java 25:",
      after: "El mod se genera en <code>build/libs/</code>."
    }
  },
  outro: {
    line: "Mira con atención la oscuridad.<br>Algo pequeño te está mirando.",
    notes: [
      "Las imágenes de esta página son ilustraciones creadas a partir de los modelos y texturas del propio mod. No son capturas del juego.",
      "Noxis Culture es un mod hecho por fans. No es un producto oficial de Minecraft y no está aprobado por Mojang ni Microsoft, ni asociado con ellos.",
      "Todos los derechos reservados."
    ]
  }
};
