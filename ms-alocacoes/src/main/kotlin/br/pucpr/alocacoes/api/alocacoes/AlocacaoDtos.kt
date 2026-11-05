package br.pucpr.alocacoes.api.alocacoes

import br.pucpr.alocacoes.application.alocacoes.AlocarAtivoComando
import br.pucpr.alocacoes.application.alocacoes.AtualizarAlocacaoComando
import br.pucpr.alocacoes.domain.alocacao.Alocacao
import java.time.LocalDate

/** Resposta de uma alocação. Os campos do tipo "Alocacao" do frontend estão incluídos. */
data class AlocacaoResponse(
    val id: Long,
    val numeroPatrimonio: String,
    val equipamento: String,
    val colaboradorId: Long,
    val colaborador: String,
    val setor: String,
    val dataRetirada: LocalDate,
    val dataDevolucao: LocalDate?,
    val vigente: Boolean,
)

data class NovaAlocacaoRequest(
    val numeroPatrimonio: String? = null,
    val equipamento: String? = null,
    val colaboradorId: Long? = null,
    val dataRetirada: LocalDate? = null,
) {
    fun paraComando() = AlocarAtivoComando(
        numeroPatrimonio = numeroPatrimonio,
        equipamento = equipamento,
        colaboradorId = colaboradorId,
        dataRetirada = dataRetirada,
    )
}

data class AtualizarAlocacaoRequest(
    val equipamento: String? = null,
    val dataRetirada: LocalDate? = null,
) {
    fun paraComando() = AtualizarAlocacaoComando(equipamento = equipamento, dataRetirada = dataRetirada)
}

data class DevolucaoRequest(val dataDevolucao: LocalDate? = null)

data class MensagemResponse(val mensagem: String)

fun Alocacao.paraResposta() = AlocacaoResponse(
    id = id,
    numeroPatrimonio = numeroPatrimonio,
    equipamento = equipamento,
    colaboradorId = colaboradorId,
    colaborador = colaboradorNome,
    setor = setor,
    dataRetirada = dataRetirada,
    dataDevolucao = dataDevolucao,
    vigente = vigente,
)
