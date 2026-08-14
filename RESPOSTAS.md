# RESPOSTAS

Aluno: Gabriel Afonso Infante Vieira
Disciplina: Laboratório de Desenvolvimento de Aplicações Móveis e Distribuídas
Roteiro: revisão de redes de computadores (U0)
OFFSET usado: 0, porque rodei tudo na minha própria máquina

---

## Parte A, TCP

### A1. O que acontece se você iniciar o cliente antes do servidor? Por que isso ocorre, considerando o funcionamento do TCP?

O cliente falha logo no `connect()`, antes de conseguir enviar qualquer mensagem. Rodei os
dois com a porta 5000 livre e os erros foram:

- Java: `java.net.ConnectException: Connection refused: connect`, lançada dentro do
  `new Socket(host, porta)`.
- Python: `ConnectionRefusedError: [WinError 10061] No connection could be made because the
  target machine actively refused it`, na linha `cliente.connect((HOST, PORTA))`.

Isso acontece porque o TCP é orientado a conexão. Antes de trafegar dados é preciso completar
o handshake de três vias (SYN, SYN-ACK, ACK). O cliente manda o SYN para `127.0.0.1:5000`, mas
não existe socket em `LISTENING` nessa porta, então a pilha TCP do sistema responde com um RST
e o `connect()` retorna erro na hora.

Quem recusa não é o programa servidor, que nem está rodando, e sim o sistema operacional. Por
isso a mensagem fala em "actively refused": a recusa é imediata e explícita, diferente de um
timeout. Na prática, em TCP a ordem de inicialização importa, o servidor tem que estar
escutando antes. Em UDP isso não acontece, justamente por não haver handshake.

### A2. Qual mecanismo do protocolo é responsável por garantir a ordem das mensagens?

O número de sequência do cabeçalho TCP, junto com os ACKs e o buffer de recepção.

O TCP não trabalha com mensagens, e sim com um fluxo contínuo de bytes. Cada segmento carrega o
número de sequência do primeiro byte que ele transporta. O receptor usa esse número para
remontar o fluxo na ordem certa: se um segmento chega adiantado, porque o anterior se perdeu ou
pegou um caminho mais lento, ele fica retido no buffer e não é entregue à aplicação até a lacuna
ser preenchida. O receptor confirma o que recebeu com ACKs, e se o emissor não recebe a
confirmação, ou recebe ACKs duplicados, ele retransmite.

Vale separar as duas garantias, que costumam ser confundidas:

- ordem: números de sequência mais reordenação no buffer de recepção;
- entrega garantida: ACK mais retransmissão.

É por isso que `entrada.readLine()` no Java e `arquivo.readline()` no Python funcionam de forma
tão direta aqui: os bytes chegam na mesma ordem em que foram escritos. O `\n` que separa uma
mensagem da outra é convenção da nossa aplicação, não do TCP.

### A3. O que aconteceria se dois clientes tentassem se conectar ao mesmo tempo? O código atual suporta isso?

Não suporta. Em [ServidorTCP.java](java/tcp/ServidorTCP.java) o `accept()` é chamado uma vez só,
fora de qualquer laço. O `while` de dentro trata apenas as mensagens daquela conexão, e quando o
cliente manda `sair` o try-with-resources fecha o `Socket` e o `ServerSocket` e o programa
termina. O `servidor_tcp.py` tem a mesma estrutura.

Testei com um servidor e dois clientes simultâneos:

1. O cliente 2 conectou normalmente, sem `ConnectException`. As duas conexões apareceram como
   `ESTABLISHED` no netstat ao mesmo tempo:

   ```
   TCP    0.0.0.0:5000           0.0.0.0:0              LISTENING       1928
   TCP    127.0.0.1:5000         127.0.0.1:61452        ESTABLISHED     1928   <- cliente 1
   TCP    127.0.0.1:5000         127.0.0.1:61453        ESTABLISHED     1928   <- cliente 2
   ```

