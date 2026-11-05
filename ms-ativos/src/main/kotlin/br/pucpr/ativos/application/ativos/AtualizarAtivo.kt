package br.pucpr.ativos.application.ativos

import br.pucpr.ativos.domain.ativo.Ativo
import br.pucpr.ativos.domain.ativo.AtivoRepository
import br.pucpr.ativos.domain.ativo.StatusAtivo
import br.pucpr.ativos.domain.ativo.exigirPorPatrimonio
import br.pucpr.ativos.domain.exigirNaoNegativo
import br.pucpr.ativos.domain.exigirTexto
import br.pucpr.ativos.domain.textoOpcional
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Atualização parcial: campo ausente (null) mantém o valor atual.
 * O patrimônio não muda, e o colaborador só muda pela alocação.
 */
data class AtualizarAtivoComando(
    val tipo: String? = null,
    val marca: String? = null,
    val modelo: String? = null,
    val numeroSerie: String? = null,
    val status: String? = null,
    val setor: String? = null,
    val dataAquisicao: LocalDate? = null,
    val valorAquisicao: BigDecimal? = null,
)

/** Caso de uso: alterar o cadastro de um ativo, incluindo troca manual de status e baixa (RF14, RF16). */
class AtualizarAtivo(private val ativos: AtivoRepository) {

    fun executar(numeroPatrimonio: String, comando: AtualizarAtivoComando): Ativo {
        val atual = ativos.exigirPorPatrimonio(numeroPatrimonio)

        var novo: Ativo = atual.copy(
            tipo = comando.tipo?.let { exigirTexto(it, "tipo") } ?: atual.tipo,
            marca = comando.marca?.let { exigirTexto(it, "marca") } ?: atual.marca,
            modelo = comando.modelo?.let { exigirTexto(it, "modelo") } ?: atual.modelo,
            numeroSerie = comando.numeroSerie?.let { exigirTexto(it, "numeroSerie") } ?: atual.numeroSerie,
            setor = if (comando.setor != null) textoOpcional(comando.setor) else atual.setor,
            dataAquisicao = comando.dataAquisicao ?: atual.dataAquisicao,
            valorAquisicao = comando.valorAquisicao?.let { exigirNaoNegativo(it, "valorAquisicao") }
                ?: atual.valorAquisicao,
        )

        val novoStatus = StatusAtivo.converter(comando.status)
        if (novoStatus != null) {
            novo = novo.alterarStatus(novoStatus)
        }

        ativos.atualizar(novo)
        return novo
    }
}
