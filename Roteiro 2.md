# Roteiro de Laboratório — Revisão de Redes de Computadores

**Disciplina:** Laboratório de Desenvolvimento de Aplicações Móveis e Distribuídas
**Unidade:** U0 — Nivelamento de Redes de Computadores e Sistemas Operacionais
**Professores:** T1 — Cleiton Tavares Silva · T2 — Cristiano de Macedo Neto
**Modalidade:** Prática, em duplas ou individual (conforme orientação do professor em sala)

> **Nota de transparência (uso de IA):** este roteiro foi diagramado e organizado com apoio do Claude (Anthropic), utilizado de forma responsável apenas para redação, estruturação e revisão do material. O aluno pode utilizar ferramentas de IA para apoiar rascunhos e revisões de código, **desde que declare o uso** na entrega e seja capaz de explicar e defender qualquer trecho entregue. Copiar e colar sem entender o funcionamento do código caracteriza uso não responsável e será tratado como falta de integridade acadêmica.

---

## 1. Objetivos

Ao final deste laboratório, o aluno deve ser capaz de:

- Diferenciar comunicação **orientada a conexão** (TCP) de **não orientada a conexão** (UDP);
- Implementar comunicação **ponto a ponto (unicast)** e **em grupo (multicast)**;
- Implementar comunicação bidirecional em tempo real usando **WebSocket**, entendendo sua relação com o protocolo HTTP;
- Comparar as mesmas soluções de rede implementadas em **Java** e em **Python**, reconhecendo o que muda (e o que não muda) entre as linguagens;
- Utilizar o **Git** de forma disciplinada, com commits pequenos, atômicos e bem descritos, documentando o progresso da implementação.

## 2. Tema do laboratório: Central de Avisos da Turma

Para dar continuidade entre as quatro partes, todos os exercícios giram em torno do mesmo cenário: uma pequena **central de comunicação da turma**, implementada de quatro formas diferentes:

| Parte | Protocolo | O que representa no cenário |
|---|---|---|
| A | TCP | Um aluno pergunta algo ao monitor e recebe uma resposta direta (conversa privada, confiável) |
| B | UDP | O mesmo pedido de horário, mas sem garantia de entrega (mensagem "solta") |
| C | Multicast | O professor avisa **todos os alunos conectados** de uma vez (grupo) |
| D | WebSocket | Um mural de avisos em tempo real, ao qual vários alunos ficam conectados simultaneamente |

Essa progressão ajuda a perceber, na prática, por que cada protocolo existe.

### 2.1 Tempo sugerido

Este roteiro foi desenhado para uma aula de laboratório (cerca de 100 minutos), mas é denso — 8 programas, 12 perguntas e 8 evidências. Use a tabela abaixo como guia de ritmo; se o tempo apertar, siga a coluna "se faltar tempo":

| Parte | Tempo sugerido em aula | Se faltar tempo |
|---|---|---|
| Preparação do ambiente (seção 3) | 10 min | Deixe feito antes da aula, se possível |
| A — TCP | 20 min | Prioridade máxima — não pule |
| B — UDP | 15 min | Prioridade máxima — não pule |
| C — Multicast | 20 min | Pode finalizar como tarefa de casa se a rede do laboratório atrapalhar (veja seção 6.5) |
| D — WebSocket | 25 min | É a parte mais avançada (exige Maven); pode ser concluída como tarefa de casa |
| Respostas em `RESPOSTAS.md` | ao longo de toda a aula | Responda cada parte logo após implementá-la, não deixe as 12 perguntas para o final |

**Se precisar priorizar, a ordem de importância é TCP → UDP → Multicast → WebSocket** — as duas primeiras partes cobrem os conceitos centrais de nivelamento (conexão vs. sem conexão) e devem ser concluídas em aula por todos.

## 3. Preparação do ambiente

Antes de começar, garanta que você tem instalado (este roteiro assume **Windows**, usando o **PowerShell** como terminal padrão):

- **Java JDK 17 ou superior** (`java -version`)
- **Maven 3.8+** (`mvn -version`) — usado apenas na Parte D (WebSocket em Java)
- **Python 3.10+**, com a opção "Add python.exe to PATH" marcada na instalação (`python --version`)
- **Git for Windows** configurado com seu nome e e-mail (`git config --global user.name` / `user.email`) — ele já inclui o Git Bash, mas os comandos deste roteiro usam PowerShell
- Um editor de sua preferência (VS Code, IntelliJ, PyCharm etc.)

> **Alerta do Firewall do Windows:** na primeira vez que você rodar cada servidor (TCP, UDP, Multicast, WebSocket), o **Firewall do Windows Defender** deve exibir um pop-up perguntando se permite que o Java/Python se comunique em redes privadas/públicas. Clique em **"Permitir acesso"** — se você clicar em "Cancelar" ou a janela passar despercebida, o programa continua rodando, mas as mensagens não chegam, dando a falsa impressão de erro no código.

### 3.1 Estrutura do repositório

Crie um repositório com a seguinte estrutura de pastas e inicialize o Git:

```
lab-redes/
├── java/
│   ├── tcp/
│   ├── udp/
│   ├── multicast/
│   └── websocket/
├── python/
│   ├── tcp/
│   ├── udp/
│   ├── multicast/
│   └── websocket/
├── evidencias/
│   ├── tcp/
│   ├── udp/
│   ├── multicast/
│   └── websocket/
├── RESPOSTAS.md
└── README.md
```

