package br.pucpr.ativos.api.erros

/**
 * Corpo padrão de erro. O frontend lê o campo "error" (frontend/lib/api.ts);
 * "campo" indica qual dado corrigir quando o erro é de validação (convenção RC03).
 */
data class ApiError(
    val error: String,
    val campo: String? = null,
)
