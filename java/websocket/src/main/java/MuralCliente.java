import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Scanner;
import java.util.concurrent.CompletionStage;

public class MuralCliente {

    // OFFSET pessoal (ver secao 3.3 do roteiro) - use o MESMO valor do servidor.
    static final int OFFSET = 0;
    static final int PORTA = 8887 + OFFSET;

    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                System.out.println("\n" + data);
                System.out.print("> ");
                webSocket.request(1);
                return null;
            }
        };

        WebSocket socket = client.newWebSocketBuilder()
                .buildAsync(URI.create("ws://localhost:" + PORTA), listener)
                .join();

        Scanner teclado = new Scanner(System.in);
        System.out.println("[WebSocket] Conectado ao mural. Digite 'sair' para encerrar.");
        while (true) {
            System.out.print("> ");
            String mensagem = teclado.nextLine();
            if (mensagem.equalsIgnoreCase("sair")) {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "Ate mais!").join();
                break;
            }
            socket.sendText(mensagem, true).join();
        }
    }
}
