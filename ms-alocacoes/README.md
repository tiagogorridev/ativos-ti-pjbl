# Microsserviço de Alocações (Azure SQL)

Parte do **Sistema de Controle de Ativos de Informática** (PJBL — PUCPR). Este serviço cuida dos
**colaboradores** e das **alocações de ativos** (entrega, devolução e histórico). Os dados ficam no
**Azure SQL Database**.

## Alunos

- Fabio Augusto Tomaselli Visentainer
- Mateus Zanettin Dall'Agnol
- Matheus Marcondes Muller
- Tiago Kasprzak Gorri

## Arquitetura

Clean Architecture em quatro camadas, com as dependências apontando sempre para dentro:

| Camada | Pacote | Conteúdo |
|---|---|---|
| Domain | `domain` | Entidades `Colaborador` e `Alocacao`, regras de negócio, portas `ColaboradorRepository` e `AlocacaoRepository`. Sem Spring e sem banco. |
| Application | `application` | Um caso de uso por arquivo (Vertical Slice), por exemplo `AlocarAtivo`, `DevolverAtivo`, `CadastrarColaborador`. Sem Spring. |
| Infrastructure | `infrastructure` | Adaptadores das portas com SQL parametrizado (Spring `JdbcClient`), migrations Flyway e composição dos casos de uso. |
| API | `api` | Controllers REST, DTOs, tratamento de erros e configuração do Swagger. |

Fatias verticais (uma pasta por funcionalidade, um arquivo por caso de uso):

```
application/
├── colaboradores/   CadastrarColaborador · ListarColaboradores · ObterColaborador
│                    AtualizarColaborador · InativarColaborador
└── alocacoes/       AlocarAtivo · ListarAlocacoes · ObterAlocacao
                     AtualizarAlocacao · DevolverAtivo · ExcluirAlocacao
```

O ativo pertence ao microsserviço de Ativos (MongoDB). Aqui ele é referenciado só pelo número de
patrimônio, sem chave estrangeira entre bancos (Database per Service).

Regras de negócio principais:

- **Um ativo tem no máximo uma alocação vigente.** A regra é garantida pelo próprio banco com um
  índice único filtrado (`uk_alocacao_vigente`), inclusive com duas requisições ao mesmo tempo.
- Colaborador inativo não recebe ativos.
- Colaborador com ativo alocado não pode ser inativado.
- Colaboradores não são apagados: a exclusão vira inativação, para preservar o histórico.
- Alocação encerrada (devolvida) é histórico: não pode ser alterada nem excluída.
- A data de devolução não pode ser anterior à de retirada.

## Tecnologias

Kotlin 2.4, Spring Boot 4.1 (Java 21), Spring JDBC (`JdbcClient`), Flyway, driver Microsoft SQL Server,
springdoc-openapi (Swagger), JUnit 5, MockMvc e ArchUnit, Gradle (Kotlin DSL), Docker.

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/colaboradores` | Lista colaboradores com a quantidade de ativos alocados |
| GET | `/api/colaboradores/{id}` | Consulta um colaborador |
| POST | `/api/colaboradores` | Cadastra um colaborador |
| PUT | `/api/colaboradores/{id}` | Altera um colaborador (parcial), inclusive reativar |
| DELETE | `/api/colaboradores/{id}` | Inativa um colaborador (exclusão lógica) |
| GET | `/api/alocacoes?vigentes=&colaboradorId=` | Lista alocações (abertas, histórico ou todas) |
| GET | `/api/alocacoes/{id}` | Consulta uma alocação |
| POST | `/api/alocacoes` | Aloca um ativo a um colaborador |
| PUT | `/api/alocacoes/{id}` | Corrige uma alocação vigente (parcial) |
| POST | `/api/alocacoes/{id}/devolucao` | Registra a devolução |
| DELETE | `/api/alocacoes/{id}` | Exclui uma alocação vigente lançada por engano |

Documentação interativa: `/swagger-ui.html` · OpenAPI em JSON: `/v3/api-docs`.

## Como rodar localmente (sem custo)

Com Docker (sobe um SQL Server local, cria o banco e o Flyway cria as tabelas com dados de exemplo):

```bash
docker compose up --build
```

Swagger em http://localhost:8083/swagger-ui.html.

Sem Docker para o serviço, apontando para o Azure SQL:

```bash
export DB_URL="jdbc:sqlserver://<servidor>.database.windows.net:1433;databaseName=<banco>;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=60"
export DB_USER="<usuario>"
export DB_PASSWORD="<senha>"
./gradlew bootRun
```

## Testes

```bash
./gradlew test
```

- **Unitários:** casos de uso com repositórios em memória (sem banco), incluindo a regra de uma
  alocação vigente por ativo.
- **HTTP:** controllers com MockMvc, validando o contrato JSON e os códigos de status.
- **Arquitetura (ArchUnit):** dependência entre camadas, domínio sem framework, acesso ao banco só na
  Infrastructure e fatias verticais independentes.

## Custo zero no Azure SQL

O banco usa a oferta gratuita do Azure SQL Database. Para não consumir a cota mensal à toa, o serviço
não mantém conexões abertas quando está ocioso (`minimum-idle: 0`) e o health check não consulta o
banco. Assim o banco gratuito pausa sozinho depois de alguns minutos sem uso. Na primeira chamada
depois de uma pausa, o banco leva cerca de um minuto para acordar.

## Variáveis de ambiente

| Variável | Padrão | Uso |
|---|---|---|
| `PORT` | `8083` | Porta HTTP |
| `DB_URL` | SQL Server local | String JDBC (no Azure SQL, nunca versionar) |
| `DB_USER` | `sa` | Usuário do banco |
| `DB_PASSWORD` | senha local de desenvolvimento | Senha do banco (no Azure SQL, nunca versionar) |
