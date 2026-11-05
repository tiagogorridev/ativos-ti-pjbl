const { app } = require("@azure/functions");
const { calcularDepreciacao } = require("../dominio/depreciacao");

// POST /api/depreciacao  — gatilho HTTP, chamado pelo BFF no GET /aggregated-data.
// Corpo: { "ativos": [ { numeroPatrimonio, tipo, status, dataAquisicao, valorAquisicao } ] }
app.http("calcularDepreciacao", {
  methods: ["POST"],
  authLevel: "function",
  route: "depreciacao",
  handler: async (request, context) => {
    let corpo;
    try {
      corpo = await request.json();
    } catch {
      return { status: 400, jsonBody: { error: "Corpo da requisição inválido. Envie um JSON válido." } };
    }

    try {
      const resultado = calcularDepreciacao(corpo?.ativos);
      context.log(`Depreciação calculada para ${resultado.itens.length} ativos.`);
      return { status: 200, jsonBody: resultado };
    } catch (erro) {
      return { status: 400, jsonBody: { error: erro.message } };
    }
  },
});
