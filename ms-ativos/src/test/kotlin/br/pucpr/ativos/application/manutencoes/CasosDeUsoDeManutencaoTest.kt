package br.pucpr.ativos.application.manutencoes

import br.pucpr.ativos.apoio.AtivoRepositoryEmMemoria
import br.pucpr.ativos.apoio.ManutencaoRepositoryEmMemoria
import br.pucpr.ativos.apoio.umAtivo
import br.pucpr.ativos.apoio.umaManutencao
import br.pucpr.ativos.application.indicadores.ConsultarIndicadores
import br.pucpr.ativos.domain.ConflitoException
import br.pucpr.ativos.domain.RecursoNaoEncontradoException
import br.pucpr.ativos.domain.ValidacaoException
import br.pucpr.ativos.domain.ativo.StatusAtivo
import br.pucpr.ativos.domain.manutencao.TipoManutencao
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Casos de uso de Manutenções e Indicadores com repositórios em memória. */
class CasosDeUsoDeManutencaoTest {

    private val comando = RegistrarManutencaoComando(
        numeroPatrimonio = "PAT-0001",
        data = LocalDate.of(2026, 8, 22),
        tipo = "Corretiva",
        descricao = "Troca de bateria",
        custo = BigDecimal("255.50"),
    )

    @Test
    fun `registra manutencao guardando a descricao do equipamento`() {
        val manutencoes = ManutencaoRepositoryEmMemoria()

        val registrada = RegistrarManutencao(AtivoRepositoryEmMemoria(umAtivo()), manutencoes).executar(comando)

        assertEquals(1, registrada.id)
        assertEquals("Dell Latitude 5440", registrada.equipamento)
        assertEquals(TipoManutencao.CORRETIVA, registrada.tipo)
        assertTrue(manutencoes.existeParaAtivo("PAT-0001"))
    }

    @Test
    fun `manutencao exige ativo cadastrado`() {
        assertFailsWith<RecursoNaoEncontradoException> {
            RegistrarManutencao(AtivoRepositoryEmMemoria(), ManutencaoRepositoryEmMemoria()).executar(comando)
        }
    }

    @Test
    fun `ativo baixado nao recebe manutencao`() {
        val ativos = AtivoRepositoryEmMemoria(umAtivo(status = StatusAtivo.BAIXADO))

        assertFailsWith<ConflitoException> {
            RegistrarManutencao(ativos, ManutencaoRepositoryEmMemoria()).executar(comando)
        }
    }

    @Test
    fun `custo negativo e tipo desconhecido sao recusados`() {
        val registrar = RegistrarManutencao(AtivoRepositoryEmMemoria(umAtivo()), ManutencaoRepositoryEmMemoria())

        assertEquals(
            "custo",
            assertFailsWith<ValidacaoException> { registrar.executar(comando.copy(custo = BigDecimal("-5"))) }.campo,
        )
        assertEquals(
            "tipo",
            assertFailsWith<ValidacaoException> { registrar.executar(comando.copy(tipo = "Urgente")) }.campo,
        )
    }

    @Test
    fun `atualiza e exclui manutencao`() {
        val manutencoes = ManutencaoRepositoryEmMemoria(umaManutencao(id = 5))

        val alterada = AtualizarManutencao(manutencoes).executar(5, AtualizarManutencaoComando(custo = BigDecimal("99.90")))
        assertEquals(BigDecimal("99.90"), alterada.custo)
        assertEquals("Troca de bateria", alterada.descricao)

        ExcluirManutencao(manutencoes).executar(5)
        assertFailsWith<RecursoNaoEncontradoException> { ObterManutencao(manutencoes).executar(5) }
    }

    @Test
    fun `indicadores somam status, setores e custo por mes`() {
        val ativos = AtivoRepositoryEmMemoria(
            umAtivo(numeroPatrimonio = "PAT-0001", id = 1, status = StatusAtivo.EM_USO, setor = "Financeiro"),
            umAtivo(numeroPatrimonio = "PAT-0002", id = 2, status = StatusAtivo.EM_USO, setor = "Financeiro"),
            umAtivo(numeroPatrimonio = "PAT-0003", id = 3),
            umAtivo(numeroPatrimonio = "PAT-0004", id = 4, status = StatusAtivo.BAIXADO),
        )
        val manutencoes = ManutencaoRepositoryEmMemoria(
            umaManutencao(id = 1, data = LocalDate.of(2026, 7, 10), custo = "100.00"),
            umaManutencao(id = 2, data = LocalDate.of(2026, 8, 1), custo = "50.00"),
            umaManutencao(id = 3, data = LocalDate.of(2026, 8, 20), custo = "25.50"),
        )

        val indicadores = ConsultarIndicadores(ativos, manutencoes).executar()

        assertEquals(4, indicadores.totalAtivos)
        assertEquals(2, indicadores.emUso)
        assertEquals(1, indicadores.emEstoque)
        assertEquals(1, indicadores.baixados)
        assertEquals(0, BigDecimal("175.50").compareTo(indicadores.custoManutencaoAcumulado))
        assertEquals("Financeiro", indicadores.ativosPorSetor.first().setor)
        assertEquals(2, indicadores.ativosPorSetor.first().quantidade)
        assertEquals(listOf("Jul/26", "Ago/26"), indicadores.custoManutencaoPorMes.map { it.mes })
        assertEquals(0, BigDecimal("75.50").compareTo(indicadores.custoManutencaoPorMes.last().valor))
    }
}
