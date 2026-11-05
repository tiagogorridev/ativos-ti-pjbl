package br.pucpr.alocacoes.application.alocacoes

import br.pucpr.alocacoes.domain.alocacao.Alocacao
import br.pucpr.alocacoes.domain.alocacao.AlocacaoRepository
import br.pucpr.alocacoes.domain.alocacao.exigirPorId
import java.time.Clock
import java.time.LocalDate

/** Caso de uso: registrar a devolução, encerrando a alocação vigente (RF22). O histórico é mantido. */
class DevolverAtivo(
    private val alocacoes: AlocacaoRepository,
    private val relogio: Clock,
) {

    fun executar(id: Long, dataDevolucao: LocalDate?): Alocacao {
        val encerrada = alocacoes.exigirPorId(id).devolver(dataDevolucao ?: LocalDate.now(relogio))
        alocacoes.atualizar(encerrada)
        return encerrada
    }
}
