import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExportadorXlsx {
    public void exportar(Path arquivo, List<RegistroColeta> coletas) throws IOException {
        Files.createDirectories(arquivo.getParent());
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet planilha = workbook.createSheet("Coletas");
            CellStyle tituloStyle = criarEstilo(workbook, IndexedColors.DARK_TEAL, true);
            CellStyle cabecalhoStyle = criarEstilo(workbook, IndexedColors.TEAL, true);
            CellStyle anomaliaStyle = workbook.createCellStyle();
            anomaliaStyle.setWrapText(true);
            anomaliaStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
            anomaliaStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle normalStyle = workbook.createCellStyle();
            normalStyle.setWrapText(true);

            Row titulo = planilha.createRow(0);
            titulo.setHeightInPoints(24);
            Cell celulaTitulo = titulo.createCell(0);
            celulaTitulo.setCellValue("AquaMap - Historico de coletas");
            celulaTitulo.setCellStyle(tituloStyle);
            planilha.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));

            Row nota = planilha.createRow(1);
            nota.createCell(0).setCellValue("Interpretacoes sao possibilidades para orientar a investigacao; nao identificam a causa nem confirmam contaminacao.");
            planilha.addMergedRegion(new CellRangeAddress(1, 1, 0, 10));

            Row fontes = planilha.createRow(2);
            fontes.createCell(0).setCellValue("Referencias: USGS pH https://www.usgs.gov/water-science-school/science/ph-and-water | EPA turbidez https://www.epa.gov/sites/default/files/2015-09/documents/2007_04_09_estuaries_monitoruments_manual.pdf | USGS temperatura https://pubs.usgs.gov/wsp/1819g/report.pdf");
            planilha.addMergedRegion(new CellRangeAddress(2, 2, 0, 10));

            String[] titulos = {
                    "Data e hora", "Responsavel", "Bairro", "pH", "Turbidez (NTU)",
                    "Temperatura (C)", "Indice AquaMap (0-10)", "Classificacao",
                    "Variacao desde a coleta anterior", "Anomalia", "Possivel significado da variacao"
            };
            Row cabecalho = planilha.createRow(3);
            cabecalho.setHeightInPoints(32);
            for (int i = 0; i < titulos.length; i++) {
                Cell cell = cabecalho.createCell(i);
                cell.setCellValue(titulos[i]);
                cell.setCellStyle(cabecalhoStyle);
            }

            int numeroLinha = 4;
            for (RegistroColeta coleta : coletas) {
                Row linha = planilha.createRow(numeroLinha++);
                linha.setHeightInPoints(42);
                definirTexto(linha, 0, coleta.getHorario().toString(), normalStyle);
                definirTexto(linha, 1, coleta.getPessoa(), normalStyle);
                definirTexto(linha, 2, coleta.getBairro(), normalStyle);
                definirNumero(linha, 3, coleta.getPh());
                definirNumero(linha, 4, coleta.getTurbidez());
                definirNumero(linha, 5, coleta.getTemperatura());
                definirNumero(linha, 6, coleta.getQualidade());
                definirTexto(linha, 7, coleta.getClassificacao(), normalStyle);
                definirTexto(linha, 8, coleta.getVariacao(), normalStyle);
                definirTexto(linha, 9, coleta.getAnomalia(), coleta.getAnomalia().startsWith("SIM")
                        ? anomaliaStyle : normalStyle);
                definirTexto(linha, 10, coleta.getInterpretacaoPossivel(), normalStyle);
            }

            planilha.createFreezePane(0, 4);
            if (numeroLinha > 4) {
                planilha.setAutoFilter(new CellRangeAddress(3, numeroLinha - 1, 0, titulos.length - 1));
            }
            int[] larguras = {23, 22, 22, 10, 17, 18, 23, 17, 55, 30, 90};
            for (int i = 0; i < larguras.length; i++) {
                planilha.setColumnWidth(i, larguras[i] * 256);
            }

            try (OutputStream saida = Files.newOutputStream(arquivo)) {
                workbook.write(saida);
            }
        }
    }

    private CellStyle criarEstilo(XSSFWorkbook workbook, IndexedColors cor, boolean negrito) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(cor.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setWrapText(true);
        Font font = workbook.createFont();
        font.setBold(negrito);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private void definirTexto(Row linha, int coluna, String texto, CellStyle style) {
        Cell cell = linha.createCell(coluna);
        cell.setCellValue(texto);
        cell.setCellStyle(style);
    }

    private void definirNumero(Row linha, int coluna, double valor) {
        linha.createCell(coluna).setCellValue(valor);
    }
}
