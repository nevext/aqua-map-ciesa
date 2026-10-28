# Relatório final (modelo CIESA): o que colocar em cada seção

> **Como usar:** copie cada bloco para o modelo do CIESA. Textos entre `[colchetes]` são para vocês preencherem. As notas em citação (`>`) são avisos para vocês e **não vão para o trabalho**.
>
> Os resultados da seção 7 vêm de testes executados de verdade no projeto em 28/10/2026 (sistema compilado, painel e socket testados). Se vocês fizerem testes com usuários, acrescentem.

---

## CAPA

CENTRO UNIVERSITÁRIO CIESA
Pró-reitoria de Ensino de Graduação
Coordenação do Curso de Bacharelado em Ciências da Computação

CURSO DE CIÊNCIAS DA COMPUTAÇÃO

**AQUAMAP: SISTEMA DE MAPEAMENTO DA QUALIDADE DA ÁGUA EM COMUNIDADES**

Autor(es): [NOMES COMPLETOS]
Professor: Francisco de Assis S. de Oliveira
Disciplina: Álgebra Linear II

MANAUS – AM
2026

---

## RESUMO

A falta de informação acessível sobre a qualidade da água é um problema comum em comunidades urbanas e periféricas, onde muitas vezes não há acompanhamento contínuo dos parâmetros básicos. Este trabalho apresenta o AquaMap, um sistema computacional que organiza medições de qualidade da água e as apresenta de forma compreensível, rastreável e georreferenciada. O objetivo foi desenvolver um protótipo capaz de receber coletas de pH, turbidez e temperatura, calcular um índice de qualidade e exibir os resultados em um painel web com mapa. A metodologia envolveu o desenvolvimento incremental de software em Java, com um sensor simulado que gera valores aleatórios dentro de faixas definidas e os envia por socket a um sistema receptor. Cada coleta é representada como um vetor em ℝ³; o Índice AquaMap (0 a 10) é obtido pelo produto escalar entre o vetor de notas dos indicadores e um vetor de pesos, e as anomalias são detectadas pelo vetor diferença entre coletas consecutivas. Os dados são armazenados em CSV e XLSX e exibidos em um painel com mapa, gráficos temporais, ranking de problemas, alertas e recomendações preventivas. Nos testes, o painel respondeu em cerca de 3 ms, todas as mensagens inválidas foram rejeitadas com mensagem adequada e o índice calculado coincidiu com o cálculo manual. Conclui-se que o protótipo cumpre o objetivo de organizar e comunicar as medições, mas não substitui a análise laboratorial nem permite declarar a potabilidade da água.

**Palavras-chave:** qualidade da água; álgebra linear; georreferenciamento; monitoramento comunitário; ciência da computação.

> O modelo veio com palavras-chave de outro trabalho ("epidemiologia computacional", "simulação"). Troquem pelas de cima, que batem com o AquaMap.

---

## ABSTRACT

The lack of accessible information about water quality is a common problem in urban and peripheral communities, where basic parameters are often not monitored continuously. This work presents AquaMap, a computer system that organizes water quality measurements and presents them in an understandable, traceable and georeferenced way. The goal was to develop a prototype able to receive pH, turbidity and temperature samples, compute a quality index and display the results on a web dashboard with a map. The methodology followed incremental software development in Java, with a simulated sensor that generates random values within defined ranges and sends them over a socket to a receiving system. Each sample is represented as a vector in ℝ³; the AquaMap Index (0 to 10) is obtained from the dot product between the vector of indicator scores and a weight vector, and anomalies are detected through the difference vector between consecutive samples. Data are stored in CSV and XLSX files and shown on a dashboard with a map, time-series charts, a ranking of problems, alerts and preventive recommendations. In the tests, the dashboard responded in about 3 ms, every invalid message was rejected with an appropriate message, and the computed index matched the manual calculation. We conclude that the prototype meets the goal of organizing and communicating measurements, but it does not replace laboratory analysis and cannot declare water fit for consumption.

