package br.pucpr.alocacoes.application.colaboradores

import br.pucpr.alocacoes.domain.alocacao.AlocacaoRepository
import br.pucpr.alocacoes.domain.colaborador.Colaborador
import br.pucpr.alocacoes.domain.colaborador.ColaboradorRepository
import br.pucpr.alocacoes.domain.colaborador.Situacao
import br.pucpr.alocacoes.domain.colaborador.exigirPorId

/**
 * Caso de uso: "excluir" um colaborador. Vira inativação lógica (RC02), para preservar o
 * histórico de alocações; não é permitido enquanto houver ativo com ele (RF11).
 */
class InativarColaborador(
    private val colaboradores: ColaboradorRepository,
    private val alocacoes: AlocacaoRepository,
) {

    fun executar(id: Long): Colaborador {
        val atual = colaboradores.exigirPorId(id)
        garantirSemAtivosAlocados(alocacoes, atual)
        val inativo = atual.copy(situacao = Situacao.INATIVO)
        colaboradores.atualizar(inativo)
        return inativo
    }
}
