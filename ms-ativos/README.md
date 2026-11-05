# Microsserviço de Ativos (MongoDB)

Parte do **Sistema de Controle de Ativos de Informática** (PJBL — PUCPR). Este serviço cuida do
**ciclo de vida dos ativos de TI** (cadastro, status, baixa) e das **manutenções e seus custos**.
Os dados ficam no **MongoDB Atlas**.

## Alunos

- Fabio Augusto Tomaselli Visentainer
- Mateus Zanettin Dall'Agnol
- Matheus Marcondes Muller
- Tiago Kasprzak Gorri

## Arquitetura

Clean Architecture em quatro camadas, com as dependências apontando sempre para dentro:

| Camada | Pacote | Conteúdo |
|---|---|---|
| Domain | `domain` | Entidades `Ativo` e `Manutencao`, regras do ciclo de vida, portas `AtivoRepository` e `ManutencaoRepository`. Sem Spring e sem MongoDB. |
| Application | `application` | Um caso de uso por arquivo (Vertical Slice), por exemplo `CriarAtivo`, `ListarAtivos`, `RegistrarManutencao`. Sem Spring. |
| Infrastructure | `infrastructure` | Adaptadores das portas com o driver oficial do MongoDB e a composição dos casos de uso. |
| API | `api` | Controllers REST, DTOs, tratamento de erros e configuração do Swagger. |

Fatias verticais (uma pasta por funcionalidade, um arquivo por caso de uso):

```
application/
├── ativos/        CriarAtivo · ListarAtivos · ObterAtivo · AtualizarAtivo · ExcluirAtivo
│                  RegistrarUsoDoAtivo · RegistrarDevolucaoDoAtivo
├── manutencoes/   RegistrarManutencao · ListarManutencoes · ObterManutencao
│                  AtualizarManutencao · ExcluirManutencao
└── indicadores/   ConsultarIndicadores
```

Regras de negócio principais:

- O número de patrimônio é único.
- O status "Em uso" só é atribuído pela alocação (serviço de Alocações), nunca pelo cadastro.
- Ativo alocado não muda de status nem é excluído antes da devolução.
- Ativo baixado não muda mais de status e não recebe manutenção.
- Ativo com manutenção registrada não é excluído; o caminho é a baixa.

## Tecnologias

Kotlin 2.4, Spring Boot 4.1 (Java 21), driver MongoDB, springdoc-openapi (Swagger), JUnit 5,
MockMvc e ArchUnit, Gradle (Kotlin DSL), Docker.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/ativos?busca=&status=` | Pesquisa ativos |
| GET | `/api/ativos/{numeroPatrimonio}` | Consulta um ativo |
| POST | `/api/ativos` | Cadastra um ativo |
| PUT | `/api/ativos/{numeroPatrimonio}` | Altera um ativo (parcial), inclusive status e baixa |
| DELETE | `/api/ativos/{numeroPatrimonio}` | Exclui um ativo sem histórico |
| PUT | `/api/ativos/{numeroPatrimonio}/uso` | Marca o ativo como "Em uso" (usado pelo serviço de Alocações) |
| DELETE | `/api/ativos/{numeroPatrimonio}/uso` | Devolve o ativo ao estoque (usado pelo serviço de Alocações) |
| GET | `/api/manutencoes?numeroPatrimonio=` | Histórico de manutenções |
| GET | `/api/manutencoes/{id}` | Consulta uma manutenção |
| POST | `/api/manutencoes` | Registra uma manutenção |
| PUT | `/api/manutencoes/{id}` | Corrige uma manutenção (parcial) |
| DELETE | `/api/manutencoes/{id}` | Exclui uma manutenção |
| GET | `/api/indicadores` | Totais por status e setor e custo de manutenção por mês |

Documentação interativa: `/swagger-ui.html` · OpenAPI em JSON: `/v3/api-docs`.

## Como rodar localmente (sem custo)

Com Docker (sobe um MongoDB local com dados de exemplo):

```bash
docker compose up --build
```

Swagger em http://localhost:8082/swagger-ui.html.

Sem Docker para o serviço, apontando para o MongoDB Atlas:

```bash
export MONGO_URI="mongodb+srv://<usuario>:<senha>@<cluster>/"
export MONGO_DB_NAME="ativos_ti"
./gradlew bootRun
```

## Testes

```bash
./gradlew test
```

- **Unitários:** regras do domínio e casos de uso com repositórios em memória (sem banco).
- **HTTP:** controllers com MockMvc, validando o contrato JSON e os códigos de status.
- **Arquitetura (ArchUnit):** dependência entre camadas, domínio sem framework, MongoDB só na
  Infrastructure e fatias verticais independentes.

## Variáveis de ambiente

| Variável | Padrão | Uso |
|---|---|---|
| `PORT` | `8082` | Porta HTTP |
| `MONGO_URI` | `mongodb://localhost:27017` | String de conexão (no Atlas, nunca versionar) |
| `MONGO_DB_NAME` | `ativos_ti` | Nome do banco |
| `SEED_ENABLED` | `false` | Carrega dados de exemplo se as coleções estiverem vazias |
