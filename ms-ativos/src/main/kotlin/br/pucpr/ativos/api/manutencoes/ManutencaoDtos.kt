package br.pucpr.ativos.api.manutencoes

import br.pucpr.ativos.application.manutencoes.AtualizarManutencaoComando
import br.pucpr.ativos.application.manutencoes.RegistrarManutencaoComando
import br.pucpr.ativos.domain.manutencao.Manutencao
import java.math.BigDecimal
import java.time.LocalDate

/** Resposta de uma manutenção: mesmo formato do tipo "Manutencao" em frontend/lib/types.ts. */
data class ManutencaoResponse(
    val id: Long,
    val data: LocalDate,
    val numeroPatrimonio: String,
    val equipamento: String,
    val tipo: String,
    val descricao: String,
    val custo: BigDecimal,
)

data class NovaManutencaoRequest(
    val numeroPatrimonio: String? = null,
    val data: LocalDate? = null,
    val tipo: String? = null,
    val descricao: String? = null,
    val custo: BigDecimal? = null,
) {
    fun paraComando() = RegistrarManutencaoComando(
        numeroPatrimonio = numeroPatrimonio,
        data = data,
        tipo = tipo,
        descricao = descricao,
        custo = custo,
    )
}

data class AtualizarManutencaoRequest(
    val data: LocalDate? = null,
    val tipo: String? = null,
    val descricao: String? = null,
    val custo: BigDecimal? = null,
) {
    fun paraComando() = AtualizarManutencaoComando(data = data, tipo = tipo, descricao = descricao, custo = custo)
}

fun Manutencao.paraResposta() = ManutencaoResponse(
    id = id,
    data = data,
    numeroPatrimonio = numeroPatrimonio,
    equipamento = equipamento,
    tipo = tipo.rotulo,
    descricao = descricao,
    custo = custo,
)
