# Entrega PJBL — Sistema de Controle de Ativos de Informática

## Alunos do grupo

- Fabio Augusto Tomaselli Visentainer
- Mateus Zanettin Dall'Agnol
- Matheus Marcondes Muller
- Tiago Kasprzak Gorri

## Link do repositório GitHub

https://github.com/tiagogorridev/ativos-ti-pjbl

## Link do site (Azure Static Web Apps)

https://kind-bush-0c7a5e010.5.azurestaticapps.net

## Azure Function (mock backend)

A Azure Function fica integrada ao Static Web App, publicada sob `/api` na
mesma URL do site (ex.: `https://<site>.azurestaticapps.net/api/...`).
Endpoints de dados mock, todos **GET**:

| Rota | Descrição |
| --- | --- |
| `GET /api/dashboard` | Indicadores gerenciais consolidados (total de ativos, em uso/estoque/manutenção/baixados, custo de manutenção acumulado, ativos por setor, custo de manutenção por mês, últimas movimentações) |
| `GET /api/colaboradores` | Colaboradores, com contagem de ativos alocados |
| `GET /api/alocacoes` | Alocações ativas |
| `GET /api/manutencoes` | Histórico de manutenções |
| `GET /api/usuarios` | Usuários do sistema e perfis |

Código-fonte de cada rota em `api/src/functions/`, dados mock isolados em
`api/src/data/` (ver [`README.md`](./README.md) para detalhes de stack e
estrutura do repositório).

## MongoDB Atlas e as 4 Azure Functions de CRUD (Ativos)

Entrega complementar do PJBL: a entidade **Ativos** passou a ser 100% real,
persistida num banco **MongoDB Atlas**, através de 4 Azure Functions
dedicadas (pesquisar, inserir, alterar e excluir). O frontend criado
anteriormente (tela `/ativos`) executa as 4 Functions: busca/filtra a
listagem, cria um novo ativo pelo botão "Novo Ativo", edita um ativo
existente e exclui um ativo — tudo persistido de fato no banco, sem mock.

| Rota | Método | Descrição |
| --- | --- | --- |
| `/api/ativos` | `GET` | Pesquisar ativos (aceita `?busca=` e `?status=` como query params) |
| `/api/ativos` | `POST` | Inserir um novo ativo |
| `/api/ativos/{numeroPatrimonio}` | `PUT` | Alterar campos de um ativo existente |
| `/api/ativos/{numeroPatrimonio}` | `DELETE` | Excluir um ativo |

Código-fonte: `api/src/functions/ativos.js` (pesquisar),
`ativosInserir.js`, `ativosAlterar.js`, `ativosExcluir.js`, e a conexão
compartilhada com o MongoDB em `api/src/lib/mongo.js`. Script de carga
inicial dos dados de exemplo: `api/scripts/seed-mongo.js`.

Tutorial completo (conta no MongoDB Student Pack ou tier gratuito M0,
criação do cluster no Atlas, usuário do banco, liberação de acesso de rede,
string de conexão, seed, configuração das variáveis de ambiente no Azure e
teste das 4 Functions):
https://claude.ai/code/artifact/212864a6-93e0-4b02-b3f2-f67ae3bd9875

### Evidências da entrega

- **Criação do banco de dados MongoDB:** print do cluster criado no MongoDB
  Atlas (Database Deployments) e da collection `ativos` populada — passo 5
  do tutorial acima.
- **Criação das 4 Azure Functions:** print do log de build/deploy do Azure
  Static Web Apps mostrando as 4 Functions publicadas, ou chamada via
  `curl`/Postman a cada uma das 4 rotas — passo 7 do tutorial acima.
- **Frontend executando as 4 Azure Functions:** print (ou gravação) da tela
  `/ativos` no site publicado, mostrando pesquisa, inserção, edição e
  exclusão de um ativo refletidas na tabela — passo 7 do tutorial acima.

*(Os prints/gravações de evidência ficam a cargo do grupo, feitos após a
configuração do MongoDB Atlas em `https://cloud.mongodb.com`, que é uma
etapa manual — só quem tem a conta consegue criá-la.)*

## Mock com Apidog

Os mesmos 6 endpoints também estão publicados como mock no Apidog (com
"Exemplo de Resposta Primeiro" como prioridade, garantindo que os dados
reais do projeto sejam retornados em vez de dados aleatórios):

- **URL base:** `https://mock.apidog.com/m1/1367360-1371779-default`

| Rota completa | Equivalente na Azure Function |
| --- | --- |
| `https://mock.apidog.com/m1/1367360-1371779-default/ativos` | `GET /api/ativos` |
| `https://mock.apidog.com/m1/1367360-1371779-default/colaboradores` | `GET /api/colaboradores` |
| `https://mock.apidog.com/m1/1367360-1371779-default/alocacoes` | `GET /api/alocacoes` |
| `https://mock.apidog.com/m1/1367360-1371779-default/manutencoes` | `GET /api/manutencoes` |
| `https://mock.apidog.com/m1/1367360-1371779-default/usuarios` | `GET /api/usuarios` |
| `https://mock.apidog.com/m1/1367360-1371779-default/dashboard` | `GET /api/dashboard` |

Passo a passo de como os mocks foram configurados no Apidog:
[`docs/apidog-import.md`](./docs/apidog-import.md).
