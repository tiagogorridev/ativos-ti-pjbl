package br.pucpr.ativos.domain.ativo

import br.pucpr.ativos.domain.RecursoNaoEncontradoException

/**
 * Porta de persistência dos ativos. O domínio define o contrato; a implementação com
 * MongoDB fica na camada Infrastructure (inversão de dependência).
 */
interface AtivoRepository {
    fun listar(filtro: FiltroDeAtivos): List<Ativo>

    fun buscarPorPatrimonio(numeroPatrimonio: String): Ativo?

    fun existePatrimonio(numeroPatrimonio: String): Boolean

    /** Próximo identificador numérico (o frontend usa "id" numérico). */
    fun proximoId(): Long

    fun inserir(ativo: Ativo)

    fun atualizar(ativo: Ativo)

    fun excluir(numeroPatrimonio: String)
}

/** Filtros da pesquisa de ativos: texto livre e status. */
data class FiltroDeAtivos(
    val busca: String? = null,
    val status: StatusAtivo? = null,
)

/** Busca o ativo ou lança 404 com uma mensagem padronizada. */
fun AtivoRepository.exigirPorPatrimonio(numeroPatrimonio: String): Ativo =
    buscarPorPatrimonio(numeroPatrimonio)
        ?: throw RecursoNaoEncontradoException("Nenhum ativo encontrado com o patrimônio $numeroPatrimonio.")