```powershell
$pastas = @(
  "lab-redes/java/tcp", "lab-redes/java/udp", "lab-redes/java/multicast", "lab-redes/java/websocket",
  "lab-redes/python/tcp", "lab-redes/python/udp", "lab-redes/python/multicast", "lab-redes/python/websocket",
  "lab-redes/evidencias/tcp", "lab-redes/evidencias/udp", "lab-redes/evidencias/multicast", "lab-redes/evidencias/websocket"
)
New-Item -ItemType Directory -Force -Path $pastas

cd lab-redes
git init
Set-Content -Path README.md -Value "# Central de Avisos da Turma — Lab de Redes" -Encoding utf8
New-Item -ItemType File -Name RESPOSTAS.md
git add .
git commit -m "chore: estrutura inicial do repositório"
```

> **Acentos no terminal:** se palavras acentuadas aparecerem trocadas na tela (ex.: `Conex??o` em vez de `Conexão`), rode `chcp 65001` antes de executar os programas para forçar UTF-8 no console — ou use o **Windows Terminal** (recomendado), que já usa UTF-8 por padrão.
>
> **Se preferir usar o Git Bash** (instalado junto com o Git for Windows) em vez do PowerShell, o comando de criação de pastas fica mais compacto: `mkdir -p lab-redes/java/{tcp,udp,multicast,websocket} lab-redes/python/{tcp,udp,multicast,websocket} lab-redes/evidencias/{tcp,udp,multicast,websocket}`. Os demais comandos deste roteiro (`javac`, `java`, `python`, `git`, `pip`) funcionam da mesma forma nos dois terminais.

Todas as respostas às questões de cada parte devem ser escritas no arquivo **`RESPOSTAS.md`**, na raiz do repositório, organizadas por parte (A, B, C, D).

### 3.2 Evidências de teste (prints de tela)

Para **cada um dos 8 exemplos** (4 protocolos × 2 linguagens), o aluno deve capturar ao menos **um print de tela** que comprove a execução correta — mostrando, tipicamente, os terminais do servidor e do(s) cliente(s) lado a lado, com as mensagens sendo trocadas.

- Os prints devem ser salvos em `evidencias/<protocolo>/`, com nome no formato `<protocolo>-<linguagem>.png` (ex.: `evidencias/tcp/tcp-java.png`, `evidencias/tcp/tcp-python.png`).
- Formato aceito: `.png` ou `.jpg`.
- O print precisa mostrar evidências reais de execução (comandos digitados, saída no terminal, mensagens trocadas) — capturas de tela de código sem execução **não são aceitas como evidência**.
- Cada parte (seções 4 a 7) indica, na sua tarefa, o momento exato em que o print daquela parte deve ser feito.
- Os prints entram no mesmo commit da respectiva parte (veja o comando `git add` em cada tarefa, que já inclui a pasta `evidencias/`).
- Para provar que o print corresponde à sua própria execução (e não a um reaproveitamento), **deixe visível, em algum terminal do print, a saída do comando `Get-Date`** (PowerShell) rodado na hora do teste. Não precisa ser bonito — só precisa aparecer no canto do terminal.

### 3.3 Portas exclusivas (evite colisão com colegas)

Se vários alunos rodarem os exemplos **na mesma rede** (Wi-Fi do laboratório) ou **na mesma máquina** ao mesmo tempo, é possível colidir com o servidor de um colega — isso é especialmente sério no **Multicast** (Parte C), onde o grupo `230.0.0.1` é compartilhado por toda a rede, não só pelo seu computador: sem uma porta própria, você pode acabar recebendo (e confundindo) os avisos de outra dupla.

Antes de começar, calcule seu `OFFSET` pessoal:

```
OFFSET = os dois últimos dígitos da sua matrícula/RA (ex.: matrícula 1234567 -> OFFSET = 67)
```

Use esse `OFFSET` somado à porta-base de **cada parte**, em todos os arquivos (Java e Python), substituindo o valor fixo sugerido no roteiro:

| Parte | Porta-base | Sua porta |
|---|---|---|
| A — TCP | 5000 | `5000 + OFFSET` |
| B — UDP | 5001 | `5001 + OFFSET` |
| C — Multicast | 4446 | `4446 + OFFSET` |
| D — WebSocket (Java) | 8887 | `8887 + OFFSET` |
| D — WebSocket (Python) | 8888 | `8888 + OFFSET` |

Isso é **obrigatório na Parte C** (Multicast) e recomendado nas demais caso você esteja num laboratório com máquinas compartilhadas. O grupo multicast (`230.0.0.1`) pode continuar o mesmo — o que isola seu tráfego dos colegas é a porta.

### 3.4 `.gitignore`

Antes do primeiro commit, crie um `.gitignore` na raiz do repositório para não versionar artefatos de build:

```powershell
@"
# Java
*.class
target/

# Python
__pycache__/
*.pyc
.venv/
venv/

# Sistema
Thumbs.db
desktop.ini
"@ | Set-Content -Path .gitignore -Encoding utf8

git add .gitignore
git commit -m "chore: adiciona .gitignore"
```

### 3.5 Trabalhando em dupla com Git

