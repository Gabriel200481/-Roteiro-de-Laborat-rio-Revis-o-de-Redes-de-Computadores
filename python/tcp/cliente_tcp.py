import socket

# OFFSET pessoal (ver secao 3.3 do roteiro) - use o MESMO valor do servidor.
OFFSET = 0

HOST = "localhost"
PORTA = 5000 + OFFSET

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as cliente:
    cliente.connect((HOST, PORTA))
    print("[TCP] Conectado ao servidor. Digite 'sair' para encerrar.")
    print("[TCP] Dica: envie 'hora' para receber o horario do servidor.")
    arquivo = cliente.makefile("r")

    while True:
        mensagem = input("> ")
        cliente.sendall((mensagem + "\n").encode("utf-8"))
        print(arquivo.readline().strip())
        if mensagem.lower() == "sair":
            break
