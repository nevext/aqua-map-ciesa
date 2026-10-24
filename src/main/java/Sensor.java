import java.util.Random;

public class Sensor {
    private final String tipo;
    private final String unidade;
    private final double valorMinimo;
    private final double valorMaximo;
    private final Random gerador = new Random();

    public Sensor(String tipo, String unidade, double valorMinimo, double valorMaximo) {
        this.tipo = tipo;
        this.unidade = unidade;
        this.valorMinimo = valorMinimo;
        this.valorMaximo = valorMaximo;
    }

    public Medicao coletar() {
        double valor = valorMinimo + gerador.nextDouble() * (valorMaximo - valorMinimo);
        return new Medicao(tipo, valor, unidade);
    }
}
