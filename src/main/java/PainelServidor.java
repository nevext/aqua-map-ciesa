import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

// Servidor web do painel. Usa o HttpServer que ja vem no Java (sem dependencia nova).
//   http://localhost:8080/          -> pagina do painel (src/main/resources/painel.html)
//   http://localhost:8080/api/dados -> dados em JSON que a pagina usa para desenhar tudo
//   POST /api/coleta                -> aba "Sensor" do painel: gera uma coleta (mesma regra do SensorApp)
//   POST /api/agendar               -> aba "Sensor" do painel: agenda a proxima coleta para daqui a 1 mes
//   POST /api/reiniciar             -> aba "Sensor" do painel: comeca do zero (arquiva as planilhas em data/backup/)
public class PainelServidor {
    private static final int PORTA = 8080;

    public static int porta() {
        return PORTA;
    }
    private static final Path ARQUIVO_COMUNIDADES = Path.of("data", "comunidades.csv");
    private static final Path ARQUIVO_AGENDAMENTOS = Path.of("data", "agendamentos.csv");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int LIMITE_ALERTAS = 15;
    private static final int TAMANHO_MAXIMO_CAMPO = 60;
    private static final Pattern RESPOSTA_COLETA =
            Pattern.compile("Indice AquaMap: ([0-9.,]+)/10 - (\\w+) \\| Anomalia: (.*)");

    private final SistemaAquaMap sistema;
    // Mesmos sensores simulados e mesmas faixas do SensorApp (terminal).
    private final Sensor sensorPH = new Sensor("pH", "pH", 6.0, 9.0);
    private final Sensor sensorTurbidez = new Sensor("Turbidez", "NTU", 0.5, 50.0);
    private final Sensor sensorTemperatura = new Sensor("Temperatura", "C", 20.0, 35.0);

    public PainelServidor(SistemaAquaMap sistema) {
        this.sistema = sistema;
    }

