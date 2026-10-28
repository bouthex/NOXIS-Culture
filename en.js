/* Noxis Culture — English (default language). Source text: README.md.
 * Every language file has exactly the same keys. When this file changes, update es, ru, zh and pt too.
 * {FABRIC_INSTALLER}, {MODRINTH}, {CURSEFORGE} and {ACTIONS} are replaced with real links by app.js. */
window.NOXIS_I18N = window.NOXIS_I18N || {};
window.NOXIS_I18N.en = {
  htmlLang: "en",
  title: "Noxis Culture — little lights, old secrets",
  description: "Noxis Culture is a Fabric mod for Minecraft Java 26.2. It brings the Noxis to your world: small dark creatures with glowing eyes and tall hats.",
  ui: { language: "Language", auto: "Automatic", sections: "Sections", readme: "README on GitHub" },
  hero: {
    alt: "Noxis Culture — little lights, old secrets",
    intro: [
      "<b>Noxis Culture</b> is a Fabric mod for Minecraft Java&nbsp;26.2.",
      "It brings the Noxis to your world: small dark creatures with glowing eyes and tall hats.",
      "Each one has a personality of its own."
    ]
  },
  nav: { universe: "The Universe", discover: "Discover", secrets: "Secrets", install: "Installation" },
  universe: {
    title: "The Noxis Universe",
    alt: "A Noxis peeking out of the dark, eyes glowing",
    lore: [
      [
        "Deep below the deepslate, where the stone forgets the sun, there is a violet crystal called <b>Noxus</b>.",
        "Nobody knows who buried it there.",
        "The Noxis act as though they do."
      ],
      [
        "They are small and soft, dark as the hour before dawn, with eyes like two warm lanterns.",
        "They trade. They collect what shines. They make friends slowly and keep them close.",
        "And they never quite finish telling you their story."
      ]
    ]
  },
  discover: {
    title: "Discover Their World",
    note: "Illustrations rendered from the mod's own models. These are not gameplay footage.",
    cards: [
      { title: "Bonds", alt: "Two Noxis holding hands",
        text: "Some Noxis choose one friend and stay close to them for good.<br>Others would rather walk alone." },
      { title: "Crystals", alt: "A Noxis fascinated by a violet crystal",
        text: "Amethyst and Noxus stop them in their tracks.<br>Watch their eyes when one is near." },
      { title: "Readers &amp; Writers", alt: "A Noxis reading and another writing",
        text: "A rare few carry a book everywhere they go.<br>What they write is not for you to read." },
      { title: "Fishbowls", alt: "A Noxis asleep inside a glass fishbowl, its hat resting outside",
        text: "A little glass room to dream in.<br>The hat waits outside until morning." }
    ],
    relics: {
      title: "Relics &amp; Materials",
      alt: "Noxite bricks, Noxus, Nixil, the Noxus Pickaxe, Noyux, the Noxis Fishbowl and the spawn egg",
      items: [
        "<b>Noxus</b> forms deep underground. Refine it into ingots, or set it as a block that gives off a faint violet glow.",
        "<b>Noyux</b> is a slow, corrosive violet liquid that eats into stone. Held in a cauldron, it can turn raw Noxus into ingots. It sets stone alight with a slow violet fire that the rain cannot put out, and where it meets water it hardens into <b>Niris</b>, a black stone with blue and violet glints.",
        "<b>Noxite Bricks</b> are the dark stone of the Noxis. Their glowing variant smelts down into <b>Nixil</b>.",
        "The <b>Noxus Pickaxe</b> carries the <i>Stellar Pulse</i>, which senses the ore veins hidden around you.",
        "<b>Noxis traders</b> value crystals, echoes and sculk. Their most loyal customers may leave with something cosmic."
      ]
    },
    hats: {
      title: "A Hat for Every Noxis",
      alt: "Noxis hats dyed in different colors",
      text: "Dye the crown and the band separately, with thousands of possible shades.<br>Set a hat down and it keeps every color exactly as it was."
    }
  },
  secrets: {
    title: "Small Lives, Great Secrets",
    alt: "A Noxis waking up and stretching",
    paras: [
      "Every Noxis is born with one of five temperaments. It shapes how they play, how they love and how often they get into trouble.",
      "Their ears say what their faces won't. Some wear capes. Some carry a light into the dark. When the rain comes, they have a plan.",
      "They settle in before they sleep and stretch when they wake. They keep small treasures, put things back where they belong, and leave gifts for the ones they care about. Now and then they stop to watch an animal, quietly, from a respectful distance. Some hum or whistle a little tune of their own, and a friend may sway along.",
      "When the sky turns orange or fills with stars, some stop beneath it and look up for a while. Two friends may sit side by side, saying nothing at all. They stoop over a new flower or a glint of crystal, and when one of them gets hurt, someone comes to stay close. Some walk hand in hand for a long stretch, and some whistle as they go. And the Artists set up a small easel, paint what they see, and leave their work for others to find."
    ],
    whisper: "Some things only happen when you are not looking. Others, only when you are.",
    tagline: "Some say the world looks different through their eyes."
  },
  install: {
    title: "Installation",
    head: ["Requirement", "Version"],
    reqs: [
      ["Minecraft: Java Edition", "<b>26.2</b>"],
      ["Fabric Loader", "<b>0.19.5</b> or newer"],
      ["Fabric API", "<b>0.161.0+26.2</b> or newer"],
      ["Java", "<b>25</b> (the official launcher provides it)"]
    ],
    steps: [
      "Install <b>Fabric Loader</b> for Minecraft 26.2 with the <a href=\"{FABRIC_INSTALLER}\">official Fabric installer</a>.",
      "Download <b>Fabric API</b> for 26.2 from <a href=\"{MODRINTH}\">Modrinth</a> or <a href=\"{CURSEFORGE}\">CurseForge</a>.",
      { text: "Download <b>Noxis Culture</b>:",
        sub: [
          "Open the <a href=\"{ACTIONS}\"><b>Actions</b></a> tab and select the latest successful <b>build</b> run.",
          "Under <b>Artifacts</b>, download <b><code>noxis-culture-jar</code></b>. You need to be signed in to GitHub.",
          "Unzip it and keep <b><code>noxis-culture-1.0.0.jar</code></b>. Skip the file that ends in <code>-sources.jar</code>."
        ] },
      "Place both <code>.jar</code> files in your <code>.minecraft/mods</code> folder.",
      "Start the game with the <b>Fabric</b> profile."
    ],
    meet: "To meet your first Noxis, open the <b>Noxis Culture</b> creative tab and use the <b>Noxis Villager Spawn Egg</b>. Optional: with <b>Mod Menu</b> installed, the mod has its own settings screen.",
    build: {
      summary: "Building from source",
      text: "Every push is built automatically by GitHub Actions. To build it yourself on a machine with Java 25:",
      after: "The mod is written to <code>build/libs/</code>."
    }
  },
  outro: {
    line: "Look closely at the dark.<br>Something small is looking back.",
    notes: [
      "The images on this page are illustrations rendered from the mod's own models and textures. They are not in-game screenshots.",
      "Noxis Culture is a fan-made mod. It is not an official Minecraft product and is not approved by or associated with Mojang or Microsoft.",
      "All rights reserved."
    ]
  }
};
