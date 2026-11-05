# Sistema de Controle de Ativos de Informática — Projeto PJBL

Frontend completo em Next.js (login mock + 7 telas) que se comunica com
Azure Functions (mock) para gerenciar o parque de ativos de TI de uma
empresa (notebooks, monitores, periféricos etc.). Projeto desenvolvido para
a disciplina de PJBL, replicando o protótipo funcional do sistema.

- **Repositório GitHub:** https://github.com/tiagogorridev/ativos-ti-pjbl
- **Site publicado (Azure Static Web Apps):** https://kind-bush-0c7a5e010.5.azurestaticapps.net
- **Grupo:** ver [`GRUPO.md`](./GRUPO.md)
- **Prompt de IA utilizado para gerar o frontend:** ver [`Prompt.md`](./Prompt.md)
- **Como importar os mocks no Apidog:** ver [`docs/apidog-import.md`](./docs/apidog-import.md)
- **Tutorial — deploy no Azure Static Web Apps:** https://claude.ai/code/artifact/2569a598-d524-4734-9045-0979df5d6f67
- **Tutorial — MongoDB Atlas + as 4 Azure Functions de CRUD:** https://claude.ai/code/artifact/212864a6-93e0-4b02-b3f2-f67ae3bd9875

## Funcionalidades / telas

1. **Login (SSO mock)** — tela inicial simulando o login corporativo via
   Microsoft Entra ID (RF05). Não há senha: basta clicar em "Entrar com
   conta corporativa".
2. **Dashboard** (`/`) — indicadores gerenciais: total de ativos, em uso,
   em manutenção, custo de manutenção acumulado, distribuição por status,
   custo de manutenção por mês e últimas movimentações (RF35).
3. **Ativos** (`/ativos`) — listagem com busca, filtro por status e
   paginação (RF13/RF17). Também recebe o resultado do modal "Ler código".
4. **Colaboradores** (`/colaboradores`) — cadastro de colaboradores com
   contagem de ativos alocados (RF09/RF12).
5. **Alocações** (`/alocacoes`) — alocações ativas, com ação de devolução
   simulada na sessão do navegador (RF21/RF22/RF25).
6. **Manutenções** (`/manutencoes`) — histórico de manutenções, custo total
   e custo médio por serviço (RF27-RF31).
7. **Relatórios** (`/relatorios`) — relatórios de ativos por status, ativos
   por setor e custo de manutenção por tipo/período, com exportação em CSV
   (RF32-RF36).
8. **Usuários e Permissões** (`/usuarios`) — usuários do sistema e matriz de
   permissões por perfil (RF06/RF07).

O botão **"Ler código"** no topo abre um modal para digitar um número de
patrimônio manualmente (RF19/RF20/RF26). A leitura via câmera do
dispositivo (`getUserMedia`) é o próximo passo natural de evolução — não foi
implementada aqui porque exige teste em dispositivo real com HTTPS.

## Stack

- **Frontend:** Next.js 16 (App Router) + **TypeScript** + Tailwind CSS,
  exportado como site estático (`output: 'export'`) para publicação no
  Azure Static Web Apps. Ícones via `lucide-react`. Tipos compartilhados em
  `frontend/lib/types.ts` (`Ativo`, `Colaborador`, `Alocacao`, `Manutencao`,
  `Usuario`, `DashboardData`).
- **Backend:** Azure Functions (Node.js, modelo de programação v4). Cinco
  rotas (`/dashboard`, `/colaboradores`, `/alocacoes`, `/manutencoes`,
  `/usuarios`) continuam servindo dados mock isolados em arquivos JSON
  (`api/src/data/`) — ver [`docs/apidog-import.md`](./docs/apidog-import.md)
  para usar o Apidog no lugar da Function local, sem alterar código. A
  entidade **Ativos** já é 100% real: as 4 Functions de CRUD
  (pesquisar/inserir/alterar/excluir) leem e gravam num banco **MongoDB
  Atlas**, via `api/src/lib/mongo.js` — ver o tutorial de configuração
  linkado no topo deste README.

## Endpoints disponíveis

Mock (dados em JSON, apenas leitura):

| Rota                 | Descrição                                              |
| --------------------- | ------------------------------------------------------- |
| `GET /api/dashboard`  | Indicadores consolidados (calculado a partir dos outros) |
| `GET /api/colaboradores` | Colaboradores + contagem de ativos alocados          |
| `GET /api/alocacoes`  | Alocações ativas                                        |
| `GET /api/manutencoes`| Histórico de manutenções                                |
| `GET /api/usuarios`   | Usuários do sistema e perfis                            |

Ativos (CRUD real, persistido no MongoDB Atlas):

| Rota                              | Descrição                                    |
| ---------------------------------- | ---------------------------------------------- |
| `GET /api/ativos`                  | Pesquisar ativos (aceita `?busca=` e `?status=`) |
| `POST /api/ativos`                 | Inserir um novo ativo                         |
| `PUT /api/ativos/{numeroPatrimonio}`  | Alterar campos de um ativo existente       |
| `DELETE /api/ativos/{numeroPatrimonio}` | Excluir um ativo                         |

Código-fonte de cada rota em `api/src/functions/` (`ativos.js`,
`ativosInserir.js`, `ativosAlterar.js`, `ativosExcluir.js`) e a conexão
compartilhada com o MongoDB em `api/src/lib/mongo.js`.

## Estrutura do repositório