Se a dupla optar por um único repositório compartilhado, cada integrante deve, sempre que possível, fazer seus próprios commits (não um só aluno "digitando" por todos). Duas formas aceitas:

- **Cada um com seu próprio usuário Git** configurado localmente (`git config user.name`/`user.email`), fazendo `push` para o mesmo repositório remoto — o histórico mostrará naturalmente quem fez o quê.
- **Commits co-autorados**, quando os dois programaram juntos na mesma máquina: adicione ao final da mensagem de commit uma linha `Co-authored-by: Nome <email>`, por exemplo:

```powershell
git commit -m "feat(tcp): implementa servidor e cliente TCP em Java e Python" -m "Co-authored-by: Nome do Colega <email@aluno.pucminas.br>"
```

> Cada `-m` adicional vira um novo parágrafo na mensagem de commit — evite tentar colocar uma quebra de linha literal dentro das aspas no PowerShell, pois o comportamento varia entre versões do console.

Repositórios onde 100% dos commits pertencem a apenas um integrante da dupla podem gerar questionamento sobre a participação individual na avaliação.

---

## 4. Parte A — TCP (comunicação confiável e orientada a conexão)

**Conceito:** o TCP estabelece uma conexão (handshake) antes de trocar dados, garante entrega e ordem das mensagens. É a base de protocolos como HTTP, SSH e FTP.

**Cenário:** um cliente conecta-se ao servidor (o "monitor da turma"), envia uma pergunta e recebe uma resposta. A conexão permanece aberta para várias trocas de mensagem, até o aluno digitar `sair`.

### 4.1 Java — `java/tcp/ServidorTCP.java`

```java
import java.io.*;
import java.net.*;

public class ServidorTCP {
    public static void main(String[] args) throws IOException {
        int porta = 5000;
        try (ServerSocket servidor = new ServerSocket(porta)) {
            System.out.println("[TCP] Servidor aguardando conexões na porta " + porta + "...");
            try (Socket cliente = servidor.accept();
                 BufferedReader entrada = new BufferedReader(
                         new InputStreamReader(cliente.getInputStream()));
                 PrintWriter saida = new PrintWriter(cliente.getOutputStream(), true)) {

                System.out.println("[TCP] Cliente conectado: " + cliente.getRemoteSocketAddress());
                String mensagem;
                while ((mensagem = entrada.readLine()) != null) {
                    System.out.println("[TCP] Recebido: " + mensagem);
                    if (mensagem.equalsIgnoreCase("sair")) {
                        saida.println("Encerrando conexão. Até mais!");
                        break;
                    }
                    saida.println("Monitor responde: recebi sua mensagem -> \"" + mensagem + "\"");
                }
            }
        }
        System.out.println("[TCP] Servidor encerrado.");
    }
}
```

### 4.2 Java — `java/tcp/ClienteTCP.java`

```java
import java.io.*;
import java.net.*;

public class ClienteTCP {
    public static void main(String[] args) throws IOException {
        String host = "localhost";
        int porta = 5000;

        try (Socket socket = new Socket(host, porta);
             PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("[TCP] Conectado ao servidor. Digite 'sair' para encerrar.");
            String linha;
            while (true) {
                System.out.print("> ");
                linha = teclado.readLine();
                saida.println(linha);
                System.out.println(entrada.readLine());
                if (linha.equalsIgnoreCase("sair")) break;
            }
        }
    }
}
```

**Como executar:**

```powershell
cd java/tcp
javac ServidorTCP.java ClienteTCP.java
java ServidorTCP        # em um terminal
java ClienteTCP         # em outro terminal
```

### 4.3 Python — `python/tcp/servidor_tcp.py`

```python
import socket

HOST = "0.0.0.0"
PORTA = 5000

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as servidor:
    servidor.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    servidor.bind((HOST, PORTA))
    servidor.listen(1)
    print(f"[TCP] Servidor aguardando conexões na porta {PORTA}...")

    conexao, endereco = servidor.accept()
    with conexao:
        print(f"[TCP] Cliente conectado: {endereco}")
        while True:
            dados = conexao.recv(1024).decode("utf-8").strip()
            if not dados:
                break
            print(f"[TCP] Recebido: {dados}")
            if dados.lower() == "sair":
                conexao.sendall("Encerrando conexão. Até mais!\n".encode("utf-8"))
                break
            resposta = f'Monitor responde: recebi sua mensagem -> "{dados}"\n'
            conexao.sendall(resposta.encode("utf-8"))

print("[TCP] Servidor encerrado.")
```

### 4.4 Python — `python/tcp/cliente_tcp.py`

```python
import socket

HOST = "localhost"
PORTA = 5000

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as cliente:
    cliente.connect((HOST, PORTA))
    print("[TCP] Conectado ao servidor. Digite 'sair' para encerrar.")
    arquivo = cliente.makefile("r")

    while True:
        mensagem = input("> ")
        cliente.sendall((mensagem + "\n").encode("utf-8"))
        print(arquivo.readline().strip())
        if mensagem.lower() == "sair":
            break
```

**Como executar:**

```powershell
cd python/tcp
python servidor_tcp.py     # em um terminal
python cliente_tcp.py      # em outro terminal
```

### 4.5 Tarefa

