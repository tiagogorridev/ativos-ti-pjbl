package br.pucpr.ativos.domain

/**
 * Exceções de negócio. O domínio só lança; a camada API traduz cada uma para um status HTTP.
 * A mensagem sempre diz o que corrigir (convenção RC03).
 */
sealed class ExcecaoDeNegocio(message: String) : RuntimeException(message)

/** O recurso pedido não existe (HTTP 404). */
class RecursoNaoEncontradoException(message: String) : ExcecaoDeNegocio(message)

/** A operação conflita com o estado atual dos dados (HTTP 409). */
class ConflitoException(message: String) : ExcecaoDeNegocio(message)

/** Dado de entrada inválido; [campo] indica qual campo corrigir (HTTP 400). */
class ValidacaoException(val campo: String, message: String) : ExcecaoDeNegocio(message)
