// Portas: o que o BFF precisa do mundo externo. As implementações ficam na Infrastructure.
import type { EventoDeAlocacao } from "./tipos.js";

export interface RespostaHttp {
  status: number;
  corpo: unknown;
}

/** Um serviço remoto (microsserviço ou Azure Function) acessado por HTTP. */
export interface ServicoHttp {
  readonly nome: string;
  /** [caminho] inclui a query string. Lança ErroDeServico (503) se o serviço não responder. */
  requisitar(metodo: string, caminho: string, corpo?: unknown): Promise<RespostaHttp>;
}

/** Publicação de eventos de domínio (Azure Service Bus em produção). */
export interface PublicadorDeEventos {
  publicar(evento: EventoDeAlocacao): Promise<void>;
}

export interface Registro {
  info(mensagem: string, extra?: unknown): void;
  aviso(mensagem: string, extra?: unknown): void;
}
