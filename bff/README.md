# BFF — Backend for Frontend

Parte do **Sistema de Controle de Ativos de Informática** (PJBL — PUCPR). O BFF é a única porta de
entrada do frontend: ele **agrega** os dados dos microsserviços e da Azure Function e **repassa** as
operações de CRUD a cada microsserviço. O frontend nunca chama os microsserviços diretamente.

## Alunos

- Fabio Augusto Tomaselli Visentainer
- Mateus Zanettin Dall'Agnol
- Matheus Marcondes Muller
- Tiago Kasprzak Gorri

## Arquitetura

```
Frontend → API Gateway → BFF ─┬─→ Microsserviço de Ativos (MongoDB Atlas)
                              ├─→ Microsserviço de Alocações (Azure SQL)
                              ├─→ Azure Function de cálculo (HTTP)
                              └─→ Azure Service Bus (eventos) → Azure Function consumidora
```

Clean Architecture em quatro camadas, com as dependências apontando sempre para dentro:

| Camada | Pasta | Conteúdo |
|---|---|---|
| Domain | `src/domain` | Tipos dos dados, portas (`ServicoHttp`, `PublicadorDeEventos`), erros e regras puras. Sem bibliotecas externas. |
| Application | `src/application` | Um caso de uso por pasta (Vertical Slice). Sem Express e sem Azure. |
| Infrastructure | `src/infrastructure` | Cliente HTTP (fetch), publicador do Azure Service Bus, configuração. |
| API | `src/api` | Rotas Express, tratamento de erros e documentação OpenAPI/Swagger. |

Fatias verticais:

```
src/application/
├── dados-agregados/   ObterDadosAgregados   GET /aggregated-data
├── dashboard/         ObterDashboard        GET /api/dashboard
├── alocacoes/         AlocarAtivo           POST /api/alocacoes
│                      DevolverAtivo         POST /api/alocacoes/{id}/devolucao
└── proxy/             EncaminharRequisicao  demais rotas /api/** (CRUD)
```

### `GET /aggregated-data`

Consulta em paralelo o microsserviço de Ativos (ativos e indicadores) e o de Alocações (alocações
vigentes e colaboradores). Em seguida envia os ativos para a Azure Function, que calcula a
depreciação. Devolve tudo em um único JSON. Se uma fonte falhar, as outras continuam na resposta e o
campo `fontes` indica qual ficou indisponível.

### Alocação e devolução com eventos

`POST /api/alocacoes` confere no serviço de Ativos se o ativo está "Em estoque", grava a alocação no
serviço de Alocações e marca o ativo como "Em uso". Se essa última etapa falhar, desfaz a alocação.
No fim, publica o evento **AtivoAlocado** na fila do Azure Service Bus. A devolução faz o caminho
inverso e publica **AtivoDevolvido**.

## Tecnologias

Node.js 22, TypeScript, Express 5, swagger-ui-express, Azure Service Bus SDK, Vitest e Supertest,
Docker.

## Endpoints

| Método | Rota | Destino |
|---|---|---|
| GET | `/aggregated-data` (ou `/api/aggregated-data`) | Ativos + Alocações + Function |
| GET | `/api/dashboard` | Ativos + Alocações |
| POST | `/api/alocacoes` | Orquestração Ativos + Alocações + Service Bus |
| POST | `/api/alocacoes/{id}/devolucao` | Orquestração Ativos + Alocações + Service Bus |
| * | `/api/ativos/**`, `/api/manutencoes/**`, `/api/indicadores` | Microsserviço de Ativos |
| * | `/api/colaboradores/**`, `/api/alocacoes/**` | Microsserviço de Alocações |
| GET | `/swagger` · `/api-docs` | Documentação |
| GET | `/health` | Verificação de saúde |

## Como rodar localmente

```bash
npm install
cp .env.example .env     # ajuste os endereços dos serviços, se precisar
npm run build
node --env-file=.env dist/main.js
```

Swagger em http://localhost:8084/swagger.

Com Docker:

```bash
docker build -t bff .
docker run -p 8084:8084 --env-file .env bff
```

## Testes

```bash
npm test
```

- **Unitários:** casos de uso com serviços remotos em memória (agregação, falha parcial, orquestração
  da alocação com compensação, publicação de eventos, proxy).
- **HTTP:** rotas com Supertest (contrato, status e Swagger).
- **Arquitetura:** leem os imports de `src/` e falham se uma camada depender de outra mais externa,
  se Domain ou Application usarem bibliotecas externas ou se uma fatia vertical depender de outra.

## Variáveis de ambiente

| Variável | Padrão | Uso |
|---|---|---|
| `PORT` | `8084` | Porta HTTP |
| `ATIVOS_URL` | `http://localhost:8082` | Microsserviço de Ativos |
| `ALOCACOES_URL` | `http://localhost:8083` | Microsserviço de Alocações |
| `FUNCTION_URL` | `http://localhost:7071/api` | Azure Function de cálculo |
| `FUNCTION_KEY` | — | Chave da Function, se ela exigir |
| `SERVICEBUS_CONNECTION_STRING` | — | Cadeia de conexão do Service Bus (sem ela, os eventos só vão para o log) |
| `SERVICEBUS_FILA` | `fila-alocacoes` | Nome da fila |
| `TIMEOUT_MS` | `90000` | Tempo limite das chamadas (os serviços desligam quando ociosos) |
