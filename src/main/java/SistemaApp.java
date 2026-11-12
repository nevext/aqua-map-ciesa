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
        int portaPainel = PainelServidor.porta();
        String urlPainel = "http://localhost:" + portaPainel;

        boolean painelOk = false;
        try {
            new PainelServidor(sistema).iniciar();
            painelOk = true;
        } catch (IOException ignorado) {
            // mensagem unica em falhaParcialOuTotal
        }

        ServerSocket servidor = null;
        try {
            servidor = new ServerSocket(PORTA);
        } catch (IOException ignorado) {
            // mensagem unica em falhaParcialOuTotal
        }

        if (!painelOk || servidor == null) {
            imprimirFalhaAoSubir(painelOk, servidor != null, urlPainel);
            if (servidor != null) {
                try {
                    servidor.close();
                } catch (IOException ignorado) {
                }
            }
            System.exit(1);
        }

        imprimirRodando(urlPainel);

        try (ServerSocket ativo = servidor) {
            while (true) {
                try (Socket conexao = ativo.accept();
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
                    System.out.println("[coleta] " + erroConexao.getMessage());
                }
            }
        } catch (IOException erroServidor) {
            System.out.println("[erro] " + erroServidor.getMessage());
        }
    }

    private static void imprimirRodando(String urlPainel) {
        System.out.println();
        System.out.println("  AquaMap — sistema ligado");
        System.out.println();
        System.out.println("  Painel  " + urlPainel + "  (deve abrir no navegador)");
        System.out.println("  Sensor  aba \"Sensor\" do painel (ou, no terminal: .\\sensor.cmd)");
        System.out.println();
        System.out.println("  Ctrl+C aqui para parar.");
        System.out.println();
    }

    private static void imprimirFalhaAoSubir(boolean painelOk, boolean receptorOk, String urlPainel) {
        System.out.println();
        System.out.println("  AquaMap — esta copia nao subiu (a outra ja esta rodando)");
        System.out.println();
        if (!painelOk && !receptorOk) {
            System.out.println("  Portas " + PainelServidor.porta() + " e " + PORTA
                    + " ocupadas — normal se voce ja rodou .\\sistema.cmd antes.");
            System.out.println("  O painel em " + urlPainel + " vem desse outro processo.");
        } else if (!painelOk) {
            System.out.println("  Causa: porta do painel (" + PainelServidor.porta() + ") em uso.");
        } else {
            System.out.println("  Causa: porta do sensor (" + PORTA + ") em uso.");
        }
        System.out.println();
        System.out.println("  O que fazer:");
        System.out.println("    1) Ja esta rodando: abra " + urlPainel + " + .\\sensor.cmd");
        System.out.println("    2) Fechar o terminal antigo com sistema (Ctrl+C)");
        System.out.println("    3) Liberar as portas: .\\parar.cmd");
        System.out.println("       manual: netstat -ano | findstr \":8080\"");
        System.out.println("               taskkill /PID <numero> /F");
        System.out.println();
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