**Keywords:** water quality; linear algebra; georeferencing; community monitoring; computer science.

---

## 1. INTRODUÇÃO

A disponibilidade de informações confiáveis sobre a qualidade da água é essencial para a saúde da população e para a preservação dos recursos hídricos. Em muitas comunidades, porém, essa informação não chega aos moradores: as análises são pontuais, demoradas e apresentadas em linguagem técnica. Em uma cidade como Manaus, cercada por rios e igarapés e com bairros que dependem de poços e caixas d'água, a ausência de acompanhamento dificulta perceber mudanças na água antes que elas se tornem um problema.

Diante disso, este trabalho parte da seguinte pergunta: como um sistema computacional pode organizar medições de qualidade da água e apresentá-las de modo compreensível, rastreável e georreferenciado? O AquaMap propõe integrar o registro das coletas por bairro e responsável, o armazenamento dos parâmetros medidos (pH, turbidez e temperatura) e a visualização em mapa, respeitando os limites de interpretação dos dados.

A disciplina de Álgebra Linear II fundamenta o núcleo de cálculo do sistema: cada coleta é tratada como um vetor de três componentes, o índice de qualidade é calculado por produto escalar e a comparação entre coletas usa o vetor diferença. O sistema não pretende declarar se a água é própria para consumo, e sim organizar e comunicar as medições, apontando quando algo merece atenção e uma nova verificação.

---

## 2. OBJETIVOS

### 2.1. Objetivo geral

Desenvolver um protótipo de sistema que receba, analise, armazene e apresente em mapa medições de qualidade da água (pH, turbidez e temperatura) de comunidades, usando conceitos de álgebra linear para calcular um índice de qualidade e detectar variações entre coletas.

### 2.2. Objetivos específicos

(a) levantar os requisitos do sistema e os parâmetros de referência para pH, turbidez e temperatura;
(b) definir os dados de cada coleta: data/hora, responsável, bairro, valores medidos e unidades;
(c) modelar cada coleta como vetor em ℝ³ e calcular o Índice AquaMap por produto escalar;
(d) detectar anomalias pelo vetor diferença entre coletas consecutivas do mesmo bairro;
(e) armazenar o histórico em arquivos CSV e XLSX e associar cada bairro a coordenadas geográficas;
(f) implementar um painel web com mapa, filtro por bairro, gráficos temporais, ranking de problemas, alertas e recomendações;
(g) validar as entradas e avaliar o protótipo com cenários de teste.

> O modelo trazia objetivos que o projeto **não fez**: banco de dados georreferenciado, cadastro/edição pela interface, permissões de usuário e avaliação com usuários. Reescrevi para bater com o que foi entregue. Se o professor exigir os itens originais, mantenham e expliquem nos resultados e na conclusão que ficaram para trabalhos futuros.

---

## 3. FUNDAMENTAÇÃO TEÓRICA

**3.1 Parâmetros de qualidade da água.** O pH indica se a água é ácida ou alcalina e influencia a eficiência do tratamento e a corrosão das tubulações. A turbidez mede a presença de partículas em suspensão (argila, matéria orgânica, microrganismos) e é expressa em uT/NTU. A temperatura interfere na solubilidade de gases e na proliferação de microrganismos. No Brasil, o padrão de potabilidade é definido pela Portaria GM/MS nº 888/2021 (BRASIL, 2021), que estabelece, entre outros valores, o limite de 5 uT para turbidez da água distribuída. Para corpos d'água, a Resolução CONAMA nº 357/2005 (BRASIL, 2005) define classes e limites, e a Organização Mundial da Saúde publica diretrizes internacionais (WHO, 2022).

> **Conferir antes de entregar:** a faixa de pH usada no código (6,5 a 8,5) é mais conservadora que a recomendada pela Portaria 888/2021 para o sistema de distribuição. O limite de 30 °C para temperatura é uma escolha didática do projeto, não um valor de norma. Escrevam isso assim no texto, sem atribuir esses dois valores à portaria.

