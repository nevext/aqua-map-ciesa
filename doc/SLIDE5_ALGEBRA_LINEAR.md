# Slide 5: Aplicação dos conteúdos da disciplina (explicação)

> Roteiro do slide 5 da apresentação (04/12/2026). Todos os números vêm de uma coleta sintética real do sistema (Tarumã, 24/10/2026). O painel não tem mais aba de laboratório: a explicação de álgebra linear fica só nos slides e no relatório.

Sugestão: dividir em **3 slides visuais** (5A, 5B, 5C) em vez de um só, porque o edital pede *explicação* e o conteúdo não cabe em uma tela.

---

## Slide 5A: Cada coleta é um vetor

**Título:** De três medidas para um índice

- A coleta é um vetor em ℝ³: **x = (pH, turbidez, temperatura) = (8,99 ; 31,11 ; 33,12)**
- Problema: pH, NTU e °C são unidades diferentes e **não podem ser somados**.
- Solução: converter cada medida em **pontos de nota (0 a 10)**, a mesma "moeda" para os três.

**Fala:** "O sensor entrega três números em unidades diferentes. Somar pH com graus não faz sentido. Então precisamos de uma regra que traduza tudo para a mesma escala."

---

## Slide 5B: Transformação linear (matriz e núcleo/imagem)

**Título:** A matriz que converte unidades em pontos

**Passo 1. Desvio d** (quanto saiu da faixa segura):

| Indicador | Faixa segura | Medido | Desvio |
|---|---|---|---|
| pH | 6,5 a 8,5 | 8,99 | 0,49 |
| Turbidez | 0 NTU | 31,11 | 31,11 |
| Temperatura | 20 a 30 °C | 33,12 | 3,12 |

**Passo 2. Penalidade p = A · d**, com a matriz fixa

```
        ┌ 2     0     0   ┐   ┌  0,49 ┐   ┌ 0,97 ┐
  A  =  │ 0    0,2    0   │ · │ 31,11 │ = │ 6,22 │ = p
        └ 0     0    0,5  ┘   └  3,12 ┘   └ 1,56 ┘
```

**Passo 3. Notas n = 10 − p** → (9,03 ; 3,78 ; 8,44)

**O que dizer sobre a matriz:**
- Cada número da diagonal é uma **taxa de câmbio**: 1 de pH vale 2 pontos, 1 NTU vale 0,2 ponto, 1 °C vale 0,5 ponto.
- A matriz é **fixa**: serve para qualquer coleta.
- É **linear**: A(2d) = 2·A(d). Dobrar o desvio dobra a penalidade. (Mostrar com a turbidez: 31,11 → penalidade 6,22; 62,22 → 12,44.)

**Núcleo e imagem de A:**
- **Núcleo(A) = {0}.** Como todos os números da diagonal são diferentes de zero, o único desvio que gera penalidade zero é o desvio zero. Ou seja, nenhum indicador é ignorado.
- **Imagem(A) = ℝ³.** A é invertível (det = 2 · 0,2 · 0,5 = 0,2). Dá para voltar da penalidade ao desvio com A⁻¹ = diag(0,5 ; 5 ; 2).

---

## Slide 5C: Autovalores e produto interno

**Título:** Autovalores da matriz e o cálculo do índice

**Autovalores e diagonalização (aplicados à matriz A):**
- Polinômio característico: **det(A − λI) = (2 − λ)(0,2 − λ)(0,5 − λ)**
- Autovalores: **λ = 2 ; 0,2 ; 0,5** (os próprios pesos de conversão)
- Autovetores: os eixos **e₁ = (1,0,0)**, **e₂ = (0,1,0)**, **e₃ = (0,0,1)** (um por indicador)
- A já é **diagonal**: A = P Λ P⁻¹ com P = I. Interpretação: cada indicador é convertido de forma independente, sem "contaminar" o outro.

**Produto interno (cálculo do índice):**
- Vetor de pesos **w = (⅓ ; ⅓ ; ⅓)**
- **Índice = n · w = (9,03 + 3,78 + 8,44) / 3 = 7,08 → REGULAR**

**Vetor diferença (alertas):**
- Entre duas coletas: **Δ = x_atual − x_anterior**
- Exemplo real (24/10 → 24/11): **Δ = (−2,83 ; −28,83 ; −11,57)**
- Alerta se |Δᵢ| passar de 0,5 (pH), 10 NTU (turbidez) ou 3 °C (temperatura).

---

## O que o código faz de verdade (para a defesa)

| Conceito | Está no código? | Onde |
|---|---|---|
| Vetor da coleta em ℝ³ | Sim | `SistemaAquaMap.receberColeta`, arrays `double[]` |
| Produto escalar (índice) | Sim | `SistemaAquaMap.produtoEscalar` |
| Vetor diferença (anomalias) | Sim | `SistemaAquaMap.analisarVariacao` |
| Matriz A diagonal | **Não literalmente.** O código aplica `10 − 2·d`, `10 − 0,2·t` e `10 − 0,5·d`, que é o mesmo cálculo que a matriz A faz | `pontuarPH`, `pontuarTurbidez`, `pontuarTemperatura` |
| Matriz do histórico (m × 3) | Só é preenchida; **não entra em nenhum cálculo** | `matrizHistorico` |
| Autovalores/autovetores da covariância, Gram-Schmidt, projeção ortogonal | **Não** | proposta de trabalho futuro |

