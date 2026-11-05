package br.pucpr.ativos.domain.manutencao

import br.pucpr.ativos.domain.ValidacaoException
import br.pucpr.ativos.domain.ativo.Ativo
import br.pucpr.ativos.domain.exigirNaoNegativo
import br.pucpr.ativos.domain.exigirTexto
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Entidade de domínio: uma manutenção feita em um ativo, com o custo correspondente (RF27 a RF31).
 * [equipamento] guarda a descrição do ativo no momento do registro, para o histórico não mudar
 * se o cadastro do ativo for editado depois.
 */
data class Manutencao(
    val id: Long,
    val numeroPatrimonio: String,
    val equipamento: String,
    val data: LocalDate,
    val tipo: TipoManutencao,
    val descricao: String,
    val custo: BigDecimal,
) {
    fun alterar(
        data: LocalDate?,
        tipo: TipoManutencao?,
        descricao: String?,
        custo: BigDecimal?,
    ): Manutencao =
        copy(
            data = data ?: this.data,
            tipo = tipo ?: this.tipo,
            descricao = if (descricao != null) exigirTexto(descricao, "descricao") else this.descricao,
            custo = if (custo != null) exigirNaoNegativo(custo, "custo") else this.custo,
        )

    companion object {
        /** Registro de uma manutenção nova para um ativo existente. */
        fun registrar(
            id: Long,
            ativo: Ativo,
            data: LocalDate?,
            tipo: TipoManutencao?,
            descricao: String?,
            custo: BigDecimal?,
        ): Manutencao {
            ativo.garantirQueAceitaManutencao()
            return Manutencao(
                id = id,
                numeroPatrimonio = ativo.numeroPatrimonio,
                equipamento = ativo.equipamento,
                data = data ?: throw ValidacaoException("data", "Informe o campo 'data' no formato AAAA-MM-DD."),
                tipo = tipo
                    ?: throw ValidacaoException("tipo", "Informe o campo 'tipo' (${TipoManutencao.rotulosValidos()})."),
                descricao = exigirTexto(descricao, "descricao"),
                custo = exigirNaoNegativo(custo, "custo"),
            )
        }
    }
}
