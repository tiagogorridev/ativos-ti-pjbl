# Azure Functions

Parte do **Sistema de Controle de Ativos de Informática** (PJBL — PUCPR). Duas funções serverless
no plano Flex Consumption (cobrança por execução, sem servidor ligado):

| Função | Gatilho | O que faz |
|---|---|---|
| `calcularDepreciacao` | HTTP `POST /api/depreciacao` | Calcula a depreciação linear dos ativos (vida útil de 5 anos, 20% ao ano) e o valor atual do parque. Chamada pelo BFF no `GET /aggregated-data`. |
| `consumirEventosDeAlocacao` | Fila do Azure Service Bus | Consome os eventos `AtivoAlocado` e `AtivoDevolvido` publicados pelo BFF e registra a trilha de auditoria. |

## Alunos

- Fabio Augusto Tomaselli Visentainer
- Mateus Zanettin Dall'Agnol
- Matheus Marcondes Muller
- Tiago Kasprzak Gorri

## Arquitetura

```
BFF ──HTTP──▶ calcularDepreciacao
BFF ──publica──▶ fila do Service Bus ──gatilho──▶ consumirEventosDeAlocacao ──▶ log de auditoria
```

As regras ficam separadas do Azure:

| Pasta | Conteúdo |
|---|---|
| `src/dominio` | Regras puras: `depreciacao.js` e `auditoria.js`. Não dependem do Azure e são testadas sozinhas. |
| `src/functions` | Adaptadores: cada arquivo registra uma função no Azure e só repassa a entrada para o domínio. |

### Contrato do cálculo

Entrada:

```json
{ "ativos": [ { "numeroPatrimonio": "PAT-0003", "tipo": "Monitor", "status": "Em estoque",
               "dataAquisicao": "2024-01-10", "valorAquisicao": 899 } ] }
```

Saída:

```json
{ "calculadoEm": "2026-11-05T15:00:00.000Z", "valorAquisicaoTotal": 899, "valorAtualTotal": 404.55,
  "depreciacaoTotal": 494.45,
  "itens": [ { "numeroPatrimonio": "PAT-0003", "valorAquisicao": 899, "valorAtual": 404.55,
               "depreciacaoAcumulada": 494.45, "percentualDepreciado": 55, "vidaUtilAnos": 5 } ] }
```

### Contrato do evento

```json
{ "tipo": "AtivoAlocado", "alocacaoId": 10, "numeroPatrimonio": "PAT-0003",
  "equipamento": "LG 24MK430H", "colaborador": "Carlos Nunes", "data": "2026-11-05",
  "ocorridoEm": "2026-11-05T15:00:00.000Z" }
```

## Tecnologias

Node.js, Azure Functions (modelo de programação v4), Azure Service Bus, testes com `node:test`.

## Como rodar localmente

Pré-requisito: [Azure Functions Core Tools v4](https://learn.microsoft.com/azure/azure-functions/functions-run-local).

```bash
npm install
cp local.settings.json.example local.settings.json   # preencha a cadeia de conexão do Service Bus
npm start                                            # http://localhost:7071/api/depreciacao
```

## Testes

```bash
npm test
```

Roda os testes unitários do domínio (`test/dominio.test.js`) e os testes de arquitetura (`test/arquitetura.test.js`), que conferem se `src/dominio` não depende do Azure e se cada arquivo de `src/functions` registra uma única Function.

## Configurações da Function App

| Configuração | Uso |
|---|---|
| `ServiceBusConnection__fullyQualifiedNamespace` | No Azure: endereço do namespace (`<namespace>.servicebus.windows.net`). A Function entra com a identidade gerenciada (papel "Azure Service Bus Data Receiver"), sem senha |
| `ServiceBusConnection` | Só para rodar localmente: cadeia de conexão do Service Bus, no `local.settings.json` (nunca versionado) |
| `SERVICEBUS_FILA` | Nome da fila consumida |
