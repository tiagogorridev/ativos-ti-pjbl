import type { Alocacao, Movimentacao } from "./tipos.js";

/**
 * Deriva as últimas movimentações (alocações e devoluções) a partir do histórico de alocações,
 * no formato que o dashboard do frontend já usa.
 */
export function movimentacoesRecentes(alocacoes: Alocacao[], limite: number): Movimentacao[] {
  const movimentacoes: Movimentacao[] = [];
  for (const alocacao of alocacoes) {
    movimentacoes.push({
      id: alocacao.id * 2,
      data: alocacao.dataRetirada,
      numeroPatrimonio: alocacao.numeroPatrimonio,
      ativo: alocacao.equipamento,
      operacao: "Alocação",
      colaborador: alocacao.colaborador,
      registradoPor: "Sistema",
    });
    if (alocacao.dataDevolucao) {
      movimentacoes.push({
        id: alocacao.id * 2 + 1,
        data: alocacao.dataDevolucao,
        numeroPatrimonio: alocacao.numeroPatrimonio,
        ativo: alocacao.equipamento,
        operacao: "Devolução",
        colaborador: alocacao.colaborador,
        registradoPor: "Sistema",
      });
    }
  }
  return movimentacoes
    .sort((a, b) => b.data.localeCompare(a.data) || b.id - a.id)
    .slice(0, limite);
}