**3.2 Procedência, calibração e critérios normativos.** Uma medição só tem valor se for possível saber de onde veio, quando foi feita, por quem e com qual instrumento. Sondas de pH e turbidímetros precisam de calibração periódica com soluções padrão, e a coleta deve seguir procedimentos de amostragem e preservação (APHA; AWWA; WEF, 2017; FUNASA, 2013). Por isso o AquaMap registra data/hora, responsável, bairro e unidade de cada valor.

**3.3 Representação de medições x declaração de potabilidade.** Organizar e exibir medições é diferente de declarar que a água é potável. A classificação sanitária exige um conjunto amplo de parâmetros (microbiológicos, químicos, organolépticos), métodos analíticos reconhecidos e laboratório habilitado (BRASIL, 2021). O AquaMap mede apenas três parâmetros, com sensor simulado. Por isso suas classificações (BOA, REGULAR, RUIM) são indicativas e servem para orientar uma nova coleta ou procurar um órgão competente, nunca para atestar potabilidade.

**3.4 Álgebra linear aplicada.** Um vetor em ℝⁿ é uma lista ordenada de n números reais. O produto escalar entre **u** e **v** é u·v = u₁v₁ + u₂v₂ + … + uₙvₙ e pode ser usado como média ponderada quando **v** é um vetor de pesos que soma 1. A diferença **u** − **v** mostra, componente a componente, quanto cada grandeza variou. Um conjunto de m vetores em ℝⁿ pode ser organizado como uma matriz m × n (BOLDRINI et al., 1980; ANTON; RORRES, 2012). Esses três conceitos formam o núcleo de cálculo do AquaMap (seção 6.3).

**3.5 Georreferenciamento e visualização.** Associar cada medição a coordenadas de latitude e longitude permite exibi-la em mapa e perceber padrões espaciais. O sistema usa a biblioteca Leaflet sobre mapas do OpenStreetMap e a biblioteca Chart.js para os gráficos temporais.

---

## 4. JUSTIFICATIVA

Muitas comunidades usam água de poços, igarapés, caixas d'água ou da rede de abastecimento sem nenhuma informação sobre sua qualidade. Quando há análise, ela é pontual e pouco acessível ao morador. Assim, problemas como turbidez alta após chuvas fortes, entrada de esgoto ou caixas d'água mal conservadas só são percebidos quando já afetam a saúde.

O AquaMap se justifica por três motivos:

- **Social:** leva a informação para a comunidade e para as escolas em um mapa colorido (verde, amarelo e vermelho) que qualquer pessoa entende, com uma orientação prática para cada problema detectado.
- **Preventivo:** o histórico por bairro e os alertas de variação brusca ajudam a perceber mudanças cedo e a pedir uma verificação técnica antes que o problema se agrave.
- **Acadêmico:** aplica a um problema real conteúdos de Álgebra Linear (vetores, produto escalar, matrizes) e de Ciência da Computação (programação orientada a objetos, comunicação cliente/servidor por socket, servidor HTTP e API JSON, persistência em arquivos e visualização web).

---

## 5. METODOLOGIA

A pesquisa é aplicada, com abordagem predominantemente quantitativa e desenvolvimento incremental de software. As etapas realizadas foram:

1. **Levantamento de requisitos e público-alvo:** definição dos usuários (moradores, escolas e agentes de saúde), dos indicadores medidos e das informações que o painel deveria mostrar.
2. **Desenho dos fluxos:** sensor → sistema receptor → arquivos → painel web.
3. **Modelagem das entidades:** `Sensor` (gera uma medição dentro de uma faixa), `Medicao` (indicador, valor, unidade e horário), `RegistroColeta` (uma coleta completa salva no histórico) e o arquivo `comunidades.csv` (bairro, latitude e longitude).
4. **Implementação:** sistema em Java 21 com Maven, comunicação por socket (porta 5050), servidor HTTP com API JSON (porta 8080), armazenamento em CSV/XLSX (Apache POI) e interface cartográfica (Leaflet + Chart.js).
5. **Testes:** testes funcionais manuais do fluxo completo, testes de entradas inválidas enviadas ao socket e medição do tempo de resposta do painel.

