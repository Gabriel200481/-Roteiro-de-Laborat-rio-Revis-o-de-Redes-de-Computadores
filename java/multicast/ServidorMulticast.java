import java.net.*;
import java.io.IOException;

public class ServidorMulticast {

    // OFFSET pessoal (ver secao 3.3 do roteiro). Execucao individual -> 0.
    static final int OFFSET = 0;

    public static void main(String[] args) throws IOException, InterruptedException {
        String grupoMulticast = "230.0.0.1";
        int porta = 4446 + OFFSET;

        InetAddress grupo = InetAddress.getByName(grupoMulticast);
        try (DatagramSocket socket = new DatagramSocket()) {
            int contador = 1;
            System.out.println("[Multicast] Enviando avisos para o grupo " + grupoMulticast + ":" + porta);
            while (contador <= 5) {
                String mensagem = "Aviso #" + contador + ": a aula comeca em " + (5 - contador) + " minuto(s)!";
                byte[] dados = mensagem.getBytes();
                DatagramPacket pacote = new DatagramPacket(dados, dados.length, grupo, porta);
                socket.send(pacote);
                System.out.println("[Multicast] Enviado: " + mensagem);
                contador++;
                Thread.sleep(2000);
            }
        }
    }
}
