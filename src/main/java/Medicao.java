import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Medicao {
    private final String indicador;
    private final double valor;
    private final String unidade;
    private final LocalDateTime horario;

    public Medicao(String indicador, double valor, String unidade) {
        this.indicador = indicador;
        this.valor = valor;
        this.unidade = unidade;
        this.horario = LocalDateTime.now();
    }

    public String getIndicador() {
        return indicador;
    }

    public double getValor() {
        return valor;
    }

    public String getUnidade() {
        return unidade;
    }

    public LocalDateTime getHorario() {
        return horario;
    }

    public String paraMensagem() {
        return indicador + ";" + valor + ";" + unidade;
    }

    public static Medicao deMensagem(String mensagem) {
        String[] partes = mensagem.split(";", 3);
        if (partes.length != 3) {
            throw new IllegalArgumentException("Formato de medicao invalido: " + mensagem);
        }
        return new Medicao(partes[0], Double.parseDouble(partes[1]), partes[2]);
    }

    @Override
    public String toString() {
        String horaFormatada = horario.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        return String.format("%s: %.2f %s (recebido em %s)",
                indicador, valor, unidade, horaFormatada);
    }
}