2. Mas o servidor nunca atendeu o cliente 2. No log dele só apareceu
   `[TCP] Recebido: sou o cliente 1`. O cliente 2 ficou parado no `readLine()`, esperando uma
   resposta que não vinha.

3. Quando o cliente 1 mandou `sair`, o servidor fechou tudo e saiu, e só aí o cliente 2 caiu com
   `java.net.SocketException: Connection reset`.

O motivo do item 1 é que o handshake é feito pelo kernel, não pelo programa. A conexão entra numa
fila de espera (backlog) do socket em `LISTENING`, e por isso já conta como `ESTABLISHED`. O
`accept()` apenas retira uma conexão dessa fila e entrega para a aplicação. Como chamamos
`accept()` uma vez só, a segunda ficou parada na fila, estabelecida mas sem ninguém lendo dela.
Para quem está usando, isso é pior que um erro claro, porque parece que conectou e simplesmente
nada responde.

Para atender vários clientes seria preciso colocar o `accept()` num laço e tratar cada conexão de
forma concorrente, já que o atendimento de um cliente bloqueia:

- Java: `while (true) { Socket c = servidor.accept(); new Thread(() -> atender(c)).start(); }`,
  ou um `ExecutorService` com pool de threads.
- Python: uma thread por conexão aceita, ou `selectors`/`asyncio` para tratar várias conexões
  num laço só.

Nos dois casos o socket de escuta fica aberto o tempo todo e a saída de um cliente deixa de
derrubar o servidor.

---

## Parte B, UDP

### B1. O que aconteceu quando você enviou uma mensagem com o servidor desligado? Compare com o TCP.

O envio não deu erro nenhum. Tanto o `socket.send(pacote)` do Java quanto o
`cliente.sendto(...)` do Python retornaram normalmente, como se estivesse tudo certo. O cliente
entrega o datagrama para a pilha de rede e segue em frente, sem ter como saber que não havia
ninguém escutando na porta 5001.

O problema só apareceu na hora de esperar a resposta, e aí os dois se comportaram de forma
diferente (as duas execuções estão em `evidencias/udp/`):

- Java: ficou travado indefinidamente no `socket.receive(resposta)`, sem exceção e sem nenhuma
  mensagem. Tive que abortar com Ctrl+C.
- Python: caiu no `cliente.recvfrom(1024)` com
  `ConnectionResetError: [WinError 10054] An existing connection was forcibly closed by the remote host`.

Essa diferença não é do UDP, é de como cada plataforma trata um detalhe do sistema operacional.
Quando um datagrama chega numa porta sem ninguém escutando, a máquina de destino costuma devolver
um ICMP Port Unreachable. No Windows esse ICMP vira erro na próxima leitura do socket, e é isso
que o Python repassa como `ConnectionResetError`. Já o Java só lança `PortUnreachableException`
quando o `DatagramSocket` está conectado, ou seja, quando se chamou `connect()`. Como o nosso não
está, ele ignora o ICMP e continua bloqueado.

Esse aviso é uma cortesia da pilha local, não uma garantia do UDP. Como o teste foi em localhost,
ele voltou na hora. Se o servidor estivesse em outra máquina, com um firewall descartando ICMP no
caminho, nenhum dos dois receberia aviso nenhum e os dois ficariam esperando para sempre. O
comportamento do Java é o que representa melhor o que o UDP realmente oferece.

Comparando com o TCP da Parte A, lá a falha aparece antes, no `connect()`, com um erro imediato
(`Connection refused`), porque o handshake precisa de alguém do outro lado para responder. Em UDP
não existe handshake nem estado de conexão, o `sendto()` só joga o pacote na rede. É isso que
"sem conexão" significa: não há nada, nem no protocolo nem no socket, representando "estou
falando com fulano". O UDP também não consegue distinguir "a mensagem chegou e a resposta se
perdeu" de "não havia servidor nenhum", nos dois casos o cliente apenas fica esperando. Se a
aplicação quiser perceber isso, ela mesma precisa usar timeout (`socket.setSoTimeout(3000)` no
Java, `cliente.settimeout(3)` no Python) e decidir se retransmite.

