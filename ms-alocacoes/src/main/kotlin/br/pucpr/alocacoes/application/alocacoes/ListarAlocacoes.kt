package br.pucpr.alocacoes.application.alocacoes

import br.pucpr.alocacoes.domain.alocacao.Alocacao
import br.pucpr.alocacoes.domain.alocacao.AlocacaoRepository

/** Caso de uso: listar alocações, vigentes ou histórico, por colaborador (RF25, RF26). */
class ListarAlocacoes(private val alocacoes: AlocacaoRepository) {

    fun executar(vigentes: Boolean?, colaboradorId: Long?): List<Alocacao> = alocacoes.listar(vigentes, colaboradorId)
}