1. Rode o servidor e o cliente em Java e troque pelo menos 3 mensagens.
2. Rode o servidor e o cliente em Python e troque pelo menos 3 mensagens.
3. **Modifique** o servidor (em Java **e** em Python) para que, ao receber a mensagem `hora`, ele responda com o horário atual do servidor, em vez do eco padrão.
4. Capture um print de tela para cada linguagem, mostrando os terminais do servidor e do cliente com as mensagens trocadas (incluindo o teste da mensagem `hora`), e salve-os como `evidencias/tcp/tcp-java.png` e `evidencias/tcp/tcp-python.png`.
5. Faça o commit do seu progresso:

```powershell
git add java/tcp python/tcp evidencias/tcp
git commit -m "feat(tcp): implementa servidor e cliente TCP em Java e Python"
```

### 4.6 Perguntas — Parte A (responder em `RESPOSTAS.md`)

1. O que acontece se você iniciar o **cliente** antes do **servidor**? Por que isso ocorre, considerando o funcionamento do TCP?
2. O TCP garante que as mensagens cheguem **na ordem** em que foram enviadas. Qual mecanismo do protocolo é responsável por isso?
3. Na sua implementação, o que aconteceria se dois clientes tentassem se conectar ao mesmo tempo? O código atual suporta isso? Justifique observando o código do servidor.

---

## 5. Parte B — UDP (comunicação sem conexão)

**Conceito:** o UDP envia datagramas sem estabelecer conexão prévia e sem garantir entrega, ordem ou ausência de duplicação. Em compensação, tem menor overhead — é usado em streaming, jogos e DNS.

**Cenário:** o mesmo pedido de horário ao monitor, mas agora "gritado" sem confirmação de que alguém está ouvindo.

### 5.1 Java — `java/udp/ServidorUDP.java`

```java
import java.net.*;

public class ServidorUDP {
    public static void main(String[] args) throws Exception {
        int porta = 5001;
        byte[] buffer = new byte[1024];

        try (DatagramSocket socket = new DatagramSocket(porta)) {
            System.out.println("[UDP] Servidor aguardando datagramas na porta " + porta + "...");
            while (true) {
                DatagramPacket pacoteRecebido = new DatagramPacket(buffer, buffer.length);
                socket.receive(pacoteRecebido);

                String mensagem = new String(pacoteRecebido.getData(), 0, pacoteRecebido.getLength());
                System.out.println("[UDP] Recebido de " + pacoteRecebido.getAddress() + ": " + mensagem);

                String resposta = "Monitor responde: recebi sua mensagem -> \"" + mensagem + "\"";
                byte[] dadosResposta = resposta.getBytes();
                DatagramPacket pacoteResposta = new DatagramPacket(
                        dadosResposta, dadosResposta.length,
                        pacoteRecebido.getAddress(), pacoteRecebido.getPort());
                socket.send(pacoteResposta);
            }
        }
    }
}
```

### 5.2 Java — `java/udp/ClienteUDP.java`

```java
import java.net.*;
import java.util.Scanner;

public class ClienteUDP {
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int porta = 5001;

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress endereco = InetAddress.getByName(host);
            Scanner teclado = new Scanner(System.in);
            byte[] buffer = new byte[1024];

            System.out.println("[UDP] Pronto para enviar. Digite 'sair' para encerrar.");
            while (true) {
                System.out.print("> ");
                String mensagem = teclado.nextLine();
                byte[] dados = mensagem.getBytes();

                DatagramPacket pacote = new DatagramPacket(dados, dados.length, endereco, porta);
                socket.send(pacote);
                if (mensagem.equalsIgnoreCase("sair")) break;

                DatagramPacket resposta = new DatagramPacket(buffer, buffer.length);
                socket.receive(resposta);
                System.out.println(new String(resposta.getData(), 0, resposta.getLength()));
            }
        }
    }
}
```

**Como executar:**

```powershell
cd java/udp
javac ServidorUDP.java ClienteUDP.java
java ServidorUDP        # em um terminal
java ClienteUDP         # em outro terminal
```

### 5.3 Python — `python/udp/servidor_udp.py`

```python
import socket

HOST = "0.0.0.0"
PORTA = 5001

with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as servidor:
    servidor.bind((HOST, PORTA))
    print(f"[UDP] Servidor aguardando datagramas na porta {PORTA}...")

    while True:
        dados, endereco_cliente = servidor.recvfrom(1024)
        mensagem = dados.decode("utf-8")
        print(f"[UDP] Recebido de {endereco_cliente}: {mensagem}")

        resposta = f'Monitor responde: recebi sua mensagem -> "{mensagem}"'
        servidor.sendto(resposta.encode("utf-8"), endereco_cliente)
```

### 5.4 Python — `python/udp/cliente_udp.py`

```python
import socket

HOST = "localhost"
PORTA = 5001

with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as cliente:
    print("[UDP] Pronto para enviar. Digite 'sair' para encerrar.")
    while True:
        mensagem = input("> ")
        cliente.sendto(mensagem.encode("utf-8"), (HOST, PORTA))
        if mensagem.lower() == "sair":
            break
        dados, _ = cliente.recvfrom(1024)
        print(dados.decode("utf-8"))
```

**Como executar:**

```powershell
cd python/udp
python servidor_udp.py     # em um terminal
python cliente_udp.py      # em outro terminal
```

### 5.5 Tarefa

