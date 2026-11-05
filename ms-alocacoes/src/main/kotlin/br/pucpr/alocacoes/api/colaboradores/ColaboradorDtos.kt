package br.pucpr.alocacoes.api.colaboradores

import br.pucpr.alocacoes.application.colaboradores.AtualizarColaboradorComando
import br.pucpr.alocacoes.application.colaboradores.CadastrarColaboradorComando
import br.pucpr.alocacoes.application.colaboradores.ColaboradorComAtivos
import br.pucpr.alocacoes.domain.colaborador.Colaborador

/** Resposta de um colaborador: mesmo formato do tipo "Colaborador" em frontend/lib/types.ts. */
data class ColaboradorResponse(
    val id: Long,
    val nome: String,
    val matricula: String,
    val setor: String,
    val situacao: String,
    val ativosAlocados: Int,
)

data class NovoColaboradorRequest(
    val nome: String? = null,
    val matricula: String? = null,
    val setor: String? = null,
) {
    fun paraComando() = CadastrarColaboradorComando(nome = nome, matricula = matricula, setor = setor)
}

data class AtualizarColaboradorRequest(
    val nome: String? = null,
    val matricula: String? = null,
    val setor: String? = null,
    val situacao: String? = null,
) {
    fun paraComando() = AtualizarColaboradorComando(nome = nome, matricula = matricula, setor = setor, situacao = situacao)
}

fun Colaborador.paraResposta(ativosAlocados: Int) = ColaboradorResponse(
    id = id,
    nome = nome,
    matricula = matricula,
    setor = setor,
    situacao = situacao.rotulo,
    ativosAlocados = ativosAlocados,
)

fun ColaboradorComAtivos.paraResposta() = colaborador.paraResposta(ativosAlocados)
