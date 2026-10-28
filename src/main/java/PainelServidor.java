import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

// Servidor web do painel. Usa o HttpServer que ja vem no Java (sem dependencia nova).
//   http://localhost:8080/          -> pagina do painel (src/main/resources/painel.html)
//   http://localhost:8080/api/dados -> dados em JSON que a pagina usa para desenhar tudo
public class PainelServidor {
    private static final int PORTA = 8080;
    private static final Path ARQUIVO_COMUNIDADES = Path.of("data", "comunidades.csv");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int LIMITE_ALERTAS = 15;

    private final SistemaAquaMap sistema;

    public PainelServidor(SistemaAquaMap sistema) {
        this.sistema = sistema;
    }

    public void iniciar() throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(PORTA), 0);
        servidor.createContext("/api/dados", this::enviarDados);
        servidor.createContext("/", this::enviarPagina);
        servidor.start(); // roda em uma thread propria, junto com o servidor do sensor
        System.out.println("Painel disponivel em http://localhost:" + PORTA);
    }

    private void enviarPagina(HttpExchange troca) throws IOException {
        try (InputStream pagina = PainelServidor.class.getResourceAsStream("/painel.html")) {
            if (pagina == null) {
                responder(troca, 404, "text/plain; charset=utf-8", "painel.html nao encontrado.");
                return;
            }
            responder(troca, 200, "text/html; charset=utf-8",
                    new String(pagina.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private void enviarDados(HttpExchange troca) throws IOException {
        List<RegistroColeta> coletas = sistema.getHistorico();
        String json = "{"
                + "\"comunidades\":" + montarComunidades(coletas) + ","
                + "\"coletas\":" + montarColetas(coletas) + ","
                + "\"ranking\":" + montarRanking(coletas) + ","
                + "\"alertas\":" + montarAlertas(coletas)
                + "}";
        responder(troca, 200, "application/json; charset=utf-8", json);
    }

    // Mapa: um ponto por bairro, colorido pela classificacao da coleta mais recente.
    private String montarComunidades(List<RegistroColeta> coletas) {
        Map<String, RegistroColeta> ultimaPorBairro = new LinkedHashMap<>();
        for (RegistroColeta coleta : coletas) {
            ultimaPorBairro.put(coleta.getBairro().toLowerCase(), coleta);
        }
        Map<String, double[]> coordenadas = carregarCoordenadas();

        Set<String> bairros = new LinkedHashSet<>(coordenadas.keySet());
        bairros.addAll(ultimaPorBairro.keySet());

        List<String> itens = new ArrayList<>();
        for (String bairro : bairros) {
            RegistroColeta ultima = ultimaPorBairro.get(bairro);
            double[] posicao = coordenadas.get(bairro);
            itens.add("{"
                    + "\"bairro\":" + texto(ultima != null ? ultima.getBairro() : bairro) + ","
                    + "\"latitude\":" + (posicao != null ? numero(posicao[0]) : "null") + ","
                    + "\"longitude\":" + (posicao != null ? numero(posicao[1]) : "null") + ","
                    + "\"indice\":" + (ultima != null ? numero(ultima.getQualidade()) : "null") + ","
                    + "\"classificacao\":" + (ultima != null ? texto(ultima.getClassificacao()) : "null") + ","
                    + "\"data\":" + (ultima != null ? texto(ultima.getHorario().format(FORMATO_DATA)) : "null")
                    + "}");
        }
        return "[" + String.join(",", itens) + "]";
    }

    // Graficos e evolucao historica: todas as coletas em ordem de data.
    private String montarColetas(List<RegistroColeta> coletas) {
        List<String> itens = new ArrayList<>();
        for (RegistroColeta coleta : coletas) {
            itens.add("{"
                    + "\"data\":" + texto(coleta.getHorario().format(FORMATO_DATA)) + ","
                    + "\"bairro\":" + texto(coleta.getBairro()) + ","
                    + "\"responsavel\":" + texto(coleta.getPessoa()) + ","
                    + "\"ph\":" + numero(coleta.getPh()) + ","
                    + "\"turbidez\":" + numero(coleta.getTurbidez()) + ","
                    + "\"temperatura\":" + numero(coleta.getTemperatura()) + ","
                    + "\"indice\":" + numero(coleta.getQualidade()) + ","
                    + "\"classificacao\":" + texto(coleta.getClassificacao())
                    + "}");
        }
        return "[" + String.join(",", itens) + "]";
    }

    // Ranking: quantas coletas tiveram cada problema, do mais frequente para o menos.
    private String montarRanking(List<RegistroColeta> coletas) {
        Map<String, Integer> contagem = new LinkedHashMap<>();
        for (RegistroColeta coleta : coletas) {
            for (String problema : RegrasQualidade.problemas(coleta)) {
                contagem.merge(problema, 1, Integer::sum);
            }
        }
        List<Map.Entry<String, Integer>> ordenado = new ArrayList<>(contagem.entrySet());
        ordenado.sort((a, b) -> b.getValue() - a.getValue());

        List<String> itens = new ArrayList<>();
        for (Map.Entry<String, Integer> item : ordenado) {
            itens.add("{\"fator\":" + texto(item.getKey()) + ",\"ocorrencias\":" + item.getValue() + "}");
        }
        return "[" + String.join(",", itens) + "]";
    }

    // Alertas: problemas das coletas mais recentes, cada um com sua recomendacao preventiva.
    private String montarAlertas(List<RegistroColeta> coletas) {
        List<String> itens = new ArrayList<>();
        for (int i = coletas.size() - 1; i >= 0 && itens.size() < LIMITE_ALERTAS; i--) {
            RegistroColeta coleta = coletas.get(i);
            for (String problema : RegrasQualidade.problemas(coleta)) {
                itens.add("{"
                        + "\"data\":" + texto(coleta.getHorario().format(FORMATO_DATA)) + ","
                        + "\"bairro\":" + texto(coleta.getBairro()) + ","
                        + "\"problema\":" + texto(problema) + ","
                        + "\"recomendacao\":" + texto(RegrasQualidade.recomendacao(problema))
                        + "}");
            }
        }
        return "[" + String.join(",", itens) + "]";
    }

    // Le data/comunidades.csv no formato: bairro;latitude;longitude
    private Map<String, double[]> carregarCoordenadas() {
        Map<String, double[]> coordenadas = new LinkedHashMap<>();
        if (!Files.exists(ARQUIVO_COMUNIDADES)) {
            return coordenadas;
        }
        try {
            for (String linha : Files.readAllLines(ARQUIVO_COMUNIDADES, StandardCharsets.UTF_8)) {
                String[] partes = linha.split(";");
                if (partes.length != 3 || partes[0].equalsIgnoreCase("bairro")) continue;
                try {
                    coordenadas.put(partes[0].trim().toLowerCase(), new double[]{
                            Double.parseDouble(partes[1].trim()), Double.parseDouble(partes[2].trim())
                    });
                } catch (NumberFormatException erro) {
                    System.out.println("[PAINEL] Coordenada invalida ignorada: " + linha);
                }
            }
        } catch (IOException erro) {
            System.out.println("[PAINEL] Nao foi possivel ler " + ARQUIVO_COMUNIDADES + ".");
        }
        return coordenadas;
    }

    private String texto(String valor) {
        return "\"" + valor.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String numero(double valor) {
        return String.format(Locale.ROOT, "%.2f", valor);
    }

    private void responder(HttpExchange troca, int status, String tipo, String conteudo) throws IOException {
        byte[] corpo = conteudo.getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", tipo);
        troca.sendResponseHeaders(status, corpo.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(corpo);
        }
    }
}
