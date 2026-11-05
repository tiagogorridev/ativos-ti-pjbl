package br.pucpr.ativos.application.manutencoes

import br.pucpr.ativos.domain.manutencao.Manutencao
import br.pucpr.ativos.domain.manutencao.ManutencaoRepository
import br.pucpr.ativos.domain.manutencao.TipoManutencao
import br.pucpr.ativos.domain.manutencao.exigirPorId
import java.math.BigDecimal
import java.time.LocalDate

/** Atualização parcial: campo ausente (null) mantém o valor atual. O ativo da manutenção não muda. */
data class AtualizarManutencaoComando(
    val data: LocalDate? = null,
    val tipo: String? = null,
    val descricao: String? = null,
    val custo: BigDecimal? = null,
)

/** Caso de uso: corrigir os dados de uma manutenção registrada (RF30). */
class AtualizarManutencao(private val manutencoes: ManutencaoRepository) {

    fun executar(id: Long, comando: AtualizarManutencaoComando): Manutencao {
        val nova = manutencoes.exigirPorId(id).alterar(
            data = comando.data,
            tipo = TipoManutencao.converter(comando.tipo),
            descricao = comando.descricao,
            custo = comando.custo,
        )
        manutencoes.atualizar(nova)
        return nova
    }
}
