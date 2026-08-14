# Central de Avisos da Turma

Roteiro de laboratório de revisão de redes de computadores.

Disciplina: Laboratório de Desenvolvimento de Aplicações Móveis e Distribuídas
Unidade: U0, nivelamento de redes de computadores e sistemas operacionais
Aluno: Gabriel Afonso Infante Vieira (individual)

O mesmo cenário, uma central de avisos da turma, implementado com quatro protocolos
diferentes, cada um em Java e em Python.

| Parte | Protocolo | Porta | Cenário |
|---|---|---|---|
| A | TCP | 5000 | Aluno pergunta ao monitor e recebe resposta direta |
| B | UDP | 5001 | Mesmo pedido, mas sem garantia de entrega |
| C | Multicast | 4446, grupo 230.0.0.1 | Professor avisa todos os alunos conectados de uma vez |
| D | WebSocket | 8887 (Java) e 8888 (Python) | Mural de avisos em tempo real |

O OFFSET da seção 3.3 do roteiro é 0, porque rodei tudo na minha própria máquina.
As portas acima já são as que estão no código.

## Estrutura

```
java/
  tcp/          ServidorTCP.java, ClienteTCP.java
  udp/          ServidorUDP.java, ClienteUDP.java
  multicast/    ServidorMulticast.java, ClienteMulticast.java
  websocket/    pom.xml + src/main/java/{MuralServidor,MuralCliente}.java
python/
  tcp/          servidor_tcp.py, cliente_tcp.py
  udp/          servidor_udp.py, cliente_udp.py
  multicast/    servidor_multicast.py, cliente_multicast.py
  websocket/    mural_servidor.py, mural_cliente.py
evidencias/     prints de execução, um por protocolo e linguagem
RESPOSTAS.md    as 12 perguntas do roteiro
```

## Como executar

Precisa de JDK 17 ou superior, Maven 3.8+ (só na Parte D) e Python 3.10+.
Testei com JDK 21.0.11, Maven 3.9.9, Python 3.13.0 e websockets 16.0, no Windows 11.

Cada exemplo é servidor mais cliente, então use dois terminais, ou três na Parte C.

```powershell
# A - TCP
cd java/tcp;   javac ServidorTCP.java ClienteTCP.java;  java ServidorTCP   # / java ClienteTCP
cd python/tcp; python servidor_tcp.py                                      # / python cliente_tcp.py

# B - UDP
cd java/udp;   javac ServidorUDP.java ClienteUDP.java;  java ServidorUDP   # / java ClienteUDP
cd python/udp; python servidor_udp.py                                      # / python cliente_udp.py

# C - Multicast (abra os clientes primeiro, depois o servidor)
cd java/multicast;   javac ServidorMulticast.java ClienteMulticast.java
                     java ClienteMulticast     # 2 terminais; depois: java ServidorMulticast
cd python/multicast; python cliente_multicast.py   # 2 terminais; depois: python servidor_multicast.py

# D - WebSocket
cd java/websocket;   mvn -q compile
                     mvn -q exec:java "-Dexec.mainClass=MuralServidor"     # / MuralCliente
cd python/websocket; pip install websockets
                     python mural_servidor.py                              # / python mural_cliente.py
```

Duas coisas que atrapalham na primeira execução:

- O Firewall do Windows abre um aviso na primeira vez que cada servidor sobe. Se você
  cancelar ou não vir a janela, o programa continua rodando mas as mensagens não chegam.
- Se as palavras acentuadas saírem trocadas, rode `chcp 65001` antes, ou use o Windows
  Terminal, que já usa UTF-8.

## Uso de IA

Usei o Claude (Anthropic) como apoio para escrever e revisar o código e as respostas.
Todo o código foi executado e testado localmente, e os prints em `evidencias/` são
capturas dessas execuções.
