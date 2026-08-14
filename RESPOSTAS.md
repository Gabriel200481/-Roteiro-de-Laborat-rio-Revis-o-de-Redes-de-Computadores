# RESPOSTAS

**Aluno:** Gabriel Afonso Infante Vieira
**Disciplina:** Laboratório de Desenvolvimento de Aplicações Móveis e Distribuídas
**Roteiro:** Revisão de Redes de Computadores (U0)
**OFFSET utilizado:** 0 (execução individual, em máquina própria)

---

## Parte A — TCP

### A1. O que acontece se você iniciar o cliente antes do servidor? Por que isso ocorre, considerando o funcionamento do TCP?

O cliente **não sobe**: ele falha imediatamente, ainda no `connect()`, antes de conseguir
enviar qualquer mensagem. Foi o que observei ao rodar cada cliente com a porta 5000 livre:

- Java (`ClienteTCP`): `java.net.ConnectException: Connection refused: connect`, lançada
  dentro de `new Socket(host, porta)` — ou seja, a linha 1 do bloco `try` nem completa.
- Python (`cliente_tcp.py`): `ConnectionRefusedError: [WinError 10061] No connection could
  be made because the target machine actively refused it`, em `cliente.connect((HOST, PORTA))`.

Isso ocorre porque o TCP é **orientado a conexão**: antes de trafegar qualquer byte de dados,
é preciso completar o *three-way handshake* (SYN → SYN-ACK → ACK). O cliente envia o SYN para
`127.0.0.1:5000`, mas, como nenhum processo fez `bind()`+`listen()` nessa porta, não existe
socket em estado `LISTENING` para responder. A pilha TCP do sistema operacional então devolve
um segmento **RST** (reset) em vez de SYN-ACK, e a chamada `connect()` retorna erro na hora.

O detalhe importante é *quem* recusa: não é o programa servidor (ele nem existe ainda), é o
próprio sistema operacional da máquina de destino. Por isso a mensagem é "actively refused" —
a recusa é imediata e explícita, diferente de um timeout. A consequência prática é que, em
TCP, **a ordem de inicialização importa**: o servidor precisa estar escutando antes do cliente.
Na Parte B veremos que em UDP isso não acontece, justamente por não haver handshake.

### A2. O TCP garante que as mensagens cheguem na ordem em que foram enviadas. Qual mecanismo do protocolo é responsável por isso?

O mecanismo é o **número de sequência** (*sequence number*) presente no cabeçalho de cada
segmento TCP, trabalhando junto com os **ACKs** e o **buffer de recepção**.

Funciona assim: o TCP não enxerga "mensagens", e sim um fluxo contínuo de bytes. Cada segmento
enviado carrega o número de sequência do primeiro byte que ele transporta. Do outro lado, o
receptor usa esses números para **remontar o fluxo na ordem correta**: se um segmento chega
adiantado (porque o anterior se perdeu ou pegou uma rota mais lenta), ele fica retido no buffer
de recepção e **não é entregue à aplicação** até que a lacuna seja preenchida. O receptor
confirma o recebimento com ACKs cumulativos, informando o próximo byte que espera; se o
emissor não recebe o ACK dentro do tempo esperado (ou recebe ACKs duplicados), ele
**retransmite** o segmento perdido.

Vale separar as duas garantias, que costumam ser confundidas:

- **Ordenação** = números de sequência + reordenação no buffer de recepção;
- **Confiabilidade** (entrega garantida) = ACK + retransmissão por timeout/ACK duplicado.

É por isso que, no código, `entrada.readLine()` (Java) e `arquivo.readline()` (Python)
funcionam de forma tão simples: a aplicação recebe os bytes exatamente na ordem em que foram
escritos, sem precisar numerar nada por conta própria. Note que o `\n` que delimita cada
mensagem é convenção **da nossa aplicação**, não do TCP — o TCP entrega bytes em ordem, e
somos nós que decidimos onde uma mensagem termina.

### A3. Na sua implementação, o que aconteceria se dois clientes tentassem se conectar ao mesmo tempo? O código atual suporta isso?

