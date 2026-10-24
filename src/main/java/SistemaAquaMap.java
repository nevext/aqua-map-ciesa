import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Stream;

public class SistemaAquaMap {
    private static final Path DIRETORIO_DADOS = Path.of("data");
    private static final Path ARQUIVO_COLETAS_ANTIGO = DIRETORIO_DADOS.resolve("coletas.csv");
    private static final Path ARQUIVO_AGENDAMENTOS = Path.of("data", "agendamentos.csv");
    private final ArrayList<RegistroColeta> historico = new ArrayList<>();
    private final ArrayList<double[]> matrizHistorico = new ArrayList<>();

    public SistemaAquaMap() {
        carregarHistorico();
    }

    public String receberColeta(String pessoa, String bairro, ArrayList<Medicao> medicoes,
                                boolean avancarMes) {
        LocalDate proximaData = dataDaProximaColeta(pessoa, bairro);
        if (!avancarMes && proximaData != null && proximaData.isAfter(LocalDate.now())) {
            return "Coleta ainda nao liberada. A proxima esta agendada para " + proximaData + ".";
        }

        double ph = Double.NaN;
        double turbidez = Double.NaN;
        double temperatura = Double.NaN;

        for (Medicao medicao : medicoes) {
            if (medicao.getIndicador().equalsIgnoreCase("pH")) {
                ph = medicao.getValor();
            } else if (medicao.getIndicador().equalsIgnoreCase("Turbidez")) {
                turbidez = medicao.getValor();
            } else if (medicao.getIndicador().equalsIgnoreCase("Temperatura")) {
                temperatura = medicao.getValor();
            }
        }

        if (Double.isNaN(ph) || Double.isNaN(turbidez) || Double.isNaN(temperatura)) {
            return "Coleta incompleta: envie pH, turbidez e temperatura.";
        }

        RegistroColeta anterior = buscarUltimaColeta(pessoa, bairro);
        LocalDateTime horario = LocalDateTime.now();
        if (avancarMes && anterior != null) {
            horario = anterior.getHorario().plusMonths(1);
        }
        double[] pontuacoes = {
                pontuarPH(ph),
                pontuarTurbidez(turbidez),
                pontuarTemperatura(temperatura)
        };
        double[] pesos = {1.0 / 3.0, 1.0 / 3.0, 1.0 / 3.0};
        double qualidade = produtoEscalar(pontuacoes, pesos);
        String classificacao = classificar(qualidade);

        String[] analise = analisarVariacao(anterior, ph, turbidez, temperatura);
        RegistroColeta atual = new RegistroColeta(pessoa, bairro, horario,
                ph, turbidez, temperatura, qualidade, classificacao,
                analise[0], analise[1], analise[2]);
        historico.add(atual);
        matrizHistorico.add(new double[]{ph, turbidez, temperatura});
        salvarColeta(atual);

        String dataHora = horario.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        System.out.printf("[AQUAMAP] %s | %s, %s | %.2f/10 %s | Anomalia: %s%n",
                dataHora, pessoa, bairro, qualidade, classificacao, atual.getAnomalia());

        return String.format("Indice AquaMap: %.2f/10 - %s | Anomalia: %s",
                qualidade, classificacao, atual.getAnomalia());
    }

