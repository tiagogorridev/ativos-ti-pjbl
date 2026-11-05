package br.pucpr.alocacoes.application.alocacoes

import br.pucpr.alocacoes.domain.ConflitoException
import br.pucpr.alocacoes.domain.ValidacaoException
import br.pucpr.alocacoes.domain.alocacao.Alocacao
import br.pucpr.alocacoes.domain.alocacao.AlocacaoRepository
import br.pucpr.alocacoes.domain.colaborador.ColaboradorRepository
import br.pucpr.alocacoes.domain.colaborador.exigirPorId
import br.pucpr.alocacoes.domain.exigirTexto
import java.time.Clock
import java.time.LocalDate

data class AlocarAtivoComando(
    val numeroPatrimonio: String? = null,
    val equipamento: String? = null,
    val colaboradorId: Long? = null,
    val dataRetirada: LocalDate? = null,
)

/**
 * Caso de uso: entregar um ativo a um colaborador (RF21).
 * Regra RF23: o mesmo ativo não pode ter duas alocações vigentes. A checagem abaixo dá uma
 * mensagem clara; o índice único do banco garante a regra mesmo com requisições simultâneas.
 */
class AlocarAtivo(
    private val colaboradores: ColaboradorRepository,
    private val alocacoes: AlocacaoRepository,
    private val relogio: Clock,
) {

    fun executar(comando: AlocarAtivoComando): Alocacao {
        val patrimonio = exigirTexto(comando.numeroPatrimonio, "numeroPatrimonio")
        val colaboradorId = comando.colaboradorId
            ?: throw ValidacaoException("colaboradorId", "Informe o campo 'colaboradorId'.")
        val colaborador = colaboradores.exigirPorId(colaboradorId)

        val vigente = alocacoes.buscarVigentePorPatrimonio(patrimonio)
        if (vigente != null) {
            throw ConflitoException(
                "O ativo $patrimonio já está alocado para ${vigente.colaboradorNome}. Registre a devolução antes de uma nova alocação.",
            )
        }

        val nova = Alocacao.nova(
            numeroPatrimonio = patrimonio,
            equipamento = comando.equipamento,
            colaborador = colaborador,
            dataRetirada = comando.dataRetirada ?: LocalDate.now(relogio),
        )
        val id = alocacoes.inserir(nova)
        return nova.copy(id = id)
    }
}