### B2. Cite dois exemplos de aplicações reais que usam UDP e explique por que a confiabilidade do TCP não é essencial ou até atrapalharia.

DNS. Uma consulta é minúscula, um pacote de pergunta e um de resposta. Com TCP seria preciso
gastar um round-trip inteiro só no handshake antes de mandar a pergunta, e mais um para encerrar,
o que deixaria a consulta bem mais lenta para transferir algumas dezenas de bytes. Como a troca
cabe em um pacote, sai mais barato resolver a confiabilidade na aplicação: se a resposta não
chega em alguns milissegundos, o resolver pergunta de novo, eventualmente para outro servidor. O
mesmo raciocínio vale para DHCP e NTP, que são transações curtas e sem estado.

Voz e vídeo em tempo real, como chamadas e transmissões ao vivo. Aqui a confiabilidade do TCP
atrapalha de verdade. O pacote de áudio referente ao milissegundo 300 da conversa só serve se
chegar a tempo de ser reproduzido no milissegundo 300. Se ele se perder, retransmitir não
adianta, porque quando a retransmissão chegar aquele trecho já passou. Pior que isso, o TCP
entrega em ordem, então enquanto ele reenvia o pacote perdido todos os pacotes seguintes, que já
chegaram e estão prontos, ficam retidos no buffer esperando a lacuna ser preenchida. É o
head-of-line blocking, e o efeito prático é a chamada travar e depois acelerar, acumulando um
atraso que não se recupera. Com UDP o pacote perdido vira no máximo um chiado de alguns
milissegundos e a conversa continua.

Pelo mesmo motivo entram nessa lista os jogos online, em que a posição de um jogador é atualizada
várias vezes por segundo e o pacote seguinte já torna o perdido irrelevante, e o próprio multicast
da Parte C, que só existe sobre UDP.

### B3. O servidor UDP não mantém registro de quem está conectado. Seria possível implementar? O que mudaria na arquitetura?

Dá para implementar, mas o trabalho todo passa a ser da aplicação. Metade do caminho já está no
código, porque o servidor já sabe quem mandou cada datagrama: no Java isso vem de
`pacoteRecebido.getAddress()` e `.getPort()`, e no Python do `endereco_cliente` devolvido pelo
`recvfrom()`. É esse par (IP, porta) que usamos para responder. O que falta não é a identidade, é
guardá-la de um datagrama para o outro.

A implementação mínima seria manter uma coleção no servidor, por exemplo um `Map` no Java ou um
`dict` no Python, indexada por (ip, porta), registrando cada remetente e a hora do último
datagrama recebido. O que mudaria:

- O servidor deixaria de ser sem estado. Hoje cada `receive()` é independente e dava para
  reiniciar o servidor entre dois datagramas sem ninguém perceber. Com registro passa a existir
  memória que cresce com o número de clientes e que se perde no reinício.
- Não existe evento de saída. Em TCP, fechar o socket gera um FIN e o servidor detecta o fim,
  no nosso `ServidorTCP` isso aparece no `readLine()` devolvendo `null`. Em UDP, um cliente que
  fecha o programa, perde a rede ou desliga o computador é indistinguível de um cliente calado. A
  única saída é inferir por inatividade: definir um timeout, por exemplo 30 segundos sem
  datagramas, exigir mensagens periódicas de keep-alive dos clientes e ter uma rotina de limpeza
  rodando em paralelo com o laço principal.
- Passaria a ser necessário um protocolo de aplicação. As mensagens deixariam de ser texto solto
  e ganhariam tipo: REGISTRAR, SAIR, PING, MENSAGEM.
