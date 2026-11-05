import request from "supertest";
import { describe, expect, it } from "vitest";
import { criarApp } from "../../src/api/app.js";
import { AlocarAtivo } from "../../src/application/alocacoes/AlocarAtivo.js";
import { DevolverAtivo } from "../../src/application/alocacoes/DevolverAtivo.js";
import { ObterDadosAgregados } from "../../src/application/dados-agregados/ObterDadosAgregados.js";
import { ObterDashboard } from "../../src/application/dashboard/ObterDashboard.js";
import { EncaminharRequisicao } from "../../src/application/proxy/EncaminharRequisicao.js";
import { AGORA, PublicadorFalso, RegistroSilencioso, ServicoFalso, alocacaoCriada, ativoEmEstoque, indicadores } from "../apoio/ServicoFalso.js";

/** Testes HTTP do BFF com os casos de uso reais e serviços remotos em memória. */
function montar() {
  const ativos = new ServicoFalso("Ativos")
    .responder("GET", "/api/ativos", 200, [ativoEmEstoque])
    .responder("GET", "/api/indicadores", 200, indicadores)
    .responder("GET", "/api/ativos/PAT-0404", 404, { error: "Nenhum ativo encontrado com o patrimônio PAT-0404." })
    .responder("POST", "/api/ativos", 201, ativoEmEstoque);
  const alocacoes = new ServicoFalso("Alocações")
    .responder("GET", "/api/alocacoes?vigentes=true", 200, [alocacaoCriada])
    .responder("GET", "/api/colaboradores", 200, []);
  const calculo = new ServicoFalso("Cálculo").foraDoAr("POST", "/depreciacao");
  const eventos = new PublicadorFalso();
  const registro = new RegistroSilencioso();
  const app = criarApp({
    obterDadosAgregados: new ObterDadosAgregados(ativos, alocacoes, calculo, AGORA),
    obterDashboard: new ObterDashboard(ativos, alocacoes),
    alocarAtivo: new AlocarAtivo(ativos, alocacoes, eventos, registro, AGORA),
    devolverAtivo: new DevolverAtivo(ativos, alocacoes, eventos, registro, AGORA),
    encaminharRequisicao: new EncaminharRequisicao(ativos, alocacoes),
  });
  return { app, ativos };
}

describe("API do BFF", () => {
  it("GET /aggregated-data devolve um único JSON com as três fontes", async () => {
    const resposta = await request(montar().app).get("/aggregated-data");

    expect(resposta.status).toBe(200);
    expect(resposta.body.ativos.itens).toHaveLength(1);
    expect(resposta.body.alocacoes.vigentes).toHaveLength(1);
    expect(resposta.body.fontes.azureFunction.status).toBe("indisponivel");
  });

  it("GET /api/aggregated-data é o mesmo endpoint pelo API Gateway", async () => {
    const resposta = await request(montar().app).get("/api/aggregated-data");
    expect(resposta.status).toBe(200);
  });

  it("repassa o CRUD com o mesmo status e corpo do microsserviço", async () => {
    const { app, ativos } = montar();

    const criado = await request(app).post("/api/ativos").send({ numeroPatrimonio: "PAT-0003" });
    const naoEncontrado = await request(app).get("/api/ativos/PAT-0404");

    expect(criado.status).toBe(201);
    expect(ativos.chamadas.find((c) => c.metodo === "POST")?.corpo).toEqual({ numeroPatrimonio: "PAT-0003" });
    expect(naoEncontrado.status).toBe(404);
    expect(naoEncontrado.body.error).toBe("Nenhum ativo encontrado com o patrimônio PAT-0404.");
  });

  it("devolve 400 para JSON inválido", async () => {
    const resposta = await request(montar().app)
      .post("/api/alocacoes")
      .set("Content-Type", "application/json")
      .send("{ invalido");
    expect(resposta.status).toBe(400);
    expect(resposta.body.error).toBeDefined();
  });

  it("expõe a documentação OpenAPI e o Swagger UI", async () => {
    const { app } = montar();

    const docs = await request(app).get("/api-docs");
    const swagger = await request(app).get("/swagger/");

    expect(docs.status).toBe(200);
    expect(Object.keys(docs.body.paths)).toContain("/aggregated-data");
    expect(swagger.status).toBe(200);
  });
});
