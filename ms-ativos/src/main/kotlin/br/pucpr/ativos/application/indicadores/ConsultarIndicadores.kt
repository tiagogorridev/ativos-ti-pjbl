package br.pucpr.ativos.application.indicadores

import br.pucpr.ativos.domain.ativo.AtivoRepository
import br.pucpr.ativos.domain.ativo.FiltroDeAtivos
import br.pucpr.ativos.domain.ativo.StatusAtivo
import br.pucpr.ativos.domain.manutencao.ManutencaoRepository
import java.math.BigDecimal
import java.time.YearMonth

data class QuantidadePorSetor(val setor: String, val quantidade: Int)

data class CustoPorMes(val mes: String, val valor: BigDecimal)

/** Indicadores do parque de ativos usados no dashboard (RF35). O BFF junta com os dados de Alocações. */
data class Indicadores(
    val totalAtivos: Int,
    val emUso: Int,
    val emEstoque: Int,
    val emManutencao: Int,
    val baixados: Int,
    val custoManutencaoAcumulado: BigDecimal,
    val ativosPorSetor: List<QuantidadePorSetor>,
    val custoManutencaoPorMes: List<CustoPorMes>,
)

/** Caso de uso: consolidar quantidades por status e setor e custos de manutenção por mês. */
class ConsultarIndicadores(
    private val ativos: AtivoRepository,
    private val manutencoes: ManutencaoRepository,
) {

    fun executar(): Indicadores {
        val todos = ativos.listar(FiltroDeAtivos())
        val historico = manutencoes.listar(null)
        val porStatus = todos.groupingBy { it.status }.eachCount()

        val porSetor = todos
            .filter { it.status != StatusAtivo.BAIXADO && !it.setor.isNullOrBlank() }
            .groupingBy { it.setor.orEmpty() }
            .eachCount()
            .map { (setor, quantidade) -> QuantidadePorSetor(setor, quantidade) }
            .sortedWith(compareByDescending<QuantidadePorSetor> { it.quantidade }.thenBy { it.setor })

        val porMes = historico
            .groupBy { YearMonth.from(it.data) }
            .toSortedMap()
            .map { (mes, itens) -> CustoPorMes(rotuloDoMes(mes), itens.fold(BigDecimal.ZERO) { soma, m -> soma + m.custo }) }

        return Indicadores(
            totalAtivos = todos.size,
            emUso = porStatus[StatusAtivo.EM_USO] ?: 0,
            emEstoque = porStatus[StatusAtivo.EM_ESTOQUE] ?: 0,
            emManutencao = porStatus[StatusAtivo.EM_MANUTENCAO] ?: 0,
            baixados = porStatus[StatusAtivo.BAIXADO] ?: 0,
            custoManutencaoAcumulado = historico.fold(BigDecimal.ZERO) { soma, m -> soma + m.custo },
            ativosPorSetor = porSetor,
            custoManutencaoPorMes = porMes,
        )
    }

    private fun rotuloDoMes(mes: YearMonth): String =
        "${MESES[mes.monthValue - 1]}/${(mes.year % 100).toString().padStart(2, '0')}"

    private companion object {
        val MESES = listOf("Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez")
    }
}
