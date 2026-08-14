import socket
from datetime import datetime

# OFFSET pessoal (ver secao 3.3 do roteiro). Execucao individual -> 0.
OFFSET = 0

HOST = "0.0.0.0"
PORTA = 5000 + OFFSET

with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as servidor:
    servidor.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    servidor.bind((HOST, PORTA))
    servidor.listen(1)
    print(f"[TCP] Servidor aguardando conexoes na porta {PORTA}...")

    conexao, endereco = servidor.accept()
    with conexao:
        print(f"[TCP] Cliente conectado: {endereco}")
        while True:
            dados = conexao.recv(1024).decode("utf-8").strip()
            if not dados:
                break
            print(f"[TCP] Recebido: {dados}")

            if dados.lower() == "sair":
                conexao.sendall("Encerrando conexao. Ate mais!\n".encode("utf-8"))
                break

            # Tarefa 4.5.3: responder com o horario atual do servidor.
            if dados.lower() == "hora":
                agora = datetime.now().strftime("%d/%m/%Y %H:%M:%S")
                resposta = f"Monitor responde: agora sao {agora} (horario do servidor)\n"
                conexao.sendall(resposta.encode("utf-8"))
                continue

            resposta = f'Monitor responde: recebi sua mensagem -> "{dados}"\n'
            conexao.sendall(resposta.encode("utf-8"))

print("[TCP] Servidor encerrado.")
