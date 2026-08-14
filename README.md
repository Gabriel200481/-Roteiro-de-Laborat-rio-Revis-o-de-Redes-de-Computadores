# Central de Avisos da Turma — Lab de Redes

Roteiro de Laboratório — Revisão de Redes de Computadores
**Disciplina:** Laboratório de Desenvolvimento de Aplicações Móveis e Distribuídas
**Unidade:** U0 — Nivelamento de Redes de Computadores e Sistemas Operacionais
**Aluno:** Gabriel Afonso Infante Vieira (individual)

O mesmo cenário — uma central de avisos da turma — implementado com quatro
protocolos diferentes, cada um em Java e em Python.

| Parte | Protocolo | Porta | Cenário |
|---|---|---|---|
| A | TCP | 5000 | Aluno pergunta ao monitor e recebe resposta direta (conversa privada, confiável) |
| B | UDP | 5001 | Mesmo pedido, mas sem garantia de entrega |
| C | Multicast | 4446 (grupo `230.0.0.1`) | Professor avisa todos os alunos conectados de uma vez |
| D | WebSocket | 8887 (Java) / 8888 (Python) | Mural de avisos em tempo real |

> `OFFSET = 0` (seção 3.3 do roteiro) — execução individual, em máquina própria.
> As portas acima já são as portas efetivas usadas no código.

## Estrutura

```
.
├── java/
│   ├── tcp/            ServidorTCP.java, ClienteTCP.java
│   ├── udp/            ServidorUDP.java, ClienteUDP.java
│   ├── multicast/      ServidorMulticast.java, ClienteMulticast.java
│   └── websocket/      pom.xml + src/main/java/{MuralServidor,MuralCliente}.java
├── python/
│   ├── tcp/            servidor_tcp.py, cliente_tcp.py
│   ├── udp/            servidor_udp.py, cliente_udp.py
│   ├── multicast/      servidor_multicast.py, cliente_multicast.py
│   └── websocket/      mural_servidor.py, mural_cliente.py
├── evidencias/         8 prints de execução (um por protocolo/linguagem)
├── RESPOSTAS.md        As 12 perguntas do roteiro respondidas
└── README.md
```

## Como executar

Pré-requisitos: JDK 17+, Maven 3.8+ (só na Parte D), Python 3.10+.
Ambiente usado nos testes: JDK 21.0.11 (Temurin), Maven 3.9.9, Python 3.13.0, Windows 11.

Cada programa é servidor + cliente, então **use dois terminais** (três na Parte C).

```powershell
# A — TCP
cd java/tcp;   javac ServidorTCP.java ClienteTCP.java;  java ServidorTCP  # / java ClienteTCP
cd python/tcp; python servidor_tcp.py                                     # / python cliente_tcp.py

# B — UDP
cd java/udp;   javac ServidorUDP.java ClienteUDP.java;  java ServidorUDP  # / java ClienteUDP
cd python/udp; python servidor_udp.py                                     # / python cliente_udp.py

# C — Multicast (clientes primeiro, depois o servidor)
cd java/multicast;   javac ServidorMulticast.java ClienteMulticast.java
                     java ClienteMulticast   # 2 terminais; depois: java ServidorMulticast
cd python/multicast; python cliente_multicast.py   # 2 terminais; depois: python servidor_multicast.py

# D — WebSocket
cd java/websocket;   mvn -q compile
                     mvn -q exec:java "-Dexec.mainClass=MuralServidor"   # / MuralCliente
cd python/websocket; pip install websockets
                     python mural_servidor.py                            # / python mural_cliente.py
```

Se palavras acentuadas aparecerem trocadas no console, rode `chcp 65001` antes
(ou use o Windows Terminal, que já usa UTF-8 por padrão).

Na primeira execução de cada servidor o Firewall do Windows pede autorização —
clique em **Permitir acesso**, senão as mensagens não chegam.

## Nota de transparência (uso de IA)

Este repositório foi implementado com apoio do Claude (Anthropic), usado para
redação, estruturação e revisão do código e das respostas. Todo o código foi
executado e testado localmente, e as evidências em `evidencias/` são capturas
reais dessas execuções.