- Duplicação e reordenação viram problema nosso, já que o UDP não numera nada. Dois datagramas
  iguais podem chegar, ou chegar fora de ordem, e se a aplicação for sensível a isso precisa dos
  próprios números de sequência.
- A segurança fica mais frágil. Sem handshake, o endereço de origem é fácil de forjar, então
  qualquer um pode se registrar fingindo ser outro, ou encher a tabela de clientes com endereços
  falsos. O handshake do TCP já dá uma prova mínima de que o remetente realmente recebe no
  endereço que alega.

Juntando os itens, registro de participantes, detecção de saída, ordenação e deduplicação, o que
estaríamos fazendo é reimplementar na aplicação boa parte do que o TCP já entrega pronto. Isso só
compensa quando se precisa de algo que o TCP não dá, como latência baixa, envio em grupo ou
controle sobre o que vale a pena retransmitir.

---

## Parte C, Multicast

### C1. Qual a diferença entre enviar a mesma mensagem para 3 clientes com unicast repetido e enviar uma única vez via multicast?

A diferença é onde a mensagem é duplicada.

No unicast repetido quem duplica é o remetente. Ele monta 3 pacotes com o mesmo conteúdo e
destinos diferentes e coloca os 3 na rede, um atrás do outro, pelo mesmo enlace de saída. Se os
três clientes estiverem no mesmo prédio, os 3 pacotes percorrem quase o mesmo caminho carregando
a mesma coisa. O consumo de banda na saída do servidor cresce junto com o número de
destinatários: 3 clientes, 3 vezes a banda; 100 clientes, 100 vezes.

No multicast quem duplica é a rede. O servidor emite um pacote só, endereçado ao grupo
230.0.0.1, e não a ninguém em particular. Esse pacote passa uma única vez por cada enlace e só é
replicado nos pontos em que o caminho até os destinatários se bifurca. A banda gasta pelo
servidor é sempre a mesma, com 2, 50 ou 500 ouvintes.

Tem uma segunda diferença, tão importante quanto: no unicast o remetente precisa conhecer a
lista de destinatários, sem os endereços não há para quem enviar. No multicast ele não sabe quem
está ouvindo, quem decide receber são os próprios clientes, quando entram no grupo com o
`joinGroup()` ou `IP_ADD_MEMBERSHIP`.

Isso fica visível comparando os códigos deste roteiro. O
[ServidorMulticast.java](java/multicast/ServidorMulticast.java) não tem lista de clientes
nenhuma, ele executa 5 `socket.send()` e pronto, e foi assim tanto no teste com 2 clientes quanto
no teste cruzado. Já o servidor WebSocket da Parte D faz unicast repetido, só que na camada de
aplicação:

```java
for (WebSocket cliente : getConnections()) {   // MuralServidor.java
    cliente.send(avisoFormatado);              // uma cópia por cliente conectado
}
```

Uma mensagem para 3 clientes vira 3 envios.

### C2. O que é o TTL configurado no socket multicast e por que ele é importante para controlar o alcance?

O TTL é um campo de 8 bits do cabeçalho IP, presente em qualquer pacote e não só em multicast.
Ele funciona como um contador de saltos: cada roteador que encaminha o pacote diminui o TTL em 1,
e quando chega a zero o pacote é descartado. A finalidade original é evitar que um pacote preso
num loop de roteamento fique circulando para sempre.

No multicast esse mesmo campo passa a servir também para limitar o alcance do grupo, já que ele
define quantos roteadores o pacote pode atravessar:

- 0: não sai da própria máquina;
- 1: só a sub-rede local, não passa por nenhum roteador (é o padrão);
- 2: atravessa até 2 roteadores, o que dá a rede do prédio ou do campus;
- valores maiores: alcance progressivamente maior.