**Frase segura para falar:** "A regra de pontuação é uma transformação linear aplicada ao vetor de desvios. Esta é a matriz dela. No código ela aparece escrita como três funções, com o mesmo resultado."

**Evite dizer:** "o sistema multiplica matrizes", "o sistema calcula autovalores dos dados" ou "usamos Gram-Schmidt".

---

## Perguntas que o professor pode fazer

**1. "Isso é mesmo uma transformação linear?"**
A parte A · d é. Duas partes do cálculo não são lineares: o **desvio** (que depende de estar dentro ou fora da faixa) e o **"10 − p"** (que é um deslocamento, ou seja, uma transformação afim). A matriz A é a parte linear.

**2. "Por que a matriz é diagonal?"**
Porque cada indicador é pontuado sozinho. Os zeros fora da diagonal dizem que o pH não altera a penalidade da turbidez, e assim por diante.

**3. "Quais são os autovalores e o que significam?"**
2, 0,2 e 0,5, que são os pesos de conversão. Como a matriz já é diagonal, os autovalores são os números da diagonal e os autovetores são os eixos canônicos.

**4. "O que seria um uso mais avançado?"**
Montar a matriz m × 3 do histórico, calcular a **matriz de covariância** e seus autovalores (análise de componentes principais) para ver quais indicadores variam juntos. A base (`matrizHistorico`) já existe, mas isso **não foi implementado**.

**5. "O índice é confiável para dizer se a água é potável?"**
Não. São só três parâmetros, o sensor é simulado e a média pode esconder um indicador ruim. Exemplo real: em 24/12/2026 a turbidez foi 16,58 NTU (acima do limite de 5), mas o índice deu 8,77 (BOA). Por isso o painel mostra os alertas separadamente.

---

## Demonstração ao vivo (slides 7 e 8: Produto Final e Resultados)

**Antes de começar:** rode `.\iniciar.cmd`. Se o painel já tiver dados, comece do zero: aba **Sensor** → **Reiniciar dados (guardando backup)**, ou `.\reiniciar.cmd`. Nada é apagado, os dados antigos vão para `data/backup/`. Começar com o painel vazio dá começo à história.

| Passo | O que fazer | O que falar (ligação com a álgebra linear) |
|---|---|---|
| 1 | Mostrar a **Visão Geral** vazia | "Ainda não há coletas: o sistema aguarda a primeira medição." |
| 2 | Aba **Sensor**: responsável `Maria`, bairro `taruma` (igual ao mapa) → **Coletar agora** | "O sensor gera o vetor **x** = (pH, turbidez, temperatura)." |
| 3 | No resultado, mostrar as 3 réguas | "Cada régua é o desvio **d**: quanto o valor saiu da faixa verde. A nota é 10 − **A·d** (slide 5B)." |
| 4 | **Visão Geral**: medidor do índice | "O índice é a média das três notas: produto interno **n · w** com **w** = (⅓ ; ⅓ ; ⅓) (slide 5C)." |
| 5 | Mostrar o aviso "Planilha criada" e abrir `data/Maria/coletas.xlsx` | "Cada coleta vai para CSV e XLSX: é a cópia de segurança dos dados." |
| 6 | **Simular próximo mês** (2 ou 3 vezes) | "Cada clique cria a coleta do mês seguinte, para montar o histórico." |
| 7 | **Visão Geral**: alertas | "**Δ** = **x**atual − **x**anterior. Se |Δᵢ| passa do limite, é variação brusca (vetor diferença)." |
| 8 | **Monitoramento**: pontos cadastrados, gráfico do índice, gráficos com a faixa verde, ranking | "Evolução no tempo, ranking dos fatores e a tabela dos pontos cadastrados." |
| 9 | **Cartilha** | "Linguagem simples para a comunidade; explica como o índice é calculado." |

**Dicas:**
- Os valores são **sorteados**, então cada demonstração dá números diferentes. Tenha o exemplo do slide 5B pronto com números reais (24/10/2026, índice 7,08).
- Se a coleta normal for recusada com "ainda não liberada", é porque existe um agendamento para aquele ponto: use **Simular próximo mês**.
- O bairro precisa ter o mesmo nome de `data/comunidades.csv` (`taruma`, sem acento) para o ponto aparecer no mapa.

## O que o edital pede no painel (Projeto 02) e onde está

| Item do edital | Onde aparece no painel |
|---|---|
| Mapa das comunidades monitoradas | Visão Geral, mapa |
| Gráficos de qualidade da água | Monitoramento: gráficos de pH, turbidez, temperatura e índice |
| Evolução histórica dos indicadores | Monitoramento: gráficos ao longo das coletas e tabela de últimas coletas |
| Ranking de fatores de contaminação | Monitoramento: ranking |
| Alertas ambientais | Visão Geral: alertas |
| Recomendações preventivas | Visão Geral: "O que fazer" em cada alerta |
| Cartilha digital | Cartilha Educacional (conceitos, saúde, como o índice é calculado e a porcentagem de pontos BOA). A porcentagem de pontos adequados e críticos também aparece nos cartões da Visão Geral |

O edital diz que a cartilha "poderá conter" também **gráficos**, **impactos ambientais da contaminação** e **evolução da qualidade da água**. A cartilha do painel ainda não tem esses três itens: se houver tempo, vale acrescentá-los (ou incluí-los na cartilha impressa).
