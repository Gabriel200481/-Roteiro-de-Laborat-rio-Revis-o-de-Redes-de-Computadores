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

_(a preencher)_

## Parte C — Multicast

_(a preencher)_

## Parte D — WebSocket

_(a preencher)_
