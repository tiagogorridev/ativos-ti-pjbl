package br.pucpr.alocacoes.domain.colaborador

import br.pucpr.alocacoes.domain.ConflitoException
import br.pucpr.alocacoes.domain.ValidacaoException
import br.pucpr.alocacoes.domain.exigirTexto

/** Situação cadastral. O rótulo é o texto trocado com o frontend. */
enum class Situacao(val rotulo: String) {
    ATIVO("Ativo"),
    INATIVO("Inativo"),
    ;

    companion object {
        fun converter(texto: String?, campo: String = "situacao"): Situacao? {
            if (texto.isNullOrBlank()) return null
            val valor = texto.trim()
            return entries.firstOrNull { it.rotulo.equals(valor, ignoreCase = true) || it.name.equals(valor, ignoreCase = true) }
                ?: throw ValidacaoException(campo, "Situação '$texto' inválida. Use 'Ativo' ou 'Inativo'.")
        }
    }
}

/**
 * Entidade de domínio: colaborador que recebe ativos (RF09 a RF12).
 * Não é excluído fisicamente: a exclusão vira inativação (convenção RC02).
 */
data class Colaborador(
    val id: Long,
    val nome: String,
    val matricula: String,
    val setor: String,
    val situacao: Situacao,
) {
    fun alterar(nome: String?, matricula: String?, setor: String?, situacao: Situacao?): Colaborador =
        copy(
            nome = nome?.let { exigirTexto(it, "nome") } ?: this.nome,
            matricula = matricula?.let { exigirTexto(it, "matricula") } ?: this.matricula,
            setor = setor?.let { exigirTexto(it, "setor") } ?: this.setor,
            situacao = situacao ?: this.situacao,
        )

    fun garantirQuePodeReceberAtivos() {
        if (situacao != Situacao.ATIVO) {
            throw ConflitoException("O colaborador $nome está inativo e não pode receber ativos.")
        }
    }

    companion object {
        fun novo(nome: String?, matricula: String?, setor: String?): Colaborador =
            Colaborador(
                id = 0,
                nome = exigirTexto(nome, "nome"),
                matricula = exigirTexto(matricula, "matricula"),
                setor = exigirTexto(setor, "setor"),
                situacao = Situacao.ATIVO,
            )
    }
}