**Não suporta.** O servidor atende **um único cliente** e depois encerra. Olhando o código de
[ServidorTCP.java](java/tcp/ServidorTCP.java), o `servidor.accept()` é chamado **uma só vez**,
fora de qualquer laço: o `while` interno percorre apenas as mensagens *daquela* conexão. Quando
esse cliente manda `sair`, o `try-with-resources` fecha o `Socket` **e** o `ServerSocket`, e o
processo termina. O `servidor_tcp.py` tem exatamente a mesma estrutura (um `accept()`, um `with`).

Rodei o teste com um servidor e dois clientes simultâneos, e o comportamento observado foi:

1. O **cliente 2 conecta normalmente** — não recebe `ConnectException`. O `netstat` mostrou as
   duas conexões em `ESTABLISHED` ao mesmo tempo:

   ```
   TCP    0.0.0.0:5000           0.0.0.0:0              LISTENING       1928
   TCP    127.0.0.1:5000         127.0.0.1:61452        ESTABLISHED     1928   <- cliente 1
   TCP    127.0.0.1:5000         127.0.0.1:61453        ESTABLISHED     1928   <- cliente 2
   ```

2. Mas o **servidor nunca atendeu o cliente 2**: no log do servidor só apareceu
   `[TCP] Recebido: sou o cliente 1`. A mensagem do cliente 2 nunca foi impressa, e o cliente 2
   ficou **travado** em `entrada.readLine()`, esperando uma resposta que não viria.

3. Quando o cliente 1 mandou `sair`, o servidor fechou tudo e saiu — e só então o cliente 2
   morreu com `java.net.SocketException: Connection reset`.

A explicação para o passo 1 é a distinção entre o que o **sistema operacional** faz e o que a
**aplicação** faz. O handshake TCP é completado pelo próprio kernel e a conexão é colocada numa
**fila de espera** (*backlog*) do socket em `LISTENING` — daí o `ESTABLISHED`. O `accept()` só
*retira* uma conexão dessa fila e a entrega ao programa. Como o nosso `accept()` roda uma vez
só, a segunda conexão fica parada na fila, estabelecida porém sem ninguém do lado da aplicação
lendo dela. Do ponto de vista do usuário, isso é pior do que um erro claro: parece que
conectou, mas nada responde.

Para suportar vários clientes seria preciso (a) colocar o `accept()` dentro de um laço infinito
e (b) tratar cada conexão de forma concorrente, já que o atendimento de um cliente bloqueia:

- **Java:** `while (true) { Socket c = servidor.accept(); new Thread(() -> atender(c)).start(); }`
  — ou um `ExecutorService` com pool de threads, evitando criar uma thread por conexão.
- **Python:** um `threading.Thread` por conexão aceita, ou, sem threads, `selectors`/`asyncio`
  para multiplexar várias conexões num único laço de eventos.

Em ambos os casos o `ServerSocket`/socket de escuta permanece aberto o tempo todo, e a saída de
um cliente deixa de derrubar o servidor e os demais.

## Parte B — UDP

### B1. O que aconteceu quando você enviou uma mensagem com o servidor desligado? Compare com o que aconteceria em TCP.

O ponto mais importante do experimento é o que **não** aconteceu: **o envio não deu erro
nenhum**. Tanto `socket.send(pacote)` (Java) quanto `cliente.sendto(...)` (Python) retornaram
normalmente, como se tudo estivesse certo. O cliente entregou o datagrama à pilha de rede e
seguiu em frente — ele **não tem como saber** que não havia ninguém escutando na porta 5001.

O problema só apareceu no passo seguinte, na hora de *esperar a resposta*, e aí os dois
programas se comportaram de forma diferente (ambos registrados em `evidencias/udp/`):

| | Comportamento observado ao receber |
|---|---|
| **Java** (`ClienteUDP`) | Ficou **travado indefinidamente** em `socket.receive(resposta)`. Nenhuma exceção, nenhuma mensagem: o terminal simplesmente parou. Tive que abortar com `Ctrl+C`. |
| **Python** (`cliente_udp.py`) | Morreu em `cliente.recvfrom(1024)` com `ConnectionResetError: [WinError 10054] An existing connection was forcibly closed by the remote host`. |