Cada coleta registra data/hora, responsável, bairro, valor e unidade de cada indicador, o índice calculado, a classificação e a análise de variação. **Todos os dados usados neste trabalho são sintéticos**, gerados pelo sensor simulado. Dados reais dependeriam de equipamento calibrado, autorização dos responsáveis pelos pontos de coleta e protocolo de amostragem adequado.

> O modelo citava as entidades Comunidade, PontoDeColeta, Amostra, Parâmetro, Medição e Usuário, banco de dados e testes unitários/de integração/de aceitação e de usabilidade. O projeto não tem banco de dados (usa arquivos), não tem entidade Usuário e não tem testes automatizados. Descrevi o que existe de fato.

---

## 6. DESENVOLVIMENTO DO SIMULADOR

### 6.1 Arquitetura

```
SensorApp ──(socket, porta 5050)──► SistemaApp / SistemaAquaMap ──► data/<responsável>/coletas.csv e coletas.xlsx
                                             │
                                             └──► PainelServidor (HTTP, porta 8080) ──► painel.html no navegador
```

| Componente | Função |
|---|---|
| `Sensor` / `Medicao` | Sensor simulado e a medição que ele gera |
| `SensorApp` | Programa do sensor: pede responsável e bairro e envia as coletas |
| `SistemaApp` | Recebe as mensagens do socket e inicia o painel |
| `SistemaAquaMap` | Índice, classificação, anomalias, histórico e agendamentos |
| `RegrasQualidade` | Limites dos indicadores e recomendações preventivas |
| `ExportadorXlsx` | Gera a planilha XLSX |
| `PainelServidor` + `painel.html` | Servidor web, API `/api/dados` e página do painel |

### 6.2 Entradas

O sensor simulado gera valores aleatórios uniformes nas faixas: pH de 6,0 a 9,0; turbidez de 0,5 a 50 NTU; temperatura de 20 a 35 °C. O usuário informa o nome do responsável e o bairro. Cada coleta é enviada como texto: um cabeçalho `COLETA;responsável;bairro` seguido de uma linha `indicador;valor;unidade` por medição.

### 6.3 Algoritmo do Índice AquaMap (álgebra linear)

**Vetor da coleta.** Cada coleta é um vetor **x** = (pH, turbidez, temperatura) ∈ ℝ³.

**Vetor de notas.** Cada componente é convertido em uma nota de 0 a 10:
- pH: n₁ = 10 − 2·d, em que d é a distância até a faixa [6,5; 8,5] (d = 0 dentro da faixa);
- turbidez: n₂ = 10 − 0,2·turbidez;
- temperatura: n₃ = 10 − 0,5·d, em que d é a distância até a faixa [20; 30] °C.

As notas são limitadas ao intervalo [0, 10], formando **n** = (n₁, n₂, n₃).

**Produto escalar.** Com o vetor de pesos **p** = (1/3, 1/3, 1/3), o índice é

  I = **n** · **p** = n₁·(1/3) + n₂·(1/3) + n₃·(1/3)

Classificação: I ≥ 8 → BOA; 5 ≤ I < 8 → REGULAR; I < 5 → RUIM. Trocar o vetor de pesos permite dar mais importância a um indicador sem mudar o resto do algoritmo.

**Exemplo real (coleta de 24/10/2026, bairro Tarumã):** **x** = (8,99; 31,11; 33,12)
- n₁ = 10 − 2·(8,99 − 8,5) = 9,03
- n₂ = 10 − 0,2·31,11 = 3,78
- n₃ = 10 − 0,5·(33,12 − 30) = 8,44
- I = (9,03 + 3,78 + 8,44)/3 = **7,08 → REGULAR** (mesmo valor salvo pelo sistema)

