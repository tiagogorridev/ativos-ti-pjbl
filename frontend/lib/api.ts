// Camada de comunicação do frontend com o backend.
//
// O frontend conversa só com o BFF, sempre passando pelo API Gateway:
//   Frontend → API Gateway → BFF → microsserviços / Azure Function
//
// Em produção, NEXT_PUBLIC_API_BASE_URL aponta para o gateway (ex.:
// https://<gateway>/api) e NEXT_PUBLIC_API_KEY leva a chave exigida por ele
// no cabeçalho X-API-Key. Em desenvolvimento, o padrão "/api" funciona com o
// modo proxy do Next.js (npm run dev:backend), que encaminha /api/* ao gateway.
import type {
  Ativo,
  AtivoEditavel,
  NovoAtivo,
  Colaborador,
  Alocacao,
  Manutencao,
  DashboardData,
} from "./types";

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "/api";
const API_KEY = process.env.NEXT_PUBLIC_API_KEY;

/** Cabeçalhos comuns: a chave do API Gateway vai em todas as chamadas, quando configurada. */
function cabecalhos(extra: Record<string, string> = {}): Record<string, string> {
  return { Accept: "application/json", ...(API_KEY ? { "X-API-Key": API_KEY } : {}), ...extra };
}

async function parseErro(response: Response): Promise<string> {
  try {
    const corpo = await response.json();
    if (corpo && typeof corpo.error === "string") return corpo.error;
  } catch {
    // resposta sem corpo JSON — usa a mensagem padrão abaixo
  }
  return `${response.status} ${response.statusText}`;
}

async function get<T>(path: string): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: "GET",
    headers: cabecalhos(),
  });

  if (!response.ok) {
    throw new Error(`Falha ao consultar ${path}: ${await parseErro(response)}`);
  }

  return response.json() as Promise<T>;
}

/** POST/PUT/DELETE com corpo JSON opcional — usado pelas mutações de Ativos. */
async function send<T>(
  method: "POST" | "PUT" | "DELETE",
  path: string,
  body?: unknown
): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method,
    headers: cabecalhos({ "Content-Type": "application/json" }),
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    throw new Error(`Falha ao ${method} ${path}: ${await parseErro(response)}`);
  }

  return response.json() as Promise<T>;
}

/** GET /api/dashboard — indicadores gerenciais consolidados (RF35). */
export function getDashboard(): Promise<DashboardData> {
  return get<DashboardData>("/dashboard");
}

/** GET /api/ativos — pesquisa ativos de TI cadastrados no MongoDB (RF13/RF17). */
export function getAtivos(): Promise<Ativo[]> {
  return get<Ativo[]>("/ativos");
}

/** POST /api/ativos — insere um novo ativo no MongoDB (RF13). */
export function inserirAtivo(novoAtivo: NovoAtivo): Promise<Ativo> {
  return send<Ativo>("POST", "/ativos", novoAtivo);
}

/** PUT /api/ativos/{numeroPatrimonio} — altera um ativo existente no MongoDB (RF13). */
export function alterarAtivo(
  numeroPatrimonio: string,
  alteracoes: AtivoEditavel
): Promise<Ativo> {
  return send<Ativo>(
    "PUT",
    `/ativos/${encodeURIComponent(numeroPatrimonio)}`,
    alteracoes
  );
}

/** DELETE /api/ativos/{numeroPatrimonio} — exclui um ativo do MongoDB (RF13). */
export function excluirAtivo(
  numeroPatrimonio: string
): Promise<{ mensagem: string }> {
  return send<{ mensagem: string }>(
    "DELETE",
    `/ativos/${encodeURIComponent(numeroPatrimonio)}`
  );
}

/** GET /api/colaboradores — colaboradores cadastrados (RF09/RF12). */
export function getColaboradores(): Promise<Colaborador[]> {
  return get<Colaborador[]>("/colaboradores");
}

/** GET /api/alocacoes?vigentes=true — alocações ativas de ativos a colaboradores (RF21/RF25). */
export function getAlocacoes(): Promise<Alocacao[]> {
  return get<Alocacao[]>("/alocacoes?vigentes=true");
}

/** POST /api/alocacoes — aloca um ativo; o BFF recusa ativo que não está "Em estoque" (RF21/RF23). */
export function alocarAtivo(numeroPatrimonio: string, colaboradorId: number): Promise<Alocacao> {
  return send<Alocacao>("POST", "/alocacoes", { numeroPatrimonio, colaboradorId });
}

/** POST /api/alocacoes/{id}/devolucao — encerra a alocação e devolve o ativo ao estoque (RF22). */
export function devolverAlocacao(id: number): Promise<Alocacao> {
  return send<Alocacao>("POST", `/alocacoes/${id}/devolucao`, {});
}

/** GET /api/manutencoes — histórico de manutenções e custos (RF27-RF31). */
export function getManutencoes(): Promise<Manutencao[]> {
  return get<Manutencao[]>("/manutencoes");
}