A diferença **não** é do protocolo UDP, é de como cada plataforma trata um detalhe do sistema
operacional. Quando um datagrama chega a uma porta sem ninguém escutando, a pilha de rede da
máquina de destino costuma devolver um **ICMP Port Unreachable**. No Windows, esse ICMP é
reportado ao socket UDP como um erro na próxima leitura — e é isso que o Python repassa como
`ConnectionResetError`. O Java, por outro lado, só promete lançar `PortUnreachableException`
quando o `DatagramSocket` está **conectado** (isto é, quando se chamou `connect()`); como o
nosso socket é não conectado, ele ignora esse ICMP e continua bloqueado no `receive()`.

Vale insistir num ponto: esse ICMP é uma **cortesia da pilha local**, não uma garantia do UDP.
Como o teste foi em `localhost`, o aviso voltou imediatamente. Se o servidor estivesse em outra
máquina, com um firewall descartando ICMP no caminho (o caso comum na internet), **nenhum dos
dois** receberia aviso algum — os dois ficariam travados esperando para sempre. Ou seja: o
comportamento "correto" e esperado do UDP é o do Java.

**Comparando com o TCP (Parte A):** em TCP a falha aparece **antes**, no `connect()`, com um
erro claro e imediato (`Connection refused` / `WinError 10061`), porque o *three-way handshake*
precisa de alguém do outro lado para responder — o próprio protocolo verifica a existência do
destinatário antes de qualquer dado trafegar. Em UDP não existe handshake nem estado de
conexão: `sendto()` é essencialmente "joga esse pacote na rede e esquece". Isso é exatamente o
que significa **"sem conexão"** — não há nada, nem no protocolo nem no socket, que represente
"estou falando com fulano". O UDP nem sequer sabe distinguir "a mensagem chegou e a resposta se
perdeu" de "não havia servidor algum": nos dois casos o cliente apenas fica esperando. Se a
aplicação quiser detectar isso, ela mesma precisa implementar **timeout** (por exemplo,
`socket.setSoTimeout(3000)` em Java ou `cliente.settimeout(3)` em Python) e uma política de
retentativa — que é, no fundo, começar a reconstruir na mão o que o TCP já entrega pronto.

### B2. Cite dois exemplos de aplicações reais que usam UDP e explique por que a confiabilidade do TCP não é essencial (ou até atrapalharia).

**1) DNS (resolução de nomes).** Uma consulta DNS típica é minúscula: um pacote de pergunta
("qual o IP de `puc.br`?") e um de resposta. Usar TCP obrigaria a gastar um *round-trip* inteiro
só no handshake **antes** de mandar a pergunta, mais outro para encerrar a conexão — ou seja, a
consulta ficaria ~3x mais lenta por causa de burocracia de conexão, para transferir algumas
dezenas de bytes. Como a troca cabe em um pacote, a confiabilidade sai muito mais barata na
aplicação: se a resposta não chega em alguns milissegundos, o *resolver* simplesmente repete a
pergunta (eventualmente para outro servidor). É o mesmo raciocínio que vale para **DHCP** e
**NTP**: transações curtas, sem estado, em que reenviar é mais barato que manter conexão.

**2) Voz e vídeo em tempo real (VoIP, chamadas de WhatsApp/Meet, transmissão ao vivo).** Aqui a
confiabilidade do TCP **atrapalha de verdade**, e não é só questão de overhead. O áudio tem um
prazo de validade: o pacote referente ao milissegundo 300 da conversa só serve se chegar a tempo
de ser reproduzido no milissegundo 300. Se ele se perder, retransmitir é inútil — quando a
retransmissão chegar, aquele trecho já passou. Pior: o TCP entrega **em ordem**, então, enquanto
ele reenvia o pacote perdido, todos os pacotes seguintes — que já chegaram e estão prontos —
ficam retidos no buffer esperando a lacuna ser preenchida (o chamado *head-of-line blocking*).
O resultado prático é a chamada "congelar" e depois acelerar, acumulando atraso que nunca mais
é recuperado. Com UDP, o pacote perdido vira no máximo um micro-chiado de alguns milissegundos e
a conversa continua fluindo — trocar um pouco de qualidade por latência baixa é exatamente o
negócio certo aqui.

Pelo mesmo motivo entram nessa lista os **jogos online** (a posição de um jogador é atualizada
dezenas de vezes por segundo; o pacote seguinte já torna o perdido irrelevante) e o próprio
**multicast** da Parte C, que só existe sobre UDP.