**Vetor diferença (anomalias).** Para duas coletas consecutivas do mesmo responsável e bairro, calcula-se **Δ** = **x**ₐₜᵤₐₗ − **x**ₐₙₜₑᵣᵢₒᵣ. Há anomalia na componente i se |Δᵢ| ultrapassa o limite: 0,5 no pH, 10 NTU na turbidez e 3 °C na temperatura. Exemplo: entre 24/10 e 24/11/2026, **Δ** = (−2,83; −28,83; −11,57), com anomalia nos três indicadores.

**Matriz do histórico.** As coletas carregadas são guardadas também como uma matriz m × 3 (uma linha por coleta), base para cálculos futuros como médias por coluna e tendências.

> A matriz do histórico existe no código (`matrizHistorico`), mas por enquanto só é preenchida e não entra em nenhum cálculo. Não afirmem que ela é usada.

### 6.4 Evolução temporal e agendamento

Após cada coleta, o sistema agenda a próxima para um mês depois (`data/agendamentos.csv`) e recusa novas coletas do mesmo bairro e responsável antes dessa data. Para simular a passagem do tempo nos testes, o sensor envia `COLETA_TESTE`, que registra a coleta com data igual à anterior mais um mês. Assim o histórico mensal pode ser construído e os gráficos temporais podem ser avaliados.

### 6.5 Interface (painel web)

A página consulta `/api/dados` a cada 5 segundos e mostra:
- **Mapa das comunidades:** um ponto por bairro, colorido conforme a última classificação;
- **Alertas e recomendações preventivas:** os problemas das coletas mais recentes (até 15), cada um com uma orientação;
- **Evolução histórica do índice**, com **filtro por bairro**;
- **Gráfico dos indicadores** (pH, turbidez, temperatura) ao longo das coletas;
- **Ranking dos fatores de contaminação:** quantas coletas tiveram cada problema.

### 6.6 Exportação de dados

Cada responsável tem uma pasta com `coletas.csv` e `coletas.xlsx`, regravados a cada nova coleta, em ordem cronológica.

### 6.7 Tratamento de erros

Mensagens vazias, comandos desconhecidos, coletas sem os três indicadores, valores não numéricos, medições mal formatadas, datas inválidas e coletas antes da data agendada são rejeitadas com mensagem explicativa, sem derrubar o sistema. Registros corrompidos no CSV são ignorados ao carregar o histórico, e falhas ao gravar arquivos são informadas no console.

### 6.8 Privacidade, perfis de acesso e histórico de alterações

O sistema guarda apenas o nome informado do responsável e o bairro, sem endereço, documento ou contato, o que reduz a exposição de dados pessoais. O nome é usado para separar as pastas; caracteres inválidos são substituídos para evitar gravação fora da pasta `data`. **Ainda não há perfis de acesso nem histórico de alterações:** qualquer pessoa com acesso ao painel vê todos os dados, e os registros não são editados pela interface. Esses dois pontos ficam previstos para trabalhos futuros.

### 6.9 Testes

Os testes foram funcionais e manuais (não há testes automatizados): execução do fluxo sensor → sistema → painel, envio de mensagens inválidas ao socket e medição do tempo de resposta do painel. Os resultados estão na seção 7.

---

## 7. RESULTADOS

> Todos os números abaixo foram obtidos executando o sistema em 28/10/2026 em um computador local, com os **dados sintéticos** já existentes em `data/david/coletas.csv`.

### 7.1 Casos de teste

