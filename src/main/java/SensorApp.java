import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Scanner;

public class SensorApp {
    private static final String SERVIDOR = "localhost";
    private static final int PORTA = 5050;

    public static void main(String[] args) {
        Sensor sensorPH = new Sensor("pH", "pH", 6.0, 9.0);
        Sensor sensorTurbidez = new Sensor("Turbidez", "NTU", 0.5, 50.0);
        Sensor sensorTemperatura = new Sensor("Temperatura", "C", 20.0, 35.0);

        try (Scanner teclado = new Scanner(System.in)) {
            System.out.println("=== AquaMap: cadastro do ponto de coleta ===");
            System.out.print("Nome da pessoa responsavel: ");
            String pessoa = limpar(teclado.nextLine());
            System.out.print("Bairro/comunidade monitorada: ");
            String bairro = limpar(teclado.nextLine());

            System.out.println("\nCadastro: " + pessoa + " - " + bairro);
            System.out.println("Enter = coleta normal | 0 = simular coleta do proximo mes | S = sair.");

            while (true) {
                String opcao = teclado.nextLine();
                if (opcao.equalsIgnoreCase("s")) {
                    break;
                }
                boolean avancarMes = opcao.equals("0");

                ArrayList<Medicao> coleta = new ArrayList<>();
                coleta.add(sensorPH.coletar());
                coleta.add(sensorTurbidez.coletar());
                coleta.add(sensorTemperatura.coletar());

                System.out.println("\n[SENSOR] Enviando uma coleta para " + bairro + "...");

                String comando = avancarMes ? "COLETA_TESTE" : "COLETA";
                String resposta = enviar(comando + ";" + pessoa + ";" + bairro, coleta);
                System.out.println("[SISTEMA] " + resposta);

                if (resposta.startsWith("Indice AquaMap:")) {
                    System.out.print("Deseja agendar a proxima coleta para daqui a 1 mes? (S/N): ");
                    String agendar = teclado.nextLine();
                    if (agendar.equalsIgnoreCase("s")) {
                        LocalDate proximaData = LocalDate.now().plusMonths(1);
                        String retornoAgendamento = enviarAgendamento(
                                "AGENDAR;" + pessoa + ";" + bairro, proximaData);
                        System.out.println("[SISTEMA] " + retornoAgendamento);
                    }
                }

                System.out.println("\nEnter = coleta normal | 0 = proximo mes simulado | S = sair.");
            }
        }

        System.out.println("Sensor encerrado.");
    }

    private static String enviar(String cabecalho, ArrayList<Medicao> dados) {
        try (Socket conexao = new Socket(SERVIDOR, PORTA);
             PrintWriter saida = new PrintWriter(conexao.getOutputStream(), true, StandardCharsets.UTF_8)) {
            saida.println(cabecalho);
            for (Medicao medicao : dados) {
                saida.println(medicao.paraMensagem());
            }
            conexao.shutdownOutput();

            try (BufferedReader resposta = new BufferedReader(new InputStreamReader(
                    conexao.getInputStream(), StandardCharsets.UTF_8))) {
                String linha = resposta.readLine();
                return linha == null ? "O sistema nao retornou uma resposta." : linha;
            }
        } catch (IOException erro) {
            return "Nao foi possivel conectar. Inicie SistemaApp no outro terminal primeiro.";
        }
    }

    private static String enviarAgendamento(String cabecalho, LocalDate data) {
        try (Socket conexao = new Socket(SERVIDOR, PORTA);
             PrintWriter saida = new PrintWriter(conexao.getOutputStream(), true, StandardCharsets.UTF_8)) {
            saida.println(cabecalho);
            saida.println(data + ";proxima-coleta");
            conexao.shutdownOutput();

            try (BufferedReader resposta = new BufferedReader(new InputStreamReader(
                    conexao.getInputStream(), StandardCharsets.UTF_8))) {
                String linha = resposta.readLine();
                return linha == null ? "O sistema nao retornou uma resposta." : linha;
            }
        } catch (IOException erro) {
            return "Nao foi possivel salvar o agendamento. Verifique se o SistemaApp esta rodando.";
        }
    }

    private static String limpar(String texto) {
        return texto.replace(";", ",").replace("\n", " ").trim();
    }
}
