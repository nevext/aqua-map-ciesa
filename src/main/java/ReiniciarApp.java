import java.io.IOException;
import java.nio.file.Path;

// Comecar do zero pelo terminal (usado pelo reiniciar.cmd, com o sistema desligado).
// Move as coletas, planilhas e agendamentos para data/backup/<data e hora>/. Nada e apagado.
public class ReiniciarApp {
    public static void main(String[] args) {
        try {
            Path destino = SistemaAquaMap.arquivarDados();
            if (destino == null) {
                System.out.println("  Nao ha dados para arquivar: ja esta limpo.");
            } else {
                System.out.println("  Dados arquivados em: " + destino);
            }
        } catch (IOException erro) {
            System.out.println("  Nao foi possivel arquivar os dados: " + erro.getMessage());
            System.exit(1);
        }
    }
}
