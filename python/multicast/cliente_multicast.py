import socket
import struct

# OFFSET pessoal (ver secao 3.3 do roteiro) - use o MESMO valor do servidor.
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
