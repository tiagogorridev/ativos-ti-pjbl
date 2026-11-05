import express, { type NextFunction, type Request, type Response } from "express";
import swaggerUi from "swagger-ui-express";
import type { AlocarAtivo } from "../application/alocacoes/AlocarAtivo.js";
import type { DevolverAtivo } from "../application/alocacoes/DevolverAtivo.js";
import type { ObterDadosAgregados } from "../application/dados-agregados/ObterDadosAgregados.js";
import type { ObterDashboard } from "../application/dashboard/ObterDashboard.js";
import type { EncaminharRequisicao } from "../application/proxy/EncaminharRequisicao.js";
import { ErroDeServico } from "../domain/erros.js";
import { documentoOpenApi } from "./openapi.js";

export interface CasosDeUso {
  obterDadosAgregados: ObterDadosAgregados;
  obterDashboard: ObterDashboard;
  alocarAtivo: AlocarAtivo;
  devolverAtivo: DevolverAtivo;
  encaminharRequisicao: EncaminharRequisicao;
}

/** Monta a aplicação HTTP. Só converte HTTP ⇄ casos de uso; não sabe onde estão os serviços. */
export function criarApp(casos: CasosDeUso): express.Express {
  const app = express();
  app.disable("x-powered-by");
  app.use(express.json());

  app.get("/health", (_req, res) => {
    res.json({ status: "UP" });
  });

  app.get("/api-docs", (_req, res) => {
    res.json(documentoOpenApi);
  });
  app.use("/swagger", swaggerUi.serve, swaggerUi.setup(documentoOpenApi));

  // Endpoint obrigatório da entrega. Também em /api/... para passar pelo API Gateway.
  const dadosAgregados = async (_req: Request, res: Response) => {
    res.json(await casos.obterDadosAgregados.executar());
  };
  app.get("/aggregated-data", dadosAgregados);
  app.get("/api/aggregated-data", dadosAgregados);

  app.get("/api/dashboard", async (_req, res) => {
    res.json(await casos.obterDashboard.executar());
  });

  app.post("/api/alocacoes", async (req, res) => {
    res.status(201).json(await casos.alocarAtivo.executar(req.body ?? {}));
  });

  app.post("/api/alocacoes/:id/devolucao", async (req, res) => {
    const corpo = (req.body ?? {}) as { dataDevolucao?: string };
    res.json(await casos.devolverAtivo.executar(String(req.params.id), corpo.dataDevolucao));
  });

  // Proxy do CRUD: qualquer outra rota /api/... vai para o microsserviço dono do recurso.
  app.use("/api", async (req, res) => {
    const resposta = await casos.encaminharRequisicao.executar(req.method, req.originalUrl, req.body);
    if (resposta.corpo === null) {
      res.status(resposta.status).end();
    } else {
      res.status(resposta.status).json(resposta.corpo);
    }
  });

  app.use((erro: unknown, _req: Request, res: Response, _next: NextFunction) => {
    if (erro instanceof ErroDeServico) {
      const corpo = erro.corpo && typeof erro.corpo === "object" && "error" in erro.corpo ? erro.corpo : { error: erro.message };
      res.status(erro.status).json(corpo);
      return;
    }
    if (erro instanceof SyntaxError) {
      res.status(400).json({ error: "Corpo da requisição inválido. Envie um JSON válido." });
      return;
    }
    console.error("[bff] Erro inesperado", erro);
    res.status(500).json({ error: "Erro interno do servidor." });
  });

  return app;
}
