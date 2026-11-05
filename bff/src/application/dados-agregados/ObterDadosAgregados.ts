import { exigirSucesso } from "../../domain/erros.js";
import type { ServicoHttp } from "../../domain/portas.js";
import type { Alocacao, Ativo, Colaborador, Depreciacao, Indicadores } from "../../domain/tipos.js";

export type StatusDaFonte = "ok" | "indisponivel";

export interface DadosAgregados {
  geradoEm: string;
  fontes: {
    microservicoAtivos: { banco: "MongoDB Atlas"; status: StatusDaFonte };
    microservicoAlocacoes: { banco: "Azure SQL"; status: StatusDaFonte };
    azureFunction: { calculo: "Depreciação"; status: StatusDaFonte };
  };
  ativos: { indicadores: Indicadores; itens: Ativo[] } | null;
  alocacoes: { vigentes: Alocacao[]; colaboradores: Colaborador[] } | null;
  depreciacao: Depreciacao | null;
}

/**
 * Caso de uso do endpoint GET /aggregated-data: consulta o microsserviço de Ativos (MongoDB),
 * o de Alocações (Azure SQL) e a Azure Function, e devolve tudo em uma única resposta.
 * Se uma fonte falhar, as outras continuam no resultado e "fontes" indica qual faltou.
 */
export class ObterDadosAgregados {
  constructor(
    private readonly ativos: ServicoHttp,
    private readonly alocacoes: ServicoHttp,
    private readonly calculo: ServicoHttp,
    private readonly agora: () => Date = () => new Date(),
  ) {}

  async executar(): Promise<DadosAgregados> {
    const [itens, indicadores, vigentes, colaboradores] = await Promise.allSettled([
      this.ativos.requisitar("GET", "/api/ativos").then((r) => exigirSucesso<Ativo[]>(r, this.ativos.nome)),
      this.ativos.requisitar("GET", "/api/indicadores").then((r) => exigirSucesso<Indicadores>(r, this.ativos.nome)),
      this.alocacoes
        .requisitar("GET", "/api/alocacoes?vigentes=true")
        .then((r) => exigirSucesso<Alocacao[]>(r, this.alocacoes.nome)),
      this.alocacoes
        .requisitar("GET", "/api/colaboradores")
        .then((r) => exigirSucesso<Colaborador[]>(r, this.alocacoes.nome)),
    ]);

    const ativosOk = itens.status === "fulfilled" && indicadores.status === "fulfilled";
    const alocacoesOk = vigentes.status === "fulfilled" && colaboradores.status === "fulfilled";

    let depreciacao: Depreciacao | null = null;
    if (itens.status === "fulfilled") {
      depreciacao = await this.calcularDepreciacao(itens.value);
    }

    return {
      geradoEm: this.agora().toISOString(),
      fontes: {
        microservicoAtivos: { banco: "MongoDB Atlas", status: ativosOk ? "ok" : "indisponivel" },
        microservicoAlocacoes: { banco: "Azure SQL", status: alocacoesOk ? "ok" : "indisponivel" },
        azureFunction: { calculo: "Depreciação", status: depreciacao ? "ok" : "indisponivel" },
      },
      ativos:
        itens.status === "fulfilled" && indicadores.status === "fulfilled"
          ? { indicadores: indicadores.value, itens: itens.value }
          : null,
      alocacoes:
        vigentes.status === "fulfilled" && colaboradores.status === "fulfilled"
          ? { vigentes: vigentes.value, colaboradores: colaboradores.value }
          : null,
      depreciacao,
    };
  }

  private async calcularDepreciacao(ativos: Ativo[]): Promise<Depreciacao | null> {
    try {
      const resposta = await this.calculo.requisitar("POST", "/depreciacao", {
        ativos: ativos.map((a) => ({
          numeroPatrimonio: a.numeroPatrimonio,
          tipo: a.tipo,
          status: a.status,
          dataAquisicao: a.dataAquisicao,
          valorAquisicao: a.valorAquisicao,
        })),
      });
      return exigirSucesso<Depreciacao>(resposta, this.calculo.nome);
    } catch {
      return null;
    }
  }
}