### B3. O servidor UDP não mantém nenhum registro de "quem está conectado". Isso seria possível de implementar? O que mudaria na arquitetura?

**Sim, é possível — mas o trabalho todo passa a ser da aplicação, não do protocolo.** E, na
verdade, metade do caminho já está no código: o servidor **já sabe** quem mandou cada datagrama.
Em Java, `pacoteRecebido.getAddress()` e `.getPort()`; em Python, o `endereco_cliente` devolvido
por `recvfrom()`. É justamente esse par `(IP, porta)` que usamos para responder. O que falta não
é a identidade — é **guardá-la entre um datagrama e outro**.

A implementação mínima seria manter uma coleção no servidor, por exemplo um
`Map<String, Instant>` (Java) ou um `dict` (Python) indexado por `(ip, porta)`, registrando cada
remetente e a hora do último datagrama recebido. O que muda na arquitetura:

- **O servidor deixa de ser sem estado.** Hoje cada `receive()` é independente e o servidor
  poderia ser reiniciado entre dois datagramas sem ninguém perceber. Com registro de clientes,
  passa a existir memória que cresce com o número de clientes e que se perde no reinício.
- **Não existe evento de "saiu".** Em TCP, fechar o socket gera FIN e o servidor detecta o fim
  (no nosso `ServidorTCP`, o `readLine()` devolve `null`). Em UDP não há nada disso: um cliente
  que fecha o programa, cai a rede ou desliga o computador é indistinguível de um cliente
  calado. A única saída é **inferir** a saída por inatividade: definir um *timeout* (ex.: 30 s
  sem datagramas = removido) e exigir **heartbeats/keep-alive** periódicos dos clientes, mais
  uma rotina de limpeza rodando em paralelo com o laço principal.
- **Passa a ser preciso um protocolo de aplicação.** Mensagens deixam de ser texto solto e
  ganham tipo: `REGISTRAR`, `SAIR`, `PING`, `MENSAGEM`. Isso é o embrião de um protocolo próprio.
- **Duplicação e reordenação viram problema seu.** Como o UDP não numera nada, dois datagramas
  iguais podem chegar (retransmissão do cliente) ou fora de ordem; se a aplicação for sensível a
  isso, ela precisa de seus próprios números de sequência.
- **Segurança fica mais frágil.** Sem handshake, o endereço de origem de um datagrama é fácil de
  forjar (*IP spoofing*): qualquer um pode se registrar fingindo ser outro, ou inflar a tabela de
  clientes com endereços falsos. O handshake do TCP dá, de graça, uma prova mínima de que o
  remetente realmente recebe no endereço que alega.

Repare no padrão: registro de participantes, detecção de saída, ordenação, deduplicação — item
por item, estaríamos reimplementando na aplicação aquilo que o TCP já oferece pronto. Isso só se
justifica quando se precisa de algo que o TCP não dá (latência baixa, envio em grupo, controle
fino sobre o que retransmitir); caso contrário, o caminho mais sensato é usar TCP. Um meio-termo
comum no mundo real é o **QUIC** (base do HTTP/3), que roda sobre UDP e reimplementa conexão,
ordenação e confiabilidade no espaço da aplicação — justamente para poder escolher quais dessas
garantias valem a pena em cada fluxo.

## Parte C — Multicast

### C1. Qual é a diferença fundamental entre enviar a mesma mensagem para 3 clientes usando unicast repetido 3 vezes e enviar uma única vez via multicast? Pense em termos de tráfego de rede.

A diferença fundamental é **onde a mensagem é duplicada**.

No **unicast repetido**, quem duplica é o **remetente**: ele monta 3 pacotes idênticos no
conteúdo, cada um com um IP de destino diferente, e coloca os 3 na rede. Os 3 pacotes saem pelo
mesmo enlace de saída do servidor, um atrás do outro. Se os três clientes estiverem no mesmo
prédio, os 3 pacotes atravessam o mesmo caminho quase inteiro, carregando o mesmo conteúdo. O
consumo de banda no enlace do servidor cresce **linearmente** com o número de destinatários: 3
clientes = 3× a banda; 100 clientes = 100× a banda.