```
ativos-ti-pjbl/
├── frontend/                     # aplicação Next.js (TypeScript)
│   ├── app/                       # rotas: /, /ativos, /colaboradores, /alocacoes,
│   │                               #   /manutencoes, /relatorios, /usuarios
│   ├── components/
│   │   ├── ui/                    # primitivos: Button, Card, Badge, Input,
│   │   │                          #   Select, Table, Pagination
│   │   ├── layout/                # Sidebar, Topbar, AppShell, LoginScreen, AuthGate
│   │   ├── common/                # PageHeader, InfoBanner, StatCard, DataState,
│   │   │                          #   BarcodeModal
│   │   └── <feature>/             # tabelas e filtros específicos de cada tela
│   │       (ativos, colaboradores, alocacoes, manutencoes, relatorios, usuarios,
│   │        dashboard)
│   └── lib/                       # api.ts, types.ts, format.ts, csv.ts, AuthContext.tsx
├── api/                          # Azure Functions (mock)
│   ├── src/functions/             # lógica de cada rota GET
│   └── src/data/                  # dados mock (JSON), isolados da lógica
├── docs/apidog-import.md         # como usar o Apidog no lugar da Function local
├── .github/workflows/            # pipeline de deploy para o Azure Static Web Apps
├── GRUPO.md
├── Prompt.md
└── README.md
```

A UI segue uma componentização em camadas: `components/ui` tem apenas
primitivos visuais sem lógica de negócio; `components/layout` monta a
casca da aplicação (sidebar, topbar, login); `components/common` reúne
peças reaproveitadas por várias telas; e cada pasta `components/<feature>`
contém só o que é específico daquela tela (tabela, filtros). As páginas em
`app/**/page.tsx` ficam curtas — apenas buscam os dados da API e compõem
esses componentes.

## Rodando localmente

### Pré-requisitos

- Node.js 18+
- [Azure Functions Core Tools v4](https://learn.microsoft.com/azure/azure-functions/functions-run-local)
  (`npm install -g azure-functions-core-tools@4 --unsafe-perm true`) — apenas
  se for rodar a Azure Function localmente.

### 1. Backend (Azure Functions)

```bash
cd api
npm install
cp local.settings.json.example local.settings.json
npm start        # inicia em http://localhost:7071
```

As rotas de **Ativos** (pesquisar/inserir/alterar/excluir) precisam de um
banco MongoDB Atlas configurado — preencha `MONGO_URI` e `MONGO_DB_NAME` em
`api/local.settings.json` (ver o tutorial completo linkado no topo deste
README). Com a `MONGO_URI` configurada, popule o banco uma única vez com os
dados de exemplo:

```bash
npm run seed      # roda api/scripts/seed-mongo.js
```

### 2. Frontend

```bash
cd frontend
npm install
npm run dev       # inicia em http://localhost:3000
```

Por padrão o frontend chama `/api/...` (caminho relativo). Para apontar o
frontend, em desenvolvimento, diretamente para a Function local ou para o
mock do Apidog, crie um arquivo `frontend/.env.local`:

```bash
NEXT_PUBLIC_API_BASE_URL=http://localhost:7071/api
# ou, usando Apidog como mock (ver docs/apidog-import.md):
# NEXT_PUBLIC_API_BASE_URL=<< URL do mock no Apidog >>
```

> Alternativa recomendada para rodar frontend + API juntos localmente, do
> jeito mais parecido com a produção: usar a
> [Azure Static Web Apps CLI](https://learn.microsoft.com/azure/static-web-apps/local-development)
> (`swa start frontend/out --api-location api`), que já faz o proxy de
> `/api` para a Function local.

## Deploy no Azure Static Web Apps

1. Suba este repositório para o GitHub (branch `main`).
2. No [Portal do Azure](https://portal.azure.com), crie um recurso **Static
   Web App**, conectado a este repositório/branch, com:
   - **App location:** `/frontend`
   - **Api location:** `/api`
   - **Output location:** `out`
3. O Azure cria automaticamente um workflow do GitHub Actions e o segredo
   `AZURE_STATIC_WEB_APPS_API_TOKEN` no repositório — o workflow já incluso
   em [`.github/workflows/azure-static-web-apps.yml`](./.github/workflows/azure-static-web-apps.yml)
   usa esse segredo.
4. Após o primeiro deploy, copie a URL gerada (ex.:
   `https://<nome-gerado>.azurestaticapps.net`) e atualize:
   - o topo deste `README.md`;
   - a entrega no AVA, junto com o link do repositório do GitHub.
5. Nas configurações do recurso Static Web App, em **Configuration →
   Environment variables**, adicione `MONGO_URI` e `MONGO_DB_NAME` (ambiente
   Production) para que as 4 Functions de Ativos consigam se conectar ao
   MongoDB Atlas em produção — passo a passo completo no tutorial de MongoDB
   linkado no topo deste README.

## Mock com Apidog

Este projeto usa o Apidog como origem dos mocks (ver passo a passo completo
em [`docs/apidog-import.md`](./docs/apidog-import.md)). Os 6 endpoints GET
(`/ativos`, `/colaboradores`, `/alocacoes`, `/manutencoes`, `/usuarios`,
`/dashboard`) já estão publicados como mock no Apidog:

- **URL base do mock no Apidog:** `https://mock.apidog.com/m1/1367360-1371779-default`

Para o frontend consumir o Apidog em vez da Azure Function local, crie
`frontend/.env.local` com:

```bash
NEXT_PUBLIC_API_BASE_URL=https://mock.apidog.com/m1/1367360-1371779-default
```
