import java.net.*;

public class ServidorUDP {

    // OFFSET pessoal (ver secao 3.3 do roteiro). Execucao individual -> 0.
    static final int OFFSET = 0;

    public static void main(String[] args) throws Exception {
        int porta = 5001 + OFFSET;
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
