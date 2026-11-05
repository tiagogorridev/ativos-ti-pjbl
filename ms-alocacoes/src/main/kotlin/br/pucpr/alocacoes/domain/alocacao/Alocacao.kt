package br.pucpr.alocacoes.domain.alocacao

import br.pucpr.alocacoes.domain.ConflitoException
import br.pucpr.alocacoes.domain.ValidacaoException
import br.pucpr.alocacoes.domain.colaborador.Colaborador
import br.pucpr.alocacoes.domain.exigirTexto
import br.pucpr.alocacoes.domain.textoOpcional
import java.time.LocalDate

/**
 * Entidade de domínio: um ativo entregue a um colaborador (RF21 a RF26).
 * Enquanto não há data de devolução, a alocação está vigente. O ativo é referenciado pelo
 * número de patrimônio, porque ele vive em outro serviço e em outro banco (Database per Service).
 * [colaboradorNome] e [setor] vêm do cadastro do colaborador e servem para leitura.
 */
data class Alocacao(
    val id: Long,
    val numeroPatrimonio: String,
    val equipamento: String,
    val colaboradorId: Long,
    val colaboradorNome: String,
    val setor: String,
    val dataRetirada: LocalDate,
    val dataDevolucao: LocalDate?,
) {
    val vigente: Boolean
        get() = dataDevolucao == null

    fun devolver(data: LocalDate): Alocacao {
        if (!vigente) {
            throw ConflitoException("A alocação $id já foi encerrada em $dataDevolucao.")
        }
        if (data.isBefore(dataRetirada)) {
            throw ValidacaoException("dataDevolucao", "A data de devolução não pode ser anterior à retirada ($dataRetirada).")
        }
        return copy(dataDevolucao = data)
    }

    fun alterar(equipamento: String?, dataRetirada: LocalDate?): Alocacao {
        if (!vigente) {
            throw ConflitoException("A alocação $id já foi encerrada e faz parte do histórico; não pode ser alterada.")
        }
        return copy(
            equipamento = textoOpcional(equipamento) ?: this.equipamento,
            dataRetirada = dataRetirada ?: this.dataRetirada,
        )
    }

    companion object {
        fun nova(numeroPatrimonio: String?, equipamento: String?, colaborador: Colaborador, dataRetirada: LocalDate): Alocacao {
            colaborador.garantirQuePodeReceberAtivos()
            val patrimonio = exigirTexto(numeroPatrimonio, "numeroPatrimonio")
            return Alocacao(
                id = 0,
                numeroPatrimonio = patrimonio,
                equipamento = textoOpcional(equipamento) ?: patrimonio,
                colaboradorId = colaborador.id,
                colaboradorNome = colaborador.nome,
                setor = colaborador.setor,
                dataRetirada = dataRetirada,
                dataDevolucao = null,
            )
        }
    }
}