| # | Caso de teste | Entrada | Resultado esperado | Resultado obtido | Situação |
|---|---|---|---|---|---|
| 1 | Compilação | `mvn compile` | Compilar sem erros | Compilou sem erros | OK |
| 2 | Painel no ar | GET `/` | Página HTML | HTTP 200, 7.710 bytes | OK |
| 3 | API de dados | GET `/api/dados` | JSON com comunidades, coletas, ranking e alertas | HTTP 200, 2.383 bytes, 4 blocos presentes | OK |
| 4 | Carregar histórico | 3 coletas no CSV | 3 coletas no painel | 3 coletas carregadas | OK |
| 5 | Cálculo do índice | Coleta de 24/10/2026 | 7,08 (cálculo manual) | 7,08, REGULAR | OK |
| 6 | Detecção de anomalia | Coletas de 10/2026 e 11/2026 | Anomalia em pH, turbidez e temperatura | "SIM: pH, turbidez, temperatura" | OK |
| 7 | Mensagem vazia | (nada) | Rejeitar | "Mensagem vazia." | OK |
| 8 | Comando desconhecido | `XYZ;a;b` | Rejeitar | "Comando desconhecido ou incompleto." | OK |
| 9 | Coleta incompleta | Só pH | Rejeitar | "Coleta incompleta: envie pH, turbidez e temperatura." | OK |
| 10 | Valor não numérico | `pH;abc;pH` | Rejeitar | "Coleta invalida: For input string: "abc"" | OK |
| 11 | Medição mal formatada | `pH` | Rejeitar | "Coleta invalida: Formato de medicao invalido: pH" | OK |
| 12 | Coleta antes do agendamento | Tarumã, agendada para 24/11/2026 | Recusar | "Coleta ainda nao liberada. A proxima esta agendada para 2026-11-24." | OK |
| 13 | Data de agendamento inválida | `2026-13-45` | Rejeitar | "Data de agendamento invalida." | OK |
| 14 | Arquivos após erros | Casos 7 a 13 | Nada gravado | Nenhum arquivo de coleta criado ou alterado | OK |

**Taxa de sucesso:** 14 de 14 casos (100%).

### 7.2 Tempo de resposta

| Operação | Tempo medido |
|---|---|
| GET `/api/dados` (5 medições) | 2,9 a 3,0 ms |
| Resposta do socket a mensagens inválidas | 0,2 a 1,9 ms |

### 7.3 Dados sintéticos registrados (Tarumã, responsável "david")

| Data | pH | Turbidez (NTU) | Temp. (°C) | Índice | Classificação | Anomalia |
|---|---|---|---|---|---|---|
| 24/10/2026 | 8,99 | 31,11 | 33,12 | 7,08 | REGULAR | Primeira coleta |
| 24/11/2026 | 6,15 | 2,29 | 21,55 | 9,62 | BOA | pH, turbidez, temperatura |
| 24/12/2026 | 8,69 | 16,58 | 22,03 | 8,77 | BOA | pH, turbidez |

Ranking gerado: pH alcalino (2), turbidez alta (2), variação brusca (2), temperatura alta (1), pH ácido (1).

### 7.4 Avaliação cartográfica

- **Posicionamento:** o Tarumã aparece no mapa, mas a API arredonda as coordenadas para duas casas decimais (−3,0150 vira −3,02). Isso desloca o ponto em até cerca de 500 m: serve para indicar o bairro, não um ponto exato de coleta.
- **Filtros:** o filtro por bairro funciona. Não há filtro por período nem por parâmetro.
- **Legendas:** o mapa usa as cores verde (BOA), amarelo (REGULAR) e vermelho (RUIM).

### 7.5 Problemas identificados

1. **O índice pode esconder um indicador ruim.** Na coleta de 24/12/2026 a turbidez (16,58 NTU) está acima do limite de 5 NTU, mas o índice deu 8,77 (BOA), porque a média compensa com as outras notas. O alerta de "Turbidez alta" aparece no painel, mas a cor do mapa fica verde.
2. **Coordenadas arredondadas** na API (item 7.4).
3. **Agendamentos duplicados:** `agendamentos.csv` tem a mesma linha repetida três vezes. Não afeta o funcionamento, porque vale a data mais recente.
4. **Dependência de internet:** o mapa e os gráficos são carregados de CDN.
5. **Dados apenas sintéticos:** não houve coleta real nem avaliação com usuários.

Como os dados são sintéticos e só três parâmetros são medidos, **nenhum resultado permite concluir que a água do Tarumã é própria ou imprópria para consumo.**

