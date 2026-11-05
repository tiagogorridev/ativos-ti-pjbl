package br.pucpr.ativos.application.manutencoes

import br.pucpr.ativos.domain.manutencao.Manutencao
import br.pucpr.ativos.domain.manutencao.ManutencaoRepository
import br.pucpr.ativos.domain.manutencao.exigirPorId

/** Caso de uso: consultar uma manutenção pelo id. */
class ObterManutencao(private val manutencoes: ManutencaoRepository) {

    fun executar(id: Long): Manutencao = manutencoes.exigirPorId(id)
}
