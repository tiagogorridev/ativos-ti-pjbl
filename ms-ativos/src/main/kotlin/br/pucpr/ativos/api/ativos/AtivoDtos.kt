package br.pucpr.ativos.api.ativos

import br.pucpr.ativos.application.ativos.AtualizarAtivoComando
import br.pucpr.ativos.application.ativos.CriarAtivoComando
import br.pucpr.ativos.application.ativos.RegistrarUsoComando
import br.pucpr.ativos.domain.ativo.Ativo
import java.math.BigDecimal
import java.time.LocalDate

/** Resposta de um ativo: mesmo formato do tipo "Ativo" em frontend/lib/types.ts. */
data class AtivoResponse(
    val id: Long,
    val tipo: String,
    val marca: String,
    val modelo: String,
    val numeroPatrimonio: String,
    val numeroSerie: String,
    val status: String,
    val colaborador: String?,
    val setor: String?,
    val dataAquisicao: LocalDate,
    val valorAquisicao: BigDecimal,
    val equipamento: String,
)

/**
 * Corpo do POST /api/ativos. O campo "colaborador" é aceito por compatibilidade com o
 * formulário do frontend, mas é ignorado: quem define o colaborador é a alocação.
 */
data class NovoAtivoRequest(
    val numeroPatrimonio: String? = null,
    val tipo: String? = null,
    val marca: String? = null,
    val modelo: String? = null,
    val numeroSerie: String? = null,
    val status: String? = null,
    val colaborador: String? = null,
    val setor: String? = null,
    val dataAquisicao: LocalDate? = null,
    val valorAquisicao: BigDecimal? = null,
) {
    fun paraComando() = CriarAtivoComando(
        numeroPatrimonio = numeroPatrimonio,
        tipo = tipo,
        marca = marca,
        modelo = modelo,
        numeroSerie = numeroSerie,
        status = status,
        setor = setor,
        dataAquisicao = dataAquisicao,
        valorAquisicao = valorAquisicao,
    )
}

/** Corpo do PUT /api/ativos/{numeroPatrimonio}: atualização parcial (campo ausente não muda). */
data class AtualizarAtivoRequest(
    val tipo: String? = null,
    val marca: String? = null,
    val modelo: String? = null,
    val numeroSerie: String? = null,
    val status: String? = null,
    val colaborador: String? = null,
    val setor: String? = null,
    val dataAquisicao: LocalDate? = null,
    val valorAquisicao: BigDecimal? = null,
) {
    fun paraComando() = AtualizarAtivoComando(
        tipo = tipo,
        marca = marca,
        modelo = modelo,
        numeroSerie = numeroSerie,
        status = status,
        setor = setor,
        dataAquisicao = dataAquisicao,
        valorAquisicao = valorAquisicao,
    )
}

/** Corpo do PUT /api/ativos/{numeroPatrimonio}/uso, enviado pelo serviço de Alocações. */
data class UsoRequest(
    val colaborador: String? = null,
    val setor: String? = null,
) {
    fun paraComando() = RegistrarUsoComando(colaborador = colaborador, setor = setor)
}

data class MensagemResponse(val mensagem: String)

fun Ativo.paraResposta() = AtivoResponse(
    id = id,
    tipo = tipo,
    marca = marca,
    modelo = modelo,
    numeroPatrimonio = numeroPatrimonio,
    numeroSerie = numeroSerie,
    status = status.rotulo,
    colaborador = colaborador,
    setor = setor,
    dataAquisicao = dataAquisicao,
    valorAquisicao = valorAquisicao,
    equipamento = equipamento,
)
