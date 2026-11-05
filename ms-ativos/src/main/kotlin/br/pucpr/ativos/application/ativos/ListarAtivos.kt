package br.pucpr.ativos.application.ativos

import br.pucpr.ativos.domain.ativo.Ativo
import br.pucpr.ativos.domain.ativo.AtivoRepository
import br.pucpr.ativos.domain.ativo.FiltroDeAtivos
import br.pucpr.ativos.domain.ativo.StatusAtivo
import br.pucpr.ativos.domain.textoOpcional

/** Caso de uso: pesquisar ativos por texto livre e por status (RF17). "Todos" desliga o filtro. */
class ListarAtivos(private val ativos: AtivoRepository) {

    fun executar(busca: String?, status: String?): List<Ativo> {
        val filtroStatus = if (status.equals("Todos", ignoreCase = true)) null else StatusAtivo.converter(status)
        return ativos.listar(FiltroDeAtivos(busca = textoOpcional(busca), status = filtroStatus))
    }
}
