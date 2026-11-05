package br.pucpr.alocacoes.application.colaboradores

import br.pucpr.alocacoes.domain.alocacao.AlocacaoRepository
import br.pucpr.alocacoes.domain.colaborador.Colaborador
import br.pucpr.alocacoes.domain.colaborador.ColaboradorRepository

/** Colaborador com a quantidade de ativos que está com ele agora. */
data class ColaboradorComAtivos(
    val colaborador: Colaborador,
    val ativosAlocados: Int,
)

/** Caso de uso: listar colaboradores com a contagem de ativos alocados (RF12). */
class ListarColaboradores(
    private val colaboradores: ColaboradorRepository,
    private val alocacoes: AlocacaoRepository,
) {

    fun executar(): List<ColaboradorComAtivos> {
        val contagem = alocacoes.contarVigentesPorColaborador()
        return colaboradores.listar().map { ColaboradorComAtivos(it, contagem[it.id] ?: 0) }
    }
}