No **multicast**, quem duplica é a **rede**. O servidor emite **um único pacote**, endereçado ao
grupo `230.0.0.1` — não a ninguém em particular. Esse pacote caminha uma única vez por cada
enlace, e só é replicado nos pontos em que o caminho até os destinatários se **bifurca**
(roteadores e switches com IGMP snooping). Em nenhum enlace o mesmo conteúdo trafega duas vezes.
A banda gasta pelo servidor é **constante**: 1 pacote, independentemente de haver 2, 50 ou 500
ouvintes.

Há uma segunda diferença, tão importante quanto a de tráfego: o **acoplamento**. No unicast, o
remetente precisa **conhecer a lista de destinatários** — sem os endereços, não há para quem
enviar. No multicast ele não sabe (nem precisa saber) quem está ouvindo: quem decide receber são
os próprios clientes, ao entrarem no grupo com o `joinGroup()` / `IP_ADD_MEMBERSHIP`. O remetente
é totalmente desacoplado da audiência.

Isso fica bem visível comparando o código deste roteiro. O
[ServidorMulticast.java](java/multicast/ServidorMulticast.java) não tem nenhuma lista de
clientes — ele executa exatamente 5 `socket.send()`, e foi assim tanto no teste com 2 clientes
quanto no teste cruzado. Já o servidor WebSocket da Parte D faz literalmente o unicast repetido,
só que na camada de aplicação:

```java
for (WebSocket cliente : getConnections()) {   // MuralServidor.java
    cliente.send(avisoFormatado);              // uma cópia por cliente conectado
}
```

Ou seja: uma mensagem para 3 clientes vira 3 envios. É a diferença entre "avisar cada aluno no
particular" e "falar no microfone da sala".

### C2. O que é o TTL (time-to-live) configurado no socket multicast e por que ele é importante para controlar o alcance dos pacotes na rede?

O **TTL** é um campo de 8 bits do cabeçalho **IP** presente em qualquer pacote, não só em
multicast. Ele funciona como um contador de saltos: **cada roteador que encaminha o pacote
decrementa o TTL em 1**, e quando chega a zero o pacote é **descartado** (e um ICMP Time
Exceeded é devolvido). A finalidade original é servir de rede de segurança contra *loops* de
roteamento — sem ele, um pacote preso num ciclo circularia para sempre, consumindo banda.

No multicast, esse mesmo campo ganha um segundo uso, que é o que interessa aqui: ele vira o
**controle de escopo (alcance) do grupo**. Como o TTL define quantos roteadores o pacote pode
atravessar, ele define na prática **até onde o aviso se espalha**:

| TTL | Até onde a mensagem chega |
|---|---|
| 0 | Não sai da própria máquina |
| 1 | Só a sub-rede local — não passa por nenhum roteador (é o **padrão**) |
| 2 | Atravessa até 2 roteadores (rede do prédio/campus) |
| valores maiores | Alcance progressivamente mais amplo |

No código deste roteiro os dois servidores usam valores diferentes, o que vale registrar:

- [servidor_multicast.py](python/multicast/servidor_multicast.py) define explicitamente
  `sock.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_TTL, 2)` — alcance de até 2 roteadores;
- [ServidorMulticast.java](java/multicast/ServidorMulticast.java) **não** configura TTL, então
  usa o padrão **1**, ficando restrito à sub-rede local. Para igualar ao Python, bastaria um
  `socket.setOption(StandardSocketOptions.IP_MULTICAST_TTL, 2)`.

Nos meus testes essa diferença não afetou o resultado, porque servidor e clientes estavam na
**mesma máquina** — nenhum roteador no caminho, nenhum decremento de TTL. Ela só apareceria num
cenário com sub-redes diferentes.

Por que isso é importante: sem limite de escopo, uma mensagem de grupo tenderia a se espalhar
muito além do público pretendido, desperdiçando banda em redes que não têm nenhum interessado —
e expondo o conteúdo a quem não deveria vê-lo. O TTL é um controle de raio **barato e grosseiro**
("não passe de N saltos"), que na prática hoje costuma ser combinado com endereços de **escopo
administrativo** (a faixa `239.0.0.0/8`, reservada para uso interno de organizações) e com
filtros nos roteadores de borda. Vale notar que TTL baixo é também a razão nº 1 de "meu cliente
não recebe nada" quando servidor e cliente estão em redes diferentes.

