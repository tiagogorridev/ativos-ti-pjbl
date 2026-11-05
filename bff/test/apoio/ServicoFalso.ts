import { ErroDeServico } from "../../src/domain/erros.js";
import type { PublicadorDeEventos, Registro, RespostaHttp, ServicoHttp } from "../../src/domain/portas.js";
import type { EventoDeAlocacao } from "../../src/domain/tipos.js";

type Resposta = RespostaHttp | "fora-do-ar";

/** Serviço HTTP em memória: responde por "MÉTODO caminho" e registra as chamadas recebidas. */
export class ServicoFalso implements ServicoHttp {
  readonly chamadas: { metodo: string; caminho: string; corpo?: unknown }[] = [];
  private readonly respostas = new Map<string, Resposta>();

  constructor(readonly nome: string) {}

  responder(metodo: string, caminho: string, status: number, corpo: unknown = null): this {
    this.respostas.set(`${metodo} ${caminho}`, { status, corpo });
    return this;
  }

  foraDoAr(metodo: string, caminho: string): this {
    this.respostas.set(`${metodo} ${caminho}`, "fora-do-ar");
    return this;
  }

  async requisitar(metodo: string, caminho: string, corpo?: unknown): Promise<RespostaHttp> {
    this.chamadas.push({ metodo, caminho, corpo });
    const resposta = this.respostas.get(`${metodo} ${caminho}`);
    if (resposta === "fora-do-ar") {
      throw new ErroDeServico(503, `O serviço de ${this.nome} não respondeu.`);
    }
    return resposta ?? { status: 404, corpo: { error: `Sem resposta para ${metodo} ${caminho}` } };
  }

  chamou(metodo: string, caminho: string): boolean {
    return this.chamadas.some((c) => c.metodo === metodo && c.caminho === caminho);
  }
}

export class PublicadorFalso implements PublicadorDeEventos {
  readonly publicados: EventoDeAlocacao[] = [];
  falhar = false;

  async publicar(evento: EventoDeAlocacao): Promise<void> {
    if (this.falhar) throw new Error("Service Bus fora do ar");
    this.publicados.push(evento);
  }
}

export class RegistroSilencioso implements Registro {
  readonly avisos: string[] = [];
  info(): void {}
  aviso(mensagem: string): void {
    this.avisos.push(mensagem);
  }
}

export const AGORA = () => new Date("2026-11-05T15:00:00Z");

export const ativoEmEstoque = {
  id: 3,
  numeroPatrimonio: "PAT-0003",
  tipo: "Monitor",
  marca: "LG",
  modelo: "24MK430H",
  numeroSerie: "LG9920T",
  status: "Em estoque",
  colaborador: null,
  setor: null,
  dataAquisicao: "2024-01-10",
  valorAquisicao: 899,
  equipamento: "LG 24MK430H",
};

export const alocacaoCriada = {
  id: 10,
  numeroPatrimonio: "PAT-0003",
  equipamento: "LG 24MK430H",
  colaboradorId: 2,
  colaborador: "Carlos Nunes",
  setor: "Suporte",
  dataRetirada: "2026-11-05",
  dataDevolucao: null,
  vigente: true,
};

export const indicadores = {
  totalAtivos: 14,
  emUso: 7,
  emEstoque: 3,
  emManutencao: 3,
  baixados: 1,
  custoManutencaoAcumulado: 2625.5,
  ativosPorSetor: [{ setor: "Financeiro", quantidade: 3 }],
  custoManutencaoPorMes: [{ mes: "Ago/26", valor: 225 }],
};
