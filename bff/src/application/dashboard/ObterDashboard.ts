import { exigirSucesso } from "../../domain/erros.js";
import { movimentacoesRecentes } from "../../domain/movimentacoes.js";
import type { ServicoHttp } from "../../domain/portas.js";
import type { Alocacao, Indicadores, Movimentacao } from "../../domain/tipos.js";

export type Dashboard = Indicadores & { ultimasMovimentacoes: Movimentacao[] };

/**
 * Caso de uso da tela de Dashboard: junta os indicadores do serviço de Ativos com as
 * movimentações recentes do serviço de Alocações, no formato "DashboardData" do frontend.
 */
export class ObterDashboard {
  constructor(
    private readonly ativos: ServicoHttp,
    private readonly alocacoes: ServicoHttp,
  ) {}

  async executar(): Promise<Dashboard> {
    const [indicadores, historico] = await Promise.all([
      this.ativos.requisitar("GET", "/api/indicadores").then((r) => exigirSucesso<Indicadores>(r, this.ativos.nome)),
      this.alocacoes.requisitar("GET", "/api/alocacoes").then((r) => exigirSucesso<Alocacao[]>(r, this.alocacoes.nome)),
    ]);
    return { ...indicadores, ultimasMovimentacoes: movimentacoesRecentes(historico, 5) };
  }
}
