# SOBRE O PROJETO

## O AquaMap é um sistema de monitoramento ambiental voltado para escolas e comunidades, com o objetivo de acompanhar e divulgar informações sobre a qualidade da água de forma simples e acessível. O projeto busca auxiliar na conscientização ambiental e na tomada de decisões preventivas, contribuindo para a preservação dos recursos hídricos e para a saúde da população.

# Como funciona

O sistema tem três partes:

```
SensorApp  ──(socket, porta 5050)──►  SistemaApp  ──►  pasta data/ (CSV e XLSX)
                                          │
                                          └──►  Painel web (porta 8080) no navegador
```

1. **SensorApp**: simula o sensor. Gera pH, turbidez e temperatura e envia para o SistemaApp.
2. **SistemaApp / SistemaAquaMap**: recebe a coleta, calcula o Índice AquaMap (0 a 10), compara com a coleta anterior (anomalias) e salva em `data/<responsavel>/coletas.csv` e `coletas.xlsx`.
3. **PainelServidor + painel.html**: o próprio SistemaApp sobe um servidor web (`HttpServer`, que já vem no Java). A página pede os dados em `/api/dados` (JSON) a cada 5 segundos e desenha:
   - **Mapa das comunidades**: um ponto por bairro, verde (BOA), amarelo (REGULAR) ou vermelho (RUIM). As coordenadas ficam em `data/comunidades.csv`.
   - **Gráficos e evolução histórica**: índice e indicadores ao longo das coletas, com filtro por bairro.
   - **Ranking dos fatores de contaminação**: quantas coletas tiveram cada problema.
   - **Alertas em tempo real**: problemas das coletas mais recentes.
   - **Recomendações preventivas**: cada alerta vem com uma orientação para a comunidade.

# Estrutura de pastas

## >.vscode = guarda configurações do editor para esse projeto
## > src/main/java é onde ficam os arquivos-fonte Java
## > src/main/resources guarda a página do painel (painel.html)
## > data guarda as coletas, os agendamentos e as coordenadas das comunidades
## pom.xml é a lista de instruções para o maven (ferramenta que ajuda a montar e organizar projetos java)
### Maven procura o código java em caminhos convencionais, src/main/java é o local padrão para o código principal e src/test/java pra testes, então o maven reconhece os arquivos sem precisarmos configurar um caminho especial
### pom.xml cria as dependências necessárias (Apache POI, para gerar o XLSX), então só roda com as dependências baixadas e instaladas

| Arquivo | Função |
|---|---|
| `Sensor.java` / `Medicao.java` | Sensor simulado e a medição que ele gera |
| `SensorApp.java` | Programa do sensor (envia as coletas) |
| `SistemaApp.java` | Programa receptor (servidor do sensor + inicia o painel) |
| `SistemaAquaMap.java` | Regras: índice, classificação, anomalias, histórico e agendamentos |
| `RegistroColeta.java` | Uma coleta salva (linha do CSV) |
| `ExportadorXlsx.java` | Gera a planilha XLSX |
| `RegrasQualidade.java` | Limites dos indicadores e recomendações preventivas |
| `PainelServidor.java` | Servidor web do painel e montagem do JSON |
| `src/main/resources/painel.html` | Página do painel (mapa com Leaflet, gráficos com Chart.js) |

# Indicadores

| Indicador | Faixa considerada adequada | Problema gerado |
|---|---|---|
| pH | 6,5 a 8,5 | "pH acido" ou "pH alcalino" |
| Turbidez | até 5 NTU (Portaria GM/MS 888/2021) | "Turbidez alta" |
| Temperatura | até 30 °C | "Temperatura alta" |

O **Índice AquaMap** é a média das notas (0 a 10) dos três indicadores: 8 ou mais = BOA, 5 a 8 = REGULAR, abaixo de 5 = RUIM.

# Alertas

Uma coleta gera alerta em dois casos:
- **Fora do limite**: algum indicador fora da faixa da tabela acima (vale até para a primeira coleta).
- **Variação brusca**: mudança grande em relação à coleta anterior (pH ±0,5, turbidez ±10 NTU, temperatura ±3 °C).

Cada alerta aparece no painel com uma recomendação preventiva (ver `RegrasQualidade.recomendacao`).

# Como executar

Precisa de Java 21 e Maven.

1. Terminal 1, sistema receptor e painel:
   ```
   mvn compile exec:java -Dexec.mainClass=SistemaApp
   ```
2. Abra **http://localhost:8080** no navegador.
3. Terminal 2, sensor:
   ```
   mvn exec:java -Dexec.mainClass=SensorApp
   ```
   Digite o nome e o bairro. Enter faz uma coleta normal e 0 simula a coleta do próximo mês. O painel atualiza sozinho.

Para um bairro aparecer no mapa, adicione uma linha em `data/comunidades.csv`:
```
bairro;latitude;longitude
taruma;-3.0150;-60.0800
```

# Limitações e próximos passos

## Não há sensor físico ainda (os valores são aleatórios) e o app android é uma etapa pós entrega em teoria
## O painel precisa de internet para carregar o mapa e os gráficos (Leaflet e Chart.js vêm de CDN)
## Os limites e recomendações são didáticos: não substituem análise laboratorial da água
