package br.pucpr.ativos.domain.manutencao

import br.pucpr.ativos.domain.RecursoNaoEncontradoException

/** Porta de persistência das manutenções (implementada com MongoDB na Infrastructure). */
interface ManutencaoRepository {
    /** Lista em ordem da mais recente para a mais antiga; filtra por ativo quando informado. */
    fun listar(numeroPatrimonio: String?): List<Manutencao>

    fun buscarPorId(id: Long): Manutencao?

    fun existeParaAtivo(numeroPatrimonio: String): Boolean

    fun proximoId(): Long

    fun inserir(manutencao: Manutencao)

    fun atualizar(manutencao: Manutencao)

    fun excluir(id: Long)
}

fun ManutencaoRepository.exigirPorId(id: Long): Manutencao =
    buscarPorId(id) ?: throw RecursoNaoEncontradoException("Nenhuma manutenção encontrada com o id $id.")
