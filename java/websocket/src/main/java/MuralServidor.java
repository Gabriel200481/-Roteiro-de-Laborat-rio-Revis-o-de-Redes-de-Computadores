import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class MuralServidor extends WebSocketServer {

    // OFFSET pessoal (ver secao 3.3 do roteiro). Execucao individual -> 0.
    static final int OFFSET = 0;
    static final int PORTA = 8887 + OFFSET;

    public MuralServidor(int porta) {
        super(new InetSocketAddress(porta));
    }

    @Override
    public void onOpen(WebSocket conexao, ClientHandshake handshake) {
        System.out.println("[WebSocket] Novo aluno conectado: " + conexao.getRemoteSocketAddress());
        conexao.send("Bem-vindo(a) ao mural de avisos da turma!");
    }

    @Override
    public void onMessage(WebSocket conexao, String mensagem) {
        System.out.println("[WebSocket] Recebido: " + mensagem);
        String avisoFormatado = "Aviso da turma: " + mensagem;
        for (WebSocket cliente : getConnections()) {
            cliente.send(avisoFormatado);
        }
    }

    @Override
    public void onClose(WebSocket conexao, int codigo, String motivo, boolean remoto) {
        System.out.println("[WebSocket] Aluno desconectado: " + conexao.getRemoteSocketAddress());
    }

    @Override
    public void onError(WebSocket conexao, Exception ex) {
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("[WebSocket] Servidor do mural iniciado na porta " + PORTA + ".");
    }

    public static void main(String[] args) {
        MuralServidor servidor = new MuralServidor(PORTA);
        servidor.start();
    }
}