1. Rode as versões Java e Python e troque mensagens.
2. Pare o servidor (`Ctrl+C`) e, com o servidor **desligado**, envie uma mensagem pelo cliente. Observe o comportamento (o cliente trava, gera erro, ou segue normalmente?).
3. Capture um print de tela para cada linguagem, mostrando a troca normal de mensagens **e** o comportamento observado no passo 2, e salve-os como `evidencias/udp/udp-java.png` e `evidencias/udp/udp-python.png`.
4. Faça o commit:

```powershell
git add java/udp python/udp evidencias/udp
git commit -m "feat(udp): implementa servidor e cliente UDP em Java e Python"
```

### 5.6 Perguntas — Parte B

1. No passo 2 da tarefa, o que aconteceu quando você enviou uma mensagem com o servidor desligado? Compare com o que aconteceria em TCP e explique a diferença observada, relacionando com o conceito de "sem conexão".
2. Cite **dois exemplos de aplicações reais** que usam UDP e explique, para cada uma, por que a confiabilidade do TCP não é essencial (ou até atrapalharia).
3. No código, o servidor UDP não mantém nenhum registro de "quem está conectado". Isso seria possível de implementar? O que mudaria na arquitetura da aplicação?

---

## 6. Parte C — Multicast (comunicação em grupo)

**Conceito:** o multicast permite que **um único envio** alcance todos os membros de um **grupo** (um endereço IP especial, na faixa `224.0.0.0` a `239.255.255.255`), sem que o remetente precise conhecer cada destinatário individualmente.

**Cenário:** o professor envia um aviso geral; todos os alunos que estiverem "inscritos" no grupo recebem a mensagem ao mesmo tempo.

### 6.1 Java — `java/multicast/ServidorMulticast.java`

```java
import java.net.*;
import java.io.IOException;

public class ServidorMulticast {
    // TODO: substitua pelo seu OFFSET pessoal (ver seção 3.3)
    static final int OFFSET = 0;

    public static void main(String[] args) throws IOException, InterruptedException {
        String grupoMulticast = "230.0.0.1";
        int porta = 4446 + OFFSET;

        InetAddress grupo = InetAddress.getByName(grupoMulticast);
        try (DatagramSocket socket = new DatagramSocket()) {
            int contador = 1;
            System.out.println("[Multicast] Enviando avisos para o grupo " + grupoMulticast + ":" + porta);
            while (contador <= 5) {
                String mensagem = "Aviso #" + contador + ": a aula começa em " + (5 - contador) + " minuto(s)!";
                byte[] dados = mensagem.getBytes();
                DatagramPacket pacote = new DatagramPacket(dados, dados.length, grupo, porta);
                socket.send(pacote);
                System.out.println("[Multicast] Enviado: " + mensagem);
                contador++;
                Thread.sleep(2000);
            }
        }
    }
}
```

### 6.2 Java — `java/multicast/ClienteMulticast.java`

```java
import java.net.*;
import java.io.IOException;

public class ClienteMulticast {
    // TODO: substitua pelo seu OFFSET pessoal (ver seção 3.3) — use o MESMO valor do servidor
    static final int OFFSET = 0;

    public static void main(String[] args) throws IOException {
        String grupoMulticast = "230.0.0.1";
        int porta = 4446 + OFFSET;

        try (MulticastSocket socket = new MulticastSocket(porta)) {
            InetAddress grupo = InetAddress.getByName(grupoMulticast);
            InetSocketAddress endpointGrupo = new InetSocketAddress(grupo, porta);

            // Em Wi-Fi corporativa/VPN o multicast costuma ser bloqueado. Para testar
            // servidor e cliente na MESMA máquina, use a interface de loopback explicitamente:
            // NetworkInterface interfaceRede = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
            NetworkInterface interfaceRede = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());

            socket.joinGroup(endpointGrupo, interfaceRede);
            System.out.println("[Multicast] Inscrito no grupo " + grupoMulticast + ":" + porta + ". Aguardando avisos...");

            byte[] buffer = new byte[1024];
            while (true) {
                DatagramPacket pacote = new DatagramPacket(buffer, buffer.length);
                socket.receive(pacote);
                String mensagem = new String(pacote.getData(), 0, pacote.getLength());
                System.out.println("[Multicast] Recebido: " + mensagem);
            }
        }
    }
}
```

**Como executar** (abra 2 ou 3 terminais de cliente para ver todos recebendo ao mesmo tempo):

```powershell
cd java/multicast
javac ServidorMulticast.java ClienteMulticast.java
java ClienteMulticast     # em um ou mais terminais, primeiro
java ServidorMulticast    # depois, em outro terminal
```

### 6.3 Python — `python/multicast/servidor_multicast.py`

```python
import socket
import struct
import time

# TODO: substitua pelo seu OFFSET pessoal (ver seção 3.3)
OFFSET = 0

GRUPO_MULTICAST = "230.0.0.1"
PORTA = 4446 + OFFSET

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
sock.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_TTL, 2)

print(f"[Multicast] Enviando avisos para o grupo {GRUPO_MULTICAST}:{PORTA}")
for contador in range(1, 6):
    mensagem = f"Aviso #{contador}: a aula começa em {5 - contador} minuto(s)!"
    sock.sendto(mensagem.encode("utf-8"), (GRUPO_MULTICAST, PORTA))
    print(f"[Multicast] Enviado: {mensagem}")
    time.sleep(2)

sock.close()
```