Os dois servidores do roteiro usam valores diferentes, o que vale registrar. O
[servidor_multicast.py](python/multicast/servidor_multicast.py) configura explicitamente
`sock.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_TTL, 2)`. O
[ServidorMulticast.java](java/multicast/ServidorMulticast.java) não configura nada, então fica
com o padrão 1 e restrito à sub-rede local. Para igualar bastaria um
`socket.setOption(StandardSocketOptions.IP_MULTICAST_TTL, 2)`. Nos meus testes essa diferença não
mudou o resultado, porque servidor e clientes estavam na mesma máquina e não havia nenhum
roteador no caminho para decrementar o TTL.

Sem esse limite, uma mensagem de grupo tenderia a se espalhar muito além do público pretendido,
gastando banda em redes onde ninguém tem interesse e expondo o conteúdo a quem não deveria ver. O
TTL é um controle de raio simples, que hoje costuma ser combinado com a faixa 239.0.0.0/8,
reservada para uso interno das organizações, e com filtros nos roteadores de borda. TTL baixo
também é uma das causas mais comuns de "meu cliente não recebe nada" quando servidor e cliente
estão em redes diferentes.

### C3. Se um cliente ficar temporariamente offline e voltar depois, ele recebe os avisos que perdeu?

Não recebe, as mensagens perdidas ficam perdidas.

Testei com três clientes Java: um ficou no ar o tempo todo, servindo de controle, e outro foi
derrubado depois do aviso #2 e reinscrito no grupo alguns segundos depois. O resultado:

```
--- CLIENTE SEMPRE NO AR ---            --- CLIENTE ANTES DE CAIR ---
Recebido: Aviso #1                      Recebido: Aviso #1
Recebido: Aviso #2                      Recebido: Aviso #2
Recebido: Aviso #3                      (derrubado aqui)
Recebido: Aviso #4
Recebido: Aviso #5                      --- MESMO CLIENTE APOS RECONECTAR ---
                                        Recebido: Aviso #5
```

O cliente que caiu perdeu os avisos #3 e #4. Ao voltar, ele só passou a receber o que foi enviado
a partir do momento da nova inscrição.

O motivo está na arquitetura da comunicação em grupo. O multicast IP é best-effort e roda sobre
UDP, herdando as mesmas não garantias da Parte B:

- Ninguém guarda histórico. O servidor faz `send()` e esquece, não existe buffer de mensagens
  antigas nem no remetente, nem nos roteadores, nem nos clientes.
- O remetente não sabe quem são os membros, que é justamente a vantagem descrita na C1. Como não
  existe lista de destinatários, não existe a noção de "fulano não recebeu", e sem isso não há
  como haver retransmissão dirigida. O servidor nem percebeu que um cliente sumiu.
- A entrega é decidida no instante do envio. O datagrama vai só para quem estava inscrito naquele
  momento, e entrar no grupo é um pedido de receber daí em diante, não um pedido de sincronização.
- Não há nem detecção de perda. Sem numeração e sem ACK, o cliente que voltou não tem como saber
  que existiram um aviso #3 e um #4. No nosso caso isso só é perceptível porque o número está
  escrito no texto da mensagem.

Funciona como rádio: quem não estava com o aparelho ligado na hora perdeu, e ligar depois não faz
a emissora repetir.

Se a aplicação precisasse dessa garantia, ela teria que ser construída por cima, porque o
protocolo não dá. Daria para numerar as mensagens e fazer o cliente pedir as que faltaram, ou
abandonar o multicast puro e usar um servidor intermediário que armazena o histórico e reentrega
na reconexão. É isso que o slide da aula quer dizer com "pode ser confiável ou não confiável,
ordenado ou não ordenado": são níveis de garantia construídos sobre o multicast básico, não
características que ele já tenha.

Vale contrastar com a Parte D: no mural WebSocket o servidor conhece cada cliente conectado, então
teria como reenviar algo para quem voltou, mas do jeito que está implementado ele também não
guarda histórico. Saber quem são os destinatários é necessário, mas não basta, alguém precisa
guardar as mensagens.

---

## Parte D, WebSocket

### D1. O que exatamente muda na conexão depois que o handshake é concluído?

