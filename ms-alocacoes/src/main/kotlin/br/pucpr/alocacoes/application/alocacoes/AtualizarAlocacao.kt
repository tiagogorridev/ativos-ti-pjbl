package br.pucpr.alocacoes.application.alocacoes

import br.pucpr.alocacoes.domain.alocacao.Alocacao
import br.pucpr.alocacoes.domain.alocacao.AlocacaoRepository
import br.pucpr.alocacoes.domain.alocacao.exigirPorId
import java.time.LocalDate

/** Atualização parcial de uma alocação vigente (correção de lançamento). */
data class AtualizarAlocacaoComando(
    val equipamento: String? = null,
    val dataRetirada: LocalDate? = null,
)

/** Caso de uso: corrigir descrição do equipamento ou data de retirada de uma alocação vigente. */
class AtualizarAlocacao(private val alocacoes: AlocacaoRepository) {

    fun executar(id: Long, comando: AtualizarAlocacaoComando): Alocacao {
        val nova = alocacoes.exigirPorId(id).alterar(comando.equipamento, comando.dataRetirada)
        alocacoes.atualizar(nova)
        return nova
    }
}
