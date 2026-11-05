package br.pucpr.alocacoes.application.colaboradores

import br.pucpr.alocacoes.domain.alocacao.AlocacaoRepository
import br.pucpr.alocacoes.domain.colaborador.ColaboradorRepository
import br.pucpr.alocacoes.domain.colaborador.exigirPorId

/** Caso de uso: consultar um colaborador e quantos ativos estão com ele. */
class ObterColaborador(
    private val colaboradores: ColaboradorRepository,
    private val alocacoes: AlocacaoRepository,
) {

    fun executar(id: Long): ColaboradorComAtivos =
        ColaboradorComAtivos(colaboradores.exigirPorId(id), alocacoes.contarVigentesDoColaborador(id))
}
