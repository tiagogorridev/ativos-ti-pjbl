package br.pucpr.alocacoes.domain.colaborador

import br.pucpr.alocacoes.domain.RecursoNaoEncontradoException

/** Porta de persistência dos colaboradores (implementada com Azure SQL na Infrastructure). */
interface ColaboradorRepository {
    fun listar(): List<Colaborador>

    fun buscarPorId(id: Long): Colaborador?

    /** Ids dos colaboradores que já usam a matrícula (para checar unicidade). */
    fun idsComMatricula(matricula: String): List<Long>

    /** Grava e devolve o id gerado pelo banco. */
    fun inserir(colaborador: Colaborador): Long

    fun atualizar(colaborador: Colaborador)
}

fun ColaboradorRepository.exigirPorId(id: Long): Colaborador =
    buscarPorId(id) ?: throw RecursoNaoEncontradoException("Nenhum colaborador encontrado com o id $id.")