A conexão TCP continua sendo a mesma. Ela não é fechada nem reaberta, não existe um segundo
handshake TCP. O que muda é o protocolo falado por cima daquele socket, que deixa de ser HTTP.

O cliente abre uma conexão TCP comum e manda uma requisição HTTP normal, só que com cabeçalhos
especiais:

```http
GET / HTTP/1.1
Host: localhost:8887
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
Sec-WebSocket-Version: 13
```

Se o servidor aceita, ele responde com `101 Switching Protocols`, e não com o 200 de sempre,
devolvendo em `Sec-WebSocket-Accept` um hash SHA-1 derivado da chave enviada, o que prova que do
outro lado existe mesmo um servidor WebSocket, e não um servidor HTTP qualquer que ignorou os
cabeçalhos. Esse 101 é o instante da troca.

A partir dele muda o seguinte:

1. Acaba o modelo requisição e resposta. Em HTTP quem fala é sempre o cliente, o servidor só
   responde a um pedido. Depois do upgrade a conexão é full-duplex, os dois lados podem enviar a
   qualquer momento.
2. Muda o formato dos dados. Em vez de cabeçalhos de texto, o fluxo passa a carregar frames
   binários, com um cabeçalho curto contendo o bit FIN, o opcode que diz se é texto, binário,
   close, ping ou pong, o bit de máscara e o tamanho do payload. Some o custo de reenviar
   cabeçalhos e cookies a cada mensagem.
3. Passa a existir fronteira de mensagem, detalhe que volta a importar na D3.
4. Vêm junto os frames de ping e pong, para detectar conexões mortas, e um encerramento com
   código de status, que é o que o `socket.sendClose(WebSocket.NORMAL_CLOSURE, "Ate mais!")` do
   `MuralCliente` usa.

Dá para ver essa mudança na evidência em `evidencias/websocket/`: assim que um cliente conecta,
ele recebe `Bem-vindo(a) ao mural de avisos da turma!` sem ter pedido nada. Esse `conexao.send()`
dentro do `onOpen` é o servidor falando primeiro, o que HTTP puro não permite.

A razão de começar com HTTP em vez de já abrir um protocolo próprio é aproveitar as portas 80 e
443 e um handshake que parece HTTP, para conseguir passar por proxies, firewalls e balanceadores
que rejeitariam uma porta fora do comum.

### D2. Comparando o mural WebSocket com o aviso multicast, qual a diferença na forma de descobrir e alcançar os destinatários?

A diferença está em onde fica a lista de destinatários: na aplicação, no caso do WebSocket, ou na
rede, no caso do multicast.

No mural, o servidor tem um objeto de conexão para cada cliente. Ele sabe quem entrou pelo
`onOpen`, quem saiu pelo `onClose` e quantos são. Para entregar uma mensagem ele percorre essa
lista e manda uma cópia para cada um, cada uma pela sua própria conexão TCP. O tráfego cresce
junto com o número de clientes, mas a entrega é confiável e ordenada e funciona pela internet
inteira, atravessando NAT e proxy.

No multicast o remetente não conhece ninguém e não existe lista. Ele manda um datagrama para o
endereço do grupo e a rede replica. Quem controla a participação são os próprios clientes, com
`joinGroup()` ou `IP_ADD_MEMBERSHIP`, e quem sabe disso são os roteadores. O tráfego é constante,
mas a entrega é best-effort, pode perder, duplicar ou desordenar, e na prática só funciona dentro
da rede local. Também não existe forma de saber que alguém saiu.

No código a diferença é literal. O servidor WebSocket itera sobre um registro:

```java
for (WebSocket cliente : getConnections()) {   // MuralServidor.java
    cliente.send(avisoFormatado);
}
```
```python
websockets.broadcast(clientes_conectados, aviso_formatado)   # mural_servidor.py
```

Ou seja, o broadcast do mural é unicast repetido na camada de aplicação. Já o
`ServidorMulticast` só faz `socket.send(pacote)` uma vez por aviso.

