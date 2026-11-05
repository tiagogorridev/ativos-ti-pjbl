import type { RespostaHttp } from "./portas.js";

/** Erro com status HTTP: devolvido ao frontend no formato { error } que ele já entende. */
export class ErroDeServico extends Error {
  constructor(
    readonly status: number,
    message: string,
    readonly corpo?: unknown,
  ) {
    super(message);
    this.name = "ErroDeServico";
  }
}

/** Devolve o corpo da resposta ou repassa o erro do serviço (mesmo status e mensagem). */
export function exigirSucesso<T>(resposta: RespostaHttp, servico: string): T {
  if (resposta.status >= 200 && resposta.status < 300) {
    return resposta.corpo as T;
  }
  throw new ErroDeServico(resposta.status, mensagemDe(resposta.corpo) ?? `Falha no serviço de ${servico}.`, resposta.corpo);
}

export function mensagemDe(corpo: unknown): string | undefined {
  if (corpo && typeof corpo === "object" && "error" in corpo && typeof corpo.error === "string") {
    return corpo.error;
  }
  return undefined;
}
