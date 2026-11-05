package br.pucpr.ativos.application.ativos

import br.pucpr.ativos.domain.ativo.Ativo
import br.pucpr.ativos.domain.ativo.AtivoRepository
import br.pucpr.ativos.domain.ativo.exigirPorPatrimonio

/** Caso de uso: consultar a ficha de um ativo pelo número de patrimônio (RF19). */
class ObterAtivo(private val ativos: AtivoRepository) {

    fun executar(numeroPatrimonio: String): Ativo = ativos.exigirPorPatrimonio(numeroPatrimonio)
}
