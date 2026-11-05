package br.pucpr.alocacoes.domain.alocacao

import br.pucpr.alocacoes.domain.RecursoNaoEncontradoException

/** Porta de persistência das alocações (implementada com Azure SQL na Infrastructure). */
interface AlocacaoRepository {
    /** [vigentes]: true só abertas, false só encerradas, null todas. */
    fun listar(vigentes: Boolean?, colaboradorId: Long?): List<Alocacao>

    fun buscarPorId(id: Long): Alocacao?

    fun buscarVigentePorPatrimonio(numeroPatrimonio: String): Alocacao?

    fun contarVigentesDoColaborador(colaboradorId: Long): Int

    fun contarVigentesPorColaborador(): Map<Long, Int>

    /**
     * Grava e devolve o id gerado. Lança ConflitoException se o ativo já tiver alocação vigente:
     * o banco garante essa regra com um índice único, mesmo com duas requisições ao mesmo tempo.
     */
    fun inserir(alocacao: Alocacao): Long

    fun atualizar(alocacao: Alocacao)

    fun excluir(id: Long)
}

fun AlocacaoRepository.exigirPorId(id: Long): Alocacao =
    buscarPorId(id) ?: throw RecursoNaoEncontradoException("Nenhuma alocação encontrada com o id $id.")
