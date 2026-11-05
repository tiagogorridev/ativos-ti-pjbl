package br.pucpr.ativos.application.manutencoes

import br.pucpr.ativos.domain.ativo.AtivoRepository
import br.pucpr.ativos.domain.ativo.exigirPorPatrimonio
import br.pucpr.ativos.domain.exigirTexto
import br.pucpr.ativos.domain.manutencao.Manutencao
import br.pucpr.ativos.domain.manutencao.ManutencaoRepository
import br.pucpr.ativos.domain.manutencao.TipoManutencao
import java.math.BigDecimal
import java.time.LocalDate

data class RegistrarManutencaoComando(
    val numeroPatrimonio: String? = null,
    val data: LocalDate? = null,
    val tipo: String? = null,
    val descricao: String? = null,
    val custo: BigDecimal? = null,
)

/** Caso de uso: registrar uma manutenção e o custo dela em um ativo existente (RF27, RF28). */
class RegistrarManutencao(
    private val ativos: AtivoRepository,
    private val manutencoes: ManutencaoRepository,
) {

    fun executar(comando: RegistrarManutencaoComando): Manutencao {
        val ativo = ativos.exigirPorPatrimonio(exigirTexto(comando.numeroPatrimonio, "numeroPatrimonio"))
        val tipo = TipoManutencao.converter(comando.tipo)

        // Valida tudo antes de reservar o identificador.
        val rascunho = Manutencao.registrar(
            id = 0,
            ativo = ativo,
            data = comando.data,
            tipo = tipo,
            descricao = comando.descricao,
            custo = comando.custo,
        )
        val manutencao = rascunho.copy(id = manutencoes.proximoId())
        manutencoes.inserir(manutencao)
        return manutencao
    }
}
