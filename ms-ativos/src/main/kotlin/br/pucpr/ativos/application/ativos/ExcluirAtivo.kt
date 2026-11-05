package br.pucpr.ativos.application.ativos

import br.pucpr.ativos.domain.ConflitoException
import br.pucpr.ativos.domain.ativo.AtivoRepository
import br.pucpr.ativos.domain.ativo.exigirPorPatrimonio
import br.pucpr.ativos.domain.manutencao.ManutencaoRepository

/**
 * Caso de uso: excluir um ativo cadastrado por engano.
 * Ativo com histórico não é excluído (convenção RC02): o caminho é a baixa.
 */
class ExcluirAtivo(
    private val ativos: AtivoRepository,
    private val manutencoes: ManutencaoRepository,
) {

    fun executar(numeroPatrimonio: String) {
        val ativo = ativos.exigirPorPatrimonio(numeroPatrimonio)
        ativo.garantirQuePodeSerExcluido()
        if (manutencoes.existeParaAtivo(numeroPatrimonio)) {
            throw ConflitoException(
                "O ativo $numeroPatrimonio tem manutenções registradas e faz parte do histórico. Use a baixa (status 'Baixado') em vez de excluir.",
            )
        }
        ativos.excluir(numeroPatrimonio)
    }
}