    public void iniciar() throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(PORTA), 0);
        servidor.createContext("/api/dados", this::enviarDados);
        servidor.createContext("/api/coleta", this::receberColetaDoPainel);
        servidor.createContext("/api/agendar", this::agendarDoPainel);
        servidor.createContext("/api/reiniciar", this::reiniciarDoPainel);
        servidor.createContext("/", this::enviarPagina);
        servidor.start(); // roda em uma thread propria, junto com o servidor do sensor
        abrirNoNavegador("http://localhost:" + PORTA);
    }

    private static void abrirNoNavegador(String url) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(url));
            }
        } catch (Exception ignorado) {
            // o link ja aparece no resumo do SistemaApp
        }
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
                + "\"alertas\":" + montarAlertas(coletas) + ","
                + "\"agendamentos\":" + montarAgendamentos()
                + "}";
        responder(troca, 200, "application/json; charset=utf-8", json);
    }

    // Aba "Sensor": o painel pede uma coleta e o proprio sistema gera os valores,
    // como o SensorApp faz no terminal. modo=proximo_mes equivale a opcao "0" do terminal.
    private void receberColetaDoPainel(HttpExchange troca) throws IOException {
        Map<String, String> campos = lerRequisicaoDoPainel(troca);
        if (campos == null) {
            return;
        }
        String pessoa = limparCampo(campos.get("pessoa"));
        String bairro = limparCampo(campos.get("bairro"));
        if (pessoa.isEmpty() || bairro.isEmpty()) {
            responderMensagem(troca, 400, false, "Informe o responsavel e o bairro.");
            return;
        }

        // Antes de coletar: se a planilha ainda nao existe, esta coleta vai criar a pasta e os arquivos.
        String pasta = sistema.pastaDasPlanilhas(pessoa);
        boolean planilhaNova = !Files.exists(Path.of(pasta, "coletas.xlsx"));

        ArrayList<Medicao> coleta = new ArrayList<>();
        coleta.add(sensorPH.coletar());
        coleta.add(sensorTurbidez.coletar());
        coleta.add(sensorTemperatura.coletar());

        boolean avancarMes = "proximo_mes".equals(campos.get("modo"));
        String mensagem = sistema.receberColeta(pessoa, bairro, coleta, avancarMes);

        Matcher resposta = RESPOSTA_COLETA.matcher(mensagem);
        if (!resposta.matches()) {
            responderMensagem(troca, 200, false, mensagem);
            return;
        }
        // Confere se o CSV e o XLSX foram mesmo gravados agora (e nao so se existem de antes).
        boolean planilhas = planilhaAtualizada(Path.of(pasta, "coletas.csv"))
                && planilhaAtualizada(Path.of(pasta, "coletas.xlsx"));
        String json = "{"
                + "\"ok\":true,"
                + "\"pasta\":" + texto(pasta) + ","
                + "\"planilhas\":" + planilhas + ","
                + "\"planilhaNova\":" + planilhaNova + ","
                + "\"mensagem\":" + texto(mensagem) + ","
                + "\"indice\":" + resposta.group(1).replace(',', '.') + ","
                + "\"classificacao\":" + texto(resposta.group(2)) + ","
                + "\"anomalia\":" + texto(resposta.group(3)) + ","
                + "\"ph\":" + String.format(Locale.ROOT, "%.4f", coleta.get(0).getValor()) + ","
                + "\"turbidez\":" + String.format(Locale.ROOT, "%.4f", coleta.get(1).getValor()) + ","
                + "\"temperatura\":" + String.format(Locale.ROOT, "%.4f", coleta.get(2).getValor())
                + "}";
        responder(troca, 200, "application/json; charset=utf-8", json);
    }

    private boolean planilhaAtualizada(Path arquivo) {
        try {
            return Files.exists(arquivo)
                    && System.currentTimeMillis() - Files.getLastModifiedTime(arquivo).toMillis() < 30_000;
        } catch (IOException erro) {
            return false;
        }
    }

    // Aba "Sensor": equivale a responder "S" a "Deseja agendar a proxima coleta para daqui a 1 mes?".
    private void agendarDoPainel(HttpExchange troca) throws IOException {
        Map<String, String> campos = lerRequisicaoDoPainel(troca);
        if (campos == null) {
            return;
        }
        String pessoa = limparCampo(campos.get("pessoa"));
        String bairro = limparCampo(campos.get("bairro"));
        if (pessoa.isEmpty() || bairro.isEmpty()) {
            responderMensagem(troca, 400, false, "Informe o responsavel e o bairro.");
            return;
        }
        String mensagem = sistema.agendarProximaColeta(pessoa, bairro, LocalDate.now().plusMonths(1));
        responderMensagem(troca, 200, mensagem.startsWith("Proxima coleta"), mensagem);
    }

    // Aba "Sensor": "Comecar do zero". Arquiva as planilhas em data/backup/ e esvazia o historico.
    private void reiniciarDoPainel(HttpExchange troca) throws IOException {
        Map<String, String> campos = lerRequisicaoDoPainel(troca);
        if (campos == null) {
            return;
        }
        if (!"sim".equals(campos.get("confirmar"))) {
            responderMensagem(troca, 400, false, "Confirmacao ausente.");
            return;
        }
        String mensagem = sistema.reiniciarDados();
        responderMensagem(troca, 200, !mensagem.startsWith("Nao foi possivel"), mensagem);
    }

    // So aceita POST vindo da propria pagina (cabecalho X-AquaMap). Outro site aberto no navegador
    // nao consegue enviar esse cabecalho sem permissao, entao nao consegue gravar coletas aqui.
    // Devolve null (ja respondendo o erro) quando a requisicao nao e valida.
    private Map<String, String> lerRequisicaoDoPainel(HttpExchange troca) throws IOException {
        if (!"POST".equals(troca.getRequestMethod())
                || !"1".equals(troca.getRequestHeaders().getFirst("X-AquaMap"))) {
            responderMensagem(troca, 403, false, "Requisicao nao permitida.");
            return null;
        }
        try {
            String corpo = new String(troca.getRequestBody().readNBytes(4096), StandardCharsets.UTF_8);
            Map<String, String> campos = new LinkedHashMap<>();
            for (String par : corpo.split("&")) {
                int separador = par.indexOf('=');
                if (separador <= 0) continue;
                campos.put(URLDecoder.decode(par.substring(0, separador), StandardCharsets.UTF_8),
                        URLDecoder.decode(par.substring(separador + 1), StandardCharsets.UTF_8));
            }
            return campos;
        } catch (RuntimeException erro) {
            responderMensagem(troca, 400, false, "Dados invalidos.");
            return null;
        }
    }

    // Mesma limpeza do SensorApp (sem ';' nem quebra de linha, que quebrariam o CSV), com limite de tamanho.
    private String limparCampo(String texto) {
        if (texto == null) {
            return "";
        }
        String limpo = texto.replace(";", ",").replaceAll("[\\r\\n\\t]+", " ").trim();
        return limpo.length() > TAMANHO_MAXIMO_CAMPO ? limpo.substring(0, TAMANHO_MAXIMO_CAMPO).trim() : limpo;
    }

    private void responderMensagem(HttpExchange troca, int status, boolean ok, String mensagem) throws IOException {
        responder(troca, status, "application/json; charset=utf-8",
                "{\"ok\":" + ok + ",\"mensagem\":" + texto(mensagem) + "}");
    }

    // Agendamentos: a data mais recente de cada responsavel + bairro (o arquivo pode ter linhas repetidas).
    private String montarAgendamentos() {
        Map<String, String[]> ultimos = new LinkedHashMap<>();
        if (Files.exists(ARQUIVO_AGENDAMENTOS)) {
            try {
                for (String linha : Files.readAllLines(ARQUIVO_AGENDAMENTOS, StandardCharsets.UTF_8)) {
                    String[] partes = linha.split(";", 3);
                    if (partes.length != 3) continue;
                    try {
                        LocalDate data = LocalDate.parse(partes[2].trim());
                        String chave = partes[0].trim().toLowerCase() + "|" + partes[1].trim().toLowerCase();
                        String[] atual = ultimos.get(chave);
                        if (atual == null || data.isAfter(LocalDate.parse(atual[2]))) {
                            ultimos.put(chave, new String[]{partes[0].trim(), partes[1].trim(), data.toString()});
                        }
                    } catch (RuntimeException linhaInvalida) {
                        // linha com data invalida: ignora
                    }
                }
            } catch (IOException erro) {
                System.out.println("[PAINEL] Nao foi possivel ler " + ARQUIVO_AGENDAMENTOS + ".");
            }
        }
        List<String> itens = new ArrayList<>();
        for (String[] item : ultimos.values()) {
            itens.add("{\"pessoa\":" + texto(item[0]) + ",\"bairro\":" + texto(item[1])
                    + ",\"data\":" + texto(item[2]) + "}");
        }
        return "[" + String.join(",", itens) + "]";
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
                    + "\"classificacao\":" + texto(coleta.getClassificacao()) + ","
                    + "\"problemas\":" + listaDeTextos(RegrasQualidade.problemas(coleta))
                    + "}");
        }
        return "[" + String.join(",", itens) + "]";
    }

    // ["a","b"]: usado para o painel montar o ranking so com as coletas do filtro escolhido.
    private String listaDeTextos(List<String> valores) {
        List<String> itens = new ArrayList<>();
        for (String valor : valores) {
            itens.add(texto(valor));
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
