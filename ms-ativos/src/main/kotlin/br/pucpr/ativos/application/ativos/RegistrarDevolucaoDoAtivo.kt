package br.pucpr.ativos.application.ativos

import br.pucpr.ativos.domain.ativo.Ativo
import br.pucpr.ativos.domain.ativo.AtivoRepository
import br.pucpr.ativos.domain.ativo.exigirPorPatrimonio

/**
 * Caso de uso: devolver o ativo ao estoque.
 * É o comando que o serviço de Alocações chama depois de registrar a devolução.
 */
class RegistrarDevolucaoDoAtivo(private val ativos: AtivoRepository) {

    fun executar(numeroPatrimonio: String): Ativo {
        val novo = ativos.exigirPorPatrimonio(numeroPatrimonio).registrarDevolucao()
        ativos.atualizar(novo)
        return novo
    }
}
