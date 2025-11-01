# BlockyMinecart

BlockyMinecart é um plugin para o servidor BlockyCRAFT que adiciona inventário acessível aos carrinhos de mina (minecarts). Permite que jogadores armazenem e transportem itens em minecarts, trazendo uma nova mecânica logística para o mundo do servidor.

## Funcionalidades

- Inventário customizado nos minecarts, com tamanho e título configuráveis.
- Inventário acessível via **Shift + Clique Direito** no minecart.
- Persistência dos itens armazenados, sobrevivendo a reinícios de servidor (persistidos em data.yml).
- Drop automático dos itens do inventário quando o minecart é destruído.
- Compatível com Uberbukkit/CraftBukkit 1060 (Java 8).
- Configuração do plugin via `config.yml`: tamanho, título do inventário, intervalo de auto-save.
- Auto-save dos dados a cada X minutos (configurável).

## Como Funciona

1. O jogador posiciona um minecart.
2. Segure **Shift** e clique com o botão direito do mouse no minecart para abrir seu inventário.
3. Armazene os itens desejados.
4. Se o minecart for destruído, todos os itens armazenados são automaticamente largados no solo.

## Persistência com SQLite

A implementação do BlockyMinecart utiliza um banco **SQLite** para garantir a persistência dos inventários dos minecarts de forma robusta e confiável, mesmo após reinícios ou crashes do servidor.

- Cada minecart recebe um **identificador único persistente** durante sua existência no mundo.
- Ao abrir o inventário ou realizar qualquer operação, o conteúdo é salvo no banco `blockyminecart.db`, localizado na pasta de dados do plugin.
- O sistema realiza backups frequentes e automáticos, além de salvar os dados imediatamente quando necessário (ex: ao destruir o minecart).
- A estrutura do banco armazena:
  - Os identificadores dos minecarts e suas posições,
  - O conteúdo de cada slot do inventário, incluindo tipo de item, quantidade e dano.
- Na reinicialização do servidor, o plugin associa os minecarts do mundo aos seus identificadores persistentes e restaura automaticamente os inventários do banco, permitindo que os itens permaneçam disponíveis para os jogadores.


## Configuração

O arquivo `config.yml` permite definir:
- `auto-save-interval`: intervalo (minutos) para salvar os dados dos inventários.
- `inventory-size`: tamanho do inventário do minecart (9, 18, 27...).
- `inventory-title`: nome exibido ao abrir o inventário.

## Arquitetura

- **BlockyMinecart.java**: classe principal, controla ciclo do plugin, configuração e listeners.
- **listeners/**: contém os listeners para eventos de interação e destruição dos minecarts.
- **storage/**: gerenciadores de persistência dos dados, inventário e serialização.
- **util/**: utilitário para gerar identificadores únicos de minecarts.
- **resources/**: arquivos de configuração e plugin definition.

## Reportar bugs ou requisitar features

Reporte bugs ou sugestões na seção [Issues](https://github.com/andradecore/BlockyMinecart/issues) do projeto. do projeto.

## Contato

- Discord: https://discord.gg/tthPMHrP
