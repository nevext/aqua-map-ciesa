# SOBRE O PROJETO

## O AquaMap é um sistema de monitoramento ambiental voltado para escolas e comunidades, com o objetivo de acompanhar e divulgar informações sobre a qualidade da água de forma simples e acessível. O projeto busca auxiliar na conscientização ambiental e na tomada de decisões preventivas, contribuindo para a preservação dos recursos hídricos e para a saúde da população.

# Como funciona

Tudo roda em um único programa (`SistemaApp`). Ele guarda as coletas em arquivos e serve um painel web, que também tem o **sensor simulado** dentro dele:

```
Painel web (navegador, porta 8080)
   │  lê:      GET  /api/dados
   │  escreve: POST /api/coleta, /api/agendar, /api/reiniciar   (aba Sensor)
   ▼
SistemaApp / SistemaAquaMap ──► pasta data/ (coletas.csv e coletas.xlsx de cada responsável)

SensorApp (opcional, no terminal) ──(socket, porta 5050)──► SistemaApp
```

1. **Painel (`PainelServidor` + `painel.html`)**: o `SistemaApp` sobe um servidor web (`HttpServer`, que já vem no Java). A página consulta `/api/dados` (JSON) a cada 5 segundos, só redesenha quando algo mudou e tem quatro telas no menu lateral:
   - **Visão Geral**: faixa de situação (verde, amarelo ou vermelho), porcentagem de pontos BOA e RUIM, medidor do Índice AquaMap, uma régua por indicador (faixa verde = valores adequados), mapa das comunidades com legenda e alertas com recomendação ("o que fazer").
   - **Monitoramento**: tabela de **pontos cadastrados** (clique filtra por bairro), resumo (índice médio, melhor, pior e tendência), gráfico do índice com faixas BOA/REGULAR/RUIM, gráficos de pH, turbidez e temperatura com a faixa adequada sombreada, ranking dos fatores de contaminação e tabela das últimas coletas.
   - **Cartilha Educacional**: texto em linguagem simples, com estatísticas do sistema e a explicação de como o índice é calculado.
   - **Sensor**: cadastro (responsável e bairro), coleta normal, simulação do próximo mês, agendamento e "começar do zero". Veja a seção abaixo.
2. **`SistemaAquaMap`**: recebe a coleta, calcula o Índice AquaMap (0 a 10), compara com a coleta anterior (anomalias) e salva em `data/<responsavel>/coletas.csv` e `coletas.xlsx`.
3. **`SensorApp` (opcional)**: o mesmo sensor, no terminal. Gera pH, turbidez e temperatura e envia pelo socket. Quem usa o painel não precisa dele.

## Aba Sensor

Substitui o terminal do sensor. Os valores são **sorteados**, como no terminal: pH de 6 a 9, turbidez de 0,5 a 50 NTU e temperatura de 20 a 35 °C.

| No terminal (`SensorApp`) | Na aba Sensor |
|---|---|
| Nome do responsável e bairro | Campos "Responsável" e "Bairro / comunidade" (o painel lembra o último cadastro) |
| Enter (coleta normal) | **Coletar agora** |
| 0 (simular o próximo mês) | **Simular próximo mês** |
| "Deseja agendar a próxima coleta? (S/N)" | **Agendar próxima coleta** (aparece depois do resultado) |

Depois de cada coleta a tela mostra o índice, o aviso de variação brusca e as réguas dos indicadores. Também mostra uma **notificação de planilha**: no painel (aviso no canto da tela e faixa no resultado) e no terminal do sistema, dizendo se `coletas.xlsx` e `coletas.csv` foram **criados** (primeira coleta do responsável) ou **atualizados**, e em que pasta.

Se o bairro digitado não estiver em `data/comunidades.csv` (por exemplo, com acento diferente), a coleta é salva e aparece nos gráficos, mas o ponto não aparece no mapa, e a tela avisa.

## Planilhas e backup

A cada coleta o sistema grava `data/<responsavel>/coletas.csv` e `coletas.xlsx`. Elas são a **cópia de segurança** dos dados: o histórico é recarregado delas quando o sistema liga.

### Começar do zero (limpar a demonstração)

Use **uma** destas opções:

- No painel: aba **Sensor**, no fim da página, botão **Reiniciar dados (guardando backup)** (pede confirmação).
- No terminal: `.\reiniciar.cmd` (desliga o sistema, arquiva os dados e liga de novo).

Nenhuma das duas apaga nada. As coletas, as planilhas e os agendamentos são **movidos** para `data/backup/AAAA-MM-DD_HH-mm-ss/`, e o sistema recomeça vazio. O mapa de comunidades (`data/comunidades.csv`) é mantido. Para desfazer, copie os arquivos de volta de `data/backup/<pasta>/` para `data/` e reinicie o sistema. A pasta `data/backup/` não vai para o Git e não é lida como histórico.

# Estrutura de pastas