### 6.4 Python — `python/multicast/cliente_multicast.py`

```python
import socket
import struct

# TODO: substitua pelo seu OFFSET pessoal (ver seção 3.3) — use o MESMO valor do servidor
OFFSET = 0

GRUPO_MULTICAST = "230.0.0.1"
PORTA = 4446 + OFFSET

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
sock.bind(("", PORTA))

grupo = socket.inet_aton(GRUPO_MULTICAST)
solicitacao_membro = struct.pack("4sL", grupo, socket.INADDR_ANY)
sock.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP, solicitacao_membro)

print(f"[Multicast] Inscrito no grupo {GRUPO_MULTICAST}:{PORTA}. Aguardando avisos...")
while True:
    dados, endereco = sock.recvfrom(1024)
    print(f"[Multicast] Recebido: {dados.decode('utf-8')}")
```

**Como executar:**

```powershell
cd python/multicast
python cliente_multicast.py     # em um ou mais terminais, primeiro
python servidor_multicast.py    # depois, em outro terminal
```

### 6.5 Solução de problemas: "meu cliente não recebe nada"

Tráfego multicast é o mais sensível à rede dos quatro protocolos deste roteiro — pode ser bloqueado por Wi-Fi corporativa, VPN da instituição, firewall ou ambientes containerizados (Docker/WSL). Siga esta ordem antes de desistir ou pedir ajuda:

1. **Teste primeiro 100% na sua própria máquina** — um terminal com o servidor e um ou dois com o cliente, todos no mesmo computador. Se necessário, troque a linha de `NetworkInterface` no `ClienteMulticast.java` para usar explicitamente o loopback (comentário já deixado no código); em Python isso já funciona por padrão via `INADDR_ANY`.
2. Se não funcionar nem localmente, confirme se **desativou a VPN** da instituição, se houver.
3. Se estiver em **Wi-Fi** e não funcionar, teste em uma rede cabeada ou no hotspot do celular — algumas redes Wi-Fi de instituições bloqueiam tráfego multicast entre dispositivos por política de segurança.
4. Se estiver rodando em **WSL, Docker ou máquina virtual**, teste direto no sistema operacional hospedeiro (host) — essas camadas de virtualização frequentemente isolam tráfego multicast por padrão.
5. **Se, mesmo assim, não funcionar:** isso não é uma falha sua. Documente no `RESPOSTAS.md` (na resposta da Pergunta 3 desta parte) o que você tentou e o resultado observado, e substitua o print de tela por uma captura mostrando a tentativa de execução (mesmo sem receber a mensagem) — o professor avalia esse relato como evidência válida, já que o objetivo é compreender o protocolo, não vencer a política de rede do prédio.

### 6.6 Tarefa

1. Abra **dois clientes** (Java ou Python, à sua escolha) e um servidor. Confirme que **ambos** os clientes recebem os mesmos avisos.
2. Teste também a combinação cruzada: cliente em Python recebendo avisos do servidor em Java (e vice-versa). Isso deve funcionar, já que ambos falam o mesmo protocolo de rede.
3. Capture um print mostrando o servidor e os dois clientes (mesma linguagem) recebendo os avisos ao mesmo tempo, para cada linguagem, e salve-os como `evidencias/multicast/multicast-java.png` e `evidencias/multicast/multicast-python.png`. Se o teste cruzado do passo 2 funcionar, um print adicional dele é bem-vindo (não obrigatório).
4. Faça o commit:

```powershell
git add java/multicast python/multicast evidencias/multicast
git commit -m "feat(multicast): implementa servidor e cliente multicast em Java e Python"
```

### 6.7 Perguntas — Parte C

1. Qual é a diferença fundamental entre enviar a mesma mensagem para 3 clientes usando **unicast repetido 3 vezes** e enviar **uma única vez** via multicast? Pense em termos de tráfego de rede.
2. O que é o **TTL** (time-to-live) configurado no socket multicast e por que ele é importante para controlar o alcance dos pacotes na rede?
3. Se um dos clientes ficar temporariamente offline e voltar depois, ele recebe os avisos que perdeu? Por quê? Relacione com a arquitetura de comunicação em grupo.

---

## 7. Parte D — WebSocket (comunicação full-duplex em tempo real)

**Conceito:** o WebSocket começa com um *handshake* HTTP (uma requisição `Upgrade`) e, a partir daí, mantém uma conexão TCP aberta para troca de mensagens **bidirecionais** em tempo real, sem o overhead de reabrir uma conexão a cada mensagem — diferente do modelo tradicional de requisição/resposta do HTTP.

**Cenário:** o "mural de avisos" da turma — vários alunos conectados simultaneamente recebem mensagens instantaneamente, e qualquer aluno pode publicar um aviso que todos veem na hora.

> **Atenção:** esta é a parte mais avançada do roteiro — o lado Java exige uma biblioteca externa (não vem pronta no JDK). Se o tempo de aula estiver curto, priorize deixar TCP, UDP e Multicast 100% concluídos e finalize esta parte como tarefa de casa (veja a seção 2.1).

### 7.1 Java — dependência (Maven)