---

## 8. CONCLUSÃO

O AquaMap cumpriu o objetivo de organizar medições de qualidade da água e apresentá-las de forma compreensível, rastreável e georreferenciada. O protótipo recebe coletas por rede, calcula o Índice AquaMap por produto escalar, detecta variações bruscas pelo vetor diferença, guarda o histórico em CSV e XLSX e exibe mapa, gráficos, ranking, alertas e recomendações em um painel web. Os 14 casos de teste foram atendidos, o painel respondeu em cerca de 3 ms e o índice calculado coincidiu com o cálculo manual, mostrando na prática a aplicação de conceitos de Álgebra Linear.

O trabalho tem limitações importantes. Os resultados dependem da qualidade das medições, e o sensor atual é simulado. O painel depende de conexão com a internet. A representatividade espacial é limitada: há um ponto por bairro, com coordenadas arredondadas. A média ponderada pode classificar como BOA uma coleta com um indicador fora do limite. Não houve avaliação com usuários. Por tudo isso, o sistema não substitui a análise laboratorial nem declara a potabilidade da água.

Como trabalhos futuros, propõe-se: coleta offline com sincronização posterior; integração com sensores físicos calibrados (por exemplo, ESP32 com sondas de pH, turbidez e temperatura); alertas e pesos configuráveis, incluindo uma regra que impeça a classificação BOA quando algum indicador estiver fora do limite; uso da matriz do histórico para médias e tendências; filtros por período e parâmetro; perfis de acesso e histórico de alterações para auditoria dos registros; interoperabilidade com bases públicas de qualidade da água; e um aplicativo Android.

---

## 9. REFERÊNCIAS

> **Atenção:** o modelo pede "somente fontes efetivamente citadas" e "não utilizar referências fictícias". As obras abaixo existem, mas **confiram cada uma** (edição, ano, editora) e removam as que vocês não consultaram de fato, junto com a citação no texto. As de software são as documentações oficiais das bibliotecas usadas no projeto.

ANTON, H.; RORRES, C. **Álgebra linear com aplicações**. 10. ed. Porto Alegre: Bookman, 2012.

APACHE SOFTWARE FOUNDATION. **Apache POI**: the Java API for Microsoft documents. Disponível em: https://poi.apache.org/. Acesso em: 28 out. 2026.

APHA; AWWA; WEF. **Standard methods for the examination of water and wastewater**. 23. ed. Washington, DC: American Public Health Association, 2017.

BOLDRINI, J. L. et al. **Álgebra linear**. 3. ed. São Paulo: Harbra, 1980.

BRASIL. Conselho Nacional do Meio Ambiente. **Resolução nº 357, de 17 de março de 2005**. Dispõe sobre a classificação dos corpos de água e diretrizes ambientais para o seu enquadramento [...]. Diário Oficial da União, Brasília, DF, 18 mar. 2005.

BRASIL. Ministério da Saúde. **Portaria GM/MS nº 888, de 4 de maio de 2021**. Altera o Anexo XX da Portaria de Consolidação GM/MS nº 5, de 28 de setembro de 2017, para dispor sobre os procedimentos de controle e de vigilância da qualidade da água para consumo humano e seu padrão de potabilidade. Diário Oficial da União, Brasília, DF, 7 maio 2021.

CHART.JS. **Chart.js documentation**. Disponível em: https://www.chartjs.org/docs/. Acesso em: 28 out. 2026.

FUNDAÇÃO NACIONAL DE SAÚDE (FUNASA). **Manual prático de análise de água**. 4. ed. Brasília: Funasa, 2013.

LEAFLET. **Leaflet**: an open-source JavaScript library for interactive maps. Disponível em: https://leafletjs.com/. Acesso em: 28 out. 2026.

WORLD HEALTH ORGANIZATION (WHO). **Guidelines for drinking-water quality**: fourth edition incorporating the first and second addenda. Geneva: WHO, 2022.
