package br.pucpr.ativos.application.manutencoes

import br.pucpr.ativos.domain.manutencao.ManutencaoRepository
import br.pucpr.ativos.domain.manutencao.exigirPorId

/** Caso de uso: excluir uma manutenção lançada por engano. */
class ExcluirManutencao(private val manutencoes: ManutencaoRepository) {

    fun executar(id: Long) {
        manutencoes.exigirPorId(id)
        manutencoes.excluir(id)
    }
}
