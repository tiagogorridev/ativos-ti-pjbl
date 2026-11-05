package br.pucpr.ativos.domain.ativo

import br.pucpr.ativos.domain.ValidacaoException

/** Situação de um ativo. O rótulo é o texto trocado com o frontend e gravado no MongoDB. */
enum class StatusAtivo(val rotulo: String) {
    EM_ESTOQUE("Em estoque"),
    EM_USO("Em uso"),
    EM_MANUTENCAO("Em manutenção"),
    BAIXADO("Baixado"),
    ;

    companion object {
        /** Aceita o rótulo ("Em uso") ou o nome da constante ("EM_USO"). */
        fun doRotulo(texto: String?): StatusAtivo? {
            val valor = texto?.trim() ?: return null
            return entries.firstOrNull {
                it.rotulo.equals(valor, ignoreCase = true) || it.name.equals(valor, ignoreCase = true)
            }
        }

        /** Converte um texto opcional; texto vazio vira null e texto desconhecido é erro de validação. */
        fun converter(texto: String?, campo: String = "status"): StatusAtivo? {
            if (texto.isNullOrBlank()) return null
            return doRotulo(texto)
                ?: throw ValidacaoException(campo, "Status '$texto' inválido. Use um destes: ${rotulosValidos()}.")
        }

        fun rotulosValidos(): String = entries.joinToString(", ") { "'${it.rotulo}'" }
    }
}