Para o servidor WebSocket em Java, usaremos a biblioteca leve **Java-WebSocket**. Crie `java/websocket/pom.xml`:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0">
  <modelVersion>4.0.0</modelVersion>
  <groupId>br.pucminas.labdamd</groupId>
  <artifactId>websocket-lab</artifactId>
  <version>1.0</version>
  <properties>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
  </properties>
  <dependencies>
    <dependency>
      <groupId>org.java-websocket</groupId>
      <artifactId>Java-WebSocket</artifactId>
      <version>1.5.6</version>
    </dependency>
  </dependencies>
</project>
```

### 7.2 Java — `java/websocket/src/main/java/MuralServidor.java`

```java
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class MuralServidor extends WebSocketServer {

    public MuralServidor(int porta) {
        super(new InetSocketAddress(porta));
    }

    @Override
    public void onOpen(WebSocket conexao, ClientHandshake handshake) {
        System.out.println("[WebSocket] Novo aluno conectado: " + conexao.getRemoteSocketAddress());
        conexao.send("Bem-vindo(a) ao mural de avisos da turma!");
    }

    @Override
    public void onMessage(WebSocket conexao, String mensagem) {
        System.out.println("[WebSocket] Recebido: " + mensagem);
        String avisoFormatado = "Aviso da turma: " + mensagem;
        for (WebSocket cliente : getConnections()) {
            cliente.send(avisoFormatado);
        }
    }

    @Override
    public void onClose(WebSocket conexao, int codigo, String motivo, boolean remoto) {
        System.out.println("[WebSocket] Aluno desconectado: " + conexao.getRemoteSocketAddress());
    }

    @Override
    public void onError(WebSocket conexao, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("[WebSocket] Servidor do mural iniciado.");
    }

    public static void main(String[] args) {
        // Rodando em máquina compartilhada com colegas? Some seu OFFSET (seção 3.3): 8887 + OFFSET
        MuralServidor servidor = new MuralServidor(8887);
        servidor.start();
    }
}
```

Cliente em Java (usando a API `java.net.http.WebSocket`, nativa do JDK 11+, sem dependências extras): `java/websocket/src/main/java/MuralCliente.java`

```java
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Scanner;
import java.util.concurrent.CompletionStage;

public class MuralCliente {
    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                System.out.println("\n" + data);
                System.out.print("> ");
                webSocket.request(1);
                return null;
            }
        };

        WebSocket socket = client.newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:8887"), listener)
                .join();

        Scanner teclado = new Scanner(System.in);
        System.out.println("[WebSocket] Conectado ao mural. Digite 'sair' para encerrar.");
        while (true) {
            System.out.print("> ");
            String mensagem = teclado.nextLine();
            if (mensagem.equalsIgnoreCase("sair")) {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "Até mais!").join();
                break;
            }
            socket.sendText(mensagem, true).join();
        }
    }
}
```

**Como executar:**

```powershell
cd java/websocket
mvn compile exec:java -Dexec.mainClass=MuralServidor      # em um terminal
mvn compile exec:java -Dexec.mainClass=MuralCliente        # em outro(s) terminal(is)
```

> Se preferir não configurar o plugin `exec` do Maven, compile e rode manualmente com `mvn package` gerando um `.jar` com as dependências (via `maven-shade-plugin`), ou rode direto pela sua IDE.

#### Alternativa sem Maven (download manual do jar)

Se o Maven não estiver disponível ou não conseguir baixar as dependências (proxy/firewall da rede), você pode compilar e rodar apontando diretamente para o `.jar` da biblioteca, sem precisar de `pom.xml`:

```powershell
New-Item -ItemType Directory -Force -Path java/websocket/lib
cd java/websocket/lib
Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/java-websocket/Java-WebSocket/1.5.6/Java-WebSocket-1.5.6.jar" -OutFile "Java-WebSocket-1.5.6.jar"
Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9.jar" -OutFile "slf4j-api-2.0.9.jar"
Invoke-WebRequest -Uri "https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.9/slf4j-simple-2.0.9.jar" -OutFile "slf4j-simple-2.0.9.jar"
cd ..

# compile e rode o servidor (no Windows, o separador de classpath é ";")
javac -cp "lib/*" -d out src/main/java/MuralServidor.java
java -cp "out;lib/*" MuralServidor

# o cliente usa apenas a API nativa do JDK, então compila e roda sem o classpath extra
javac -d out src/main/java/MuralCliente.java
java -cp out MuralCliente
```

> Se o download por `Invoke-WebRequest` falhar (proxy da instituição), baixe os três `.jar` manualmente pelo navegador nas URLs acima e salve-os em `java/websocket/lib/`.

### 7.3 Python — `python/websocket/mural_servidor.py`

Instale a biblioteca antes de começar: `pip install websockets`

```python
import asyncio
import websockets

clientes_conectados = set()

async def tratar_conexao(websocket):
    clientes_conectados.add(websocket)
    print(f"[WebSocket] Novo aluno conectado. Total: {len(clientes_conectados)}")
    await websocket.send("Bem-vindo(a) ao mural de avisos da turma!")
    try:
        async for mensagem in websocket:
            print(f"[WebSocket] Recebido: {mensagem}")
            aviso_formatado = f"Aviso da turma: {mensagem}"
            websockets.broadcast(clientes_conectados, aviso_formatado)
    finally:
        clientes_conectados.remove(websocket)
        print(f"[WebSocket] Aluno desconectado. Total: {len(clientes_conectados)}")

