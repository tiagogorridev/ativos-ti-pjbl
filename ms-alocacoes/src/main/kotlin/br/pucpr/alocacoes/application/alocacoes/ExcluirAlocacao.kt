package br.pucpr.alocacoes.application.alocacoes

import br.pucpr.alocacoes.domain.ConflitoException
import br.pucpr.alocacoes.domain.alocacao.AlocacaoRepository
import br.pucpr.alocacoes.domain.alocacao.exigirPorId

/**
 * Caso de uso: excluir uma alocação lançada por engano. Só vale para alocação vigente;
 * alocação encerrada é histórico e não é apagada (RC02).
 */
class ExcluirAlocacao(private val alocacoes: AlocacaoRepository) {

    fun executar(id: Long) {
        val alocacao = alocacoes.exigirPorId(id)
        if (!alocacao.vigente) {
            throw ConflitoException("A alocação $id já foi encerrada e faz parte do histórico; não pode ser excluída.")
        }
        alocacoes.excluir(id)
    }
}