    public String agendarProximaColeta(String pessoa, String bairro, LocalDate data) {
        String linha = limpar(pessoa) + ";" + limpar(bairro) + ";" + data;
        try {
            Files.createDirectories(ARQUIVO_AGENDAMENTOS.getParent());
            Files.writeString(ARQUIVO_AGENDAMENTOS, linha + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
            return "Proxima coleta de " + bairro + " agendada para " + data + ".";
        } catch (IOException erro) {
            return "Nao foi possivel salvar o agendamento: " + erro.getMessage();
        }
    }

    public void mostrarAgendamentos() {
        if (!Files.exists(ARQUIVO_AGENDAMENTOS)) {
            return;
        }
        try {
            for (String linha : Files.readAllLines(ARQUIVO_AGENDAMENTOS, StandardCharsets.UTF_8)) {
                String[] partes = linha.split(";", 3);
                if (partes.length == 3 && !partes[2].isBlank()) {
                    LocalDate data = LocalDate.parse(partes[2]);
                    String situacao = data.isAfter(LocalDate.now()) ? "agendada" : "pendente";
                    System.out.println("  " + partes[1] + " - " + partes[0] + ": " + data + " (" + situacao + ")");
                }
            }
        } catch (IOException | RuntimeException erro) {
            System.out.println("[AQUAMAP] Nao foi possivel ler os agendamentos salvos.");
        }
    }

    private LocalDate dataDaProximaColeta(String pessoa, String bairro) {
        if (!Files.exists(ARQUIVO_AGENDAMENTOS)) {
            return null;
        }
        LocalDate maisRecente = null;
        try {
            for (String linha : Files.readAllLines(ARQUIVO_AGENDAMENTOS, StandardCharsets.UTF_8)) {
                String[] partes = linha.split(";", 3);
                if (partes.length == 3 && partes[0].equalsIgnoreCase(pessoa)
                        && partes[1].equalsIgnoreCase(bairro)) {
                    LocalDate data = LocalDate.parse(partes[2]);
                    if (maisRecente == null || data.isAfter(maisRecente)) {
                        maisRecente = data;
                    }
                }
            }
        } catch (IOException | RuntimeException erro) {
            System.out.println("[AQUAMAP] Nao foi possivel consultar a data agendada.");
        }
        return maisRecente;
    }

    private double produtoEscalar(double[] pontuacoes, double[] pesos) {
        double resultado = 0.0;
        for (int i = 0; i < pontuacoes.length; i++) {
            resultado += pontuacoes[i] * pesos[i];
        }
        return resultado;
    }

    private double pontuarPH(double ph) {
        double distancia = 0.0;
        if (ph < 6.5) {
            distancia = 6.5 - ph;
        } else if (ph > 8.5) {
            distancia = ph - 8.5;
        }
        return limitar(10.0 - distancia * 2.0);
    }

    private double pontuarTurbidez(double turbidez) {
        return limitar(10.0 - turbidez * 0.2);
    }

    private double pontuarTemperatura(double temperatura) {
        double distancia = 0.0;
        if (temperatura < 20.0) {
            distancia = 20.0 - temperatura;
        } else if (temperatura > 30.0) {
            distancia = temperatura - 30.0;
        }
        return limitar(10.0 - distancia * 0.5);
    }

    private double limitar(double pontuacao) {
        return Math.max(0.0, Math.min(10.0, pontuacao));
    }

    private String classificar(double qualidade) {
        if (qualidade >= 8.0) {
            return "BOA";
        }
        if (qualidade >= 5.0) {
            return "REGULAR";
        }
        return "RUIM";
    }

    private RegistroColeta buscarUltimaColeta(String pessoa, String bairro) {
        for (int i = historico.size() - 1; i >= 0; i--) {
            RegistroColeta coleta = historico.get(i);
            if (coleta.getPessoa().equalsIgnoreCase(pessoa)
                    && coleta.getBairro().equalsIgnoreCase(bairro)) {
                return coleta;
            }
        }
        return null;
    }

    private String[] analisarVariacao(RegistroColeta anterior,
                                      double ph, double turbidez, double temperatura) {
        if (anterior == null) {
            return new String[]{"Primeira coleta", "NAO AVALIADA", "Ainda nao ha coleta anterior para comparar."};
        }

        double[] vetorAtual = {ph, turbidez, temperatura};
        double[] vetorAnterior = {anterior.getPh(), anterior.getTurbidez(), anterior.getTemperatura()};
        double[] variacao = new double[vetorAtual.length];
        for (int i = 0; i < vetorAtual.length; i++) {
            variacao[i] = vetorAtual[i] - vetorAnterior[i];
        }
        double deltaPH = variacao[0];
        double deltaTurbidez = variacao[1];
        double deltaTemperatura = variacao[2];
        String resumoVariacao = String.format("desde %s: pH %+.2f, turbidez %+.2f NTU, temperatura %+.2f C",
                anterior.getHorario().format(DateTimeFormatter.ofPattern("MM/yyyy")),
                deltaPH, deltaTurbidez, deltaTemperatura);

        ArrayList<String> anomalias = new ArrayList<>();
        if (Math.abs(deltaPH) >= 0.5) anomalias.add("pH");
        if (Math.abs(deltaTurbidez) >= 10.0) anomalias.add("turbidez");
        if (Math.abs(deltaTemperatura) >= 3.0) anomalias.add("temperatura");

        String resumoAnomalia = anomalias.isEmpty() ? "NAO" : "SIM: " + String.join(", ", anomalias);
        String interpretacao = interpretarVariacao(deltaPH, deltaTurbidez, deltaTemperatura, anomalias);
        return new String[]{resumoVariacao, resumoAnomalia, interpretacao};
    }

    private String interpretarVariacao(double deltaPH, double deltaTurbidez,
                                       double deltaTemperatura, ArrayList<String> anomalias) {
        if (anomalias.isEmpty()) {
            return "Variacao abaixo dos limites didaticos; isso nao comprova ausencia de contaminantes.";
        }

        ArrayList<String> interpretacoes = new ArrayList<>();
        if (anomalias.contains("pH")) {
            String direcao = deltaPH > 0 ? "aumento" : "queda";
            interpretacoes.add("O " + direcao + " do pH pode refletir processos naturais ou entrada de substancias; o pH sozinho nao identifica a causa.");
        }
        if (anomalias.contains("turbidez")) {
            if (deltaTurbidez > 0) {
                interpretacoes.add("O aumento pode estar ligado a particulas suspensas, escoamento apos chuva, erosao ou outras fontes; turbidez isolada nao prova contaminacao.");
            } else {
                interpretacoes.add("A queda pode indicar menos particulas suspensas nesta amostra; observe se a tendencia continua.");
            }
        }
        if (anomalias.contains("temperatura")) {
            String direcao = deltaTemperatura > 0 ? "A subida" : "A queda";
            interpretacoes.add(direcao + " da temperatura pode acompanhar clima ou estacao, radiacao solar ou entrada de agua subterranea.");
        }
        return String.join(" ", interpretacoes);
    }

    private void carregarHistorico() {
        carregarArquivoColetas(ARQUIVO_COLETAS_ANTIGO);
        try {
            if (!Files.exists(DIRETORIO_DADOS)) return;
            try (Stream<Path> arquivos = Files.walk(DIRETORIO_DADOS)) {
                arquivos.filter(Files::isRegularFile)
                        .filter(caminho -> caminho.getFileName().toString().equalsIgnoreCase("coletas.csv"))
                        .filter(caminho -> !caminho.equals(ARQUIVO_COLETAS_ANTIGO))
                        .forEach(this::carregarArquivoColetas);
            }
        } catch (IOException erro) {
            System.out.println("[AQUAMAP] Nao foi possivel carregar o historico: " + erro.getMessage());
        }
    }

    private void carregarArquivoColetas(Path arquivo) {
        if (!Files.exists(arquivo)) return;
        try {
            for (String linha : Files.readAllLines(arquivo, StandardCharsets.UTF_8)) {
                if (linha.startsWith("data_hora;")) continue;
                try {
                    RegistroColeta coleta = RegistroColeta.deCSV(linha);
                    historico.add(coleta);
                    matrizHistorico.add(new double[]{
                            coleta.getPh(), coleta.getTurbidez(), coleta.getTemperatura()
                    });
                } catch (RuntimeException erroRegistro) {
                    System.out.println("[AQUAMAP] Um registro invalido do historico foi ignorado.");
                }
            }
        } catch (IOException erro) {
            System.out.println("[AQUAMAP] Nao foi possivel ler " + arquivo + ".");
        }
    }

    private void salvarColeta(RegistroColeta coleta) {
        try {
            Path pastaPessoa = DIRETORIO_DADOS.resolve(nomeSeguro(coleta.getPessoa()));
            Path arquivoCSV = pastaPessoa.resolve("coletas.csv");
            Path arquivoXlsx = pastaPessoa.resolve("coletas.xlsx");
            Files.createDirectories(pastaPessoa);
            ArrayList<RegistroColeta> coletasDaPessoa = new ArrayList<>();
            for (RegistroColeta registro : historico) {
                if (registro.getPessoa().equalsIgnoreCase(coleta.getPessoa())) {
                    coletasDaPessoa.add(registro);
                }
            }
            coletasDaPessoa.sort(Comparator.comparing(RegistroColeta::getHorario));
            StringBuilder conteudo = new StringBuilder(RegistroColeta.CABECALHO_CSV)
                    .append(System.lineSeparator());
            for (RegistroColeta registro : coletasDaPessoa) {
                conteudo.append(registro.paraCSV()).append(System.lineSeparator());
            }
            Files.writeString(arquivoCSV, conteudo,
                    StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.WRITE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
            new ExportadorXlsx().exportar(arquivoXlsx, coletasDaPessoa);
        } catch (IOException erro) {
            System.out.println("[AQUAMAP] Nao foi possivel atualizar os arquivos da coleta: " + erro.getMessage());
        }
    }

    private String nomeSeguro(String pessoa) {
        String seguro = pessoa.trim().replaceAll("[<>:\"/\\\\|?*]", "_").replace("..", "_");
        return seguro.isBlank() ? "sem_nome" : seguro;
    }

    private String limpar(String texto) {
        return texto.replace(";", ",").replace("\n", " ").trim();
    }
}
