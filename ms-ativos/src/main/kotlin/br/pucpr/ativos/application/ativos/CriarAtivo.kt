package br.pucpr.ativos.application.ativos

import br.pucpr.ativos.domain.ConflitoException
import br.pucpr.ativos.domain.ValidacaoException
import br.pucpr.ativos.domain.ativo.Ativo
import br.pucpr.ativos.domain.ativo.AtivoRepository
import br.pucpr.ativos.domain.ativo.StatusAtivo
import br.pucpr.ativos.domain.exigirTexto
import java.math.BigDecimal
import java.time.LocalDate

/** Dados de entrada do cadastro. Tudo é opcional aqui; as regras ficam no domínio. */
data class CriarAtivoComando(
    val numeroPatrimonio: String? = null,
    val tipo: String? = null,
    val marca: String? = null,
    val modelo: String? = null,
    val numeroSerie: String? = null,
    val status: String? = null,
    val setor: String? = null,
    val dataAquisicao: LocalDate? = null,
    val valorAquisicao: BigDecimal? = null,
)

/** Caso de uso: cadastrar um ativo novo (RF13). O número de patrimônio é único. */
class CriarAtivo(private val ativos: AtivoRepository) {

    fun executar(comando: CriarAtivoComando): Ativo {
        val patrimonio = exigirTexto(comando.numeroPatrimonio, "numeroPatrimonio")
        val status = StatusAtivo.converter(comando.status) ?: StatusAtivo.EM_ESTOQUE
        val dataAquisicao = comando.dataAquisicao
            ?: throw ValidacaoException("dataAquisicao", "Informe o campo 'dataAquisicao' no formato AAAA-MM-DD.")

        if (ativos.existePatrimonio(patrimonio)) {
            throw ConflitoException("Já existe um ativo com o patrimônio $patrimonio.")
        }

        // Valida tudo antes de reservar o identificador.
        val rascunho = Ativo.novo(
            id = 0,
            numeroPatrimonio = patrimonio,
            tipo = comando.tipo,
            marca = comando.marca,
            modelo = comando.modelo,
            numeroSerie = comando.numeroSerie,
            status = status,
            setor = comando.setor,
            dataAquisicao = dataAquisicao,
            valorAquisicao = comando.valorAquisicao,
        )
        val ativo = rascunho.copy(id = ativos.proximoId())
        ativos.inserir(ativo)
        return ativo
    }
}