### C3. Se um dos clientes ficar temporariamente offline e voltar depois, ele recebe os avisos que perdeu? Por quê?

**Não recebe.** As mensagens perdidas estão perdidas para sempre.

Testei exatamente esse cenário com três clientes Java: um permaneceu no ar o tempo todo
(controle), e outro foi derrubado depois do aviso #2 e reinscrito no grupo alguns segundos
depois. O resultado:

```
--- CLIENTE SEMPRE NO AR ---            --- CLIENTE ANTES DE CAIR ---
Recebido: Aviso #1                      Recebido: Aviso #1
Recebido: Aviso #2                      Recebido: Aviso #2
Recebido: Aviso #3                      (derrubado aqui)
Recebido: Aviso #4
Recebido: Aviso #5                      --- MESMO CLIENTE APOS RECONECTAR ---
                                        Recebido: Aviso #5
```

O cliente que caiu perdeu os avisos **#3 e #4** definitivamente: ao voltar, ele só passou a
receber o que foi enviado **a partir** do instante da nova inscrição.

A razão está na arquitetura da comunicação em grupo usada aqui. O multicast IP é
**best-effort** e roda sobre **UDP**, herdando todas as (não) garantias da Parte B:

- **Ninguém guarda histórico.** O servidor faz `send()` e esquece; não há buffer de mensagens
  antigas em lugar nenhum — nem no remetente, nem nos roteadores, nem nos clientes.
- **O remetente não sabe quem são os membros**, que é justamente a vantagem do C1. Como não
  existe lista de destinatários, não existe a noção de "fulano não recebeu" — e sem essa noção,
  não há como haver retransmissão dirigida. O servidor sequer percebeu que um cliente sumiu.
- **A entrega é decidida no instante do envio.** Um datagrama é encaminhado apenas para quem
  estava inscrito no grupo naquele momento. Entrar no grupo (`joinGroup()` / IGMP) é um pedido
  de "quero receber **daqui em diante**", nunca um pedido de sincronização.
- **Não há sequer detecção de perda.** Como não há numeração nem ACK, o cliente que voltou não
  tem como saber que existiram um aviso #3 e um #4 — no nosso caso isso só é perceptível porque
  nós, humanos, vemos o número na mensagem.

A analogia que funciona bem é a do **rádio**: quem não estava com o aparelho ligado na hora,
perdeu; ligar depois não faz a emissora repetir. É o oposto do modelo de **caixa postal**.

Se a aplicação precisasse dessa garantia, seria preciso construí-la por cima, porque o protocolo
não dá — por exemplo, numerando as mensagens e fazendo o cliente pedir as que faltam (NACK), ou
abandonando o multicast puro e usando um **broker com persistência** (MQTT com mensagens
retidas, Kafka, RabbitMQ), que armazena o histórico e reentrega na reconexão. É o que o slide da
aula quer dizer com "pode ser confiável ou não confiável, ordenado ou não ordenado": esses são
**níveis de garantia construídos sobre** o multicast básico, não características que ele já tenha.

Vale contrastar com a Parte D: no mural WebSocket o servidor **conhece** cada cliente conectado
(`getConnections()` / o `set` `clientes_conectados`), então ele *teria* como reenviar algo a
quem voltou — mas, do jeito que está implementado, também não guarda histórico. Saber quem são
os destinatários é condição necessária, porém não suficiente: alguém precisa **guardar** as
mensagens.

## Parte D — WebSocket

### D1. O WebSocket começa com uma requisição HTTP contendo o cabeçalho `Upgrade: websocket`. O que exatamente "muda" na conexão depois que esse handshake é concluído?

O que **não** muda é o mais importante para entender: a **conexão TCP é exatamente a mesma**.
Ela não é fechada nem reaberta — não há um segundo *three-way handshake*. O que muda é o
**protocolo falado por cima daquele socket**, que deixa de ser HTTP.

O handshake funciona assim. O cliente abre uma conexão TCP comum e manda uma requisição HTTP
normal, só que com cabeçalhos especiais:

```http
GET / HTTP/1.1
Host: localhost:8887
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
Sec-WebSocket-Version: 13
```

