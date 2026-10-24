import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;

public class SistemaApp {
    private static final int PORTA = 5050;

    public static void main(String[] args) {
        SistemaAquaMap sistema = new SistemaAquaMap();

        System.out.println("=== AquaMap: sistema receptor ===");
        System.out.println("Coletas e agendamentos ficam salvos na pasta data.");
        System.out.println("Agendamentos salvos:");
        sistema.mostrarAgendamentos();
        System.out.println("Aguardando o sensor na porta " + PORTA + "...\n");

        try (ServerSocket servidor = new ServerSocket(PORTA)) {
            while (true) {
                try (Socket conexao = servidor.accept();
                     BufferedReader entrada = new BufferedReader(new InputStreamReader(
                             conexao.getInputStream(), StandardCharsets.UTF_8))) {
                    ArrayList<String> mensagens = new ArrayList<>();
                    String linha;
                    while ((linha = entrada.readLine()) != null) {
                        mensagens.add(linha);
                    }

                    String resposta = processarMensagens(sistema, mensagens);
                    try (PrintWriter saida = new PrintWriter(
                            conexao.getOutputStream(), true, StandardCharsets.UTF_8)) {
                        saida.println(resposta);
                    }
                } catch (IOException | RuntimeException erroConexao) {
                    System.out.println("[AQUAMAP] Erro ao receber dados: " + erroConexao.getMessage());
                }
            }
        } catch (IOException erroServidor) {
            System.out.println("Nao foi possivel iniciar o servidor: " + erroServidor.getMessage());
        }
    }

    private static String processarMensagens(SistemaAquaMap sistema, ArrayList<String> mensagens) {
        if (mensagens.isEmpty()) {
            return "Mensagem vazia.";
        }

        String[] cabecalho = mensagens.get(0).split(";", 3);
        if ((cabecalho[0].equals("COLETA") || cabecalho[0].equals("COLETA_TESTE"))
                && cabecalho.length == 3) {
            ArrayList<Medicao> medicoes = new ArrayList<>();
            try {
                for (int i = 1; i < mensagens.size(); i++) {
                    medicoes.add(Medicao.deMensagem(mensagens.get(i)));
                }
                boolean avancarMes = cabecalho[0].equals("COLETA_TESTE");
                return sistema.receberColeta(cabecalho[1], cabecalho[2], medicoes, avancarMes);
            } catch (IllegalArgumentException erro) {
                return "Coleta invalida: " + erro.getMessage();
            }
        }

        if (cabecalho[0].equals("AGENDAR") && cabecalho.length == 3 && mensagens.size() == 2) {
            String[] dados = mensagens.get(1).split(";", 2);
            if (dados.length != 2) {
                return "Agendamento invalido.";
            }
            try {
                return sistema.agendarProximaColeta(cabecalho[1], cabecalho[2], LocalDate.parse(dados[0]));
            } catch (RuntimeException erro) {
                return "Data de agendamento invalida.";
            }
        }

        return "Comando desconhecido ou incompleto.";
    }
}