## >.vscode = guarda configurações do editor para esse projeto
## > src/main/java é onde ficam os arquivos-fonte Java
## > src/main/resources guarda a página do painel (painel.html)
## > data guarda as coletas (uma pasta por responsável), os agendamentos e as coordenadas das comunidades
## > data/backup guarda as cópias criadas por "começar do zero" (não vai para o Git)
## pom.xml é a lista de instruções para o maven (ferramenta que ajuda a montar e organizar projetos java)
### Maven procura o código java em caminhos convencionais, src/main/java é o local padrão para o código principal e src/test/java pra testes, então o maven reconhece os arquivos sem precisarmos configurar um caminho especial
### pom.xml cria as dependências necessárias (Apache POI, para gerar o XLSX), então só roda com as dependências baixadas e instaladas

| Arquivo | Função |
|---|---|
| `Sensor.java` / `Medicao.java` | Sensor simulado e a medição que ele gera |
| `SensorApp.java` | Sensor no terminal (opcional; envia as coletas pelo socket) |
| `SistemaApp.java` | Programa principal (servidor do socket + inicia o painel) |
| `SistemaAquaMap.java` | Regras: índice, classificação, anomalias, histórico, agendamentos e arquivamento dos dados |
| `RegistroColeta.java` | Uma coleta salva (linha do CSV) |
| `ExportadorXlsx.java` | Gera a planilha XLSX |
| `RegrasQualidade.java` | Limites dos indicadores e recomendações preventivas |
| `PainelServidor.java` | Servidor web: página, `/api/dados` e as rotas da aba Sensor |
| `ReiniciarApp.java` | "Começar do zero" pelo terminal (usado pelo `reiniciar.cmd`) |
| `src/main/resources/painel.html` | Página do painel (mapa com Leaflet, gráficos com Chart.js) |
| `iniciar.cmd` / `parar.cmd` / `reiniciar.cmd` | Ligar o sistema, desligar e começar do zero |
| `sistema.cmd` / `sensor.cmd` | Ligar só o sistema / só o sensor do terminal |

# Indicadores

| Indicador | Faixa considerada adequada | Problema gerado |
|---|---|---|
| pH | 6,5 a 8,5 | "pH acido" ou "pH alcalino" |
| Turbidez | até 5 NTU (Portaria GM/MS 888/2021) | "Turbidez alta" |
| Temperatura | até 30 °C | "Temperatura alta" |

O **Índice AquaMap** é a média das notas (0 a 10) dos três indicadores: 8 ou mais = BOA, 5 a 8 = REGULAR, abaixo de 5 = RUIM. Cada nota começa em 10 e perde pontos assim:

| Indicador | Perda de pontos |
|---|---|
| pH | 2 pontos por unidade fora da faixa 6,5 a 8,5 |
| Turbidez | 0,2 ponto por NTU |
| Temperatura | 0,5 ponto por °C fora da faixa 20 a 30 °C |

Como é uma média, uma nota boa pode esconder um indicador ruim: por isso o painel mostra a régua de cada indicador e os alertas separados.

# Alertas

Uma coleta gera alerta em dois casos:
- **Fora do limite**: algum indicador fora da faixa da tabela acima (vale até para a primeira coleta).
- **Variação brusca**: mudança grande em relação à coleta anterior (pH ±0,5, turbidez ±10 NTU, temperatura ±3 °C).

Cada alerta aparece no painel com uma recomendação preventiva (ver `RegrasQualidade.recomendacao`).

# Como executar

Precisa de Java 21 e Maven.

**Atalho:** rode `.\iniciar.cmd`. Ele liga o sistema numa janela própria e o painel abre no navegador. Se o sistema já estiver ligado, só abre o painel. Para encerrar, feche a janela do sistema ou rode `.\parar.cmd`.

No painel, abra a aba **Sensor**, cadastre o responsável e o bairro e envie as coletas. Não é preciso abrir terminal para o sensor.

Passo a passo manual (alternativa ao atalho):

1. Terminal 1, sistema e painel:
   ```
   .\sistema.cmd
   ```
   (ou `mvn compile exec:java@sistema`)
2. Abra **http://localhost:8080** no navegador.
3. (Opcional) Terminal 2, sensor no terminal:
   ```
   .\sensor.cmd
   ```
   (ou `mvn compile exec:java@sensor`)
   Digite o nome e o bairro. Enter faz uma coleta normal e 0 simula a coleta do próximo mês. O painel atualiza sozinho.

Para um bairro aparecer no mapa, adicione uma linha em `data/comunidades.csv` (o nome digitado na aba Sensor deve ser igual ao do arquivo, sem diferença de acento):
```
bairro;latitude;longitude
taruma;-3.0150;-60.0800
```

# Limitações e próximos passos

## Não há sensor físico ainda (os valores são aleatórios, não dá para digitar valores) e o app android é uma etapa pós entrega em teoria
## O painel precisa de internet para carregar o mapa e os gráficos (Leaflet e Chart.js vêm de CDN)
## Não há login nem perfis de acesso: quem alcançar a porta 8080 pode registrar coletas e reiniciar os dados (use só em rede confiável)
## Os limites e recomendações são didáticos: não substituem análise laboratorial da água
