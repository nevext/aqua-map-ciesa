import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RegistroColeta {
    public static final String CABECALHO_CSV =
            "data_hora;responsavel;bairro;pH;turbidez_NTU;temperatura_C;indice_0a10;classificacao;variacao;anomalia;interpretacao_possivel";
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final String pessoa;
    private final String bairro;
    private final LocalDateTime horario;
    private final double ph;
    private final double turbidez;
    private final double temperatura;
    private final double qualidade;
    private final String classificacao;
    private final String variacao;
    private final String anomalia;
    private final String interpretacaoPossivel;

    public RegistroColeta(String pessoa, String bairro, LocalDateTime horario,
                          double ph, double turbidez, double temperatura,
                          double qualidade, String classificacao,
                          String variacao, String anomalia, String interpretacaoPossivel) {
        this.pessoa = pessoa;
        this.bairro = bairro;
        this.horario = horario;
        this.ph = ph;
        this.turbidez = turbidez;
        this.temperatura = temperatura;
        this.qualidade = qualidade;
        this.classificacao = classificacao;
        this.variacao = variacao;
        this.anomalia = anomalia;
        this.interpretacaoPossivel = interpretacaoPossivel;
    }

    public String getPessoa() { return pessoa; }
    public String getBairro() { return bairro; }
    public LocalDateTime getHorario() { return horario; }
    public double getPh() { return ph; }
    public double getTurbidez() { return turbidez; }
    public double getTemperatura() { return temperatura; }
    public double getQualidade() { return qualidade; }
    public String getClassificacao() { return classificacao; }
    public String getVariacao() { return variacao; }
    public String getAnomalia() { return anomalia; }
    public String getInterpretacaoPossivel() { return interpretacaoPossivel; }

    public String paraCSV() {
        return String.join(";", horario.format(FORMATO_DATA), limpar(pessoa), limpar(bairro),
                Double.toString(ph), Double.toString(turbidez), Double.toString(temperatura),
                Double.toString(qualidade), limpar(classificacao), limpar(variacao),
                limpar(anomalia), limpar(interpretacaoPossivel));
    }

    public static RegistroColeta deCSV(String linha) {
        String[] campos = linha.split(";", -1);
        if (campos.length == 8) {
            // Compatibilidade com o formato antigo, salvo antes das pastas por pessoa.
            return new RegistroColeta(campos[0], campos[1], LocalDateTime.parse(campos[2], FORMATO_DATA),
                    Double.parseDouble(campos[3]), Double.parseDouble(campos[4]),
                    Double.parseDouble(campos[5]), Double.parseDouble(campos[6]), campos[7],
                    "Historico importado", "Sem dado de anomalia", "Interpretacao nao registrada");
        }
        if (campos.length == 10) {
            // Compatibilidade com o formato anterior que ja tinha variacao e anomalia.
            return new RegistroColeta(campos[1], campos[2], LocalDateTime.parse(campos[0], FORMATO_DATA),
                    Double.parseDouble(campos[3]), Double.parseDouble(campos[4]),
                    Double.parseDouble(campos[5]), Double.parseDouble(campos[6]), campos[7],
                    campos[8], campos[9], "Interpretacao nao registrada");
        }
        if (campos.length != 11) {
            throw new IllegalArgumentException("Registro de coleta invalido");
        }
        return new RegistroColeta(campos[1], campos[2], LocalDateTime.parse(campos[0], FORMATO_DATA),
                Double.parseDouble(campos[3]), Double.parseDouble(campos[4]),
                Double.parseDouble(campos[5]), Double.parseDouble(campos[6]), campos[7],
                campos[8], campos[9], campos[10]);
    }

    private static String limpar(String texto) {
        return texto.replace(";", ",").replace("\n", " ").replace("\r", " ").trim();
    }
}
