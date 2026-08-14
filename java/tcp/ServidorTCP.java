import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ServidorTCP {

    // OFFSET pessoal (ver secao 3.3 do roteiro). Execucao individual -> 0.
    static final int OFFSET = 0;

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static void main(String[] args) throws IOException {
        int porta = 5000 + OFFSET;
        try (ServerSocket servidor = new ServerSocket(porta)) {
            System.out.println("[TCP] Servidor aguardando conexoes na porta " + porta + "...");
            try (Socket cliente = servidor.accept();
                 BufferedReader entrada = new BufferedReader(
                         new InputStreamReader(cliente.getInputStream()));
                 PrintWriter saida = new PrintWriter(cliente.getOutputStream(), true)) {

                System.out.println("[TCP] Cliente conectado: " + cliente.getRemoteSocketAddress());
                String mensagem;
                while ((mensagem = entrada.readLine()) != null) {
                    System.out.println("[TCP] Recebido: " + mensagem);

                    if (mensagem.equalsIgnoreCase("sair")) {
                        saida.println("Encerrando conexao. Ate mais!");
                        break;
                    }

                    // Tarefa 4.5.3: responder com o horario atual do servidor.
                    if (mensagem.equalsIgnoreCase("hora")) {
                        String agora = LocalDateTime.now().format(FORMATO);
                        saida.println("Monitor responde: agora sao " + agora
                                + " (horario do servidor)");
                        continue;
                    }

                    saida.println("Monitor responde: recebi sua mensagem -> \"" + mensagem + "\"");
                }
            }
        }
        System.out.println("[TCP] Servidor encerrado.");
    }
}
