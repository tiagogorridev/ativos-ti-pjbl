package br.pucpr.ativos.application.ativos

import br.pucpr.ativos.domain.ativo.Ativo
import br.pucpr.ativos.domain.ativo.AtivoRepository
import br.pucpr.ativos.domain.ativo.exigirPorPatrimonio
import br.pucpr.ativos.domain.exigirTexto

data class RegistrarUsoComando(
    val colaborador: String? = null,
    val setor: String? = null,
)

/**
 * Caso de uso: marcar o ativo como "Em uso" por um colaborador.
 * É o comando que o serviço de Alocações chama depois de gravar a alocação no Azure SQL.
 */
class RegistrarUsoDoAtivo(private val ativos: AtivoRepository) {

    fun executar(numeroPatrimonio: String, comando: RegistrarUsoComando): Ativo {
        val colaborador = exigirTexto(comando.colaborador, "colaborador")
        val novo = ativos.exigirPorPatrimonio(numeroPatrimonio).registrarUso(colaborador, comando.setor)
        ativos.atualizar(novo)
        return novo
    }
}
