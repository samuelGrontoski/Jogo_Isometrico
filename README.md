# ⚔️ Out of the Abyss - Protótipo Isométrico (libGDX)

## 👀 Visão Geral do Projeto
Este projeto é um protótipo funcional de um jogo de ação com visão top-down isométrica construído 
sobre o framework **libGDX (Java)**. A arquitetura foi desenvolvida com forte ênfase em 
**alta performance**, **boas práticas de Orientação a Objetos (POO)** e 
**gerenciamento de memória eficiente**.

---

## ⚙️ Funcionalidades Implementadas

### 🎮 Gameplay e Controles
* **Movimentação 8-Way Isométrica:** O jogador pode se mover e atacar em 8 direções adaptadas visualmente para a perspectiva isométrica.
* **Ataque com Mira via Mouse (Mouse Aiming):** Uso de transformações de matriz reversa e trigonometria para calcular a direção do ataque de forma contínua em 360 graus, realizando *snapping* visual apenas para a animação.
* **Mecânica de Dash:** Movimentação rápida que ignora colisões parcialmente, com sistema de cooldown e geração de rastro (sombras temporárias).
* **Combate com Hitboxes Independentes:** Separação entre tempo de animação (travamento do input) e o frame exato em que o dano é calculado, garantindo um "Game Feel" preciso.

### 🧠 Inteligência Artificial (Inimigos)
* **Comportamento de Bando (Flocking/Separation):** Os inimigos perseguem o jogador, mas aplicam forças vetoriais de repulsão entre si para evitar que se amontoem no mesmo pixel.
* **Sistema de Respawn Contínuo:** Inimigos derrotados aguardam um tempo limite e retornam ao mapa em posições aleatórias.

### ⚙️ Engenharia e Arquitetura (Padrões libGDX)
* **Gestão de Memória via AssetManager:** Todos os recursos (texturas) são carregados de forma centralizada e assíncrona, prevenindo travamentos (stuttering) e memory leaks.
* **Object Pooling (Poolable):** Inimigos e Sombras do Dash são instanciados apenas uma vez. Quando "morrem", são desativados e devolvidos ao Pool de memória para serem reciclados, mitigando o acionamento do *Garbage Collector* da JVM.
* **Event-Driven Input (InputProcessor):** A leitura de periféricos (teclado/mouse) foi isolada na classe `PlayerController`. Impede problemas de *polling* direto e facilita a criação de gatilhos (*triggers*) de disparo único (como combos e botões de interface).
* **Painter's Algorithm (Z-Sorting):** Sistema de ordenação dinâmica de renderização. Entidades mais baixas no eixo Y da tela são desenhadas por último, criando a ilusão correta de profundidade isométrica.
* **Resolução Agnóstica (Viewports):** Uso de `FitViewport` emparelhados com `OrthographicCamera` para garantir que o jogo mantenha o *aspect ratio* exato em qualquer resolução, dividindo câmeras do mundo das câmeras de UI estática.

### 🛠️ Ferramentas de Depuração (Debug)
* **Alternância de Ecrã (F11):** Troca dinâmica entre modo de Janela e Tela Cheia em tempo de execução consultando a API do sistema operacional (LWJGL).
* **Overlay de Interface (F3):** Exibição em tempo real do FPS, coordenadas matemáticas e coordenadas de tela, isolado em um Viewport próprio para não sofrer distorções.
* **Renderização de Colisões e Status (F3 + B):** Utilização isolada do `ShapeRenderer` via lógica de *combo de teclas* para desenhar primitivos geométricos mostrando Hitboxes, Raio de Ataque e Barras de Vida flutuantes dinâmicas calculadas sob a elevação do sprite inimigo.

---

## 📁 Estrutura de Classes (Dicionário)
1.  **`JogoIsometrico`**: Core do Game. Inicializa o `AssetManager` e os Viewports globais.
2.  **`TelaCarregamento`**: `Screen` assíncrona focada em alimentar o AssetManager sem travar a thread principal.
3.  **`MenuInicial`**: Tela baseada em `Scene2D` que implementa UI e transições de tela interativas (Hover/Actions).
4.  **`GameScreen`**: O loop principal (`Model` e `View`). Consome pools, organiza as `Lists` de desenho e renderiza o mundo com `SpriteBatch` e `ShapeRenderer`.
5.  **`Player`**: Lógica de física, restrição de limites, aplicação do dano e transição de animações do personagem.
6.  **`PlayerController`**: Implementação do `InputAdapter` que converte cliques de hardware em variáveis de estado e gatilhos lógicos.
7.  **`Morcego`**: Entidade inimiga. Gerencia sua IA vetorial, status de HP e reinicialização lógica para suporte à interface `Poolable`.
8.  **`Pedra`**: Representação de obstáculos estáticos no cenário isométrico.
9.  **`SombraDash`**: Entidade visual `Poolable` de vida curta usada para o efeito de "fade out" do dash.
10. **`ObjetoRenderizavel`**: Estrutura base contendo a textura, coordenadas X/Y e a coordenada de ordenação `sortY`.

---

## 🚀 Como Executar o Projeto

1. Certifique-se de ter o **Java JDK** instalado na sua máquina.

2. Clone este repositório:
   ```bash
   git clone https://github.com/samuelGrontoski/Jogo_Isometrico.git

3. Importe o projeto na sua IDE favorita (IntelliJ IDEA, Eclipse ou Android Studio) como um projeto Gradle.

4. Execute a classe `Lwjgl3Launcher.java` (localizada no pacote lwjgl3) para iniciar o jogo.

---

Desenvolvido com ☕ e LibGDX.
