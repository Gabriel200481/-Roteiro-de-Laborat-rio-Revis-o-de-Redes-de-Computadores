import socket

# OFFSET pessoal (ver secao 3.3 do roteiro) - use o MESMO valor do servidor.
OFFSET = 0

HOST = "localhost"
PORTA = 5001 + OFFSET

with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as cliente:
    print("[UDP] Pronto para enviar. Digite 'sair' para encerrar.")
    while True:
        mensagem = input("> ")
        cliente.sendto(mensagem.encode("utf-8"), (HOST, PORTA))
        if mensagem.lower() == "sair":
            break
        dados, _ = cliente.recvfrom(1024)
        print(dados.decode("utf-8"))
