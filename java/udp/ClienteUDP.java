import java.net.*;
import java.util.Scanner;

public class ClienteUDP {

    // OFFSET pessoal (ver secao 3.3 do roteiro) - use o MESMO valor do servidor.
    static final int OFFSET = 0;

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int porta = 5001 + OFFSET;

        try (DatagramSocket socket = new DatagramSocket()) {
            InetAddress endereco = InetAddress.getByName(host);
            Scanner teclado = new Scanner(System.in);
            byte[] buffer = new byte[1024];

            System.out.println("[UDP] Pronto para enviar. Digite 'sair' para encerrar.");
            while (true) {
                System.out.print("> ");
                String mensagem = teclado.nextLine();
                byte[] dados = mensagem.getBytes();

                DatagramPacket pacote = new DatagramPacket(dados, dados.length, endereco, porta);
                socket.send(pacote);
                if (mensagem.equalsIgnoreCase("sair")) break;

                DatagramPacket resposta = new DatagramPacket(buffer, buffer.length);
                socket.receive(resposta);
                System.out.println(new String(resposta.getData(), 0, resposta.getLength()));
            }
        }
    }
}
