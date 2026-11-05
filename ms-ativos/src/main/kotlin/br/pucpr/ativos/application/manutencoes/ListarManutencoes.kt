package br.pucpr.ativos.application.manutencoes

import br.pucpr.ativos.domain.manutencao.Manutencao
import br.pucpr.ativos.domain.manutencao.ManutencaoRepository
import br.pucpr.ativos.domain.textoOpcional

/** Caso de uso: histórico de manutenções, geral ou de um ativo (RF29). */
class ListarManutencoes(private val manutencoes: ManutencaoRepository) {

    fun executar(numeroPatrimonio: String?): List<Manutencao> = manutencoes.listar(textoOpcional(numeroPatrimonio))
}
