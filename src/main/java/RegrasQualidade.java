import java.util.ArrayList;
import java.util.List;

// Limites fixos de cada indicador e a recomendacao preventiva para cada problema.
// Os limites de pH e temperatura sao os mesmos usados no calculo do Indice AquaMap.
public class RegrasQualidade {
    public static final double PH_MINIMO = 6.5;
    public static final double PH_MAXIMO = 8.5;
    public static final double TURBIDEZ_MAXIMA = 5.0; // NTU, limite da Portaria GM/MS 888/2021
    public static final double TEMPERATURA_MAXIMA = 30.0;

    public static final String PH_ACIDO = "pH acido";
    public static final String PH_ALCALINO = "pH alcalino";
    public static final String TURBIDEZ_ALTA = "Turbidez alta";
    public static final String TEMPERATURA_ALTA = "Temperatura alta";
    public static final String VARIACAO_BRUSCA = "Variacao brusca";

    // Lista os problemas encontrados em uma coleta (lista vazia = tudo dentro do limite).
    public static List<String> problemas(RegistroColeta coleta) {
        List<String> problemas = new ArrayList<>();
        if (coleta.getPh() < PH_MINIMO) problemas.add(PH_ACIDO);
        if (coleta.getPh() > PH_MAXIMO) problemas.add(PH_ALCALINO);
        if (coleta.getTurbidez() > TURBIDEZ_MAXIMA) problemas.add(TURBIDEZ_ALTA);
        if (coleta.getTemperatura() > TEMPERATURA_MAXIMA) problemas.add(TEMPERATURA_ALTA);
        if (coleta.getAnomalia().startsWith("SIM")) problemas.add(VARIACAO_BRUSCA);
        return problemas;
    }

    public static String recomendacao(String problema) {
        switch (problema) {
            case PH_ACIDO:
                return "Evite consumir sem tratamento. Verifique esgoto ou materia organica em decomposicao perto do ponto e repita a coleta.";
            case PH_ALCALINO:
                return "Verifique a entrada de produtos de limpeza, cal ou residuos de obra perto do ponto e repita a coleta.";
            case TURBIDEZ_ALTA:
                return "Filtre e ferva a agua antes de consumir. Observe se houve chuva forte, erosao ou obras perto do ponto.";
            case TEMPERATURA_ALTA:
                return "Mantenha caixas d'agua tampadas e na sombra: agua quente favorece a proliferacao de microrganismos.";
            case VARIACAO_BRUSCA:
                return "Mudanca grande desde a ultima coleta: repita a medicao e avise a escola ou a unidade de saude local.";
            default:
                return "Repita a coleta para confirmar o resultado.";
        }
    }
}
