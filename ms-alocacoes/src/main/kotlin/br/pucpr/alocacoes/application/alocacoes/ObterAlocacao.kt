package br.pucpr.alocacoes.application.alocacoes

import br.pucpr.alocacoes.domain.alocacao.Alocacao
import br.pucpr.alocacoes.domain.alocacao.AlocacaoRepository
import br.pucpr.alocacoes.domain.alocacao.exigirPorId

/** Caso de uso: consultar uma alocação. */
class ObterAlocacao(private val alocacoes: AlocacaoRepository) {

    fun executar(id: Long): Alocacao = alocacoes.exigirPorId(id)
}
