package br.pucpr.alocacoes.domain

/** Exige texto preenchido e devolve o valor sem espaços nas pontas. */
fun exigirTexto(valor: String?, campo: String): String {
    val texto = valor?.trim()
    if (texto.isNullOrEmpty()) {
        throw ValidacaoException(campo, "Informe o campo '$campo'.")
    }
    return texto
}

/** Texto opcional: espaços viram null. */
fun textoOpcional(valor: String?): String? = valor?.trim()?.ifEmpty { null }
