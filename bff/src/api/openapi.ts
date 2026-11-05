// Documentação OpenAPI 3 do BFF, exibida pelo Swagger UI em /swagger.

const erro = {
  description: "Erro (mesmo formato dos microsserviços)",
  content: {
    "application/json": {
      schema: {
        type: "object",
        properties: { error: { type: "string" }, campo: { type: "string", nullable: true } },
      },
    },
  },
};

const json = (descricao: string, exemplo?: unknown) => ({
  description: descricao,
  content: { "application/json": { schema: { type: "object" }, ...(exemplo ? { example: exemplo } : {}) } },
});

function proxy(tag: string, recurso: string, comId: string) {
  return {
    [`/api/${recurso}`]: {
      get: { tags: [tag], summary: `Lista ${recurso} (repassado ao microsserviço)`, responses: { "200": json("Lista") } },
      post: {
        tags: [tag],
        summary: `Cadastra (repassado ao microsserviço)`,
        requestBody: { content: { "application/json": { schema: { type: "object" } } } },
        responses: { "201": json("Criado"), "400": erro, "409": erro },
      },
    },
    [`/api/${recurso}/{${comId}}`]: {
      parameters: [{ name: comId, in: "path", required: true, schema: { type: "string" } }],
      get: { tags: [tag], summary: "Consulta (repassado ao microsserviço)", responses: { "200": json("Item"), "404": erro } },
      put: {
        tags: [tag],
        summary: "Altera (repassado ao microsserviço)",
        requestBody: { content: { "application/json": { schema: { type: "object" } } } },
        responses: { "200": json("Alterado"), "404": erro, "409": erro },
      },
      delete: { tags: [tag], summary: "Exclui (repassado ao microsserviço)", responses: { "200": json("Excluído"), "409": erro } },
    },
  };
}

export const documentoOpenApi = {
  openapi: "3.0.3",
  info: {
    title: "BFF — Sistema de Controle de Ativos de Informática",
    version: "1.0.0",
    description:
      "Backend for Frontend: única porta de entrada do frontend. Agrega o microsserviço de Ativos (MongoDB), " +
      "o de Alocações (Azure SQL) e a Azure Function de cálculo, e repassa o CRUD a cada microsserviço.",
  },
  tags: [
    { name: "Agregação", description: "Respostas montadas a partir de vários serviços" },
    { name: "Alocações", description: "Orquestração entre os dois microsserviços, com publicação de eventos" },
    { name: "Ativos", description: "Repassado ao microsserviço de Ativos (MongoDB)" },
    { name: "Manutenções", description: "Repassado ao microsserviço de Ativos (MongoDB)" },
    { name: "Colaboradores", description: "Repassado ao microsserviço de Alocações (Azure SQL)" },
  ],
  paths: {
    "/aggregated-data": {
      get: {
        tags: ["Agregação"],
        summary: "Dados agregados dos dois microsserviços e da Azure Function",
        description:
          "Consulta o microsserviço de Ativos, o de Alocações e a Function de depreciação e devolve tudo em um único JSON. " +
          "Se uma fonte falhar, as demais continuam na resposta e o campo 'fontes' indica qual faltou.",
        responses: { "200": json("Dados agregados") },
      },
    },
    "/api/dashboard": {
      get: { tags: ["Agregação"], summary: "Indicadores e movimentações recentes para a tela de Dashboard", responses: { "200": json("Dashboard") } },
    },
    "/api/alocacoes": {
      get: {
        tags: ["Alocações"],
        summary: "Lista alocações (repassado ao microsserviço de Alocações)",
        parameters: [
          { name: "vigentes", in: "query", schema: { type: "boolean" } },
          { name: "colaboradorId", in: "query", schema: { type: "integer" } },
        ],
        responses: { "200": json("Lista") },
      },
      post: {
        tags: ["Alocações"],
        summary: "Aloca um ativo",
        description:
          "Confere o ativo no serviço de Ativos, grava a alocação no serviço de Alocações, marca o ativo como 'Em uso' " +
          "e publica o evento AtivoAlocado no Azure Service Bus.",
        requestBody: {
          content: {
            "application/json": {
              schema: {
                type: "object",
                required: ["numeroPatrimonio", "colaboradorId"],
                properties: {
                  numeroPatrimonio: { type: "string", example: "PAT-0003" },
                  colaboradorId: { type: "integer", example: 2 },
                  dataRetirada: { type: "string", format: "date" },
                },
              },
            },
          },
        },
        responses: { "201": json("Alocação criada"), "400": erro, "404": erro, "409": erro, "503": erro },
      },
    },
    "/api/alocacoes/{id}/devolucao": {
      post: {
        tags: ["Alocações"],
        summary: "Registra a devolução",
        description: "Encerra a alocação, devolve o ativo ao estoque e publica o evento AtivoDevolvido.",
        parameters: [{ name: "id", in: "path", required: true, schema: { type: "integer" } }],
        requestBody: { content: { "application/json": { schema: { type: "object", properties: { dataDevolucao: { type: "string", format: "date" } } } } } },
        responses: { "200": json("Alocação encerrada"), "404": erro, "409": erro },
      },
    },
    ...proxy("Ativos", "ativos", "numeroPatrimonio"),
    ...proxy("Manutenções", "manutencoes", "id"),
    ...proxy("Colaboradores", "colaboradores", "id"),
    "/api/indicadores": {
      get: { tags: ["Ativos"], summary: "Indicadores do parque (repassado ao microsserviço de Ativos)", responses: { "200": json("Indicadores") } },
    },
  },
};
