import { ErroDeServico } from "../../domain/erros.js";
import type { RespostaHttp, ServicoHttp } from "../../domain/portas.js";

/** Adaptador HTTP com fetch nativo do Node, com tempo limite e erro padronizado. */
export class ServicoHttpFetch implements ServicoHttp {
  private readonly baseUrl: string;

  constructor(
    readonly nome: string,
    baseUrl: string,
    private readonly timeoutMs: number,
    private readonly cabecalhosFixos: Record<string, string> = {},
  ) {
    this.baseUrl = baseUrl.replace(/\/+$/, "");
  }

  async requisitar(metodo: string, caminho: string, corpo?: unknown): Promise<RespostaHttp> {
    const cabecalhos: Record<string, string> = { Accept: "application/json", ...this.cabecalhosFixos };
    if (corpo !== undefined) {
      cabecalhos["Content-Type"] = "application/json";
    }

    let resposta: Response;
    try {
      resposta = await fetch(`${this.baseUrl}${caminho}`, {
        method: metodo,
        headers: cabecalhos,
        body: corpo !== undefined ? JSON.stringify(corpo) : undefined,
        signal: AbortSignal.timeout(this.timeoutMs),
      });
    } catch {
      throw new ErroDeServico(503, `O serviço de ${this.nome} não respondeu. Tente novamente em instantes.`);
    }

    const texto = await resposta.text();
    let json: unknown = null;
    if (texto) {
      try {
        json = JSON.parse(texto);
      } catch {
        json = { error: texto };
      }
    }
    return { status: resposta.status, corpo: json };
  }
}