async def main():
    # Rodando em máquina compartilhada com colegas? Some seu OFFSET (seção 3.3): 8888 + OFFSET
    print("[WebSocket] Servidor do mural iniciado na porta 8888.")
    async with websockets.serve(tratar_conexao, "0.0.0.0", 8888):
        await asyncio.Future()  # mantém o servidor rodando indefinidamente

asyncio.run(main())
```

### 7.4 Python — `python/websocket/mural_cliente.py`

```python
import asyncio
import websockets

async def escutar(websocket):
    async for mensagem in websocket:
        print(f"\n{mensagem}")
        print("> ", end="", flush=True)

async def main():
    uri = "ws://localhost:8888"
    async with websockets.connect(uri) as websocket:
        print("[WebSocket] Conectado ao mural. Digite 'sair' para encerrar.")
        tarefa_escuta = asyncio.create_task(escutar(websocket))

        loop = asyncio.get_event_loop()
        while True:
            mensagem = await loop.run_in_executor(None, input, "> ")
            if mensagem.lower() == "sair":
                break
            await websocket.send(mensagem)

        tarefa_escuta.cancel()

asyncio.run(main())
```

**Como executar:**

```powershell
cd python/websocket
pip install websockets
python mural_servidor.py     # em um terminal
python mural_cliente.py      # em outro(s) terminal(is)
```

### 7.5 Tarefa

1. Rode o servidor em Java (porta 8887) com pelo menos dois clientes Java conectados e confirme que uma mensagem enviada por um cliente aparece nos demais.
2. Repita o teste com o servidor e os clientes em Python (porta 8888).
3. Capture um print mostrando o servidor e os dois clientes recebendo a mesma mensagem em tempo real, para cada linguagem, e salve-os como `evidencias/websocket/websocket-java.png` e `evidencias/websocket/websocket-python.png`.
4. Faça o commit:

```powershell
git add java/websocket python/websocket evidencias/websocket
git commit -m "feat(websocket): implementa mural em tempo real com WebSocket em Java e Python"
```

### 7.6 Perguntas — Parte D

1. O WebSocket começa com uma requisição HTTP contendo o cabeçalho `Upgrade: websocket`. O que exatamente "muda" na conexão depois que esse handshake é concluído?
2. Compare o mural via WebSocket (Parte D) com o aviso via Multicast (Parte C). Ambos entregam uma mensagem a vários destinatários — qual a diferença na forma como cada um descobre e alcança os destinatários?
3. Por que o WebSocket é mais adequado do que TCP "cru" (como o da Parte A) para este cenário de mural em tempo real, mesmo os dois sendo, no fundo, conexões TCP contínuas?

---

## 8. Checklist de entrega

- [ ] Repositório Git com a estrutura de pastas indicada, hospedado conforme orientação do professor (ex.: GitHub/GitLab da instituição)
- [ ] Ao menos **4 commits principais** (um por parte — TCP, UDP, Multicast, WebSocket), com mensagens claras no padrão `tipo(escopo): descrição` (ex.: `feat(tcp): ...`)
- [ ] Histórico de commits **incremental** — evite um único commit gigante no final; commits pequenos ao longo do desenvolvimento também são bem-vindos e contam a favor
- [ ] Os 4 pares de solução (TCP, UDP, Multicast, WebSocket), cada um em **Java e Python**, executando corretamente
- [ ] Pasta `evidencias/` com **8 prints de tela** (um por protocolo/linguagem) comprovando a execução de cada exemplo, conforme indicado na tarefa de cada parte
- [ ] Arquivo `RESPOSTAS.md` completo, com as 12 perguntas (3 por parte) respondidas de forma própria e fundamentada no código implementado

## 9. Critérios de avaliação

| Critério | O que é observado |
|---|---|
| **Commits** | Existência, granularidade e clareza das mensagens de commit ao longo de todo o desenvolvimento (não apenas no commit final) |
| **Funcionamento do código** | As 8 soluções (4 protocolos × 2 linguagens) compilam/executam e realizam a comunicação esperada |
| **Evidências de teste** | Presença, na pasta `evidencias/`, dos 8 prints de tela exigidos, mostrando execução real (não apenas código) de cada protocolo nas duas linguagens |
| **Respostas às questões** | Compreensão demonstrada nas respostas de `RESPOSTAS.md`, com referência ao comportamento observado no código, não apenas definições copiadas de fontes externas |

A ponderação exata entre esses critérios é definida pelo professor responsável pela turma (T1 ou T2), conforme os critérios de avaliação apresentados em sala.

## 10. Referências

- OLIVEIRA VALENTE, Marco Túlio. *Redes de Computadores*. Notas de aula, DCC/UFMG.
- Oracle. *Java Networking Documentation* — pacote `java.net`. Disponível em: https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/package-summary.html
- Python Software Foundation. *socket — Low-level networking interface*. Disponível em: https://docs.python.org/3/library/socket.html
- Python `websockets` project. Disponível em: https://websockets.readthedocs.io/
- Java-WebSocket project (TooTallNate). Disponível em: https://github.com/TooTallNate/Java-WebSocket
- MDN Web Docs. *The WebSocket API*. Disponível em: https://developer.mozilla.org/en-US/docs/Web/API/WebSockets_API