Se o servidor aceita, ele responde com o status **`101 Switching Protocols`** (e não o 200 de
sempre), devolvendo em `Sec-WebSocket-Accept` um hash SHA-1 derivado da chave enviada — prova de
que do outro lado há mesmo um servidor WebSocket, e não um servidor HTTP qualquer que ignorou os
cabeçalhos. **Esse `101` é o instante da troca.**

A partir dele, muda o seguinte:

1. **Some o modelo requisição/resposta.** Em HTTP, quem fala é sempre o cliente: o servidor só
   pode responder a um pedido. Depois do upgrade a conexão é **full-duplex e simétrica** —
   qualquer um dos dois lados pode enviar a qualquer momento, sem ninguém ter pedido nada.
2. **Muda o formato dos dados.** Em vez de cabeçalhos de texto, o fluxo passa a carregar
   **frames** binários do WebSocket, com um cabeçalho curto (bit FIN, *opcode* indicando
   texto/binário/close/ping/pong, bit de máscara e tamanho do payload). Como o cabeçalho tem
   poucos bytes, some o overhead de reenviar cabeçalhos e cookies a cada mensagem.
3. **Passa a existir fronteira de mensagem.** O frame diz onde cada mensagem começa e termina —
   detalhe que volta a importar na pergunta D3.
4. **Ganha-se manutenção de conexão embutida:** frames de *ping/pong* para detectar conexões
   mortas e um *close handshake* com código de status para encerrar de forma limpa (é o que o
   `socket.sendClose(WebSocket.NORMAL_CLOSURE, "Ate mais!")` do `MuralCliente` faz).

Dá para **ver essa mudança acontecendo** na evidência `evidencias/websocket/`: assim que um
cliente conecta, ele recebe `Bem-vindo(a) ao mural de avisos da turma!` **sem ter pedido nada**.
Esse `conexao.send(...)` dentro do `onOpen` é exatamente o tipo de coisa que HTTP puro não
permite — o servidor falando primeiro.

Por fim, por que começar com HTTP em vez de já abrir um protocolo próprio? Para **atravessar a
infraestrutura existente**: usando as portas 80/443 e um handshake que parece HTTP, a conexão
passa por proxies, firewalls e balanceadores que rejeitariam uma porta exótica. É uma decisão
pragmática de compatibilidade — o assunto da última parte da D3.

### D2. Compare o mural via WebSocket (Parte D) com o aviso via Multicast (Parte C). Qual a diferença na forma como cada um descobre e alcança os destinatários?

A diferença está em **onde mora a lista de destinatários**: na *aplicação* (WebSocket) ou na
*rede* (multicast).

| | **Mural WebSocket (D)** | **Aviso multicast (C)** |
|---|---|---|
| Quem conhece a audiência | O **servidor**: tem um objeto de conexão por cliente | **Ninguém** no remetente; os roteadores conhecem os inscritos |
| Como o destinatário entra | Abre uma **conexão TCP** com o servidor (handshake) | Faz `joinGroup()` / `IP_ADD_MEMBERSHIP` (IGMP) — nem fala com o remetente |
| Como a mensagem é entregue | O servidor **percorre a lista e envia uma cópia para cada** | **Um único** datagrama; a rede replica nas bifurcações |
| Tráfego para N clientes | **N** cópias (cresce linearmente) | **1** cópia (constante) |
| Garantias | Confiável e ordenado (é TCP) | Best-effort: pode perder, duplicar, desordenar |
| Alcance real | Toda a internet (NAT, proxy, TLS) | Na prática só a rede local — TTL e bloqueio de ISPs |
| Sabe quem saiu? | Sim, via `onClose` | Não — nunca fica sabendo |

No código a diferença é literal. O servidor WebSocket **itera sobre um registro**:

```java
for (WebSocket cliente : getConnections()) {   // MuralServidor.java
    cliente.send(avisoFormatado);
}
```
```python
websockets.broadcast(clientes_conectados, aviso_formatado)   # mural_servidor.py
```

Ou seja, o "broadcast" do mural é **unicast repetido na camada de aplicação**. Já o
`ServidorMulticast` não tem lista nenhuma: faz `socket.send(pacote)` uma vez por aviso e pronto.

Isso apareceu nas evidências. O servidor WebSocket **contou** os clientes
(`Novo aluno conectado. Total: 1`, `Total: 2`) e registrou o endereço de cada um; o servidor
multicast imprimiu exatamente os mesmos 5 `Enviado:` no teste com dois clientes e no teste
cruzado — para ele, **é indiferente quem está ouvindo, ou se há alguém**.

