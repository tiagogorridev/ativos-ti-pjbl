package br.pucpr.ativos.domain.ativo

import br.pucpr.ativos.domain.ConflitoException
import br.pucpr.ativos.domain.exigirNaoNegativo
import br.pucpr.ativos.domain.exigirTexto
import br.pucpr.ativos.domain.textoOpcional
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Entidade de domínio: um ativo de TI (notebook, monitor, periférico...).
 *
 * Não conhece MongoDB, Spring nem HTTP. As regras do ciclo de vida ficam nos métodos:
 * o status "Em uso" só é atribuído pela alocação, e um ativo baixado não muda mais de status.
 * O construtor é usado para reconstituir dados já gravados; cadastros novos passam por [novo].
 */
data class Ativo(
    val id: Long,
    val numeroPatrimonio: String,
    val tipo: String,
    val marca: String,
    val modelo: String,
    val numeroSerie: String,
    val status: StatusAtivo,
    val colaborador: String?,
    val setor: String?,
    val dataAquisicao: LocalDate,
    val valorAquisicao: BigDecimal,
) {
    /** Descrição usada em listagens e relatórios ("Dell Latitude 5440"). */
    val equipamento: String
        get() = "$marca $modelo".trim()

    /** Entrega o ativo a um colaborador. Chamado pelo serviço de Alocações. */
    fun registrarUso(colaborador: String, setor: String?): Ativo {
        if (status != StatusAtivo.EM_ESTOQUE) {
            throw ConflitoException(
                "O ativo $numeroPatrimonio está '${status.rotulo}'. Só ativos 'Em estoque' podem ser alocados.",
            )
        }
        return copy(
            status = StatusAtivo.EM_USO,
            colaborador = exigirTexto(colaborador, "colaborador"),
            setor = textoOpcional(setor),
        )
    }

    /** Devolve o ativo ao estoque. Chamado pelo serviço de Alocações. */
    fun registrarDevolucao(): Ativo {
        if (status != StatusAtivo.EM_USO) {
            throw ConflitoException("O ativo $numeroPatrimonio não está 'Em uso'; não há devolução a registrar.")
        }
        return copy(status = StatusAtivo.EM_ESTOQUE, colaborador = null, setor = null)
    }

    /** Troca manual de status pela tela de ativos (manutenção, retorno ao estoque, baixa). */
    fun alterarStatus(novo: StatusAtivo): Ativo {
        if (novo == status) return this
        if (status == StatusAtivo.BAIXADO) {
            throw ConflitoException("O ativo $numeroPatrimonio foi baixado e não pode mudar de status.")
        }
        if (status == StatusAtivo.EM_USO) {
            throw ConflitoException(
                "O ativo $numeroPatrimonio está alocado. Registre a devolução no serviço de Alocações antes de mudar o status.",
            )
        }
        if (novo == StatusAtivo.EM_USO) {
            throw ConflitoException(
                "O status 'Em uso' é definido pela alocação. Registre uma alocação no serviço de Alocações.",
            )
        }
        return copy(status = novo)
    }

    fun garantirQuePodeSerExcluido() {
        if (status == StatusAtivo.EM_USO) {
            throw ConflitoException(
                "O ativo $numeroPatrimonio está alocado e não pode ser excluído. Registre a devolução antes.",
            )
        }
    }

    fun garantirQueAceitaManutencao() {
        if (status == StatusAtivo.BAIXADO) {
            throw ConflitoException("O ativo $numeroPatrimonio foi baixado e não recebe novas manutenções.")
        }
    }

    companion object {
        /** Cadastro de um ativo novo, com todas as validações de entrada. */
        fun novo(
            id: Long,
            numeroPatrimonio: String?,
            tipo: String?,
            marca: String?,
            modelo: String?,
            numeroSerie: String?,
            status: StatusAtivo,
            setor: String?,
            dataAquisicao: LocalDate,
            valorAquisicao: BigDecimal?,
        ): Ativo {
            if (status == StatusAtivo.EM_USO) {
                throw ConflitoException(
                    "Um ativo novo não pode ser cadastrado 'Em uso'. Cadastre-o 'Em estoque' e registre a alocação no serviço de Alocações.",
                )
            }
            return Ativo(
                id = id,
                numeroPatrimonio = exigirTexto(numeroPatrimonio, "numeroPatrimonio"),
                tipo = exigirTexto(tipo, "tipo"),
                marca = exigirTexto(marca, "marca"),
                modelo = exigirTexto(modelo, "modelo"),
                numeroSerie = exigirTexto(numeroSerie, "numeroSerie"),
                status = status,
                colaborador = null,
                setor = textoOpcional(setor),
                dataAquisicao = dataAquisicao,
                valorAquisicao = exigirNaoNegativo(valorAquisicao, "valorAquisicao"),
            )
        }
    }
}