Isso apareceu nas evidências. O servidor WebSocket contou os clientes, imprimindo
`Novo aluno conectado. Total: 1` e depois `Total: 2`, e registrou o endereço de cada um. O
servidor multicast imprimiu exatamente os mesmos 5 `Enviado:` no teste com dois clientes e no
teste cruzado, porque para ele tanto faz quem está ouvindo, ou se há alguém.

Na prática, o multicast é muito mais eficiente para distribuir a mesma informação para muitos
ouvintes numa rede controlada, mas não funciona pela internet e não garante entrega. O WebSocket
gasta N vezes mais banda, mas entrega de forma confiável, atravessa qualquer rede, sabe quem está
conectado e permite conversa nos dois sentidos: no mural qualquer aluno publica e todos veem,
enquanto no multicast a comunicação é de mão única, do professor para a turma.

### D3. Por que o WebSocket é mais adequado que TCP cru para o mural em tempo real, se os dois são conexões TCP contínuas?

No nível do transporte são a mesma coisa. A diferença é tudo aquilo que o WebSocket já resolve e
que em TCP puro teríamos que construir à mão.

Fronteira de mensagem. O TCP entrega um fluxo de bytes, sem separar mensagens. Na Parte A nós
inventamos uma convenção para contornar isso, o `println()` com `readLine()` usando o `\n` como
separador. Funciona, mas quebra se um aviso contiver uma quebra de linha, e não serve para dados
binários, como uma imagem no mural. O WebSocket resolve isso no protocolo: cada `send()` é uma
mensagem, e o `onMessage` recebe exatamente aquela mensagem inteira, com tipo, inclusive
remontando as que foram fragmentadas em vários frames.

Recepção assíncrona, que é o ponto principal para tempo real. O laço do `ClienteTCP` da Parte A é
estritamente alternado:

```java
saida.println(linha);                      // envia
System.out.println(entrada.readLine());    // e fica bloqueado esperando a resposta
```

Enquanto ele está parado no `teclado.readLine()` esperando o usuário digitar, não consegue receber
nada do servidor. Num mural isso não funciona, porque as mensagens dos outros alunos chegam a
qualquer momento, sem relação com o que este usuário está fazendo. Com TCP puro seria preciso
criar uma thread só para leitura e sincronizar as duas. A API de WebSocket já é orientada a
eventos, o `onText` do `MuralCliente` e a task `escutar()` do `mural_cliente.py` rodam em paralelo
com a leitura do teclado, e é por isso que no print o cliente 2 exibe o aviso do cliente 1 sem ter
feito nada.

Ciclo de vida e vários clientes. O `ServidorTCP` da Parte A atende um cliente e morre, como
descrito na A3. A biblioteca de WebSocket já entrega `onOpen`, `onClose`, `onError`, a coleção
`getConnections()` com todos os conectados e ping e pong automáticos para detectar quem caiu sem
avisar. Em TCP cru isso seria um pool de threads e uma estrutura de dados concorrente escritos
por nós.

Alcance e compatibilidade com a web. Um servidor TCP cru na porta 5000 costuma ser barrado por
firewall e proxy corporativo, e nenhum navegador consegue abrir um socket TCP arbitrário. Como o
WebSocket nasce de um handshake HTTP nas portas 80 e 443, ele atravessa essa infraestrutura,
aceita TLS com `wss://` e é suportado direto pelo navegador com `new WebSocket("ws://...")`. Para
um mural que os alunos abririam no navegador, TCP cru nem seria uma opção.

Resumindo, o WebSocket não substitui o TCP, ele padroniza uma camada em cima dele com
enquadramento de mensagens, comunicação nos dois sentidos orientada a eventos, controle de
conexão e compatibilidade com a web. Usar TCP puro aqui significaria reimplementar tudo isso com
menos qualidade, que é o mesmo tipo de troca que apareceu na B3.
