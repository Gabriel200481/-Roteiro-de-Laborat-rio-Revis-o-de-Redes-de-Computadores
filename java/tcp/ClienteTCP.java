import java.io.*;
import java.net.*;

public class ClienteTCP {

    // OFFSET pessoal (ver secao 3.3 do roteiro) - use o MESMO valor do servidor.
    static final int OFFSET = 0;

    public static void main(String[] args) throws IOException {
        String host = "localhost";
        int porta = 5000 + OFFSET;

        try (Socket socket = new Socket(host, porta);
             PrintWriter saida = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader entrada = new BufferedReader(
                     new InputStreamReader(socket.getInputStream()));
             BufferedReader teclado = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("[TCP] Conectado ao servidor. Digite 'sair' para encerrar.");
            System.out.println("[TCP] Dica: envie 'hora' para receber o horario do servidor.");
            String linha;
            while (true) {
                System.out.print("> ");
                linha = teclado.readLine();
                if (linha == null) break;
                saida.println(linha);
                System.out.println(entrada.readLine());
                if (linha.equalsIgnoreCase("sair")) break;
            }
        }
    }
}
