package br.pucpr.alocacoes.application.colaboradores

import br.pucpr.alocacoes.domain.ConflitoException
import br.pucpr.alocacoes.domain.alocacao.AlocacaoRepository
import br.pucpr.alocacoes.domain.colaborador.Colaborador
import br.pucpr.alocacoes.domain.colaborador.ColaboradorRepository
import br.pucpr.alocacoes.domain.colaborador.Situacao
import br.pucpr.alocacoes.domain.colaborador.exigirPorId

/** Atualização parcial: campo ausente (null) mantém o valor atual. */
data class AtualizarColaboradorComando(
    val nome: String? = null,
    val matricula: String? = null,
    val setor: String? = null,
    val situacao: String? = null,
)

/** Caso de uso: alterar o cadastro de um colaborador, inclusive reativar (RF10). */
class AtualizarColaborador(
    private val colaboradores: ColaboradorRepository,
    private val alocacoes: AlocacaoRepository,
) {

    fun executar(id: Long, comando: AtualizarColaboradorComando): Colaborador {
        val atual = colaboradores.exigirPorId(id)
        val novo = atual.alterar(comando.nome, comando.matricula, comando.setor, Situacao.converter(comando.situacao))

        if (colaboradores.idsComMatricula(novo.matricula).any { it != id }) {
            throw ConflitoException("Já existe outro colaborador com a matrícula ${novo.matricula}.")
        }
        if (novo.situacao == Situacao.INATIVO && atual.situacao == Situacao.ATIVO) {
            garantirSemAtivosAlocados(alocacoes, novo)
        }
        colaboradores.atualizar(novo)
        return novo
    }
}

/** RF11: colaborador com ativo alocado não pode ser inativado. */
internal fun garantirSemAtivosAlocados(alocacoes: AlocacaoRepository, colaborador: Colaborador) {
    val emUso = alocacoes.contarVigentesDoColaborador(colaborador.id)
    if (emUso > 0) {
        throw ConflitoException(
            "O colaborador ${colaborador.nome} está com $emUso ativo(s) alocado(s). Registre as devoluções antes de inativá-lo.",
        )
    }
}