A consequência prática de projeto: o multicast é imbatível em eficiência para distribuir a mesma
informação a muitos ouvintes numa rede controlada (TV/rádio IP, cotações de bolsa, descoberta de
serviços numa LAN), mas não funciona pela internet e não garante entrega. O WebSocket gasta N
vezes mais banda, porém entrega de forma confiável, atravessa qualquer rede, sabe exatamente
quem está conectado — e, principalmente, permite **conversa nos dois sentidos**: no mural
qualquer aluno publica e todos veem, enquanto no multicast a comunicação é de mão única, do
professor para a turma.

### D3. Por que o WebSocket é mais adequado do que TCP "cru" (como o da Parte A) para este cenário de mural em tempo real, mesmo os dois sendo, no fundo, conexões TCP contínuas?

De fato os dois são a mesma coisa no nível do transporte — a diferença é tudo que o WebSocket já
resolve e que, em TCP puro, teríamos que construir à mão. Quatro pontos, do mais concreto ao
mais estrutural:

**1) Fronteira de mensagem.** O TCP entrega um **fluxo de bytes**, sem separar mensagens. Na
Parte A nós inventamos uma convenção para contornar isso: `println()` / `readLine()`, usando o
`\n` como separador. Funciona, mas é frágil — se um aviso contiver uma quebra de linha, ele
chega partido em dois; e não há como enviar dados binários (uma imagem no mural, por exemplo).
O WebSocket resolve isso no protocolo: cada `send()` é **uma mensagem**, e o `onMessage` /
`async for` recebe exatamente aquela mensagem, inteira, com tipo (texto ou binário) — inclusive
remontando mensagens grandes que tenham sido fragmentadas em vários frames.

**2) Recepção assíncrona — e este é o ponto decisivo para "tempo real".** Olhe o laço do
`ClienteTCP` da Parte A:

```java
saida.println(linha);              // envia
System.out.println(entrada.readLine());   // e fica BLOQUEADO esperando a resposta
```

Ele é estritamente alternado: fala, espera resposta, fala de novo. Enquanto está parado no
`teclado.readLine()` esperando o usuário digitar, ele **não consegue receber nada** do servidor.
Num mural isso é fatal: as mensagens dos outros alunos chegam a qualquer momento, sem relação
com o que este usuário está fazendo. Com TCP puro a saída seria criar **uma thread só para
leitura** e sincronizar as duas. A API de WebSocket já é orientada a eventos — o `onText` do
`MuralCliente` e a *task* `escutar()` do `mural_cliente.py` rodam em paralelo com a leitura do
teclado — e é justamente por isso que, no print da evidência, o cliente 2 exibe o aviso do
cliente 1 **sem ter feito nada**.

**3) Ciclo de vida e gestão de vários clientes.** O `ServidorTCP` da Parte A atende **um** cliente
e morre (pergunta A3). A biblioteca de WebSocket entrega pronto o que faltava: `onOpen`,
`onClose`, `onError`, a coleção `getConnections()` com todos os conectados, e *ping/pong*
automático para detectar quem caiu sem avisar. Em TCP cru, isso é um pool de threads e uma
estrutura de dados concorrente escritos por nós.

**4) Alcance e compatibilidade com a web.** Um servidor TCP cru na porta 5000 tende a ser
barrado por firewall/proxy corporativo — e, principalmente, **nenhum navegador consegue abrir um
socket TCP arbitrário**. Como o WebSocket nasce de um handshake HTTP nas portas 80/443, ele
atravessa essa infraestrutura, aceita TLS (`wss://`) e é suportado nativamente por qualquer
navegador com `new WebSocket("ws://...")`. Para um mural que os alunos abririam no navegador, TCP
cru simplesmente **não é uma opção**.

Resumindo: WebSocket não substitui o TCP, ele **padroniza uma camada em cima do TCP** com
enquadramento de mensagens, comunicação bidirecional orientada a eventos, controle de conexão e
compatibilidade com a web. Usar TCP puro aqui significaria reimplementar, com menos qualidade,
tudo isso — o mesmo tipo de trade-off que apareceu na pergunta B3.
