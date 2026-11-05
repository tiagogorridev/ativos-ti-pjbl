package br.pucpr.ativos.domain.manutencao

import br.pucpr.ativos.domain.ValidacaoException

/** Tipo de manutenção. O rótulo é o texto trocado com o frontend e gravado no MongoDB. */
enum class TipoManutencao(val rotulo: String) {
    CORRETIVA("Corretiva"),
    PREVENTIVA("Preventiva"),
    ;

    companion object {
        fun doRotulo(texto: String?): TipoManutencao? {
            val valor = texto?.trim() ?: return null
            return entries.firstOrNull {
                it.rotulo.equals(valor, ignoreCase = true) || it.name.equals(valor, ignoreCase = true)
            }
        }

        fun converter(texto: String?, campo: String = "tipo"): TipoManutencao? {
            if (texto.isNullOrBlank()) return null
            return doRotulo(texto)
                ?: throw ValidacaoException(campo, "Tipo '$texto' inválido. Use um destes: ${rotulosValidos()}.")
        }

        fun rotulosValidos(): String = entries.joinToString(", ") { "'${it.rotulo}'" }
    }
}
