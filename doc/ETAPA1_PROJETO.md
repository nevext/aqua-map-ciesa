# ETAPA 1: PROJETO

## IDENTIFICAÇÃO

- **Curso:** ____________________
- **Disciplina:** ____________________
- **Professor(a):** ____________________
- **Acadêmico(s):** ____________________
- **Turma:** ____________________
- **Período:** ____________________

---

## 1. TEMA DO PROJETO

### 1.1 Título do Projeto

**AquaMap: Sistema de Monitoramento Comunitário da Qualidade da Água**

### 1.2 Área de Atuação

( ) Educação ( ) Saúde **(X) Meio Ambiente** ( ) Tecnologia Social ( ) Mobilidade Urbana ( ) Inclusão Social ( ) Outro: ________

> Se o formulário permitir marcar mais de uma, marque também **Saúde** e **Tecnologia Social**.

### 1.3 Tema

Desenvolvimento de um sistema de monitoramento da qualidade da água voltado para escolas e comunidades. O sistema recebe coletas de um sensor (pH, turbidez e temperatura), calcula um índice simples de qualidade (Índice AquaMap, de 0 a 10), identifica problemas e variações bruscas entre coletas e divulga os resultados em um painel web com mapa dos bairros, gráficos históricos, alertas e recomendações preventivas para a população.

---

## 2. JUSTIFICATIVA

**Problema social identificado.** Muitas comunidades, principalmente em bairros periféricos e áreas próximas a igarapés, consomem ou usam água sem nenhuma informação sobre sua qualidade. Quando existe análise, ela costuma ser feita por órgãos técnicos, com resultados demorados e em linguagem pouco acessível. Com isso, problemas como contaminação por esgoto, turbidez após chuvas fortes ou caixas d'água mal conservadas só são percebidos quando já causam doenças de veiculação hídrica.

**Importância para a comunidade.** O AquaMap coloca a informação sobre a água nas mãos da própria comunidade e das escolas. Os resultados aparecem em um mapa colorido (verde, amarelo e vermelho), fácil de entender por qualquer pessoa, e cada problema detectado vem acompanhado de uma orientação prática (por exemplo: "filtre e ferva a água antes de consumir").

**Contribuição para a melhoria da realidade.** O sistema permite:
- acompanhar a evolução da qualidade da água ao longo do tempo, por bairro;
- emitir alertas quando algum indicador sai da faixa adequada ou muda de forma brusca entre uma coleta e outra;
- identificar os fatores de contaminação mais frequentes (ranking), ajudando a priorizar ações;
- orientar a população com medidas preventivas imediatas;
- gerar registros (planilhas CSV/XLSX) que podem ser levados a escolas, unidades de saúde ou órgãos responsáveis.

**Relevância acadêmica.** O projeto aplica, em um problema real, conteúdos do curso como programação orientada a objetos em Java, comunicação em rede via sockets (cliente/servidor), persistência de dados em arquivos (CSV e XLSX com Apache POI), servidor HTTP e API JSON, desenvolvimento web (mapa com Leaflet e gráficos com Chart.js) e organização de projeto com Maven. Também envolve pesquisa sobre parâmetros de potabilidade (Portaria GM/MS nº 888/2021) e une tecnologia, meio ambiente e saúde pública em uma atividade de extensão.

---

## 3. OBJETIVOS

### 3.1 Objetivo Geral

Desenvolver um sistema de monitoramento da qualidade da água que colete, analise e divulgue de forma simples e acessível informações sobre pH, turbidez e temperatura da água de escolas e comunidades, contribuindo para a conscientização ambiental e para a prevenção de problemas de saúde.

### 3.2 Objetivos Específicos

- Simular um sensor que mede pH, turbidez e temperatura da água e envia as coletas pela rede;
- Calcular o Índice AquaMap (0 a 10) e classificar a água como BOA, REGULAR ou RUIM;
- Detectar indicadores fora dos limites adequados e variações bruscas em relação à coleta anterior;
- Armazenar o histórico de coletas por responsável em arquivos CSV e XLSX;
- Agendar coletas periódicas (mensais) por bairro;
- Disponibilizar um painel web com mapa das comunidades, gráficos de evolução histórica, ranking dos fatores de contaminação e alertas em tempo real;
- Oferecer recomendações preventivas para cada problema identificado;
- Promover a conscientização da comunidade e de estudantes sobre a preservação dos recursos hídricos.

