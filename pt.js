/* Noxis Culture — Português (Brasil). Tradução de README.md (fonte: en.js).
 * Mesmas chaves de en.js. O jogo ainda não tem português: o nome que aparece no jogo vai entre parênteses (.ig). */
window.NOXIS_I18N = window.NOXIS_I18N || {};
window.NOXIS_I18N.pt = {
  htmlLang: "pt",
  title: "Noxis Culture — pequenas luzes, velhos segredos",
  description: "Noxis Culture é um mod Fabric para Minecraft Java 26.2. Ele traz os Noxis para o seu mundo: pequenas criaturas sombrias de olhos brilhantes e chapéus altos.",
  ui: { language: "Idioma", auto: "Automático", sections: "Seções", readme: "README no GitHub" },
  hero: {
    alt: "Noxis Culture — pequenas luzes, velhos segredos",
    intro: [
      "<b>Noxis Culture</b> é um mod Fabric para Minecraft Java&nbsp;26.2.",
      "Ele traz os Noxis para o seu mundo: pequenas criaturas sombrias de olhos brilhantes e chapéus altos.",
      "Cada um tem uma personalidade própria."
    ]
  },
  nav: { universe: "O universo", discover: "Descubra", secrets: "Segredos", install: "Instalação" },
  universe: {
    title: "O universo Noxis",
    alt: "Um Noxis espiando na escuridão, com os olhos acesos",
    lore: [
      [
        "Muito abaixo da ardósia profunda, onde a pedra esquece o sol, existe um cristal violeta chamado <b>Noxus</b>.",
        "Ninguém sabe quem o enterrou ali.",
        "Os Noxis agem como se soubessem."
      ],
      [
        "São pequenos e macios, escuros como a hora antes do amanhecer, com olhos que lembram duas lanternas cálidas.",
        "Eles negociam. Colecionam o que brilha. Fazem amigos devagar e os mantêm por perto.",
        "E nunca terminam de contar a sua história."
      ]
    ]
  },
  discover: {
    title: "Descubra o mundo deles",
    note: "Ilustrações renderizadas a partir dos próprios modelos do mod. Não são imagens de gameplay.",
    cards: [
      { title: "Laços", alt: "Dois Noxis de mãos dadas",
        text: "Alguns Noxis escolhem um amigo e ficam ao lado dele para sempre.<br>Outros preferem caminhar sozinhos." },
      { title: "Cristais", alt: "Um Noxis fascinado por um cristal violeta",
        text: "Ametista e Noxus os fazem parar na hora.<br>Repare nos olhos deles quando um está por perto." },
      { title: "Leitores e escritores", alt: "Um Noxis lendo e outro escrevendo",
        text: "Uns poucos levam um livro para onde quer que vão.<br>O que eles escrevem não é para você ler." },
      { title: "Aquários", alt: "Um Noxis dormindo dentro de um aquário de vidro, com o chapéu do lado de fora",
        text: "Um pequeno quarto de vidro para sonhar.<br>O chapéu espera do lado de fora até de manhã." }
    ],
    relics: {
      title: "Relíquias e materiais",
      alt: "Tijolos de Noxite, Noxus, Nixil, a Picareta de Noxus, Noyux, o aquário Noxis e o ovo gerador",
      items: [
        "O <b>Noxus</b> se forma nas profundezas. Refine-o em lingotes ou coloque-o como um bloco que emite um leve brilho violeta.",
        "O <b>Noyux</b> é um líquido violeta, lento e corrosivo, que desgasta a pedra. Guardado em um caldeirão, ele pode transformar Noxus bruto em lingotes. Ele incendeia a pedra com um fogo violeta lento que a chuva não apaga e, ao encontrar a água, endurece em <b>Niris</b>, uma pedra negra com reflexos azuis e violeta.",
        "Os <b>Tijolos de Noxite</b> <span class=\"ig\">(Noxite Bricks)</span> são a pedra escura dos Noxis. A variante brilhante pode ser fundida em <b>Nixil</b>.",
        "A <b>Picareta de Noxus</b> <span class=\"ig\">(Noxus Pickaxe)</span> carrega o <i>Pulso Estelar</i> <span class=\"ig\">(Stellar Pulse)</span>, que sente os veios de minério escondidos ao seu redor.",
        "Os <b>comerciantes Noxis</b> valorizam cristais, ecos e sculk. Seus clientes mais fiéis podem sair com algo cósmico."
      ]
    },
    hats: {
      title: "Um chapéu para cada Noxis",
      alt: "Chapéus Noxis tingidos de cores diferentes",
      text: "Tinja a copa e a fita separadamente, com milhares de tons possíveis.<br>Coloque um chapéu no mundo e ele guarda cada cor exatamente como estava."
    }
  },
  secrets: {
    title: "Pequenas vidas, grandes segredos",
    alt: "Um Noxis acordando e se espreguiçando",
    paras: [
      "Todo Noxis nasce com um de cinco temperamentos. Ele define como brinca, como ama e com que frequência se mete em encrenca.",
      "As orelhas dizem o que o rosto não diz. Alguns usam capa. Alguns levam uma luz para a escuridão. Quando a chuva chega, eles têm um plano.",
      "Eles se ajeitam antes de dormir e se espreguiçam ao acordar. Guardam pequenos tesouros, colocam as coisas de volta no lugar e deixam presentes para quem amam. De vez em quando, param para observar um animal, em silêncio e a uma distância respeitosa. Alguns cantarolam ou assobiam uma melodia própria, e um amigo pode balançar no ritmo.",
      "Quando o céu fica laranja ou se enche de estrelas, alguns param e olham para cima por um tempo. Dois amigos podem sentar lado a lado sem dizer nada. Eles se abaixam diante de uma flor nova ou do brilho de um cristal, e quando um deles se machuca, outro vem ficar por perto. Alguns caminham de mãos dadas por um bom trecho, e outros assobiam enquanto andam. E os Artistas montam um pequeno cavalete, pintam o que veem e deixam suas obras para que outros as encontrem."
    ],
    whisper: "Algumas coisas só acontecem quando você não está olhando. Outras, só quando está.",
    tagline: "Dizem que o mundo parece diferente através dos olhos deles."
  },
  install: {
    title: "Instalação",
    head: ["Requisito", "Versão"],
    reqs: [
      ["Minecraft: Java Edition", "<b>26.2</b>"],
      ["Fabric Loader", "<b>0.19.5</b> ou mais recente"],
      ["Fabric API", "<b>0.161.0+26.2</b> ou mais recente"],
      ["Java", "<b>25</b> (o launcher oficial já inclui)"]
    ],
    steps: [
      "Instale o <b>Fabric Loader</b> para o Minecraft 26.2 com o <a href=\"{FABRIC_INSTALLER}\">instalador oficial do Fabric</a>.",
      "Baixe o <b>Fabric API</b> para 26.2 no <a href=\"{MODRINTH}\">Modrinth</a> ou no <a href=\"{CURSEFORGE}\">CurseForge</a>.",
      { text: "Baixe o <b>Noxis Culture</b>:",
        sub: [
          "Abra a aba <a href=\"{ACTIONS}\"><b>Actions</b></a> e selecione a execução <b>build</b> bem-sucedida mais recente.",
          "Em <b>Artifacts</b>, baixe <b><code>noxis-culture-jar</code></b>. É preciso estar conectado ao GitHub.",
          "Descompacte e fique com <b><code>noxis-culture-1.0.0.jar</code></b>. Ignore o arquivo que termina em <code>-sources.jar</code>."
        ] },
      "Coloque os dois arquivos <code>.jar</code> na pasta <code>.minecraft/mods</code>.",
      "Inicie o jogo com o perfil <b>Fabric</b>."
    ],
    meet: "Para conhecer seu primeiro Noxis, abra a aba criativa <b>Noxis Culture</b> e use o ovo gerador <b>Noxis Villager Spawn Egg</b>. Opcional: com o <b>Mod Menu</b> instalado, o mod tem sua própria tela de configurações.",
    build: {
      summary: "Compilar a partir do código-fonte",
      text: "Cada push é compilado automaticamente pelo GitHub Actions. Para compilar você mesmo, em uma máquina com Java 25:",
      after: "O mod é gerado em <code>build/libs/</code>."
    }
  },
  outro: {
    line: "Olhe com atenção para o escuro.<br>Algo pequeno está olhando de volta.",
    notes: [
      "As imagens desta página são ilustrações renderizadas a partir dos próprios modelos e texturas do mod. Não são capturas de tela do jogo.",
      "Noxis Culture é um mod feito por fãs. Não é um produto oficial do Minecraft e não é aprovado pela Mojang ou pela Microsoft nem associado a elas.",
      "Todos os direitos reservados."
    ]
  }
};
