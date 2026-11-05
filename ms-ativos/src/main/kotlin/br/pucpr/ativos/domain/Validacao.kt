package br.pucpr.ativos.domain

import java.math.BigDecimal

/** Exige texto preenchido e devolve o valor sem espaços nas pontas. */
fun exigirTexto(valor: String?, campo: String): String {
    val texto = valor?.trim()
    if (texto.isNullOrEmpty()) {
        throw ValidacaoException(campo, "Informe o campo '$campo'.")
    }
    return texto
}

/** Exige um valor monetário informado e não negativo. */
fun exigirNaoNegativo(valor: BigDecimal?, campo: String): BigDecimal {
    if (valor == null) {
        throw ValidacaoException(campo, "Informe o campo '$campo'.")
    }
    if (valor.signum() < 0) {
        throw ValidacaoException(campo, "O campo '$campo' não pode ser negativo.")
    }
    return valor
}

/** Texto opcional: espaços viram null. */
fun textoOpcional(valor: String?): String? = valor?.trim()?.ifEmpty { null }