---

## 4. METODOLOGIA

### 4.1 Público-Alvo

Moradores de comunidades e bairros que utilizam água de poços, igarapés, caixas d'água ou rede de abastecimento; estudantes e professores de escolas da região; e agentes comunitários e de saúde que podem usar as informações para orientar a população.

### 4.2 Local de Aplicação

Escolas e comunidades da cidade, começando pelo bairro **Tarumã** (ponto já cadastrado no mapa do sistema). Novos bairros podem ser incluídos apenas adicionando suas coordenadas no arquivo `data/comunidades.csv`. O desenvolvimento e os testes foram realizados no ambiente acadêmico.

### 4.3 Procedimentos Metodológicos

1. **Levantamento do problema e pesquisa:** estudo sobre a qualidade da água nas comunidades e sobre os parâmetros de referência (pH entre 6,5 e 8,5; turbidez até 5 NTU, conforme a Portaria GM/MS nº 888/2021; temperatura até 30 °C).
2. **Definição dos requisitos:** escolha dos indicadores, das regras do índice (média das notas dos três indicadores: 8 ou mais = BOA, 5 a 8 = REGULAR, abaixo de 5 = RUIM) e dos critérios de alerta (fora do limite ou variação brusca: pH ±0,5, turbidez ±10 NTU, temperatura ±3 °C).
3. **Desenvolvimento do sensor (SensorApp):** programa em Java que simula as medições e as envia ao sistema por socket (porta 5050), identificando o responsável e o bairro da coleta.
4. **Desenvolvimento do sistema receptor (SistemaApp / SistemaAquaMap):** recebe as coletas, calcula o índice, compara com a coleta anterior, controla o agendamento mensal e salva o histórico em `data/<responsável>/coletas.csv` e `coletas.xlsx`.
5. **Regras de qualidade e recomendações (RegrasQualidade):** definição dos limites e das orientações preventivas para cada problema (pH ácido, pH alcalino, turbidez alta, temperatura alta e variação brusca).
6. **Desenvolvimento do painel web (PainelServidor + painel.html):** servidor HTTP na porta 8080 com API `/api/dados` (JSON), atualização automática a cada 5 segundos, mapa das comunidades, gráficos, filtro por bairro, ranking de contaminação, alertas e recomendações.
7. **Testes:** execução de coletas simuladas, incluindo coletas de meses seguintes, para validar o cálculo do índice, os alertas, o agendamento, a geração das planilhas e a atualização do painel.
8. **Apresentação e divulgação:** demonstração do sistema para a turma/comunidade, explicando como interpretar o mapa, os alertas e as recomendações.
9. **Próximos passos:** integração com um sensor físico real e desenvolvimento de um aplicativo Android.

### 4.4 Recursos Necessários

**Recursos Humanos**
- Acadêmicos responsáveis pelo desenvolvimento, testes e apresentação do sistema;
- Professor(a) orientador(a);
- Representantes da comunidade/escola para realizar as coletas e acompanhar os resultados.

**Recursos Tecnológicos**
- Computador com Java 21 e Maven;
- Biblioteca Apache POI (geração de planilhas XLSX);
- Leaflet (mapa) e Chart.js (gráficos), carregados via internet;
- Navegador web e conexão com a internet;
- Editor de código (VS Code) e Git/GitHub para versionamento;
- Futuramente: sensor físico de pH, turbidez e temperatura (ex.: placa Arduino/ESP32 com sondas) e smartphone Android.

**Recursos Materiais**
- Recipientes limpos para coleta de amostras de água;
- Material para identificação dos pontos de coleta (etiquetas, caderno de registro);
- Material de divulgação para a comunidade (cartazes ou folhetos com as recomendações preventivas).
